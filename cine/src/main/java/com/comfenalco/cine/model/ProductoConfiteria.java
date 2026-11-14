package com.comfenalco.cine.model;

import java.math.BigDecimal;

import org.hibernate.annotations.ColumnDefault;

import com.comfenalco.cine.model.enums.CategoriaProducto;
import com.comfenalco.cine.model.enums.EstadoProducto;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/*
Se crea la entidad productos confitería porque se va a almacenar información de esta
Se verifica mediante un uniqueConstraints que el nombre de cada producto sea único */
@Entity
@Table(
    name = "productos_confiteria",
    uniqueConstraints = @UniqueConstraint(name = "uq_producto_nombre", columnNames = "nombre"),

//Haciendo uso del CheckConstraint se valida que el precio de un producto sea igual o mayor que 0
    check = {
        @CheckConstraint(
            name = "chk_producto_precio", 
            constraint = "precio >= 0"
        )
    }
)
public class ProductoConfiteria {
    //El Id se usa para definir la llave primaria, en este caso la PK recibe el nombre de "id_producto"
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_producto")
    private Long id;


    /*
    Se valida que se añada un nombre, 
    se usa NotBlank para validar en los tipos String que la propiedad no sea nula ni esté vacía */
    @NotBlank(message = "El nombre es obligatorio")

    //La longitud del nombre del producto no puede superar los 100 caracteres  
    @Size(max = 100)

    /*Se crea la columna nombre que no puede ser nula, y en la BD se define el tamaño de esta propiedad 
    con un máximo de 100 caracteres usando length*/
   @Column(nullable = false, length = 100)
    private String nombre;


/* 
Se define la columna con el tipo de dato TEXT de SQL porque una descripción puede ser una cadena de texto muy larga, 
definir una longitud fija sería limitarse*/ 
    @Column(columnDefinition = "TEXT")
    private String descripcion;

    /*Se verifica que el precio no sea nulo, que el decimal minimo sea mayor a 0*/
    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.00", message = "El valor no puede ser negativo")

    //Con la anotación Digits se limita la cantidad de digitos que puede tener el precio
    @Digits(integer = 8, fraction = 2, message = "El precio admite máximo 8 enteros y 2 decimales")

    //Con precision se indica el número total de dígitos que puede tener el valor, y con scale la cant despues del punto
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Size(max = 500)
    /*Se usa Pattern para validar que la cadena de texto cumpla con un formato o patrón definido y regexp define el patrón */
    @Pattern(
        regexp = "^https?://\\S+$", 
        message = "La URL debe empezar con http:// o https://")
    @Column(name = "imagen_url", length = 500)
    private String imagenUrl;

    @NotNull(message = "La categoría es obligatoria")
    //Se convierte el tipo enum en un string
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CategoriaProducto categoria;

    //La columna por defecto está activa
    @ColumnDefault("'ACTIVO'")
    @NotNull(message = "El estado del producto es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EstadoProducto estado;
    
    protected ProductoConfiteria(){
        
    }

    public ProductoConfiteria(String nombre, String descripcion, BigDecimal precio, String imagenUrl,
            CategoriaProducto categoria) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precio = precio;
        this.imagenUrl = imagenUrl;
        this.categoria = categoria;
        this.estado = EstadoProducto.ACTIVO;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }

    public String getImagenUrl() {
        return imagenUrl;
    }

    public void setImagenUrl(String imagenUrl) {
        this.imagenUrl = imagenUrl;
    }

    public CategoriaProducto getCategoria() {
        return categoria;
    }

    public void setCategoria(CategoriaProducto categoria) {
        this.categoria = categoria;
    }

    public EstadoProducto getEstado() {
        return estado;
    }

    public void setEstado(EstadoProducto estado) {
        this.estado = estado;
    }

}