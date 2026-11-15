package com.comfenalco.cine.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.comfenalco.cine.dto.request.ProductoConfiteriaRequest;
import com.comfenalco.cine.dto.response.ProductoConfiteriaResponse;
import com.comfenalco.cine.model.enums.CategoriaProducto;

//Se crea el servicio de los productos de confitería, 
//aquí se definen las acciones sobre los productos: buscar, listar, crear, editar y cambiar su estado
public interface ProductoConfiteriaService {

    // Busca un producto por su id y devuelve sus datos
    ProductoConfiteriaResponse buscarPorId(Long id);

    // Lista los productos de una categoría (ej. bebidas, comidas)
    List<ProductoConfiteriaResponse> listarPorCategoria(CategoriaProducto categoria);

    // Crea un producto nuevo con los datos del request y devuelve el producto creado
    ProductoConfiteriaResponse crear(ProductoConfiteriaRequest request);

    // Cambia los datos de un producto que ya existe (id) con los del request y devuelve el producto actualizado
    ProductoConfiteriaResponse editar(Long id, ProductoConfiteriaRequest request);

    // Cambia el estado del producto (ej. de activo a inactivo), no cambia sus demás datos
    void cambiarEstado(Long id, com.comfenalco.cine.model.enums.EstadoProducto nuevoEstado);

    // Devuelve los productos activos indexados por ID en un solo mapa
    // Útil para cargar varios productos del carrito con una sola consulta (evita N+1)
    // N+1 es hacer una consulta por cada producto, aquí se hace una sola para todos
    // El mapa guarda: id del producto -> datos del producto
    Map<Long, ProductoConfiteriaResponse> buscarActivosPorIds(Collection<Long> ids);
}