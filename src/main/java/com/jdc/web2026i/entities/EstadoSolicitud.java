package com.jdc.web2026i.entities;

/**
 * Estado de una solicitud durante su ciclo de vida.
 */
public enum EstadoSolicitud {
    NUEVO,
    ASIGNADO,
    EN_PROCESO,
    RESUELTO,
    CERRADO,
    REABIERTO
}
