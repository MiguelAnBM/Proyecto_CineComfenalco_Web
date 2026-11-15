package com.comfenalco.cine.model;

import java.util.ArrayList;
import java.util.List;

// No es una entidad
public final class Asientos {

    public static final int FILAS = 8; // A-H
    public static final int COLUMNAS = 10; // 1-10

    private Asientos() {

    }
    public static List<String> todos() {
        
    // Se arma el código del asiento
        List<String> codigos = new ArrayList<>();
        for (int f = 0; f < FILAS; f++) {
            for (int c = 1; c <= COLUMNAS; c++) {
            //las letras son números por dentro ('A' = 65), así que al sumar nos lleva a la letra de la fila
            //(char) convierte el número otra vez en letra.
            //El "" (texto vacío) es un String, cuando es así se concatena en vez de sumar
                codigos.add("" + (char) ('A' + f) + c);
            }
        }
        return codigos;
    }
}
