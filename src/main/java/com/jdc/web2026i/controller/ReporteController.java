package com.jdc.web2026i.controller;

import com.jdc.web2026i.entities.CategoriaSolicitud;
import com.jdc.web2026i.entities.EstadoSolicitud;
import com.jdc.web2026i.entities.PrioridadSolicitud;
import com.jdc.web2026i.entities.UsuarioEntity;
import com.jdc.web2026i.services.ReporteService;
import com.jdc.web2026i.services.UsuarioService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Descarga del reporte CSV con los filtros aplicados. Solo coordinacion: la
 * ruta esta restringida por la configuracion de seguridad.
 */
@Controller
@RequestMapping("/coordinacion")
public class ReporteController {

    private static final String BOM = "\uFEFF";

    private final ReporteService reporteService;
    private final UsuarioService usuarioService;

    public ReporteController(ReporteService reporteService, UsuarioService usuarioService) {
        this.reporteService = reporteService;
        this.usuarioService = usuarioService;
    }

    @GetMapping("/reporte.csv")
    public ResponseEntity<byte[]> descargar(@RequestParam(value = "estado", required = false) String estado,
                                            @RequestParam(value = "prioridad", required = false) String prioridad,
                                            @RequestParam(value = "categoria", required = false) String categoria,
                                            @RequestParam(value = "desde", required = false) String desde,
                                            @RequestParam(value = "hasta", required = false) String hasta,
                                            Authentication autenticacion) {
        UsuarioEntity coordinador = usuarioService.obtenerPorNombreUsuario(autenticacion.getName());
        EstadoSolicitud estadoAplicado = valorEnum(EstadoSolicitud.class, estado);
        PrioridadSolicitud prioridadAplicada = valorEnum(PrioridadSolicitud.class, prioridad);
        CategoriaSolicitud categoriaAplicada = valorEnum(CategoriaSolicitud.class, categoria);
        LocalDate desdeAplicado = fechaOpcional(desde);
        LocalDate hastaAplicado = fechaOpcional(hasta);

        String csv = reporteService.generarCsv(estadoAplicado, prioridadAplicada, categoriaAplicada,
                desdeAplicado, hastaAplicado);
        reporteService.registrarExportacion(coordinador, describirFiltros(estadoAplicado, prioridadAplicada,
                categoriaAplicada, desdeAplicado, hastaAplicado));

        byte[] cuerpo = (BOM + csv).getBytes(StandardCharsets.UTF_8);
        HttpHeaders cabeceras = new HttpHeaders();
        cabeceras.setContentType(MediaType.parseMediaType("text/csv;charset=UTF-8"));
        cabeceras.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\""
                + reporteService.nombreArchivo(LocalDateTime.now()) + "\"");
        return new ResponseEntity<>(cuerpo, cabeceras, HttpStatus.OK);
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

    private <E extends Enum<E>> E valorEnum(Class<E> tipo, String valor) {
        String limpio = textoOpcional(valor);
        if (limpio == null) {
            return null;
        }
        try {
            return Enum.valueOf(tipo, limpio.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException excepcion) {
            return null;
        }
    }

    private LocalDate fechaOpcional(String valor) {
        String limpio = textoOpcional(valor);
        if (limpio == null) {
            return null;
        }
        try {
            return LocalDate.parse(limpio);
        } catch (DateTimeParseException excepcion) {
            return null;
        }
    }

    private String textoOpcional(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }
}
