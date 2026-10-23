package com.jdc.web2026i.controller;

import com.jdc.web2026i.entities.CategoriaSolicitud;
import com.jdc.web2026i.entities.EstadoSolicitud;
import com.jdc.web2026i.entities.PrioridadSolicitud;
import com.jdc.web2026i.entities.UsuarioEntity;
import com.jdc.web2026i.services.SolicitudService;
import com.jdc.web2026i.services.UsuarioService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Busqueda de solicitudes con filtros combinables, limitada por el rol del
 * usuario en la capa de servicio.
 */
@Controller
public class BusquedaController {

    private final SolicitudService solicitudService;
    private final UsuarioService usuarioService;

    public BusquedaController(SolicitudService solicitudService, UsuarioService usuarioService) {
        this.solicitudService = solicitudService;
        this.usuarioService = usuarioService;
    }

    @GetMapping("/solicitudes/buscar")
    public String buscar(@RequestParam(value = "q", required = false) String texto,
                         @RequestParam(value = "estado", required = false) String estado,
                         @RequestParam(value = "prioridad", required = false) String prioridad,
                         @RequestParam(value = "categoria", required = false) String categoria,
                         Authentication autenticacion,
                         Model model) {
        UsuarioEntity usuario = usuarioService.obtenerPorNombreUsuario(autenticacion.getName());
        String textoAplicado = textoOpcional(texto);
        EstadoSolicitud estadoAplicado = valorEnum(EstadoSolicitud.class, estado);
        PrioridadSolicitud prioridadAplicada = valorEnum(PrioridadSolicitud.class, prioridad);
        CategoriaSolicitud categoriaAplicada = valorEnum(CategoriaSolicitud.class, categoria);

        model.addAttribute("usuarioActual", usuario);
        model.addAttribute("titulo", "Buscar solicitudes");
        model.addAttribute("resultados", solicitudService.buscar(textoAplicado, estadoAplicado,
                prioridadAplicada, categoriaAplicada, usuario));
        model.addAttribute("texto", textoAplicado == null ? "" : textoAplicado);
        model.addAttribute("estado", estadoAplicado == null ? "" : estadoAplicado.name());
        model.addAttribute("prioridad", prioridadAplicada == null ? "" : prioridadAplicada.name());
        model.addAttribute("categoria", categoriaAplicada == null ? "" : categoriaAplicada.name());
        model.addAttribute("estados", EstadoSolicitud.values());
        model.addAttribute("prioridades", PrioridadSolicitud.values());
        model.addAttribute("categorias", CategoriaSolicitud.values());
        model.addAttribute("filtrosAplicados", describirFiltros(textoAplicado, estadoAplicado,
                prioridadAplicada, categoriaAplicada));
        return "solicitudes/buscar";
    }

    private String describirFiltros(String texto, EstadoSolicitud estado, PrioridadSolicitud prioridad,
                                    CategoriaSolicitud categoria) {
        List<String> partes = new ArrayList<>();
        if (texto != null) {
            partes.add("texto=" + texto);
        }
        if (estado != null) {
            partes.add("estado=" + estado.name());
        }
        if (prioridad != null) {
            partes.add("prioridad=" + prioridad.name());
        }
        if (categoria != null) {
            partes.add("categoria=" + categoria.name());
        }
        if (partes.isEmpty()) {
            return "sin filtros";
        }
        return String.join("; ", partes);
    }

    /**
     * Un valor de enum desconocido o vacio se ignora: la consulta conserva el
     * resto de filtros y nunca responde con un error de validacion.
     */
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

    private String textoOpcional(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }
}
