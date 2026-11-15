package com.comfenalco.cine.service;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.comfenalco.cine.dto.response.FacturaAdminResponse;
import com.comfenalco.cine.dto.response.FacturaResponse;
import com.comfenalco.cine.dto.response.FacturaResumenResponse;
import com.comfenalco.cine.dto.request.VentaRequest;
import com.comfenalco.cine.model.enums.EstadoFactura;

//Se crea el servicio de ventas, aquí se definen las acciones sobre las facturas:
// vender, buscar, listar, anular y generar el comprobante
public interface VentaService {

    // Crea la factura con los boletos y productos que vienen en el request y devuelve sus datos
    // idUsuario es el taquillero
    FacturaResponse vender(Long idUsuario, VentaRequest request);

    // Busca una factura por su id y devuelve sus datos
    FacturaResponse buscarPorId(Long id);

    // Se listan las facturas emitidas por un taquillero paginadas para que el administrador pueda anularlas
    // Page es una página de resultados, y con Pageable se indica qué página pedir y cuántas facturas traer
    Page<FacturaResumenResponse> listarEmitidasPorTaquillero(Long idTaquillero, Pageable pageable);

    // Todas las facturas con filtros opcionales para el panel de administración
    // Se puede filtrar por taquillero, estado y rango de fechas
    Page<FacturaAdminResponse> listarFacturasFiltradas(Long idTaquillero, EstadoFactura estado,
                                                       LocalDateTime desde, LocalDateTime hasta,
                                                       Pageable pageable);

    // Anula una factura, esta queda como ANULADA y sus asientos quedan libres
    // idUsuario es el usuario que hace la anulación e idFactura es la factura a anular
    FacturaResponse anular(Long idUsuario, Long idFactura);

    /** Genera el texto plano con el comprobante de la factura para descarga o impresión. */
    String generarTextoFactura(FacturaResponse factura);
}