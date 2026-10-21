package com.jdc.web2026i.controller;

import com.jdc.web2026i.DTO.AsignacionFormularioDTO;
import com.jdc.web2026i.entities.EstadoSolicitud;
import com.jdc.web2026i.entities.PrioridadSolicitud;
import com.jdc.web2026i.entities.Rol;
import com.jdc.web2026i.entities.SolicitudEntity;
import com.jdc.web2026i.entities.UsuarioEntity;
import com.jdc.web2026i.repository.UsuarioRepository;
import com.jdc.web2026i.services.AccesoDenegadoException;
import com.jdc.web2026i.services.SolicitudNoEncontradaException;
import com.jdc.web2026i.services.SolicitudService;
import com.jdc.web2026i.services.UsuarioService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Bandeja del coordinador: ver todas las solicitudes, asignarlas y priorizarlas.
 */
@Controller
@RequestMapping("/coordinacion")
public class CoordinacionController {

    private final SolicitudService solicitudService;
    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;

    public CoordinacionController(SolicitudService solicitudService, UsuarioService usuarioService,
                                  UsuarioRepository usuarioRepository) {
        this.solicitudService = solicitudService;
        this.usuarioService = usuarioService;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping("/solicitudes")
    public String lista(@RequestParam(value = "orden", required = false) String orden,
                        @RequestParam(value = "estado", required = false) String estado,
                        Authentication autenticacion,
                        Model model) {
        model.addAttribute("usuarioActual", usuarioService.obtenerPorNombreUsuario(autenticacion.getName()));
        model.addAttribute("titulo", "Bandeja de coordinacion");
        String ordenAplicado = orden == null || orden.isBlank() ? "fecha" : orden;
        EstadoSolicitud estadoAplicado = estadoValido(estado);
        model.addAttribute("orden", ordenAplicado);
        model.addAttribute("estado", estadoAplicado == null ? null : estadoAplicado.name());
        model.addAttribute("estados", EstadoSolicitud.values());
        model.addAttribute("solicitudes",
                filtrarPorEstado(solicitudService.listarParaCoordinacion(ordenAplicado), estadoAplicado));
        model.addAttribute("prioridades", PrioridadSolicitud.values());
        model.addAttribute("agentes", agentesActivos());
        model.addAttribute("formularioAsignacion", new AsignacionFormularioDTO());
        return "coordinacion/lista";
    }

    @PostMapping("/solicitudes/{id}/asignar")
    public String asignar(@PathVariable("id") Long id,
                          @RequestParam(value = "agenteId", required = false) String agenteId,
                          Authentication autenticacion,
                          RedirectAttributes redireccion) {
        UsuarioEntity coordinador = usuarioService.obtenerPorNombreUsuario(autenticacion.getName());
        try {
            solicitudService.asignar(id, idAgente(agenteId), coordinador);
        } catch (IllegalArgumentException | AccesoDenegadoException
                 | SolicitudNoEncontradaException excepcion) {
            redireccion.addFlashAttribute("error", excepcion.getMessage());
            return "redirect:/coordinacion/solicitudes";
        }
        redireccion.addFlashAttribute("ok", "Solicitud asignada.");
        return "redirect:/coordinacion/solicitudes";
    }

    @PostMapping("/solicitudes/{id}/prioridad")
    public String cambiarPrioridad(@PathVariable("id") Long id,
                                   @RequestParam("prioridad") PrioridadSolicitud prioridad,
                                   @RequestParam(value = "justificacion", required = false) String justificacion,
                                   @RequestParam(value = "fechaObjetivo", required = false) String fechaObjetivo,
                                   Authentication autenticacion,
                                   RedirectAttributes redireccion) {
        UsuarioEntity coordinador = usuarioService.obtenerPorNombreUsuario(autenticacion.getName());
        try {
            solicitudService.cambiarPrioridad(id, prioridad, textoOpcional(justificacion),
                    fechaOpcional(fechaObjetivo), coordinador);
        } catch (IllegalArgumentException | AccesoDenegadoException
                 | SolicitudNoEncontradaException excepcion) {
            redireccion.addFlashAttribute("error", excepcion.getMessage());
            return "redirect:/coordinacion/solicitudes";
        }
        redireccion.addFlashAttribute("ok", "Prioridad actualizada.");
        return "redirect:/coordinacion/solicitudes";
    }

    private List<UsuarioEntity> agentesActivos() {
        List<UsuarioEntity> agentes = new ArrayList<>(usuarioRepository.findByRolAndActivoTrue(Rol.AGENTE));
        agentes.sort((a, b) -> a.getNombreCompleto().compareToIgnoreCase(b.getNombreCompleto()));
        return agentes;
    }

    private List<SolicitudEntity> filtrarPorEstado(List<SolicitudEntity> solicitudes, EstadoSolicitud estado) {
        if (estado == null) {
            return solicitudes;
        }
        List<SolicitudEntity> filtradas = new ArrayList<>();
        for (SolicitudEntity solicitud : solicitudes) {
            if (solicitud.getEstado() == estado) {
                filtradas.add(solicitud);
            }
        }
        return filtradas;
    }

    /**
     * Valor de estado pedido por la vista. Un valor desconocido o vacio se
     * ignora y la bandeja queda sin filtro.
     */
    private EstadoSolicitud estadoValido(String valor) {
        String limpio = textoOpcional(valor);
        if (limpio == null) {
            return null;
        }
        try {
            return EstadoSolicitud.valueOf(limpio.trim().toUpperCase());
        } catch (IllegalArgumentException excepcion) {
            return null;
        }
    }

    private Long idAgente(String valor) {
        String limpio = textoOpcional(valor);
        if (limpio == null) {
            throw new IllegalArgumentException("Seleccione el agente que atendera la solicitud.");
        }
        try {
            return Long.valueOf(limpio);
        } catch (NumberFormatException excepcion) {
            throw new IllegalArgumentException("El agente indicado no es valido.");
        }
    }

    private String textoOpcional(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }

    private LocalDate fechaOpcional(String valor) {
        String limpio = textoOpcional(valor);
        if (limpio == null) {
            return null;
        }
        try {
            return LocalDate.parse(limpio);
        } catch (DateTimeParseException excepcion) {
            throw new IllegalArgumentException("La fecha objetivo no es valida.");
        }
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public String prioridadInvalida(RedirectAttributes redireccion) {
        redireccion.addFlashAttribute("error", "La prioridad indicada no es valida.");
        return "redirect:/coordinacion/solicitudes";
    }
}
