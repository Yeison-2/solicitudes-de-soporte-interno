package com.jdc.web2026i.services;

import com.jdc.web2026i.entities.ComentarioEntity;
import com.jdc.web2026i.entities.UsuarioEntity;

import java.util.List;

/**
 * Registro y consulta de comentarios de trabajo sobre una solicitud.
 * No existe edicion ni borrado: un comentario creado es inmutable.
 */
public interface ComentarioService {

    ComentarioEntity registrar(Long idSolicitud, String texto, UsuarioEntity autor);

    /**
     * Comentarios de una solicitud, solo para el dueño, el coordinador o el
     * agente asignado.
     */
    List<ComentarioEntity> listar(Long idSolicitud, UsuarioEntity usuario);
}
