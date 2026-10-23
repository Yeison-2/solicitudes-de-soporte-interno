package com.jdc.web2026i.controller;

import com.jdc.web2026i.DTO.IndicadoresDTO;
import com.jdc.web2026i.entities.CategoriaSolicitud;
import com.jdc.web2026i.entities.EstadoSolicitud;
import com.jdc.web2026i.entities.PrioridadSolicitud;
import com.jdc.web2026i.services.IndicadorService;
import com.jdc.web2026i.services.UsuarioService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/**
 * Indicadores agregados para la coordinacion: volumen por estado y tiempo
 * mediano de ciclo. Sin rankings ni agrupacion por persona.
 */
@Controller
@RequestMapping("/coordinacion")
public class IndicadorController {

    private final IndicadorService indicadorService;
    private final UsuarioService usuarioService;

    public IndicadorController(IndicadorService indicadorService, UsuarioService usuarioService) {
        this.indicadorService = indicadorService;
        this.usuarioService = usuarioService;
    }

    @GetMapping("/indicadores")
    public String indicadores(@RequestParam(value = "estado", required = false) String estado,
                              @RequestParam(value = "prioridad", required = false) String prioridad,
                              @RequestParam(value = "categoria", required = false) String categoria,
                              @RequestParam(value = "desde", required = false) String desde,
                              @RequestParam(value = "hasta", required = false) String hasta,
                              Authentication autenticacion,
                              Model model) {
        EstadoSolicitud estadoAplicado = valorEnum(EstadoSolicitud.class, estado);
        PrioridadSolicitud prioridadAplicada = valorEnum(PrioridadSolicitud.class, prioridad);
        CategoriaSolicitud categoriaAplicada = valorEnum(CategoriaSolicitud.class, categoria);
        LocalDate desdeAplicado = fechaOpcional(desde);
        LocalDate hastaAplicado = fechaOpcional(hasta);

        IndicadoresDTO indicadores = indicadorService.calcular(estadoAplicado, prioridadAplicada,
                categoriaAplicada, desdeAplicado, hastaAplicado);

        model.addAttribute("usuarioActual", usuarioService.obtenerPorNombreUsuario(autenticacion.getName()));
        model.addAttribute("titulo", "Indicadores del servicio");
        model.addAttribute("indicadores", indicadores);
        model.addAttribute("filtrosAplicados", indicadores.getFiltrosAplicados());
        model.addAttribute("estados", EstadoSolicitud.values());
        model.addAttribute("prioridades", PrioridadSolicitud.values());
        model.addAttribute("categorias", CategoriaSolicitud.values());
        model.addAttribute("estado", estadoAplicado == null ? "" : estadoAplicado.name());
        model.addAttribute("prioridad", prioridadAplicada == null ? "" : prioridadAplicada.name());
        model.addAttribute("categoria", categoriaAplicada == null ? "" : categoriaAplicada.name());
        model.addAttribute("desde", desdeAplicado == null ? "" : desdeAplicado.toString());
        model.addAttribute("hasta", hastaAplicado == null ? "" : hastaAplicado.toString());
        return "coordinacion/indicadores";
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

    /**
     * Fecha ISO pedida por la vista. Una fecha invalida se ignora, igual que
     * un valor de enum desconocido.
     */
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
