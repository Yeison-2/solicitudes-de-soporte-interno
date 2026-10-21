package com.jdc.web2026i.controller;

import com.jdc.web2026i.entities.NotificacionEntity;
import com.jdc.web2026i.entities.UsuarioEntity;
import com.jdc.web2026i.services.NotificacionService;
import com.jdc.web2026i.services.UsuarioService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * Bandeja de avisos del agente. Al abrir la pagina se marcan como leidas
 * y luego se listan.
 */
@Controller
@RequestMapping("/notificaciones")
public class NotificacionController {

    private final NotificacionService notificacionService;
    private final UsuarioService usuarioService;

    public NotificacionController(NotificacionService notificacionService, UsuarioService usuarioService) {
        this.notificacionService = notificacionService;
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public String lista(Authentication autenticacion, Model model) {
        UsuarioEntity usuario = usuarioService.obtenerPorNombreUsuario(autenticacion.getName());
        notificacionService.marcarLeidas(usuario);
        List<NotificacionEntity> notificaciones = notificacionService.listar(usuario);
        model.addAttribute("usuarioActual", usuario);
        model.addAttribute("titulo", "Notificaciones");
        model.addAttribute("notificaciones", notificaciones);
        return "notificaciones/lista";
    }
}
