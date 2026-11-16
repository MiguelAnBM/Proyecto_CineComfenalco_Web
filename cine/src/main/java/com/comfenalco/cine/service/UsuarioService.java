package com.comfenalco.cine.service;

import java.util.List;

import com.comfenalco.cine.dto.request.UsuarioCrearRequest;
import com.comfenalco.cine.dto.request.UsuarioEditarRequest;
import com.comfenalco.cine.dto.response.UsuarioResponse;
import com.comfenalco.cine.model.Usuario;
import com.comfenalco.cine.model.enums.EstadoUsuario;

public interface UsuarioService {

    // Consultar todos los taquilleros
    List<UsuarioResponse> listarTaquilleros();

    // Consultar los taquilleros activos
    List<UsuarioResponse> listarTaquillerosActivos();

    // Buscar un usuario por id
    UsuarioResponse buscarPorId(Long id);

    //Buscar un usuario por username
    UsuarioResponse buscarPorUsername(String username);

    // Crear un Usuario
    UsuarioResponse crear(UsuarioCrearRequest request);

    // Editar un usuario
    UsuarioResponse editar(Long id, UsuarioEditarRequest request);

    // Cambiar el estado de un usuario
    void cambiarEstado(Long id, EstadoUsuario nuevoEstado);

    // Solo para otros servicios (por ejemplo, ventas). No devolver esta entidad desde un controlador
    Usuario obtenerEntidad(Long id);
    
}
