package com.jdc.web2026i.services;

import com.jdc.web2026i.DTO.SolicitudFormularioDTO;
import com.jdc.web2026i.entities.PrioridadSolicitud;
import com.jdc.web2026i.entities.RegistroAuditoriaEntity;
import com.jdc.web2026i.entities.SolicitudEntity;
import com.jdc.web2026i.entities.UsuarioEntity;

import java.time.LocalDate;
import java.util.List;

/**
 * Reglas de negocio del ciclo de vida de las solicitudes de soporte.
 */
public interface SolicitudService {

    SolicitudEntity crear(SolicitudFormularioDTO formulario, UsuarioEntity solicitante);

    List<SolicitudEntity> listarPropias(UsuarioEntity solicitante);

    List<SolicitudEntity> listarAsignadas(UsuarioEntity agente);

    SolicitudEntity obtenerParaUsuario(Long idSolicitud, UsuarioEntity usuario);

    List<RegistroAuditoriaEntity> historial(Long idSolicitud, UsuarioEntity usuario);

    List<SolicitudEntity> listarParaCoordinacion(String orden);

    void asignar(Long idSolicitud, Long idAgente, UsuarioEntity coordinador);

    void cambiarEstado(Long idSolicitud, String accion, UsuarioEntity actor);

    void confirmar(Long idSolicitud, UsuarioEntity solicitante);

    void reabrir(Long idSolicitud, String motivo, UsuarioEntity solicitante);

    void cambiarPrioridad(Long idSolicitud, PrioridadSolicitud nuevaPrioridad, String justificacion,
                          LocalDate fechaObjetivo, UsuarioEntity actor);
}
