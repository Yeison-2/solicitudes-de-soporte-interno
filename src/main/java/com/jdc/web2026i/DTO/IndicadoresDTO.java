package com.jdc.web2026i.DTO;

import com.jdc.web2026i.entities.EstadoSolicitud;

import java.util.Map;

/**
 * Resultado agregado de indicadores: volumen por estado y tiempo mediano de
 * ciclo. Nunca agrupa por persona.
 */
public class IndicadoresDTO {

    private Map<EstadoSolicitud, Long> volumen;
    private long total;
    private Double medianaHorasCiclo;
    private int cerradasSinFechaCierre;
    private String filtrosAplicados;

    public IndicadoresDTO(Map<EstadoSolicitud, Long> volumen, long total, Double medianaHorasCiclo,
                          int cerradasSinFechaCierre, String filtrosAplicados) {
        this.volumen = volumen;
        this.total = total;
        this.medianaHorasCiclo = medianaHorasCiclo;
        this.cerradasSinFechaCierre = cerradasSinFechaCierre;
        this.filtrosAplicados = filtrosAplicados;
    }

    public Map<EstadoSolicitud, Long> getVolumen() {
        return volumen;
    }

    public long getTotal() {
        return total;
    }

    public Double getMedianaHorasCiclo() {
        return medianaHorasCiclo;
    }

    public int getCerradasSinFechaCierre() {
        return cerradasSinFechaCierre;
    }

    public String getFiltrosAplicados() {
        return filtrosAplicados;
    }
}
