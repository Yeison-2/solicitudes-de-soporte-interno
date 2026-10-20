package com.jdc.web2026i.controller;

import com.jdc.web2026i.entities.UsuarioEntity;
import com.jdc.web2026i.services.UsuarioService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Pantallas de acceso y de inicio por rol.
 */
@Controller
public class LoginController {

    private final UsuarioService usuarioService;

    public LoginController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("titulo", "Ingreso");
        return "login";
    }

    @GetMapping("/inicio")
    public String inicio(Authentication autenticacion, Model model) {
        UsuarioEntity usuario = usuarioService.obtenerPorNombreUsuario(autenticacion.getName());
        model.addAttribute("usuarioActual", usuario);
        model.addAttribute("titulo", "Inicio");
        return "inicio";
    }
}
