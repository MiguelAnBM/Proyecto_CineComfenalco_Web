package com.comfenalco.cine.dto.response;

//Se crea el DTO AsientoResponse, es lo que se devuelve para mostrar cada asiento en el mapa de la sala
//Tiene el código del asiento y si está ocupado (true) o libre (false) */
public record AsientoResponse(String codigo, boolean ocupado) {
}
