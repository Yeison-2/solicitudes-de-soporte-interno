package com.jdc.web2026i.controller;

import com.jdc.web2026i.entities.PrioridadSolicitud;
import com.jdc.web2026i.entities.UsuarioEntity;
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

/**
 * Bandeja del coordinador: ver todas las solicitudes y priorizarlas.
 */
@Controller
@RequestMapping("/coordinacion")
public class CoordinacionController {

    private final SolicitudService solicitudService;
    private final UsuarioService usuarioService;

    public CoordinacionController(SolicitudService solicitudService, UsuarioService usuarioService) {
        this.solicitudService = solicitudService;
        this.usuarioService = usuarioService;
    }

    @GetMapping("/solicitudes")
    public String lista(@RequestParam(value = "orden", required = false) String orden,
                        Authentication autenticacion,
                        Model model) {
        model.addAttribute("usuarioActual", usuarioService.obtenerPorNombreUsuario(autenticacion.getName()));
        model.addAttribute("titulo", "Bandeja de coordinacion");
        model.addAttribute("orden", orden == null || orden.isBlank() ? "fecha" : orden);
        model.addAttribute("solicitudes", solicitudService.listarParaCoordinacion(orden));
        model.addAttribute("prioridades", PrioridadSolicitud.values());
        return "coordinacion/lista";
    }

    @PostMapping("/solicitudes/{id}/prioridad")
    public String cambiarPrioridad(@PathVariable("id") Long id,
                                   @RequestParam("prioridad") PrioridadSolicitud prioridad,
                                   Authentication autenticacion,
                                   RedirectAttributes redireccion) {
        UsuarioEntity coordinador = usuarioService.obtenerPorNombreUsuario(autenticacion.getName());
        solicitudService.cambiarPrioridad(id, prioridad, coordinador);
        redireccion.addFlashAttribute("ok", "Prioridad actualizada.");
        return "redirect:/coordinacion/solicitudes";
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public String prioridadInvalida(RedirectAttributes redireccion) {
        redireccion.addFlashAttribute("error", "La prioridad indicada no es valida.");
        return "redirect:/coordinacion/solicitudes";
    }
}
