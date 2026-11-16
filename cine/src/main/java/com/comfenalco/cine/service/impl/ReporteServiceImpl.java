package com.comfenalco.cine.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.comfenalco.cine.service.ReporteService;
import com.comfenalco.cine.utils.ValidadorRango;
import com.comfenalco.cine.dto.response.FuncionTaquilleraResponse;
import com.comfenalco.cine.dto.response.OcupacionSalaResponse;
import com.comfenalco.cine.dto.response.ResumenVentasResponse;
import com.comfenalco.cine.dto.resumen.TotalPorUsuario;
import com.comfenalco.cine.dto.response.VentaDiariaResponse;
import com.comfenalco.cine.dto.response.VentaPorPeliculaResponse;
import com.comfenalco.cine.dto.response.VentaPorProductoResponse;
import com.comfenalco.cine.dto.response.VentaPorTaquilleroResponse;
import com.comfenalco.cine.exception.ReglaNegocioException;
import com.comfenalco.cine.model.enums.EstadoFactura;
import com.comfenalco.cine.repository.DetalleBoletoRepository;
import com.comfenalco.cine.repository.DetalleConfiteriaRepository;
import com.comfenalco.cine.repository.FacturaRepository;
import com.comfenalco.cine.repository.FuncionRepository;
import com.comfenalco.cine.repository.SalaRepository;

import java.time.Clock;

@Service
@Transactional(readOnly = true)
public class ReporteServiceImpl implements ReporteService {

    // Tope fijo para el parámetro "limite" de los reportes,
    // evita devolver miles de filas (protege la query y la vista)
    private static final int MAX_LIMITE = 50;

    private final FacturaRepository facturaRepository;
    private final DetalleBoletoRepository detalleBoletoRepository;
    private final DetalleConfiteriaRepository detalleConfiteriaRepository;
    private final FuncionRepository funcionRepository;
    private final SalaRepository salaRepository;
    private final Clock reloj;

    public ReporteServiceImpl(FacturaRepository facturaRepository,
                              DetalleBoletoRepository detalleBoletoRepository,
                              DetalleConfiteriaRepository detalleConfiteriaRepository,
                              FuncionRepository funcionRepository,
                              SalaRepository salaRepository,
                              Clock reloj) {
        this.facturaRepository = facturaRepository;
        this.detalleBoletoRepository = detalleBoletoRepository;
        this.detalleConfiteriaRepository = detalleConfiteriaRepository;
        this.funcionRepository = funcionRepository;
        this.salaRepository = salaRepository;
        this.reloj = reloj;
    }

    @Override
    public ResumenVentasResponse resumen(LocalDate desde, LocalDate hasta) {

        ValidadorRango.validar(desde, hasta);

        // Convertimos las fechas a rango horario [inicio, fin):
        // inicio es medianoche del "desde", fin es medianoche del día siguiente al "hasta",
        // así "hasta" queda incluido completo en la consulta.
        LocalDateTime inicio = desde.atStartOfDay();
        LocalDateTime fin = hasta.plusDays(1).atStartOfDay();

        // Sumas de dinero del periodo: boletos por un lado, confitería por otro
        BigDecimal totalBoletos = detalleBoletoRepository.totalBoletosEnRango(inicio, fin);
        BigDecimal totalConfiteria = detalleConfiteriaRepository.totalConfiteriaEnRango(inicio, fin);

        // Armamos el resumen con: totales de dinero, facturas emitidas,
        // cantidad de boletos vendidos y funciones que ya terminaron
        return new ResumenVentasResponse(
                desde, hasta,
                totalBoletos, totalConfiteria, totalBoletos.add(totalConfiteria),
                facturaRepository.countByEstadoAndFechaHoraGreaterThanEqualAndFechaHoraLessThan(
                        EstadoFactura.EMITIDA, inicio, fin),
                detalleBoletoRepository.contarBoletosEnRango(inicio, fin),
                contarFuncionesRealizadas(desde, hasta));
    }

    @Override
    public List<VentaPorPeliculaResponse> boletosPorPelicula(LocalDate desde, LocalDate hasta, int limite) {

        ValidadorRango.validar(desde, hasta);

        validarLimite(limite);

        // La base de datos ordena y corta el resultado al top "limite" (página 0, sin offset)
        return detalleBoletoRepository.boletosPorPelicula(
                desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay(), PageRequest.of(0, limite));
    }

    @Override
    public List<VentaPorProductoResponse> productosMasVendidos(LocalDate desde, LocalDate hasta, int limite) {

        ValidadorRango.validar(desde, hasta);

        validarLimite(limite);

        return detalleConfiteriaRepository.productosMasVendidos(
                desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay(), PageRequest.of(0, limite));
    }

    @Override
    public List<VentaDiariaResponse> ventasPorDia(LocalDate desde, LocalDate hasta) {

        ValidadorRango.validar(desde, hasta);

        LocalDateTime inicio = desde.atStartOfDay();

        LocalDateTime fin = hasta.plusDays(1).atStartOfDay();

        // Traemos boletos y confitería agrupados por día, y los convertimos en mapas fecha -> total
        Map<LocalDate, BigDecimal> boletos = aMapa(detalleBoletoRepository.totalBoletosPorDia(inicio, fin));
        Map<LocalDate, BigDecimal> confiteria = aMapa(detalleConfiteriaRepository.totalConfiteriaPorDia(inicio, fin));

        // Días sin ventas no vienen en la consulta: los rellenamos con cero más abajo
        List<VentaDiariaResponse> serie = new ArrayList<>();

        // Recorremos todos los días del rango en orden, completando los días sin datos con 0
        for (LocalDate dia = desde; !dia.isAfter(hasta); dia = dia.plusDays(1)) {
            BigDecimal b = boletos.getOrDefault(dia, BigDecimal.ZERO);
            BigDecimal c = confiteria.getOrDefault(dia, BigDecimal.ZERO);
            serie.add(new VentaDiariaResponse(dia, b, c, b.add(c)));
        }

        return serie;
    }

