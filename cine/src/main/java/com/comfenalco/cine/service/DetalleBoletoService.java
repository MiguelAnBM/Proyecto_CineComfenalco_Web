package com.comfenalco.cine.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.comfenalco.cine.dto.response.AsientoResponse;

//Se crea el servicio de los boletos, aquí se definen las acciones sobre los asientos y boletos de una función
public interface DetalleBoletoService {

    // Los 80 asientos de la función, en orden de A1 a H10, marcados como ocupados o libres
    // Sirve para mostrar el mapa de la sala al elegir asientos
    List<AsientoResponse> mapaDeAsientos(Long idFuncion);

    // Se usa antes de vender, no devuelve nada: si todo está bien, simplemente sigue
    void validarDisponibles(Long idFuncion, Collection<String> codigosAsiento);

    // Para bloquear cambios de sala, fecha u hora en una función con boletos vendidos
    // Devuelve true si la función ya tiene al menos un boleto vigente
    boolean tieneBoletosVendidos(Long idFuncion);

    // Boletos vigentes (no anulados) de una función
    // Devuelve la cantidad de boletos
    long contarBoletos(Long idFuncion);

    // Boletos vigentes de varias funciones en una sola consulta. Las que no tienen boletos no aparecen en el mapa
    Map<Long, Long> contarBoletosPorFuncion(Collection<Long> idsFuncion);
}