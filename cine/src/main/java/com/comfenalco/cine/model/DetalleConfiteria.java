package com.comfenalco.cine.model;

import java.math.BigDecimal;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

//Se crea la entidad detalles_confitería ya que se estará almacenando datos de este objeto en la BD
@Entity
/*
Se crea una tabla cuyo nombre es detalles_confiteria, en esta 
se utiliza el uniqueconstraints como un contrato en el que
se pide que la pareja "id_factura" e "id_producto" no se repita*/ 
@Table( 
    name = "detalles_confiteria",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_conf_factura_producto", 
        columnNames = {"id_factura", "id_producto"}
    ), 
/*Se usa la anotación CheckConstraint para validar que,
 cantidad sea mayor a 0 y precio unitario sea igual o mayor a 0*/ 
    check = {
        @CheckConstraint(
            name = "chk_conf_cantidad", 
            constraint = "cantidad > 0"
        ),
        @CheckConstraint(
            name = "chk_conf_precio",
            constraint = "precio_unitario >= 0"
        )
    }
)
public class DetalleConfiteria {
    //El id es la llave primaria de nuestra tabla, en la BD se muestra con el nombre "id_detalle_confitería"
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle_confiteria")
    private Long id;

    //Se valida que la factura no sea nula
    @NotNull(message = "La factura no puede ser nula")


    /*Se crea una FK para referenciar a la clase/tabla factura. ON DELETE RESTRICT no deja
    borrar una factura que tenga detalles, y ON UPDATE CASCADE actualiza el id aquí si
    cambia en la factura. Con LAZY la factura se carga solo cuando se necesita */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "id_factura",
        nullable = false,
        foreignKey = @ForeignKey(
            name = "fk_detconf_factura",
            foreignKeyDefinition = 
                "FOREIGN KEY (id_factura) REFERENCES facturas(id_factura) " +
                "ON DELETE RESTRICT ON UPDATE CASCADE"
        )
    )
    //Se referencia la clase factura, para hacer la relación entre entidades
    private Factura factura;

    
    @NotNull(message = "El producto no puede ser nulo")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    //Se crea una FK para referenciar a la clase/tabla productos_confiteria
    @JoinColumn(
        name = "id_producto",
        nullable = false,
        foreignKey = @ForeignKey(
            name = "fk_detconf_producto",
            foreignKeyDefinition = 
                "FOREIGN KEY (id_producto) REFERENCES productos_confiteria(id_producto) " +
                "ON DELETE RESTRICT ON UPDATE CASCADE"
        )
    )
    
    //Se crea una FK para referenciar a la clase/tabla productos_confiteria,
    //  con las mismas reglas de borrado y actualización que la factura
    private ProductoConfiteria productoConfiteria;

    
    /*Se verifica que la cantidad no sea nula, y que el minimo sea 1*/
    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser al menos {value}")
    @Column(nullable = false)
    private Integer cantidad;
 
    
    /*Se verifica que la precio unitario no sea nulo, y que el minimo sea 0*/
    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.00", message = "El precio no puede ser negativo")
    
    //Con la anotación Digits se limita la cantidad de digitos que puede tener el precio unitario
    @Digits(integer = 8, fraction = 2, message = "El precio admite máximo {integer} enteros y {fraction} decimales")

    //Con precision se indica el número total de dígitos que puede tener el valor, y con scale la cant despues del punto
    @Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario;

    protected DetalleConfiteria() {

    }
    //Se está validando en el constructor que el productoConfitería y la cantidad no sean nulos
    public DetalleConfiteria(ProductoConfiteria productoConfiteria, Integer cantidad) {
        if (productoConfiteria == null) {
            throw new IllegalArgumentException("El producto no puede ser nulo");
        }

        if (cantidad == null) {
            throw new IllegalArgumentException("La cantidad no puede ser nula");
        }

        this.productoConfiteria = productoConfiteria;
        this.cantidad = cantidad;
        //Se copia el precio al momento de la venta, si el precio del producto cambia después, las facturas viejas no se alteran
        this.precioUnitario = productoConfiteria.getPrecio();
    }

    // Subtotal de la línea (cantidad x precio unitario), esto se calcula
    public BigDecimal calcularSubtotal() {

        // Para evitar NullPointerException, si el precio o la cantidad es nula, se devolverá 0
        if (precioUnitario == null || cantidad == null) {
            return BigDecimal.ZERO;
        }

        //Este es el procedimiento para calcular el subtotal
        return precioUnitario.multiply(BigDecimal.valueOf(cantidad));
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

    public ProductoConfiteria getProductoConfiteria() {
        return productoConfiteria;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }
    
}


