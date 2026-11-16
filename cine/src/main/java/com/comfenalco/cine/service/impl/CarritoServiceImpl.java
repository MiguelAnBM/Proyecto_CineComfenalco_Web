package com.comfenalco.cine.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

import com.comfenalco.cine.dto.request.ItemConfiteriaRequest;
import com.comfenalco.cine.dto.response.CarritoResumenResponse;
import com.comfenalco.cine.dto.response.ProductoConfiteriaResponse;
import com.comfenalco.cine.dto.response.RenglonCarritoResponse;
import com.comfenalco.cine.exception.NoEncontradoException;
import com.comfenalco.cine.model.enums.EstadoProducto;
import com.comfenalco.cine.service.CarritoService;
import com.comfenalco.cine.service.ProductoConfiteriaService;

/**
 * Implementación del carrito de cafetería con alcance de sesión (@SessionScope).
 * Spring crea automáticamente una instancia independiente para cada usuario conectado,
 * aislando los pedidos de cada cliente y liberando al controlador de manejar sesiones HTTP.
 */
//Esta clase hace el trabajo de CarritoService, aquí está el código de cada acción
//Cada usuario conectado tiene su propio carrito (por eso lleva @Service y @SessionScope)
@Service
@SessionScope
public class CarritoServiceImpl implements CarritoService {

    // Guarda los IDs de producto y sus cantidades para la sesión actual
    // Los productos quedan en el orden en que se agregaron
    private final Map<Long, Integer> itemsDelCarrito = new LinkedHashMap<>();

    //Sirve para consultar los datos de los productos
    private final ProductoConfiteriaService productoConfiteriaService;

    //Recibe el servicio de productos para poder usarlo en esta clase
    public CarritoServiceImpl(ProductoConfiteriaService productoConfiteriaService) {
        this.productoConfiteriaService = productoConfiteriaService;
    }

    //Agrega una unidad del producto al carrito
    @Override
    public void agregar(Long idProducto) {
        //Si no llega el producto, no existe o está inactivo, no hace nada
        if (idProducto == null || buscarProductoActivo(idProducto) == null) {
            return;
        }
        //Si el producto ya estaba, le suma 1 a su cantidad, y si no estaba, lo agrega con cantidad 1
        itemsDelCarrito.merge(idProducto, 1, Integer::sum);
    }

    //Quita una unidad del producto, y si era la última, lo saca del carrito
    @Override
    public void quitar(Long idProducto) {
        //Si el producto no está en el carrito, no hace nada
        if (idProducto == null || !itemsDelCarrito.containsKey(idProducto)) {
            return;
        }
        int cantidadActual = itemsDelCarrito.get(idProducto);
        //Si hay más de una unidad, resta una. Si solo hay una, lo saca
        if (cantidadActual > 1) {
            itemsDelCarrito.put(idProducto, cantidadActual - 1);
        } else {
            itemsDelCarrito.remove(idProducto);
        }
    }

    //Saca todos los productos del carrito
    @Override
    public void vaciar() {
        itemsDelCarrito.clear();
    }

    //Arma el resumen del carrito: una línea por producto con su subtotal, y el total
    @Override
    public CarritoResumenResponse obtenerResumen() {

        // Snapshot de IDs para hacer una sola consulta batch en lugar de N consultas (una por producto)
        List<Long> idsSnapshot = new ArrayList<>(itemsDelCarrito.keySet());

        // Una sola consulta: productos activos cuyos IDs están en el carrito
        Map<Long, ProductoConfiteriaResponse> productosActivos =
                productoConfiteriaService.buscarActivosPorIds(idsSnapshot);

        //Aquí se van guardando las líneas del carrito, y el total empieza en 0
        List<RenglonCarritoResponse> lineas = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        //Se recorre el carrito producto por producto, así se puede sacar uno mientras se recorre
        Iterator<Map.Entry<Long, Integer>> iterator = itemsDelCarrito.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<Long, Integer> entrada = iterator.next();
            Long idProducto = entrada.getKey();

            // Si el producto ya no existe o está inactivo, se elimina del carrito
            ProductoConfiteriaResponse producto = productosActivos.get(idProducto);
            if (producto == null) {
                iterator.remove();
                continue;
            }

            //Subtotal = precio x cantidad
            BigDecimal subtotal = producto.precio().multiply(BigDecimal.valueOf(entrada.getValue()));
            lineas.add(new RenglonCarritoResponse(
                    producto.id(),
                    producto.nombre(),
                    producto.precio(),
                    entrada.getValue(),
                    subtotal
            ));
            total = total.add(subtotal);
        }

        return new CarritoResumenResponse(lineas, total);
    }

    //Pasa el carrito a la lista que necesita VentaService para hacer la factura (id y cantidad de cada producto)
    @Override
    public List<ItemConfiteriaRequest> obtenerItemsParaVenta() {

        // Snapshot de IDs para hacer una sola consulta batch
        List<Long> idsSnapshot = new ArrayList<>(itemsDelCarrito.keySet());

        // Una sola consulta: productos activos cuyos IDs están en el carrito
        Map<Long, ProductoConfiteriaResponse> productosActivos =
                productoConfiteriaService.buscarActivosPorIds(idsSnapshot);

        List<ItemConfiteriaRequest> itemsParaVenta = new ArrayList<>();
        Iterator<Map.Entry<Long, Integer>> iterator = itemsDelCarrito.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<Long, Integer> entrada = iterator.next();
            Long idProducto = entrada.getKey();

            // Si el producto ya no existe o está inactivo, se elimina del carrito
            if (!productosActivos.containsKey(idProducto)) {
                iterator.remove();
                continue;
            }

            //Los productos que siguen activos se agregan a la lista
            itemsParaVenta.add(new ItemConfiteriaRequest(entrada.getKey(), entrada.getValue()));
        }

        return itemsParaVenta;
    }

    // Busca un producto individual por ID (se mantiene para agregar al carrito uno a uno)
    //Devuelve el producto si existe y está activo, si no, devuelve null (nada)
    private ProductoConfiteriaResponse buscarProductoActivo(Long idProducto) {
        try {
            ProductoConfiteriaResponse producto = productoConfiteriaService.buscarPorId(idProducto);
            return producto != null && producto.estado() == EstadoProducto.ACTIVO ? producto : null;
        } catch (NoEncontradoException e) {
            //Si el producto no existe, devuelve null en vez de dar error
            return null;
        }
    }
}