package com.jdc.web2026i.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Solicitud de soporte creada por un solicitante.
 */
@Entity
@Table(name = "solicitudes", indexes = {
        @Index(name = "idx_solicitudes_estado", columnList = "estado"),
        @Index(name = "idx_solicitudes_prioridad", columnList = "prioridad"),
        @Index(name = "idx_solicitudes_categoria", columnList = "categoria")
})
public class SolicitudEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_solicitud")
    private Long idSolicitud;

    @NotBlank
    @Size(max = 100)
    @Column(name = "titulo", nullable = false, length = 100)
    private String titulo;

    @NotBlank
    @Size(max = 1000)
    @Column(name = "descripcion", nullable = false, length = 1000)
    private String descripcion;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "categoria", nullable = false, length = 30)
    private CategoriaSolicitud categoria;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "prioridad", nullable = false, length = 20)
    private PrioridadSolicitud prioridad;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoSolicitud estado;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_solicitante", nullable = false)
    private UsuarioEntity solicitante;

    @NotNull
    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @NotNull
    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_agente_asignado")
    private UsuarioEntity agenteAsignado;

    @Column(name = "fecha_asignacion")
    private LocalDateTime fechaAsignacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_asignado_por")
    private UsuarioEntity asignadoPor;

    @Size(max = 300)
    @Column(name = "justificacion_prioridad", length = 300)
    private String justificacionPrioridad;

    @Column(name = "fecha_objetivo")
    private LocalDate fechaObjetivo;

    @Size(max = 300)
    @Column(name = "motivo_reapertura", length = 300)
    private String motivoReapertura;

    public Long getIdSolicitud() {
        return idSolicitud;
    }

    public void setIdSolicitud(Long idSolicitud) {
        this.idSolicitud = idSolicitud;
    }

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

    public EstadoSolicitud getEstado() {
        return estado;
    }

    public void setEstado(EstadoSolicitud estado) {
        this.estado = estado;
    }

    public UsuarioEntity getSolicitante() {
        return solicitante;
    }

    public void setSolicitante(UsuarioEntity solicitante) {
        this.solicitante = solicitante;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(LocalDateTime fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }

    public String getFechaCreacionTexto() {
        return formatear(fechaCreacion);
    }

    public String getFechaActualizacionTexto() {
        return formatear(fechaActualizacion);
    }

    public UsuarioEntity getAgenteAsignado() {
        return agenteAsignado;
    }

    public void setAgenteAsignado(UsuarioEntity agenteAsignado) {
        this.agenteAsignado = agenteAsignado;
    }

    public LocalDateTime getFechaAsignacion() {
        return fechaAsignacion;
    }

    public String getFechaAsignacionTexto() {
        return formatear(fechaAsignacion);
    }

    public void setFechaAsignacion(LocalDateTime fechaAsignacion) {
        this.fechaAsignacion = fechaAsignacion;
    }

    public UsuarioEntity getAsignadoPor() {
        return asignadoPor;
    }

    public void setAsignadoPor(UsuarioEntity asignadoPor) {
        this.asignadoPor = asignadoPor;
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

    public String getMotivoReapertura() {
        return motivoReapertura;
    }

    public void setMotivoReapertura(String motivoReapertura) {
        this.motivoReapertura = motivoReapertura;
    }

    public String getFechaObjetivoTexto() {
        if (fechaObjetivo == null) {
            return "";
        }
        return fechaObjetivo.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    private String formatear(LocalDateTime fecha) {
        if (fecha == null) {
            return "";
        }
        return fecha.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }
}
