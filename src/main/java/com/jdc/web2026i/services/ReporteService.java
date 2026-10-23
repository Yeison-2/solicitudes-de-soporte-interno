package com.jdc.web2026i.services;

import com.jdc.web2026i.entities.CategoriaSolicitud;
import com.jdc.web2026i.entities.EstadoSolicitud;
import com.jdc.web2026i.entities.PrioridadSolicitud;
import com.jdc.web2026i.entities.UsuarioEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Reporte acotado de solicitudes y registro de su exportacion.
 */
public interface ReporteService {

    /**
     * CSV RFC 4180 con lista blanca de columnas: sin descripcion, comentarios,
     * justificaciones, motivos, credenciales ni hashes.
     */
    String generarCsv(EstadoSolicitud estado, PrioridadSolicitud prioridad,
                      CategoriaSolicitud categoria, LocalDate desde, LocalDate hasta);

    /**
     * Nombre del archivo con la marca de fecha y hora de la descarga.
     */
    String nombreArchivo(LocalDateTime fecha);

    /**
     * Registra la exportacion en auditoria, con el filtro aplicado legible.
     */
    void registrarExportacion(UsuarioEntity actor, String filtros);
}
