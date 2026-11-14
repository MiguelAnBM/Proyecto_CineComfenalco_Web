package com.comfenalco.cine.model;

import org.hibernate.annotations.ColumnDefault;

import com.comfenalco.cine.model.enums.EstadoUsuario;
import com.comfenalco.cine.model.enums.RolUsuario;

import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Tabla "usuarios", con unique username (para el login del admin) y un check para 
// validar que el taquillero no tenga contraseña pero el administrador sí.
// El username está pensado como una funcionalidad futura en caso de que se añada
// login de taquilleros.
@Entity 
@Table(
    name = "usuarios",
    uniqueConstraints = @UniqueConstraint(name = "uq_usuario_username", columnNames = "username"),
    check = {
        @CheckConstraint(
            name = "chk_usuario_admin_password",
            constraint = 
                "(rol = 'ADMINISTRADOR' AND contrasena_hash IS NOT NULL) OR " +
                "(rol = 'TAQUILLERO' AND contrasena_hash IS NULL)"
        )
    }
)
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Long id;

    // Nombre completo obligatorio, máximo 100 caracteres
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100)
    @Column(name = "nombre_completo", nullable = false, length = 100)
    private String nombreCompleto;

    // Username obligatorio, máximo 50 caracteres
    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Size(max = 50)
    @Column(nullable = false, length = 50)
    private String username;

    // Rol obligatorio, tipo ENUM convertido
    @NotNull(message = "El rol es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private RolUsuario rol;

    // Contraseña, nullable para taquilleros
    @Size(max = 255)
    @Column(name = "contrasena_hash", length = 255)
    private String contrasenaHash;

    // Estado Activo/Inactivo, por defecto Activo, tipo ENUM convertido
    @ColumnDefault("'ACTIVO'")
    @NotNull(message = "El estado es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EstadoUsuario estado;

    protected Usuario(){

    }
    
    public Usuario(String nombreCompleto, String username, RolUsuario rol, String contrasenaHash) {
        this.nombreCompleto = nombreCompleto;
        this.username = username;
        this.rol = rol;
        this.contrasenaHash = contrasenaHash;
        this.estado = EstadoUsuario.ACTIVO; // Por defecto Activo
    }

    // Replicación de la regla del CHECK, @AssertTrue valida una condición lógica y devuelve un booleano,
    // si es false, falla y devuelve el mensaje de error
    @AssertTrue(message = "El administrador debe tener contraseña y el taquillero no")
    private boolean isContrasenaCoherenteConRol() {
        if (rol == RolUsuario.ADMINISTRADOR) return contrasenaHash != null && !contrasenaHash.isBlank();
        if (rol == RolUsuario.TAQUILLERO) return contrasenaHash == null;
        return true; // si rol es null, ya lo reporta @NotNull
    }

    //──────────────────────────────────── GETTERS Y SETTERS ────────────────────────────────────────────────

    public Long getId() {
        return id;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public RolUsuario getRol() {
        return rol;
    }

    public void setRol(RolUsuario rol) {
        this.rol = rol;
    }

    public String getContrasenaHash() {
        return contrasenaHash;
    }

    public void setContrasenaHash(String contrasenaHash) {
        this.contrasenaHash = contrasenaHash;
    }

    public EstadoUsuario getEstado() {
        return estado;
    }

    public void setEstado(EstadoUsuario estado) {
        this.estado = estado;
    }

    
}
