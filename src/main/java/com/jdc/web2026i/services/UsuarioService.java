package com.jdc.web2026i.services;

import com.jdc.web2026i.entities.UsuarioEntity;

/**
 * Operaciones de consulta sobre los usuarios del sistema.
 */
public interface UsuarioService {

    UsuarioEntity obtenerPorNombreUsuario(String nombreUsuario);
}
