package com.jdc.web2026i.DTO;

import com.jdc.web2026i.entities.CategoriaSolicitud;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos que el solicitante envia al crear una solicitud. El id, la fecha,
 * el estado, la prioridad y el propietario los asigna el sistema.
 */
public class SolicitudFormularioDTO {

    @NotBlank(message = "El titulo es obligatorio.")
    @Size(min = 1, max = 100, message = "El titulo debe tener entre 1 y 100 caracteres.")
    private String titulo;

    @NotBlank(message = "La descripcion es obligatoria.")
    @Size(min = 1, max = 1000, message = "La descripcion debe tener entre 1 y 1000 caracteres.")
    private String descripcion;

    @NotNull(message = "La categoria es obligatoria.")
    private CategoriaSolicitud categoria;

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public CategoriaSolicitud getCategoria() {
        return categoria;
    }

    public void setCategoria(CategoriaSolicitud categoria) {
        this.categoria = categoria;
    }
}
