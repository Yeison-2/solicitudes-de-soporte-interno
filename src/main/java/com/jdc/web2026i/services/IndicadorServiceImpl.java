package com.jdc.web2026i.services;

import com.jdc.web2026i.DTO.IndicadoresDTO;
import com.jdc.web2026i.entities.CategoriaSolicitud;
import com.jdc.web2026i.entities.EstadoSolicitud;
import com.jdc.web2026i.entities.PrioridadSolicitud;
import com.jdc.web2026i.entities.RegistroAuditoriaEntity;
import com.jdc.web2026i.entities.SolicitudEntity;
import com.jdc.web2026i.repository.RegistroAuditoriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class IndicadorServiceImpl implements IndicadorService {

    private static final String TIPO_ENTIDAD = "Solicitud";
    private static final String ACCION_CONFIRMACION = "CONFIRMACION";

    private final SolicitudService solicitudService;
    private final RegistroAuditoriaRepository registroAuditoriaRepository;

    public IndicadorServiceImpl(SolicitudService solicitudService,
                                RegistroAuditoriaRepository registroAuditoriaRepository) {
        this.solicitudService = solicitudService;
        this.registroAuditoriaRepository = registroAuditoriaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public IndicadoresDTO calcular(EstadoSolicitud estado, PrioridadSolicitud prioridad,
                                   CategoriaSolicitud categoria, LocalDate desde, LocalDate hasta) {
        List<SolicitudEntity> solicitudes =
                solicitudService.listarParaReporte(estado, prioridad, categoria, desde, hasta);

        Map<EstadoSolicitud, Long> volumen = new LinkedHashMap<>();
        for (EstadoSolicitud valor : EstadoSolicitud.values()) {
            volumen.put(valor, 0L);
        }
        for (SolicitudEntity solicitud : solicitudes) {
            volumen.merge(solicitud.getEstado(), 1L, Long::sum);
        }

        List<Double> horasCiclo = new ArrayList<>();
        int cerradasSinFechaCierre = 0;
        for (SolicitudEntity solicitud : solicitudes) {
            if (solicitud.getEstado() != EstadoSolicitud.CERRADO) {
                continue;
            }
            LocalDateTime cierre = fechaConfirmacion(solicitud.getIdSolicitud());
            if (cierre == null) {
                cerradasSinFechaCierre++;
                continue;
            }
            horasCiclo.add(Duration.between(solicitud.getFechaCreacion(), cierre).toMinutes() / 60.0);
        }

        Double mediana = horasCiclo.isEmpty() ? null : mediana(horasCiclo);
        return new IndicadoresDTO(volumen, solicitudes.size(), mediana, cerradasSinFechaCierre,
                describirFiltros(estado, prioridad, categoria, desde, hasta));
    }

    /**
     * Fecha del ultimo evento de confirmacion de la solicitud. Si no existe,
     * la solicitud no aporta a la mediana y se informa aparte.
     */
    private LocalDateTime fechaConfirmacion(Long idSolicitud) {
        List<RegistroAuditoriaEntity> registros =
                registroAuditoriaRepository.findByTipoEntidadAndEntidadIdAndAccionOrderByFechaDesc(
                        TIPO_ENTIDAD, idSolicitud, ACCION_CONFIRMACION);
        if (registros.isEmpty()) {
            return null;
        }
        return registros.get(0).getFecha();
    }

    private Double mediana(List<Double> valores) {
        List<Double> ordenados = new ArrayList<>(valores);
        ordenados.sort(Double::compareTo);
        int tamano = ordenados.size();
        int mitad = tamano / 2;
        if (tamano % 2 == 1) {
            return ordenados.get(mitad);
        }
        return (ordenados.get(mitad - 1) + ordenados.get(mitad)) / 2.0;
    }

    private String describirFiltros(EstadoSolicitud estado, PrioridadSolicitud prioridad,
                                    CategoriaSolicitud categoria, LocalDate desde, LocalDate hasta) {
        List<String> partes = new ArrayList<>();
        if (estado != null) {
            partes.add("estado=" + estado.name());
        }
        if (prioridad != null) {
            partes.add("prioridad=" + prioridad.name());
        }
        if (categoria != null) {
            partes.add("categoria=" + categoria.name());
        }
        if (desde != null) {
            partes.add("desde=" + desde);
        }
        if (hasta != null) {
            partes.add("hasta=" + hasta);
        }
        if (partes.isEmpty()) {
            return "sin filtros";
        }
        return String.join("; ", partes);
    }
}
