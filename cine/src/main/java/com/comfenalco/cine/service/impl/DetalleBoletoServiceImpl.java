package com.comfenalco.cine.service.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.comfenalco.cine.service.DetalleBoletoService;
import com.comfenalco.cine.dto.response.AsientoResponse;
import com.comfenalco.cine.dto.resumen.ConteoPorFuncion;
import com.comfenalco.cine.exception.NoEncontradoException;
import com.comfenalco.cine.exception.ReglaNegocioException;
import com.comfenalco.cine.model.Asientos;
import com.comfenalco.cine.repository.DetalleBoletoRepository;
import com.comfenalco.cine.repository.FuncionRepository;


//Se crea la clase que hace el trabajo de DetalleBoletoService:
// mostrar el mapa de asientos, validar que estén libres y contar boletos
//Todos sus métodos solo leen datos, por eso la clase es readOnly
@Service
@Transactional(readOnly = true)
public class DetalleBoletoServiceImpl implements DetalleBoletoService {

    // Se construye una sola vez de A1 a H10
    //Es la lista de todos los asientos que existen en la sala, sirve para saber si un código es válido
    private static final Set<String> ASIENTOS_VALIDOS = Set.copyOf(Asientos.todos());

    //Los repositorios son los que hablan con la BD
    private final DetalleBoletoRepository detalleBoletoRepository;
    private final FuncionRepository funcionRepository;

    //Aquí llegan los repositorios que se necesitan para trabajar
    public DetalleBoletoServiceImpl(DetalleBoletoRepository detalleBoletoRepository,
                                    FuncionRepository funcionRepository) {
        this.detalleBoletoRepository = detalleBoletoRepository;
        this.funcionRepository = funcionRepository;
    }

    //Devuelve los 80 asientos de la función, cada uno marcado como ocupado o libre
    @Override
    public List<AsientoResponse> mapaDeAsientos(Long idFuncion) {

        //Primero se verifica que la función exista, y si no, sale un error
        if (!funcionRepository.existsById(idFuncion)) {
            throw new NoEncontradoException("La función no existe");
        }

        // Set para que contains() no recorra la lista 80 veces
        Set<String> ocupados = new HashSet<>(detalleBoletoRepository.findAsientosOcupados(idFuncion));

        //Se recorren los 80 asientos: si el código está entre los ocupados sale como ocupado, y si no, como libre
        return Asientos.todos().stream()
                .map(codigo -> new AsientoResponse(codigo, ocupados.contains(codigo)))
                .toList();
    }

    //Se verifica que los asientos elegidos se puedan comprar. Si alguno falla sale un error, y si todo está bien simplemente sigue
    @Override
    public void validarDisponibles(Long idFuncion, Collection<String> codigosAsiento) {

        //Si no llega ningún asiento, sale un error
        if (codigosAsiento == null || codigosAsiento.isEmpty()) {
            throw new ReglaNegocioException("Selecciona al menos un asiento");
        }

        // Una sola consulta para toda la selección
        Set<String> ocupados = new HashSet<>(detalleBoletoRepository.findAsientosOcupados(idFuncion));
        //En vistos se van guardando los asientos que ya se revisaron, así se detectan los repetidos
        Set<String> vistos = new HashSet<>();

        //Se revisa asiento por asiento, y antes de revisarlo se normaliza el código (a1 pasa a A1)
        for (String original : codigosAsiento) {
            String codigo = normalizar(original);

            //Tiene que ser un asiento que exista (de A1 a H10)
            if (codigo == null || !ASIENTOS_VALIDOS.contains(codigo)) {
                throw new ReglaNegocioException("La selección incluye un asiento que no existe");
            }

            //Si el asiento ya estaba en vistos, lo eligieron dos veces (add dice que no cuando ya estaba)
            if (!vistos.add(codigo)) {
                throw new ReglaNegocioException("El asiento " + codigo + " está repetido en la selección");
            }

            //Si está entre los ocupados, ya se vendió
            if (ocupados.contains(codigo)) {
                throw new ReglaNegocioException("El asiento " + codigo + " ya fue vendido");
            }
        }
    }

    //Dice si la función tiene al menos un boleto vigente, sirve para bloquear cambios en la función
    @Override
    public boolean tieneBoletosVendidos(Long idFuncion) {
        return detalleBoletoRepository.existsByFuncionIdAndAsientoActivoIsNotNull(idFuncion);
    }

    //Cuenta los boletos vigentes (no anulados) de la función
    @Override
    public long contarBoletos(Long idFuncion) {
        return detalleBoletoRepository.countByFuncionIdAndAsientoActivoIsNotNull(idFuncion);
    }

    //Cuenta los boletos vigentes de varias funciones a la vez
    @Override
    public Map<Long, Long> contarBoletosPorFuncion(Collection<Long> idsFuncion) {

        //Si no llegan ids, se devuelve un mapa vacío sin consultar la BD
        if (idsFuncion == null || idsFuncion.isEmpty()) {
            return Map.of();
        }

        Map<Long, Long> conteos = new HashMap<>();

        //Se arma el mapa: id de la función -> cantidad de boletos (las funciones sin boletos no aparecen)
        for (ConteoPorFuncion fila : detalleBoletoRepository.contarBoletosPorFuncion(idsFuncion)) {
            conteos.put(fila.idFuncion(), fila.boletos());
        }

        return conteos;
    }

    // MÉTODOS PRIVADOS

    // Convierte a1 -> A1
    //También le quita los espacios de los lados, y si el código es null devuelve null
    private String normalizar(String codigo) {
        return (codigo == null) ? null : codigo.trim().toUpperCase(Locale.ROOT);
    }
}