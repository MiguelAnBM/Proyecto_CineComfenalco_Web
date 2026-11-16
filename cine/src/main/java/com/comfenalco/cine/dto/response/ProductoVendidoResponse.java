package com.comfenalco.cine.dto.response;

import java.math.BigDecimal;

import com.comfenalco.cine.model.DetalleConfiteria;

//Se crea el DTO ProductoVendidoResponse, es lo que se devuelve para mostrar cada producto de confitería que se vendió en una factura
//Tiene el nombre, la cantidad, el precio de una unidad y el subtotal 
public record ProductoVendidoResponse(
    String nombre, Integer cantidad, BigDecimal precioUnitario, BigDecimal subtotal
) implements java.io.Serializable {

    //Convierte un DetalleConfiteria en este DTO, así a la vista solo llegan los datos que necesita
    public static ProductoVendidoResponse de(DetalleConfiteria d) {
        //El nombre se saca del producto, y la cantidad y el precio vienen de la línea de la factura
        return new ProductoVendidoResponse(
                d.getProductoConfiteria().getNombre(), d.getCantidad(),
                d.getPrecioUnitario(), d.calcularSubtotal());
    }
}