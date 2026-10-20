package com.jdc.web2026i.controller;

import com.jdc.web2026i.DTO.SolicitudFormularioDTO;
import com.jdc.web2026i.entities.CategoriaSolicitud;
import com.jdc.web2026i.entities.SolicitudEntity;
import com.jdc.web2026i.entities.UsuarioEntity;
import com.jdc.web2026i.services.AccesoDenegadoException;
import com.jdc.web2026i.services.SolicitudNoEncontradaException;
import com.jdc.web2026i.services.SolicitudService;
import com.jdc.web2026i.services.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Flujo del solicitante: listar sus solicitudes, crear una nueva y ver el detalle.
 */
@Controller
@RequestMapping("/solicitudes")
public class SolicitudController {

    private final SolicitudService solicitudService;
    private final UsuarioService usuarioService;

    public SolicitudController(SolicitudService solicitudService, UsuarioService usuarioService) {
        this.solicitudService = solicitudService;
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public String lista(Authentication autenticacion, Model model) {
        UsuarioEntity usuario = usuarioService.obtenerPorNombreUsuario(autenticacion.getName());
        model.addAttribute("usuarioActual", usuario);
        model.addAttribute("titulo", "Mis solicitudes");
        model.addAttribute("solicitudes", solicitudService.listarPropias(usuario));
        return "solicitudes/lista";
    }

    @GetMapping("/nueva")
    public String nueva(Authentication autenticacion, Model model) {
        model.addAttribute("usuarioActual", usuarioService.obtenerPorNombreUsuario(autenticacion.getName()));
        model.addAttribute("titulo", "Nueva solicitud");
        model.addAttribute("solicitud", new SolicitudFormularioDTO());
        model.addAttribute("categorias", CategoriaSolicitud.values());
        return "solicitudes/nueva";
    }

    @PostMapping("/nueva")
    public String crear(@Valid @ModelAttribute("solicitud") SolicitudFormularioDTO formulario,
                        BindingResult resultado,
                        Authentication autenticacion,
                        Model model,
                        RedirectAttributes redireccion) {
        if (resultado.hasErrors()) {
            model.addAttribute("usuarioActual", usuarioService.obtenerPorNombreUsuario(autenticacion.getName()));
            model.addAttribute("titulo", "Nueva solicitud");
            model.addAttribute("categorias", CategoriaSolicitud.values());
            return "solicitudes/nueva";
        }
        UsuarioEntity solicitante = usuarioService.obtenerPorNombreUsuario(autenticacion.getName());
        SolicitudEntity creada = solicitudService.crear(formulario, solicitante);
        redireccion.addFlashAttribute("ok",
                "Solicitud registrada con el numero " + creada.getIdSolicitud() + ".");
        return "redirect:/solicitudes";
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable("id") Long id, Authentication autenticacion, Model model) {
        UsuarioEntity usuario = usuarioService.obtenerPorNombreUsuario(autenticacion.getName());
        model.addAttribute("usuarioActual", usuario);
        model.addAttribute("titulo", "Detalle de la solicitud");
        try {
            model.addAttribute("solicitud", solicitudService.obtenerParaUsuario(id, usuario));
        } catch (SolicitudNoEncontradaException excepcion) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, excepcion.getMessage());
        } catch (AccesoDenegadoException excepcion) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, excepcion.getMessage());
        }
        return "solicitudes/detalle";
    }
}
