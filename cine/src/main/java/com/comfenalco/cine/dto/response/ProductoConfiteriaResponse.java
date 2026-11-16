package com.comfenalco.cine.dto.response;

import java.math.BigDecimal;

import com.comfenalco.cine.model.ProductoConfiteria;
import com.comfenalco.cine.model.enums.CategoriaProducto;
import com.comfenalco.cine.model.enums.EstadoProducto;

//Se crea el DTO ProductoConfiteriaResponse, es lo que se devuelve para mostrar un producto de la confitería
//Tiene su id, nombre, descripción, precio, imagen, categoría y estado
public record ProductoConfiteriaResponse(
    Long id, String nombre, String descripcion, BigDecimal precio,
    String imagenUrl, CategoriaProducto categoria, EstadoProducto estado
) {

    //Se convierte un ProductoConfiteria en este DTO, así a la vista solo llegan los datos que necesita
    public static ProductoConfiteriaResponse de(ProductoConfiteria p) {
        //Aquí cada dato se copia tal cual del producto
        return new ProductoConfiteriaResponse(
                p.getId(), p.getNombre(), p.getDescripcion(), p.getPrecio(),
                p.getImagenUrl(), p.getCategoria(), p.getEstado());
    }
}