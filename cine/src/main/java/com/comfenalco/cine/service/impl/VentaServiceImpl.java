package com.comfenalco.cine.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.comfenalco.cine.service.DetalleBoletoService;
import com.comfenalco.cine.service.FuncionVendibilidad;
import com.comfenalco.cine.service.UsuarioService;
import com.comfenalco.cine.service.VentaService;
import com.comfenalco.cine.dto.response.FacturaAdminResponse;
import com.comfenalco.cine.dto.response.FacturaResponse;
import com.comfenalco.cine.dto.response.FacturaResumenResponse;
import com.comfenalco.cine.dto.request.ItemConfiteriaRequest;
import com.comfenalco.cine.dto.request.VentaRequest;
import com.comfenalco.cine.exception.NoEncontradoException;
import com.comfenalco.cine.exception.ReglaNegocioException;
import com.comfenalco.cine.model.DetalleBoleto;
import com.comfenalco.cine.model.DetalleConfiteria;
import com.comfenalco.cine.model.Factura;
import com.comfenalco.cine.model.Funcion;
import com.comfenalco.cine.model.ProductoConfiteria;
import com.comfenalco.cine.model.Usuario;
import com.comfenalco.cine.model.enums.EstadoFactura;
import com.comfenalco.cine.model.enums.EstadoProducto;
import com.comfenalco.cine.model.enums.EstadoUsuario;
import com.comfenalco.cine.model.enums.RolUsuario;
import com.comfenalco.cine.repository.FacturaRepository;
import com.comfenalco.cine.repository.FuncionRepository;
import com.comfenalco.cine.repository.ProductoConfiteriaRepository;

import java.time.Clock;

//Esta clase es la que realmente hace lo que VentaService promete: vender, buscar, listar, anular y generar el comprobante
//Casi todos los métodos solo leen datos, por eso la clase es readOnly, y los que sí guardan cambios tienen su propio @Transactional
@Service
@Transactional(readOnly = true)
public class VentaServiceImpl implements VentaService {

    // Es el nombre de la regla de la BD que no deja vender el mismo asiento dos
    // veces, se usa más abajo para saber cuándo el error fue por eso
    private static final String RESTRICCION_ASIENTO = "uq_boleto_funcion_asiento";

    // Aquí están las clases que necesita para trabajar: los repositorios para
    // hablar con la BD y los servicios de usuarios y boletos
    private final FacturaRepository facturaRepository;
    private final FuncionRepository funcionRepository;
    private final ProductoConfiteriaRepository productoRepository;
    private final UsuarioService usuarioService;
    private final DetalleBoletoService detalleBoletoService;
    // El reloj es para saber la fecha y la hora actual
    private final Clock reloj;

    // Aquí se encuentran todas las clases que se necesitan para trabajar
    public VentaServiceImpl(FacturaRepository facturaRepository,
            FuncionRepository funcionRepository,
            ProductoConfiteriaRepository productoRepository,
            UsuarioService usuarioService,
            DetalleBoletoService detalleBoletoService,
            Clock reloj) {
        this.facturaRepository = facturaRepository;
        this.funcionRepository = funcionRepository;
        this.productoRepository = productoRepository;
        this.usuarioService = usuarioService;
        this.detalleBoletoService = detalleBoletoService;
        this.reloj = reloj;
    }

