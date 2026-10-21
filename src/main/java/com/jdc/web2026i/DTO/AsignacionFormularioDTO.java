package com.jdc.web2026i.DTO;

import jakarta.validation.constraints.NotNull;

/**
 * Datos que el coordinador envia al asignar una solicitud a un agente.
 */
public class AsignacionFormularioDTO {

    @NotNull(message = "El agente es obligatorio.")
    private Long agenteId;

    public Long getAgenteId() {
        return agenteId;
    }

    public void setAgenteId(Long agenteId) {
        this.agenteId = agenteId;
    }
}
