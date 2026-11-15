package com.comfenalco.cine.service;

import java.util.List;

import com.comfenalco.cine.dto.request.ItemConfiteriaRequest;
import com.comfenalco.cine.dto.response.CarritoResumenResponse;

/**
 * Se encarga del carrito de compras de la confitería.
 * Guarda los productos que cada cliente va agregando mientras dura su sesión,
 * y deja listos los datos para mostrarlos en pantalla y para cobrar.
 */
public interface CarritoService {

    //Suma una unidad de un producto al carrito.
    void agregar(Long idProducto);

    
     //Resta una unidad de un producto del carrito, y si se queda sin unidades, lo saca.
    void quitar(Long idProducto);

    
    //Saca todos los productos del carrito.
    void vaciar();

    //Calcula cuánto cuesta cada producto y el total del pedido.
    CarritoResumenResponse obtenerResumen();

    
    //Convierte el carrito en la lista de DTOs que necesita VentaService para crear la factura.
    List<ItemConfiteriaRequest> obtenerItemsParaVenta();
}