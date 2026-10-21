package com.jdc.web2026i.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Registro de auditoria de un cambio realizado sobre una entidad del dominio.
 */
@Entity
@Table(name = "registro_auditoria")
public class RegistroAuditoriaEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_registro")
    private Long idRegistro;

    @NotBlank
    @Size(max = 30)
    @Column(name = "tipo_entidad", nullable = false, length = 30)
    private String tipoEntidad;

    @NotNull
    @Column(name = "entidad_id", nullable = false)
    private Long entidadId;

    @NotBlank
    @Size(max = 30)
    @Column(name = "campo", nullable = false, length = 30)
    private String campo;

    @Size(max = 300)
    @Column(name = "valor_anterior", length = 300)
    private String valorAnterior;

    @Size(max = 300)
    @Column(name = "valor_nuevo", length = 300)
    private String valorNuevo;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_actor", nullable = false)
    private UsuarioEntity actor;

    @NotNull
    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    @NotBlank
    @Size(max = 30)
    @Column(name = "accion", nullable = false, length = 30)
    private String accion;

    public Long getIdRegistro() {
        return idRegistro;
    }

    public void setIdRegistro(Long idRegistro) {
        this.idRegistro = idRegistro;
    }

    public String getTipoEntidad() {
        return tipoEntidad;
    }

    public void setTipoEntidad(String tipoEntidad) {
        this.tipoEntidad = tipoEntidad;
    }

    public Long getEntidadId() {
        return entidadId;
    }

    public void setEntidadId(Long entidadId) {
        this.entidadId = entidadId;
    }

    public String getCampo() {
        return campo;
    }

    public void setCampo(String campo) {
        this.campo = campo;
    }

    public String getValorAnterior() {
        return valorAnterior;
    }

    public void setValorAnterior(String valorAnterior) {
        this.valorAnterior = valorAnterior;
    }

    public String getValorNuevo() {
        return valorNuevo;
    }

    public void setValorNuevo(String valorNuevo) {
        this.valorNuevo = valorNuevo;
    }

    public UsuarioEntity getActor() {
        return actor;
    }

    public void setActor(UsuarioEntity actor) {
        this.actor = actor;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    /**
     * Fecha formateada para las vistas (mismo formato que el resto de fichas).
     */
    public String getFechaTexto() {
        if (fecha == null) {
            return "";
        }
        return fecha.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public String getAccion() {
        return accion;
    }

    public void setAccion(String accion) {
        this.accion = accion;
    }
}
