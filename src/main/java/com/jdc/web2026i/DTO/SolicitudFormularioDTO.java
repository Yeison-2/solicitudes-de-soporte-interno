package com.jdc.web2026i.DTO;

import com.jdc.web2026i.entities.CategoriaSolicitud;
import com.jdc.web2026i.entities.PrioridadSolicitud;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Datos que el solicitante envia al crear una solicitud. El id, la fecha,
 * el estado y el propietario los asigna el sistema. La prioridad inicial es
 * opcional: si no se indica, o si no es BAJA/MEDIA/ALTA, se aplica MEDIA.
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

    private PrioridadSolicitud prioridad;

    @Size(max = 300, message = "La justificacion no puede superar los 300 caracteres.")
    private String justificacionPrioridad;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaObjetivo;

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

    public PrioridadSolicitud getPrioridad() {
        return prioridad;
    }

    public void setPrioridad(PrioridadSolicitud prioridad) {
        this.prioridad = prioridad;
    }

    public String getJustificacionPrioridad() {
        return justificacionPrioridad;
    }

    public void setJustificacionPrioridad(String justificacionPrioridad) {
        this.justificacionPrioridad = justificacionPrioridad;
    }

    public LocalDate getFechaObjetivo() {
        return fechaObjetivo;
    }

    public void setFechaObjetivo(LocalDate fechaObjetivo) {
        this.fechaObjetivo = fechaObjetivo;
    }
}
