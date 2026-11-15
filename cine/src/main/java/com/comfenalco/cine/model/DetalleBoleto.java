package com.comfenalco.cine.model;

import java.math.BigDecimal;
import java.util.Locale;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

//Se crea la entidad detalles_boleto ya que se estará almacenando datos de este objeto en la BD
@Entity 
/*
Se crea una tabla cuyo nombre es detalles_boleto, en esta 
se utiliza el uniqueconstraints como un contrato en el que
se pide que la pareja "id_funcion" y "asiento_activo" no se repita*/
@Table(
    name = "detalles_boleto",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_boleto_funcion_asiento", 
        columnNames = {"id_funcion", "asiento_activo"}
    ),
/*Se usa la anotación CheckConstraint para validar que el precio unitario sea igual o mayor a 0,
que el código del asiento sea una letra de la A a la H seguida de un número del 1 al 10
(el 'c' del REGEXP_LIKE distingue mayúsculas de minúsculas), y que asiento_activo
sea null o igual a codigo_asiento*/
    check = {
        @CheckConstraint(
            name = "chk_boleto_precio", 
            constraint = "precio_unitario >= 0"
        ),
        @CheckConstraint(
            name = "chk_boleto_asiento",
            constraint = "REGEXP_LIKE(codigo_asiento, '^[A-H]([1-9]|10)$', 'c')"
        ),
        @CheckConstraint(
            name = "chk_boleto_asiento_activo",
            constraint = "asiento_activo IS NULL OR asiento_activo = codigo_asiento"
        )
    }
)
public class DetalleBoleto {

    //El id es la llave primaria de nuestra tabla, en la BD se muestra con el nombre "id_detalle_boleto"
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle_boleto")
    private Long id;

    /*Se valida que la factura no sea nula, esto se hace en tres capas:
    @NotNull en Java antes de guardar, optional = false y nullable = false
    lo exigen en JPA y en la BD, si falla la validación en Java*/
    @NotNull(message = "La factura no puede ser nula")

    /*Se crea una FK para referenciar a la clase/tabla factura. ON DELETE RESTRICT no deja
    borrar una factura que tenga boletos, y ON UPDATE CASCADE actualiza el id aquí si
    cambia en la factura. Con LAZY la factura se carga solo cuando se necesita*/
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "id_factura",
        nullable = false,
        foreignKey = @ForeignKey(
            name = "fk_detboleto_factura",
            foreignKeyDefinition = 
                "FOREIGN KEY (id_factura) REFERENCES facturas(id_factura) " +
                "ON DELETE RESTRICT ON UPDATE CASCADE"
        )
    )
    //Se referencia la clase factura, para hacer la relación entre entidades
    private Factura factura;

    @NotNull(message = "La función no puede ser nula")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    //Se crea una FK para referenciar a la clase/tabla funciones, con las mismas reglas de borrado y actualización que la factura
    @JoinColumn(
        name = "id_funcion",
        nullable = false,
        foreignKey = @ForeignKey(
            name = "fk_detboleto_funcion",
            foreignKeyDefinition = 
                "FOREIGN KEY (id_funcion) REFERENCES funciones(id_funcion) " +
                "ON DELETE RESTRICT ON UPDATE CASCADE"
        )
    )
    //Se referencia la clase Funcion, para hacer la relación entre entidades
    private Funcion funcion;

    /*Se verifica que el asiento no esté vacío y que cumpla el formato: una letra de la A a la H
    seguida de un número del 1 al 10, es la misma regla del CHECK de la tabla,
    una se valida en Java antes de guardar y la otra en la BD*/
    @NotBlank(message = "El asiento es obligatorio")
    @Pattern(
        regexp = "^[A-H]([1-9]|10)$",
        message = "El asiento debe ser una letra de la A a la H seguida de un número del 1 al 10 (ej: A1, H10)"
    )
    //length = 3 porque el código más largo es "H10"
    @Column(name = "codigo_asiento", nullable = false, length = 3)
    private String codigoAsiento;


    @Column(name = "asiento_activo", length = 3)
    private String asientoActivo;

    /*Se verifica que el precio unitario no sea nulo, y que el mínimo sea 0.
    Con Digits se limita la cantidad de dígitos (8 enteros y 2 decimales),
    con precision y scale se hace lo mismo en la BD*/
    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.00", message = "El valor no puede ser negativo")
    @Digits(integer = 8, fraction = 2, message = "El precio admite máximo {integer} enteros y {fraction} decimales")
    @Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario;
    
    protected DetalleBoleto() {

    }

    //Se está validando en el constructor que la función y su sala no sean nulas
    public DetalleBoleto(Funcion funcion, String codigoAsiento) {
        if (funcion == null || funcion.getSala() == null) {
            throw new IllegalArgumentException("La función y su sala son obligatorias");
        }

        this.funcion = funcion;
        //El código del asiento se limpia con trim y se pasa a mayúsculas 
        this.codigoAsiento = (codigoAsiento == null) ? null : codigoAsiento.trim().toUpperCase(Locale.ROOT);
        this.asientoActivo = this.codigoAsiento;
        // el precio se copia de la sala al momento de la venta
        this.precioUnitario = funcion.getSala().getPrecioBoleto();   
    }


    // Libera el asiento poniendo asientoActivo en null, el boleto queda guardado como historial
    void liberarAsiento() {
        this.asientoActivo = null;
    }

    public Long getId() {
        return id;
    }

    public Factura getFactura() {
        return factura;
    }

    // Solo Factura (mismo paquete) puede asignar la factura
    void setFactura(Factura factura) {
        this.factura = factura;
    }

    public Funcion getFuncion() {
        return funcion;
    }

    public String getCodigoAsiento() {
        return codigoAsiento;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }
    
}