package com.jdc.web2026i.services;

import com.jdc.web2026i.DTO.SolicitudFormularioDTO;
import com.jdc.web2026i.entities.PrioridadSolicitud;
import com.jdc.web2026i.entities.SolicitudEntity;
import com.jdc.web2026i.entities.UsuarioEntity;

import java.util.List;

/**
 * Reglas de negocio del ciclo inicial de las solicitudes de soporte.
 */
public interface SolicitudService {

    SolicitudEntity crear(SolicitudFormularioDTO formulario, UsuarioEntity solicitante);

    List<SolicitudEntity> listarPropias(UsuarioEntity solicitante);

    SolicitudEntity obtenerParaUsuario(Long idSolicitud, UsuarioEntity usuario);

    List<SolicitudEntity> listarParaCoordinacion(String orden);

    void cambiarPrioridad(Long idSolicitud, PrioridadSolicitud nuevaPrioridad, UsuarioEntity actor);
}
