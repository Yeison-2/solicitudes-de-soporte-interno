package com.jdc.web2026i.controller;

import com.jdc.web2026i.DTO.ComentarioFormularioDTO;
import com.jdc.web2026i.DTO.SolicitudFormularioDTO;
import com.jdc.web2026i.entities.CategoriaSolicitud;
import com.jdc.web2026i.entities.EstadoSolicitud;
import com.jdc.web2026i.entities.PrioridadSolicitud;
import com.jdc.web2026i.entities.Rol;
import com.jdc.web2026i.entities.SolicitudEntity;
import com.jdc.web2026i.entities.UsuarioEntity;
import com.jdc.web2026i.services.AccesoDenegadoException;
import com.jdc.web2026i.services.ComentarioService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

/**
 * Flujo del solicitante y del agente: listar solicitudes, crear una nueva, ver
 * el detalle, comentar y avanzar el estado.
 */
@Controller
@RequestMapping("/solicitudes")
public class SolicitudController {

    private final SolicitudService solicitudService;
    private final UsuarioService usuarioService;
    private final ComentarioService comentarioService;

    public SolicitudController(SolicitudService solicitudService, UsuarioService usuarioService,
                               ComentarioService comentarioService) {
        this.solicitudService = solicitudService;
        this.usuarioService = usuarioService;
        this.comentarioService = comentarioService;
    }

    @GetMapping
    public String lista(Authentication autenticacion, Model model) {
        UsuarioEntity usuario = usuarioService.obtenerPorNombreUsuario(autenticacion.getName());
        model.addAttribute("usuarioActual", usuario);
        model.addAttribute("titulo", "Mis solicitudes");
        model.addAttribute("solicitudes", solicitudService.listarPropias(usuario));
        return "solicitudes/lista";
    }

    @GetMapping("/mis-asignadas")
    public String misAsignadas(Authentication autenticacion, Model model) {
        UsuarioEntity agente = usuarioService.obtenerPorNombreUsuario(autenticacion.getName());
        model.addAttribute("usuarioActual", agente);
        model.addAttribute("titulo", "Mis asignadas");
        model.addAttribute("solicitudes", solicitudService.listarAsignadas(agente));
        return "solicitudes/misAsignadas";
    }

    @GetMapping("/nueva")
    public String nueva(Authentication autenticacion, Model model) {
        model.addAttribute("usuarioActual", usuarioService.obtenerPorNombreUsuario(autenticacion.getName()));
        model.addAttribute("titulo", "Nueva solicitud");
        model.addAttribute("solicitud", new SolicitudFormularioDTO());
        model.addAttribute("categorias", CategoriaSolicitud.values());
        model.addAttribute("prioridades", prioridadesDelSolicitante());
        return "solicitudes/nueva";
    }

