package com.comfenalco.cine.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.comfenalco.cine.dto.resumen.TotalPorUsuario;
import com.comfenalco.cine.dto.response.VentaPorProductoResponse;
import com.comfenalco.cine.model.DetalleConfiteria;

//Se crea el repositorio de DetalleConfiteria, aquí solo van las consultas de reportes de ventas de confitería
public interface DetalleConfiteriaRepository extends Repository<DetalleConfiteria, Long> {

    /*Total de confitería vendida en un rango de fechas.
    El rango incluye "desde" y excluye "hasta", y solo se cuentan las facturas EMITIDA,
    las anuladas no suman. COALESCE devuelve 0 si no hay ventas, para que no llegue null*/
    @Query("""
        SELECT COALESCE(SUM(d.precioUnitario * d.cantidad), 0) FROM DetalleConfiteria d
        WHERE d.factura.fechaHora >= :desde AND d.factura.fechaHora < :hasta
            AND d.factura.estado = com.comfenalco.cine.model.enums.EstadoFactura.EMITIDA
        """)
    BigDecimal totalConfiteriaEnRango(@Param("desde") LocalDateTime desde,
                                    @Param("hasta") LocalDateTime hasta);
    
    /*Ranking de productos más vendidos en el rango: nombre, unidades y total vendido.
    Se agrupa por producto (id y nombre) y se ordena del más vendido al menos vendido, si hay empate, por nombre.
    Con Pageable se limita cuántos productos traer*/
    @Query("""
        SELECT new com.comfenalco.cine.dto.response.VentaPorProductoResponse(
                    p.nombre, SUM(d.cantidad), SUM(d.precioUnitario * d.cantidad))
        FROM DetalleConfiteria d
        JOIN d.factura f
        JOIN d.productoConfiteria p
        WHERE f.fechaHora >= :desde AND f.fechaHora < :hasta
            AND f.estado = com.comfenalco.cine.model.enums.EstadoFactura.EMITIDA
        GROUP BY p.id, p.nombre
        ORDER BY SUM(d.cantidad) DESC, p.nombre
        """)
    List<VentaPorProductoResponse> productosMasVendidos(@Param("desde") LocalDateTime desde,
                                                        @Param("hasta") LocalDateTime hasta,
                                                        Pageable pageable);


    // Total de confitería por día en el rango, una fila por cada día
    // Filas: [año, mes, día, total]. Un día sin ventas no aparece en el resultado
    @Query("""
        SELECT year(f.fechaHora), month(f.fechaHora), day(f.fechaHora),
                SUM(d.precioUnitario * d.cantidad)
        FROM DetalleConfiteria d
        JOIN d.factura f
        WHERE f.fechaHora >= :desde AND f.fechaHora < :hasta
            AND f.estado = com.comfenalco.cine.model.enums.EstadoFactura.EMITIDA
        GROUP BY year(f.fechaHora), month(f.fechaHora), day(f.fechaHora)
        """)
    List<Object[]> totalConfiteriaPorDia(@Param("desde") LocalDateTime desde,
                                        @Param("hasta") LocalDateTime hasta);

                                        
    // Total de confitería vendida por usuario (facturas emitidas, rango por fecha de venta)
    // Se agrupa por usuario, así se obtiene una fila por cada uno, y con SELECT new se crea el DTO TotalPorUsuario
    @Query("""
        SELECT new com.comfenalco.cine.dto.resumen.TotalPorUsuario(
                    f.usuario.id, SUM(d.precioUnitario * d.cantidad))
        FROM DetalleConfiteria d
        JOIN d.factura f
        WHERE f.fechaHora >= :desde AND f.fechaHora < :hasta
            AND f.estado = com.comfenalco.cine.model.enums.EstadoFactura.EMITIDA
        GROUP BY f.usuario.id
        """)
    List<TotalPorUsuario> totalConfiteriaPorUsuario(@Param("desde") LocalDateTime desde,
                                                    @Param("hasta") LocalDateTime hasta);
}