    @Override
    public List<OcupacionSalaResponse> ocupacionPorSala(LocalDate desde, LocalDate hasta) {

        ValidadorRango.validar(desde, hasta);

        // Ocupación de las funciones del rango, no de las ventas hechas en el rango
        return salaRepository.ocupacionPorSala(desde, hasta);
    }

    @Override
    public List<VentaPorTaquilleroResponse> ventasPorTaquillero(LocalDate desde, LocalDate hasta) {

        ValidadorRango.validar(desde, hasta);

        LocalDateTime inicio = desde.atStartOfDay();

        LocalDateTime fin = hasta.plusDays(1).atStartOfDay();

        // Totales de boletos y confitería por usuario, como mapa idUsuario -> total
        Map<Long, BigDecimal> boletos = aMapaPorUsuario(detalleBoletoRepository.totalBoletosPorUsuario(inicio, fin));
        Map<Long, BigDecimal> confiteria = aMapaPorUsuario(detalleConfiteriaRepository.totalConfiteriaPorUsuario(inicio, fin));

        // Partimos de las facturas por taquillero (una fila por usuario) y le sumamos sus ventas en boletos y confitería
        return facturaRepository.facturasPorUsuario(inicio, fin).stream()
                .map(conteo -> {
                    // Si el taquillero no vendió boletos o confitería, queda en 0
                    BigDecimal b = boletos.getOrDefault(conteo.idUsuario(), BigDecimal.ZERO);
                    BigDecimal c = confiteria.getOrDefault(conteo.idUsuario(), BigDecimal.ZERO);
                    return new VentaPorTaquilleroResponse(conteo.nombre(), conteo.facturas(), b, c, b.add(c));
                })
                // Ordenamos del que más vendió al que menos
                .sorted(Comparator.comparing(VentaPorTaquilleroResponse::total, Comparator.reverseOrder()))
                .toList();
    }

    @Override
    public List<FuncionTaquilleraResponse> funcionesMasTaquilleras(LocalDate desde, LocalDate hasta, int limite) {

        ValidadorRango.validar(desde, hasta);
        
        validarLimite(limite);

        // La base de datos devuelve el top "limite" de funciones más taquilleras (página 0, sin offset)
        return detalleBoletoRepository.funcionesMasTaquilleras(
                desde.atStartOfDay(),
                hasta.plusDays(1).atStartOfDay(),
                hasta,
                PageRequest.of(0, limite));
    }

    //──────────────────────────────────── MÉTODOS PRIVADOS ────────────────────────────────────────────────

    private Map<Long, BigDecimal> aMapaPorUsuario(List<TotalPorUsuario> filas) {
        // Convertimos las filas (idUsuario, total) en un mapa para buscar rápido por usuario
        Map<Long, BigDecimal> mapa = new HashMap<>();

        for (TotalPorUsuario fila : filas) {
            mapa.put(fila.idUsuario(), fila.total());
        }

        return mapa;
    }

    // Una función cuenta como realizada si terminó según su duración.
    // Se calcula en memoria porque la duración está guardada como texto ("2H 30M"), no como minutos.
    private long contarFuncionesRealizadas(LocalDate desde, LocalDate hasta) {

        // Referencia de tiempo actual (inyectada para poder controlarla en pruebas)
        LocalDateTime ahora = LocalDateTime.now(reloj);

        // Traemos las funciones del rango y contamos solo las que ya terminaron:
        // hora de inicio + duración debe quedar antes de "ahora"
        return funcionRepository.findEnRango(desde, hasta).stream()
                .filter(funcion -> !LocalDateTime.of(funcion.getFecha(), funcion.getHoraInicio())
                        .plusMinutes(funcion.getPelicula().duracionEnMinutos())
                        .isAfter(ahora))
                .count();
    }

    // Validamos que el límite pedido esté entre 1 y MAX_LIMITE
    private void validarLimite(int limite) {
        // Si el límite está fuera de 1..MAX_LIMITE, cortamos con un error de regla de negocio
        if (limite < 1 || limite > MAX_LIMITE) {
            throw new ReglaNegocioException("El límite debe estar entre 1 y " + MAX_LIMITE);
        }
    }

    // Cada fila es [año, mes, día, total]
    private Map<LocalDate, BigDecimal> aMapa(List<Object[]> filas) {
        // Convertimos las filas crudas de la base de datos (año, mes, día, total) en un mapa fecha -> total
        Map<LocalDate, BigDecimal> mapa = new HashMap<>();

        for (Object[] fila : filas) {
            // El año/mes/día vienen como números desde la consulta agrupada
            LocalDate dia = LocalDate.of(
                    ((Number) fila[0]).intValue(),
                    ((Number) fila[1]).intValue(),
                    ((Number) fila[2]).intValue());
            mapa.put(dia, (BigDecimal) fila[3]);
        }

        return mapa;
    }
}
