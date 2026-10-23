package com.jdc.web2026i.services;

import com.jdc.web2026i.DTO.SolicitudFormularioDTO;
import com.jdc.web2026i.entities.CategoriaSolicitud;
import com.jdc.web2026i.entities.EstadoSolicitud;
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

    /**
     * Busqueda con filtros combinables limitada por el rol del usuario:
     * solicitante solo sus solicitudes, agente solo las asignadas a el,
     * coordinador y auditor todas.
     */
    List<SolicitudEntity> buscar(String texto, EstadoSolicitud estado, PrioridadSolicitud prioridad,
                                 CategoriaSolicitud categoria, UsuarioEntity usuario);

    /**
     * Listado para indicadores y reporte: mismos filtros, sin restriccion de
     * rol. No es una vista de usuario, solo alimenta agregados y exportacion.
     */
    List<SolicitudEntity> listarParaReporte(EstadoSolicitud estado, PrioridadSolicitud prioridad,
                                            CategoriaSolicitud categoria, LocalDate desde, LocalDate hasta);

    void asignar(Long idSolicitud, Long idAgente, UsuarioEntity coordinador);

    void cambiarEstado(Long idSolicitud, String accion, UsuarioEntity actor);

    void confirmar(Long idSolicitud, UsuarioEntity solicitante);

    void reabrir(Long idSolicitud, String motivo, UsuarioEntity solicitante);

    void cambiarPrioridad(Long idSolicitud, PrioridadSolicitud nuevaPrioridad, String justificacion,
                          LocalDate fechaObjetivo, UsuarioEntity actor);
}
