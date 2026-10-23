package com.jdc.web2026i.services;

import com.jdc.web2026i.DTO.IndicadoresDTO;
import com.jdc.web2026i.entities.CategoriaSolicitud;
import com.jdc.web2026i.entities.EstadoSolicitud;
import com.jdc.web2026i.entities.PrioridadSolicitud;

import java.time.LocalDate;

/**
 * Indicadores agregados del servicio de soporte.
 */
public interface IndicadorService {

    /**
     * Calcula el volumen por estado (con los estados en cero incluidos) y la
     * mediana de horas de ciclo de las solicitudes cerradas del conjunto.
     */
    IndicadoresDTO calcular(EstadoSolicitud estado, PrioridadSolicitud prioridad,
                            CategoriaSolicitud categoria, LocalDate desde, LocalDate hasta);
}