    // Aqui se revisa que la venta esté bien, se arma la factura y se guarda
    // Si algo sale mal en el camino no se guarda nada, gracias al @Transactional
    @Override
    @Transactional
    public FacturaResponse vender(Long idUsuario, VentaRequest request) {

        validarSolicitud(request);

        // Se mira si la venta trae boletos y si trae productos
        boolean hayBoletos = request.asientos() != null && !request.asientos().isEmpty();
        boolean hayProductos = request.productos() != null && !request.productos().isEmpty();

        // Una venta tiene que traer algo: boletos, productos o las dos cosas
        if (!hayBoletos && !hayProductos) {
            throw new ReglaNegocioException("La venta debe incluir al menos un boleto o un producto");
        }
        // Los asientos y la función van juntos, no puede venir uno sin el otro
        if (hayBoletos && request.idFuncion() == null) {
            throw new ReglaNegocioException("Indica la función de los boletos");
        }
        if (!hayBoletos && request.idFuncion() != null) {
            throw new ReglaNegocioException("Selecciona al menos un asiento");
        }

        // Se bloquea la función, las ventas de la misma función se hacen en fila
        Funcion funcion = hayBoletos ? bloquearFuncionVendible(request.idFuncion()) : null;

    
        // Quien vende tiene que estar activo y ser taquillero
        Usuario taquillero = usuarioService.obtenerEntidad(idUsuario);
        if (taquillero.getEstado() != EstadoUsuario.ACTIVO) {
            throw new ReglaNegocioException("El usuario no está activo");
        }
        if (taquillero.getRol() != RolUsuario.TAQUILLERO) {
            throw new ReglaNegocioException("Solo un taquillero activo puede realizar ventas");
        }

        // Asientos: existen, no se repiten y están libres
        if (hayBoletos) {
            detalleBoletoService.validarDisponibles(funcion.getId(), request.asientos());
        }

        // Se arma la factura. Los precios se encuentran en la BD
        Factura factura = new Factura(taquillero, request.metodoPago(),
                request.nombreCliente(), request.correoCliente());

        // Por cada asiento se agrega un boleto a la factura
        if (hayBoletos) {
            for (String codigo : request.asientos()) {
                factura.agregarBoleto(new DetalleBoleto(funcion, codigo));
            }
        }

        // Los productos repetidos se juntan primero (consolidar) y después cada uno se
        // agrega a la factura
        if (hayProductos) {
            for (Map.Entry<Long, Integer> linea : consolidar(request.productos()).entrySet()) {
                factura.agregarProducto(crearDetalleConfiteria(linea.getKey(), linea.getValue()));
            }
        }

        // 5. Se guarda la factura con todo su contenido, en una sola transacción
        try {
            factura = facturaRepository.save(factura);
        } catch (DataIntegrityViolationException ex) {
            // Si dos personas intentan comprar el mismo asiento al mismo tiempo, la BD deja
            // pasar a una y a la otra le da error, aquí se le avisa con un mensaje claro
            if (esAsientoDuplicado(ex)) {
                throw new ReglaNegocioException(
                        "Uno de los asientos acaba de ser vendido. Actualiza el mapa e intenta de nuevo");
            }
            // Si el error fue por otra cosa, se vuelve a lanzar tal cual
            throw ex;
        }

        return FacturaResponse.de(factura);
    }

