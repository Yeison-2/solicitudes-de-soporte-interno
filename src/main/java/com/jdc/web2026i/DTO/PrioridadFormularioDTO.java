package com.jdc.web2026i.DTO;

import com.jdc.web2026i.entities.PrioridadSolicitud;
import jakarta.validation.constraints.NotNull;

/**
 * Datos que el coordinador envia al cambiar la prioridad de una solicitud.
 */
public class PrioridadFormularioDTO {

    @NotNull(message = "La prioridad es obligatoria.")
    private PrioridadSolicitud prioridad;

    public PrioridadSolicitud getPrioridad() {
        return prioridad;
    }

    public void setPrioridad(PrioridadSolicitud prioridad) {
        this.prioridad = prioridad;
    }
}
