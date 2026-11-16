package com.comfenalco.cine.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import com.comfenalco.cine.model.DetalleBoleto;
import com.comfenalco.cine.model.Funcion;

//Se crea el DTO BoletoResponse, es lo que se devuelve para mostrar cada boleto con película, sala, fecha, hora, asiento y precio
public record BoletoResponse(
    String pelicula, String sala, LocalDate fecha, LocalTime horaInicio,
    String asiento, BigDecimal precio
) implements java.io.Serializable {

    //Convierte un DetalleBoleto (la entidad de la BD) en este DTO, así a la vista solo llegan los datos que necesita
    public static BoletoResponse de(DetalleBoleto b) {
        //La película, la sala, la fecha y la hora no están en el boleto, se sacan de su función
        Funcion f = b.getFuncion();
        return new BoletoResponse(
                f.getPelicula().getTitulo(), f.getSala().getNombre(),
                f.getFecha(), f.getHoraInicio(),
                //El asiento y el precio sí vienen del boleto
                b.getCodigoAsiento(), b.getPrecioUnitario());
    }
}