    // Busca la factura por su id y devuelve todos sus datos, con boletos y
    // productos
    @Override
    public FacturaResponse buscarPorId(Long id) {

        // Se carga la factura con sus boletos, y si no existe sale un error
        Factura factura = facturaRepository.findDetalleById(id)
                .orElseThrow(() -> new NoEncontradoException("La factura no existe"));

        // Esta consulta solo termina de cargar sus productos
        factura = facturaRepository.findConProductosById(id)
                .orElseThrow(() -> new NoEncontradoException("La factura no existe"));

        return FacturaResponse.de(factura);
    }

    
    @Override
    public Page<FacturaResumenResponse> listarEmitidasPorTaquillero(Long idTaquillero, Pageable pageable) {

        // Si no llega el taquillero, se devuelve una página vacía
        if (idTaquillero == null) {
            return Page.empty();
        }

        // Se trae solo la página solicitada
        Page<Factura> paginaFacturas = facturaRepository.findEmitidasPorUsuarioPaginada(
                idTaquillero, pageable);

        // Si no hay facturas, también se devuelve una página vacía
        if (paginaFacturas.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        //Extrae los IDs de la página para cargar detalles en lote (sin N+1)
        List<Long> idsFacturas = paginaFacturas.getContent().stream()
                .map(Factura::getId)
                .toList();

        // Carga boletos y productos de esos IDs (2 consultas batch, no por factura)
        facturaRepository.findConBoletosByIdIn(idsFacturas);
        facturaRepository.findConProductosByIdIn(idsFacturas);

        //Mapea a DTO preservando el orden de la página original
        List<FacturaResumenResponse> resumen = new ArrayList<>();
        for (Factura f : paginaFacturas.getContent()) {
            resumen.add(FacturaResumenResponse.de(f));
        }

        // Se devuelve la página ya armada, junto con el total de facturas que existen,
        // para saber cuántas páginas hay
        return new PageImpl<>(resumen, pageable, paginaFacturas.getTotalElements());
    }

    // Todas las facturas con filtros opcionales. Mismo patrón que
    // listarEmitidasPorTaquillero:
    // pagina en BD, luego carga boletos y productos de la página en 2 queries
    // batch.
    @Override
    public Page<FacturaAdminResponse> listarFacturasFiltradas(Long idTaquillero, EstadoFactura estado,
            LocalDateTime desde, LocalDateTime hasta,
            Pageable pageable) {

        // Paginación en base de datos con filtros opcionales
        Page<Factura> paginaFacturas = facturaRepository.buscarFacturasFiltradas(
                idTaquillero, estado, desde, hasta, pageable);

        // Si ninguna factura coincide con los filtros, se devuelve una página vacía
        if (paginaFacturas.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        // IDs de la página para cargar detalles en lote 
        List<Long> idsFacturas = paginaFacturas.getContent().stream()
                .map(Factura::getId)
                .toList();

        // Hidrata boletos y productos en el contexto de persistencia
        facturaRepository.findConBoletosByIdIn(idsFacturas);
        facturaRepository.findConProductosByIdIn(idsFacturas);

        // Mapea a DTO preservando el orden de la página original
        List<FacturaAdminResponse> resumen = new ArrayList<>();
        for (Factura f : paginaFacturas.getContent()) {
            resumen.add(FacturaAdminResponse.de(f));
        }

        return new PageImpl<>(resumen, pageable, paginaFacturas.getTotalElements());
    }

    // Anula una factura. Solo lo puede hacer un administrador activo, y solo si
    // ninguna de las funciones ha empezado todavía
    @Override
    @Transactional
    public FacturaResponse anular(Long idUsuario, Long idFactura) {

        // Se verifica que el usuario cumpla con las condiciones para el permiso: no se guarda en la factura
        Usuario usuario = usuarioService.obtenerEntidad(idUsuario);
        if (usuario.getRol() != RolUsuario.ADMINISTRADOR || usuario.getEstado() != EstadoUsuario.ACTIVO) {
            throw new ReglaNegocioException("Solo un administrador activo puede anular facturas");
        }

        // Se busca la factura, y si no existe sale un error
        Factura factura = facturaRepository.findDetalleById(idFactura)
                .orElseThrow(() -> new NoEncontradoException("La factura no existe"));

        // Una factura que ya está anulada no se puede anular otra vez
        if (factura.getEstado() == EstadoFactura.ANULADA) {
            throw new ReglaNegocioException("La factura ya está anulada");
        }

        // Se mira cada boleto: si la función ya empezó, no se deja anular
        LocalDateTime ahora = LocalDateTime.now(reloj);
        for (DetalleBoleto boleto : factura.getBoletos()) {
            Funcion funcion = boleto.getFuncion();
            boolean comenzo = !LocalDateTime.of(funcion.getFecha(), funcion.getHoraInicio()).isAfter(ahora);

            if (comenzo) {
                throw new ReglaNegocioException(
                        "No se puede anular: una de las funciones de la factura ya comenzó");
            }
        }

        factura.anular(); // sin save(): Hibernate guarda al terminar la transacción

        factura = facturaRepository.findConProductosById(idFactura) // termina de cargar los productos para el
                                                                    // comprobante
                .orElseThrow(() -> new NoEncontradoException("La factura no existe"));

        return FacturaResponse.de(factura);
    }

    // MÉTODOS PRIVADOS
    // Estos solo se usan dentro de esta clase

    // Se verifica que la venta no llegue vacía (null)
    private void validarSolicitud(VentaRequest request) {

        if (request == null) {
            throw new ReglaNegocioException("La venta no es válida");
        }
    }

    // Se busca la función y se bloquea para que nadie más venda sobre ella mientras
    // tanto
    // Luego se verifica que todavía se puedan vender boletos, esto depende de la
    // hora de inicio y de lo que dura la película
    private Funcion bloquearFuncionVendible(Long idFuncion) {

        Funcion funcion = funcionRepository.findByIdForUpdate(idFuncion)
                .orElseThrow(() -> new NoEncontradoException("La función no existe"));

        LocalDateTime ahora = LocalDateTime.now(reloj);
        LocalDateTime inicio = LocalDateTime.of(funcion.getFecha(), funcion.getHoraInicio());

        if (!FuncionVendibilidad.esVendible(inicio, funcion.getPelicula().duracionEnMinutos(), ahora)) {
            throw new ReglaNegocioException("No se pueden vender boletos para esta función");
        }

        return funcion;
    }

    // Una sola línea por producto (UNIQUE id_factura + id_producto): se suman las
    // repetidas
    private Map<Long, Integer> consolidar(List<ItemConfiteriaRequest> items) {

        Map<Long, Integer> cantidades = new LinkedHashMap<>();

        // Se van sumando las cantidades de cada producto, y si alguno viene incompleto
        // sale un error
        for (ItemConfiteriaRequest item : items) {
            if (item == null || item.idProducto() == null || item.cantidad() == null) {
                throw new ReglaNegocioException("Uno de los productos de la venta no es válido");
            }
            cantidades.merge(item.idProducto(), item.cantidad(), Integer::sum);
        }

        // Se verifica que ninguna cantidad pase del máximo permitido
        cantidades.values().forEach(cantidad -> {
            if (cantidad > ItemConfiteriaRequest.CANTIDAD_MAXIMA) {
                throw new ReglaNegocioException(
                        "La cantidad máxima por producto es " + ItemConfiteriaRequest.CANTIDAD_MAXIMA);
            }
        });

        return cantidades;
    }

    // Se busca el producto, se verifica que exista y esté activo, y con eso se crea
    // la línea de la factura
    private DetalleConfiteria crearDetalleConfiteria(Long idProducto, Integer cantidad) {

        ProductoConfiteria producto = productoRepository.findById(idProducto)
                .orElseThrow(() -> new NoEncontradoException("Uno de los productos no existe"));

        if (producto.getEstado() != EstadoProducto.ACTIVO) {
            throw new ReglaNegocioException("El producto «" + producto.getNombre() + "» no está disponible");
        }

        return new DetalleConfiteria(producto, cantidad);
    }

    // Se mira si el error de la BD fue por un asiento repetido, buscando el nombre
    // de la regla en el mensaje
    private boolean esAsientoDuplicado(DataIntegrityViolationException ex) {
        String mensaje = ex.getMostSpecificCause().getMessage();
        // H2 escribe el nombre de la restricción en mayúsculas y MySQL en minúsculas
        return mensaje != null && mensaje.toLowerCase(Locale.ROOT).contains(RESTRICCION_ASIENTO);
    }

    // -------------------------------------------------------------------------
    // Generación del comprobante en texto plano
    // -------------------------------------------------------------------------

    // Aquí se arma el comprobante de la factura en texto, línea por línea
    @Override
    public String generarTextoFactura(FacturaResponse f) {
        StringBuilder sb = new StringBuilder();
        // Primero va el encabezado
        sb.append("==================================================\n");
        sb.append("                 CINE COMFENALCO                  \n");
        sb.append("           FACTURA DE VENTA ELECTRÓNICA           \n");
        sb.append("==================================================\n");
        // Luego los datos de la factura, si falta alguno se pone uno por defecto (N/A,
        // General o Cliente General)
        sb.append(String.format("Factura No.:       #FACT-%05d%n", f.id()));
        sb.append(String.format("Fecha y Hora:      %s%n",
                f.fechaHora() != null ? f.fechaHora().toString().replace('T', ' ') : "N/A"));
        sb.append(String.format("Taquillero:        %s%n",
                f.taquillero() != null ? f.taquillero() : "General"));
        sb.append(String.format("Cliente:           %s%n",
                (f.nombreCliente() != null && !f.nombreCliente().isBlank()) ? f.nombreCliente() : "Cliente General"));
        // El correo solo se muestra si el cliente lo dio
        if (f.correoCliente() != null && !f.correoCliente().isBlank()) {
            sb.append(String.format("Correo:            %s%n", f.correoCliente()));
        }
        sb.append(String.format("Método de Pago:    %s%n", f.metodoPago()));
        sb.append(String.format("Estado Factura:    %s%n", f.estado()));
        sb.append("--------------------------------------------------\n");

        // Los boletos solo salen si la factura tiene boletos
        if (f.boletos() != null && !f.boletos().isEmpty()) {
            sb.append("BOLETOS DE CINE:\n");
            for (var b : f.boletos()) {
                sb.append(String.format(" * %s | %s | Asiento: %s | $%s%n",
                        b.pelicula(), b.sala(), b.asiento(), b.precio()));
                sb.append(String.format("   Función: %s %s%n", b.fecha(), b.horaInicio()));
            }
            sb.append("--------------------------------------------------\n");
        }

        // Lo mismo pasa con la confitería, solo sale si hay productos
        if (f.productos() != null && !f.productos().isEmpty()) {
            sb.append("CONFITERÍA Y CAFETERÍA:\n");
            for (var p : f.productos()) {
                sb.append(String.format(" * %dx %s (Unit: $%s) = $%s%n",
                        p.cantidad(), p.nombre(), p.precioUnitario(), p.subtotal()));
            }
            sb.append("--------------------------------------------------\n");
        }

        // Al final va el total y el mensaje de despedida
        sb.append(String.format("TOTAL PAGADO:      $%s%n", f.total()));
        sb.append("==================================================\n");
        sb.append("      ¡Gracias por su visita a Cine Comfenalco!   \n");
        sb.append("==================================================\n");
        return sb.toString();
    }
}