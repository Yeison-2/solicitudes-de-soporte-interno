package com.jdc.web2026i.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos que el agente asignado envia al registrar un comentario de trabajo.
 */
public class ComentarioFormularioDTO {

    @NotBlank(message = "El comentario es obligatorio.")
    @Size(max = 1000, message = "El comentario no puede superar los 1000 caracteres.")
    private String texto;

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }
}