    @PostMapping("/nueva")
    public String crear(@Valid @ModelAttribute("solicitud") SolicitudFormularioDTO formulario,
                        BindingResult resultado,
                        Authentication autenticacion,
                        Model model,
                        RedirectAttributes redireccion) {
        if (resultado.hasErrors()) {
            return volverANueva(autenticacion, model);
        }
        UsuarioEntity solicitante = usuarioService.obtenerPorNombreUsuario(autenticacion.getName());
        SolicitudEntity creada;
        try {
            creada = solicitudService.crear(formulario, solicitante);
        } catch (IllegalArgumentException excepcion) {
            model.addAttribute("error", excepcion.getMessage());
            return volverANueva(autenticacion, model);
        }
        redireccion.addFlashAttribute("ok",
                "Solicitud registrada con el numero " + creada.getIdSolicitud() + ".");
        return "redirect:/solicitudes";
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable("id") Long id, Authentication autenticacion, Model model) {
        UsuarioEntity usuario = usuarioService.obtenerPorNombreUsuario(autenticacion.getName());
        model.addAttribute("usuarioActual", usuario);
        model.addAttribute("titulo", "Detalle de la solicitud");
        SolicitudEntity solicitud;
        try {
            solicitud = solicitudService.obtenerParaUsuario(id, usuario);
        } catch (SolicitudNoEncontradaException excepcion) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, excepcion.getMessage());
        } catch (AccesoDenegadoException excepcion) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, excepcion.getMessage());
        }
        model.addAttribute("solicitud", solicitud);
        model.addAttribute("comentarios", comentarioService.listar(solicitud.getIdSolicitud(), usuario));
        model.addAttribute("historial", solicitudService.historial(solicitud.getIdSolicitud(), usuario));
        model.addAttribute("puedeComentar", esAgenteAsignado(solicitud, usuario));
        model.addAttribute("acciones", accionesDisponibles(solicitud, usuario));
        model.addAttribute("puedeCerrar", esPropietario(solicitud, usuario)
                && solicitud.getEstado() == EstadoSolicitud.RESUELTO);
        return "solicitudes/detalle";
    }

    @PostMapping("/{id}/comentarios")
    public String comentar(@PathVariable("id") Long id,
                           @Valid @ModelAttribute("comentario") ComentarioFormularioDTO formulario,
                           BindingResult resultado,
                           Authentication autenticacion,
                           RedirectAttributes redireccion) {
        if (resultado.hasErrors()) {
            redireccion.addFlashAttribute("error", "Escriba el comentario antes de enviarlo.");
            return "redirect:/solicitudes/" + id;
        }
        UsuarioEntity autor = usuarioService.obtenerPorNombreUsuario(autenticacion.getName());
        try {
            comentarioService.registrar(id, formulario.getTexto(), autor);
        } catch (IllegalArgumentException | AccesoDenegadoException excepcion) {
            redireccion.addFlashAttribute("error", excepcion.getMessage());
            return "redirect:/solicitudes/" + id;
        }
        redireccion.addFlashAttribute("ok", "Comentario registrado.");
        return "redirect:/solicitudes/" + id;
    }

    @PostMapping("/{id}/estado")
    public String cambiarEstado(@PathVariable("id") Long id,
                               @RequestParam("accion") String accion,
                               Authentication autenticacion,
                               RedirectAttributes redireccion) {
        UsuarioEntity actor = usuarioService.obtenerPorNombreUsuario(autenticacion.getName());
        try {
            solicitudService.cambiarEstado(id, accion, actor);
        } catch (AccesoDenegadoException excepcion) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, excepcion.getMessage());
        } catch (IllegalArgumentException excepcion) {
            redireccion.addFlashAttribute("error", excepcion.getMessage());
            return "redirect:/solicitudes/" + id;
        }
        redireccion.addFlashAttribute("ok", "Estado de la solicitud actualizado.");
        return "redirect:/solicitudes/" + id;
    }

    @PostMapping("/{id}/confirmar")
    public String confirmar(@PathVariable("id") Long id,
                            Authentication autenticacion,
                            RedirectAttributes redireccion) {
        UsuarioEntity solicitante = usuarioService.obtenerPorNombreUsuario(autenticacion.getName());
        try {
            solicitudService.confirmar(id, solicitante);
        } catch (IllegalArgumentException | AccesoDenegadoException excepcion) {
            redireccion.addFlashAttribute("error", excepcion.getMessage());
            return "redirect:/solicitudes/" + id;
        }
        redireccion.addFlashAttribute("ok", "Solicitud confirmada y cerrada.");
        return "redirect:/solicitudes/" + id;
    }

    @PostMapping("/{id}/reabrir")
    public String reabrir(@PathVariable("id") Long id,
                          @RequestParam(value = "motivo", required = false) String motivo,
                          Authentication autenticacion,
                          RedirectAttributes redireccion) {
        UsuarioEntity solicitante = usuarioService.obtenerPorNombreUsuario(autenticacion.getName());
        try {
            solicitudService.reabrir(id, motivo, solicitante);
        } catch (IllegalArgumentException | AccesoDenegadoException excepcion) {
            redireccion.addFlashAttribute("error", excepcion.getMessage());
            return "redirect:/solicitudes/" + id;
        }
        redireccion.addFlashAttribute("ok", "Solicitud reabierta.");
        return "redirect:/solicitudes/" + id;
    }

    private String volverANueva(Authentication autenticacion, Model model) {
        model.addAttribute("usuarioActual", usuarioService.obtenerPorNombreUsuario(autenticacion.getName()));
        model.addAttribute("titulo", "Nueva solicitud");
        model.addAttribute("categorias", CategoriaSolicitud.values());
        model.addAttribute("prioridades", prioridadesDelSolicitante());
        return "solicitudes/nueva";
    }

    /**
     * El solicitante no puede elegir CRITICA: esa prioridad queda reservada
     * a la coordinacion.
     */
    private List<PrioridadSolicitud> prioridadesDelSolicitante() {
        return List.of(PrioridadSolicitud.MEDIA, PrioridadSolicitud.ALTA, PrioridadSolicitud.BAJA);
    }

    private boolean esPropietario(SolicitudEntity solicitud, UsuarioEntity usuario) {
        return solicitud.getSolicitante().getIdUsuario().equals(usuario.getIdUsuario());
    }

    private boolean esAgenteAsignado(SolicitudEntity solicitud, UsuarioEntity usuario) {
        return usuario.getRol() == Rol.AGENTE
                && solicitud.getAgenteAsignado() != null
                && solicitud.getAgenteAsignado().getIdUsuario().equals(usuario.getIdUsuario());
    }

    /**
     * Acciones de estado que el usuario en sesion puede ejecutar sobre la
     * solicitud. La validacion real se hace igualmente en el servicio.
     */
    private List<String> accionesDisponibles(SolicitudEntity solicitud, UsuarioEntity usuario) {
        List<String> acciones = new ArrayList<>();
        if (esAgenteAsignado(solicitud, usuario)) {
            EstadoSolicitud estado = solicitud.getEstado();
            if (estado == EstadoSolicitud.ASIGNADO || estado == EstadoSolicitud.REABIERTO) {
                acciones.add("iniciar");
            }
            if (estado == EstadoSolicitud.EN_PROCESO) {
                acciones.add("resolver");
            }
        }
        return acciones;
    }
}
