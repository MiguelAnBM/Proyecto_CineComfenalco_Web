package com.comfenalco.cine.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.comfenalco.cine.dto.response.UsuarioResponse;
import com.comfenalco.cine.service.UsuarioService;

// Esta clase proporciona atributos globales para todas las vistas HTML de Thymeleaf para
// cualquier página sabe quién es el usuario actual y si es Administrador
@ControllerAdvice
public class GlobalModelAttributes {

    private final UsuarioService usuarioService;

    public GlobalModelAttributes(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    // Devuelve el nombre que se mostrará en la barra lateral (Sidebar),
    // si es el administrador conectado, busca su nombre completo,
    // si no, muestra "Taquillero"
    @ModelAttribute("nombreUsuarioActual")
    public String obtenerNombreUsuarioActual() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();

        // Verificamos si hay una sesión iniciada real
        if (autenticacion != null && autenticacion.isAuthenticated() 
                && !"anonymousUser".equals(autenticacion.getName())) {
            try {
                UsuarioResponse administrador = usuarioService.buscarPorUsername(autenticacion.getName());
                return administrador.nombreCompleto();
            } catch (Exception e) {
                return autenticacion.getName();
            }
        }

        // Si es visitante de mostrador
        return "Taquillero";
    }

    // Indica si el usuario conectado tiene el rol de Administrador,
    // sirve para mostrar u ocultar opciones como "Cerrar sesión" en la vista
    @ModelAttribute("esAdmin")
    public boolean esAdministradorConectado() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();

        if (autenticacion != null && autenticacion.isAuthenticated()) {
            return autenticacion.getAuthorities().stream()
                    .anyMatch(autoridad -> "ROLE_ADMINISTRADOR".equals(autoridad.getAuthority()));
        }

        return false;
    }

    // Devuelve las primeras 2 letras del nombre del usuario en mayúsculas,
    // maneja nombre nulo, vacío o de 1 solo carácter sin lanzar excepción
    @ModelAttribute("inicialesUsuario")
    public String obtenerInicialesUsuario() {
        String nombre = obtenerNombreUsuarioActual();

        if (nombre == null || nombre.isBlank()) {
            return "??";
        }

        String recortado = nombre.trim();
        return recortado.substring(0, Math.min(2, recortado.length())).toUpperCase();
    }
}

