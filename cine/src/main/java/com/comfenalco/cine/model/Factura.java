package com.comfenalco.cine.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.annotations.ColumnDefault;

import com.comfenalco.cine.model.enums.EstadoFactura;
import com.comfenalco.cine.model.enums.MetodoPago;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


//Se crea la entidad facturas porque se va a almacenar información de esta
@Entity 
@Table(
    name = "facturas",
    indexes = {
        //columnList usa los nombres de columna 
        @Index(name = "idx_factura_fecha", columnList = "fecha_hora"),
        @Index(name = "idx_factura_usuario_fecha", columnList = "id_usuario, fecha_hora")
    },
    check = @CheckConstraint(
        name = "chk_factura_anulacion",
        constraint = 
            "(estado = 'EMITIDA' AND fecha_anulacion IS NULL) OR " +
            "(estado = 'ANULADA' AND fecha_anulacion IS NOT NULL)")
)
public class Factura {
    //El Id se usa para definir la llave primaria, en este caso la PK recibe el nombre de "id_factura"
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_factura")
    private Long id;

    // Control optimista: evita anular dos veces la misma factura en paralelo
    @Version
    @Column(nullable = false)
    private Long version;

    /*Se crea una FK para referenciar a la clase/tabla usuarios
     ON DELETE RESTRICT no deja borrar un usuario que tenga facturas, 
     y ON UPDATE CASCADE actualiza el id aquí si cambia en usuarios*/
    @NotNull(message = "El usuario no puede ser nulo")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "id_usuario",
        nullable = false,
        foreignKey = @ForeignKey(
            name = "fk_factura_usuario",
            foreignKeyDefinition = 
                "FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario) " +
                "ON DELETE RESTRICT ON UPDATE CASCADE"
        )
    )
    //Se referencia la clase usuario, para hacer la relación entre entidades
    private Usuario usuario;
    
    //CURRENT_TIMESTAMP(6) hace que se genere la marca de tiempo con 6 dígitos de precisión 
    //Con updatable = false no se puede modificar después de crearse
    @ColumnDefault("CURRENT_TIMESTAMP(6)")
    @Column(name = "fecha_hora", nullable = false, updatable = false)
    private LocalDateTime fechaHora;
    
    @NotNull(message = "El método de pago es obligatorio")
    //Se convierte el tipo enum en un string, se guarda el nombre y no su posición
    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pago", nullable = false, length = 10)
    private MetodoPago metodoPago;
 
    //El nombre y el correo del cliente son opcionales
    @Size(max = 100)
    @Column(name = "nombre_cliente", length = 100)
    private String nombreCliente;

    //Verifica que correoCliente tenga formato de correo electrónico 
    @Email(message = "El correo no tiene un formato válido")   
    @Size(max = 150)
    @Column(name = "correo_cliente", length = 150)
    private String correoCliente;

    /*Este es el lado de la relación de uno a muchos, con mappedBy se indica que este lado NO es el dueño,
    el dueño es el atributo "factura" de DetalleBoleto, que es donde está la FK.

    Con cascade lo que le suceda a la entidad padre le va a pasar a sus entidades hijas,
     es decir que, las operaciones de la factura se van a propagar a DetalleBoleto y DetalleConfiteria.
    PERSIST: Para guardar una factura, MERGE: Al actualizar una factura*/
    @OneToMany(mappedBy = "factura", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<DetalleBoleto> boletos = new ArrayList<>();

    
    //Igual que los boletos, pero para los productos de confitería
    @OneToMany(mappedBy = "factura", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<DetalleConfiteria> productos = new ArrayList<>();

    // Se valida que el estado no sea nulo, por defecto estará emitida 
    // (el constructor le asigna EMITIDA y @ColumnDefault lo deja como valor por defecto en la BD)
    @NotNull
    @ColumnDefault("'EMITIDA'")
    //Se convierte el tipo enum en un string
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 10)
    private EstadoFactura estado;

    @Column(name = "fecha_anulacion")
    private LocalDateTime fechaAnulacion;
    
    protected Factura() {
        
    }


    public Factura(Usuario usuario, MetodoPago metodoPago, String nombreCliente,
            String correoCliente) {
        this.usuario = usuario;
        this.metodoPago = metodoPago;
        this.nombreCliente = nombreCliente;
        this.correoCliente = correoCliente;
        this.estado = EstadoFactura.EMITIDA;
    }
    //Se establece automáticamente la fecha y hora en una columna JPA antes de guardar la entidad
    @PrePersist
    private void asignarFechaHora() {
        if (fechaHora == null) {
            fechaHora = LocalDateTime.now();
        }
    }

    // Mantiene sincronizados los dos lados de la relación
    //  Agrega el boleto a la lista y le asigna esta factura (así factura_id no queda en NULL)
    public void agregarBoleto(DetalleBoleto boleto) {
        if (boleto == null) {
            throw new IllegalArgumentException("El boleto no puede ser nulo");
        }
        //Lo mete en la lista de la factura
        boletos.add(boleto);
        //Le asigna esta factura al boleto
        boleto.setFactura(this);
    }
    //Lo anterior sucede nuevamente aquí
    public void agregarProducto(DetalleConfiteria detalle) {
        if (detalle == null) {
            throw new IllegalArgumentException("El producto no puede ser nulo");
        }

        productos.add(detalle);
        detalle.setFactura(this);
    }
    //Este getter devuelve la lista de productos
    public List<DetalleConfiteria> getProductos() {
        // Al usar Collections.unmodifiableList se devuelve una vista de solo lectura de la lista
        //Así no se puede agregar por fuera sin pasar por agregarProducto
        return Collections.unmodifiableList(productos);
    }

    //Se calcula el total de la factura (boletos + productos)
    public BigDecimal calcularTotal() {
        //Se usa BigDecimal porque es dinero y no pierde precisión como el double
        BigDecimal total = BigDecimal.ZERO;
        
         //Los dos bucles solo van sumando sobre el mismo total
        //Se recorre cada boleto de la factura, se toma su precio unitario y si no es null, lo suma
        for (DetalleBoleto boleto : boletos) {
            BigDecimal precio = boleto.getPrecioUnitario();
            if (precio != null) {
                total = total.add(precio);
            }
        } 
        //Recorre cada producto de confitería, y se hacen los calculos con calcularSubtotal
        for (DetalleConfiteria detalle : productos) {
            total = total.add(detalle.calcularSubtotal());
        }
       
        //Se devuelve la suma final, total = suma de precios de boletos + suma de subtotales de productos
        return total;
    }

    // Vista de sólo lectura de boletos para no alterar sincronización
    public List<DetalleBoleto> getBoletos() {
        return Collections.unmodifiableList(boletos);
    }

    //Cambia el estado a ANULADA, guarda la fecha de anulación y libera los asientos
    public void anular() {
        if (estado == EstadoFactura.ANULADA) {
            throw new IllegalStateException("La factura ya está anulada");
        }
        this.estado = EstadoFactura.ANULADA;
        this.fechaAnulacion = LocalDateTime.now();
        boletos.forEach(DetalleBoleto::liberarAsiento);
    }

    public EstadoFactura getEstado() { 
        return estado; 
    }
    public LocalDateTime getFechaAnulacion() { 
        return fechaAnulacion; 
    } 

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public MetodoPago getMetodoPago() {
        return metodoPago;
    }

    public String getNombreCliente() {
        return nombreCliente;
    }

    public void setNombreCliente(String nombreCliente) {
        this.nombreCliente = nombreCliente;
    }

    public String getCorreoCliente() {
        return correoCliente;
    }

    public void setCorreoCliente(String correoCliente) {
        this.correoCliente = correoCliente;
    }

    
}
