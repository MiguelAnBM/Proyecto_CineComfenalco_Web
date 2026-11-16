package com.comfenalco.cine.utils;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import com.comfenalco.cine.exception.ReglaNegocioException;

//Clase para validar un rango de fechas
public final class ValidadorRango {

    // Máximos días de consulta a futuro
    public static final int MAX_DIAS_CONSULTA = 366;

    private ValidadorRango() {
    }

    public static void validar(LocalDate desde, LocalDate hasta) {
        
        // Validamos que los rangos no sean nulos ni en el pasado
        if (desde == null || hasta == null || hasta.isBefore(desde)) {
            throw new ReglaNegocioException("El rango de fechas no es válido");
        }

        // Validamos que no se exceda el intérvalo máximo de consulta
        if (ChronoUnit.DAYS.between(desde, hasta) >= MAX_DIAS_CONSULTA) {
            throw new ReglaNegocioException("El rango máximo es de " + MAX_DIAS_CONSULTA + " días");
        }
    }
}
