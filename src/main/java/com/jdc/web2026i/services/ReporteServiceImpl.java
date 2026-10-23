package com.jdc.web2026i.services;

import com.jdc.web2026i.entities.CategoriaSolicitud;
import com.jdc.web2026i.entities.EstadoSolicitud;
import com.jdc.web2026i.entities.PrioridadSolicitud;
import com.jdc.web2026i.entities.RegistroAuditoriaEntity;
import com.jdc.web2026i.entities.SolicitudEntity;
import com.jdc.web2026i.entities.UsuarioEntity;
import com.jdc.web2026i.repository.RegistroAuditoriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ReporteServiceImpl implements ReporteService {

    private static final String TIPO_ENTIDAD = "Solicitud";
    private static final String ACCION_CONFIRMACION = "CONFIRMACION";
    private static final String ACCION_EXPORTACION = "EXPORTACION_REPORTE";
    private static final String CAMPO_FILTROS = "filtros";
    private static final long ENTIDAD_GENERAL = 0L;
    private static final int LONGITUD_MAXIMA_VALOR = 300;
    private static final String SEPARADOR = ",";
    private static final String FIN_DE_LINEA = "\r\n";
    private static final String[] COLUMNAS_ENCABEZADO = {"ID", "Titulo", "Categoria", "Prioridad", "Estado",
            "FechaCreacion", "FechaActualizacion", "AgenteAsignado", "FechaCierre"};
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter FORMATO_NOMBRE = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm");

    private final SolicitudService solicitudService;
    private final RegistroAuditoriaRepository registroAuditoriaRepository;

    public ReporteServiceImpl(SolicitudService solicitudService,
                              RegistroAuditoriaRepository registroAuditoriaRepository) {
        this.solicitudService = solicitudService;
        this.registroAuditoriaRepository = registroAuditoriaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public String generarCsv(EstadoSolicitud estado, PrioridadSolicitud prioridad,
                             CategoriaSolicitud categoria, LocalDate desde, LocalDate hasta) {
        List<SolicitudEntity> solicitudes =
                solicitudService.listarParaReporte(estado, prioridad, categoria, desde, hasta);
        StringBuilder csv = new StringBuilder();
        for (int indice = 0; indice < COLUMNAS_ENCABEZADO.length; indice++) {
            if (indice > 0) {
                csv.append(SEPARADOR);
            }
            csv.append(celda(COLUMNAS_ENCABEZADO[indice]));
        }
        csv.append(FIN_DE_LINEA);
        for (SolicitudEntity solicitud : solicitudes) {
            csv.append(celda(String.valueOf(solicitud.getIdSolicitud()))).append(SEPARADOR)
                    .append(celda(solicitud.getTitulo())).append(SEPARADOR)
                    .append(celda(solicitud.getCategoria() == null ? null : solicitud.getCategoria().name()))
                    .append(SEPARADOR)
                    .append(celda(solicitud.getPrioridad() == null ? null : solicitud.getPrioridad().name()))
                    .append(SEPARADOR)
                    .append(celda(solicitud.getEstado() == null ? null : solicitud.getEstado().name()))
                    .append(SEPARADOR)
                    .append(celda(formatear(solicitud.getFechaCreacion()))).append(SEPARADOR)
                    .append(celda(formatear(solicitud.getFechaActualizacion()))).append(SEPARADOR)
                    .append(celda(solicitud.getAgenteAsignado() == null
                            ? null : solicitud.getAgenteAsignado().getNombreUsuario()))
                    .append(SEPARADOR)
                    .append(celda(formatear(fechaCierre(solicitud))))
                    .append(FIN_DE_LINEA);
        }
        return csv.toString();
    }

    @Override
    public String nombreArchivo(LocalDateTime fecha) {
        LocalDateTime momento = fecha == null ? LocalDateTime.now() : fecha;
        return "reporte-solicitudes-" + momento.format(FORMATO_NOMBRE) + ".csv";
    }

    @Override
    @Transactional
    public void registrarExportacion(UsuarioEntity actor, String filtros) {
        String detalle = filtros == null || filtros.isBlank() ? "sin filtros" : filtros.trim();
        if (detalle.length() > LONGITUD_MAXIMA_VALOR) {
            detalle = detalle.substring(0, LONGITUD_MAXIMA_VALOR);
        }
        RegistroAuditoriaEntity registro = new RegistroAuditoriaEntity();
        registro.setTipoEntidad(TIPO_ENTIDAD);
        registro.setEntidadId(ENTIDAD_GENERAL);
        registro.setCampo(CAMPO_FILTROS);
        registro.setValorAnterior(null);
        registro.setValorNuevo(detalle);
        registro.setActor(actor);
        registro.setFecha(LocalDateTime.now());
        registro.setAccion(ACCION_EXPORTACION);
        registroAuditoriaRepository.save(registro);
    }

    /**
     * Fecha de cierre tomada del ultimo evento de confirmacion registrado.
     */
    private LocalDateTime fechaCierre(SolicitudEntity solicitud) {
        if (solicitud.getEstado() != EstadoSolicitud.CERRADO) {
            return null;
        }
        List<RegistroAuditoriaEntity> registros =
                registroAuditoriaRepository.findByTipoEntidadAndEntidadIdAndAccionOrderByFechaDesc(
                        TIPO_ENTIDAD, solicitud.getIdSolicitud(), ACCION_CONFIRMACION);
        if (registros.isEmpty()) {
            return null;
        }
        return registros.get(0).getFecha();
    }

    private String formatear(LocalDateTime fecha) {
        return fecha == null ? "" : fecha.format(FORMATO_FECHA);
    }

    /**
     * Campo RFC 4180 entrecomillado, con comillas escapadas y neutralizacion
     * antiformula para los valores que Excel interpretaria como formula.
     */
    private String celda(String valor) {
        String limpio = valor == null ? "" : valor;
        if (!limpio.isEmpty()) {
            char primero = limpio.charAt(0);
            if (primero == '=' || primero == '+' || primero == '-' || primero == '@') {
                limpio = "'" + limpio;
            }
        }
        return "\"" + limpio.replace("\"", "\"\"") + "\"";
    }
}
