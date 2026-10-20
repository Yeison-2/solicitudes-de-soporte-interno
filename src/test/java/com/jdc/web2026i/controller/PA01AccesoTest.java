package com.jdc.web2026i.controller;

import com.jdc.web2026i.entities.Rol;
import com.jdc.web2026i.entities.UsuarioEntity;
import com.jdc.web2026i.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PA-01: cubre CA-001-01 a CA-001-04 (acceso, mensaje generico, cierre y roles).
 */
class PA01AccesoTest extends PruebaBase {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void credencialesValidasPermitenEntrar() throws Exception {
        MockHttpSession sesion = iniciarSesion("solicitante1", "Clave#2026");
        assertNotNull(sesion, "La sesion deberia quedar iniciada");
        mockMvc.perform(get("/inicio").session(sesion))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Solicitante Uno")));
    }

    @Test
    void credencialesInvalidasMuestranMensajeGenerico() throws Exception {
        MockHttpSession sesion = nuevaSesion();
        mockMvc.perform(post("/login")
                        .session(sesion)
                        .param("username", "solicitante1")
                        .param("password", "clave-equivocada")
                        .param("_csrf", tokenCsrfPara(sesion, "/login")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));

        mockMvc.perform(get("/login").param("error", ""))
                .andExpect(content().string(containsString("Usuario o contrasena incorrectos.")));
    }

    @Test
    void usuarioInexistenteMismoResultado() throws Exception {
        MockHttpSession sesion = nuevaSesion();
        mockMvc.perform(post("/login")
                        .session(sesion)
                        .param("username", "no-existe")
                        .param("password", "Clave#2026")
                        .param("_csrf", tokenCsrfPara(sesion, "/login")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void usuarioInactivoNoEntra() throws Exception {
        UsuarioEntity inactivo = new UsuarioEntity();
        inactivo.setNombreUsuario("inactivo1");
        inactivo.setClaveHash(passwordEncoder.encode("Clave#2026"));
        inactivo.setNombreCompleto("Usuario Inactivo");
        inactivo.setRol(Rol.SOLICITANTE);
        inactivo.setActivo(false);
        inactivo.setFechaCreacion(LocalDateTime.now());
        usuarioRepository.save(inactivo);

        MockHttpSession sesion = nuevaSesion();
        mockMvc.perform(post("/login")
                        .session(sesion)
                        .param("username", "inactivo1")
                        .param("password", "Clave#2026")
                        .param("_csrf", tokenCsrfPara(sesion, "/login")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void cierreDeSesionRedirigeAlLogin() throws Exception {
        MockHttpSession sesion = iniciarSesion("solicitante1", "Clave#2026");
        mockMvc.perform(post("/logout")
                        .session(sesion)
                        .param("_csrf", tokenCsrfPara(sesion, "/inicio")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"));

        mockMvc.perform(get("/inicio"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void solicitanteNoAccedeARutasDeCoordinador() throws Exception {
        MockHttpSession sesion = iniciarSesion("solicitante1", "Clave#2026");
        mockMvc.perform(get("/coordinacion/solicitudes").session(sesion))
                .andExpect(status().isForbidden());
    }
}
