package com.jdc.web2026i.controller;

import com.jdc.web2026i.entities.Rol;
import com.jdc.web2026i.entities.UsuarioEntity;
import com.jdc.web2026i.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base comun de las pruebas de aceptacion del sprint: aplica la cadena de
 * seguridad, permite crear usuarios de prueba propios y ofrece un inicio de
 * sesion reutilizable.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
abstract class PruebaBase {

    protected static final String CLAVE_PRUEBA = "Clave#2026";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected UsuarioRepository usuarioRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    /**
     * Crea un usuario exclusivo de la prueba. Cada prueba se aisla en su propia
     * transaccion, de modo que no depende de las cuentas semilla ni de datos
     * previos en la base de datos.
     */
    protected UsuarioEntity crearUsuarioDePrueba(String nombreUsuario, Rol rol) {
        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setNombreUsuario(nombreUsuario);
        usuario.setNombreCompleto("Usuario " + nombreUsuario);
        usuario.setClaveHash(passwordEncoder.encode(CLAVE_PRUEBA));
        usuario.setRol(rol);
        usuario.setActivo(true);
        usuario.setFechaCreacion(LocalDateTime.now());
        return usuarioRepository.save(usuario);
    }

    protected MockHttpSession iniciarSesion(String usuario, String clave) throws Exception {
        MvcResult pagina = mockMvc.perform(get("/login")).andReturn();
        MockHttpSession sesion = (MockHttpSession) pagina.getRequest().getSession(false);

        MvcResult resultado = mockMvc.perform(post("/login")
                        .session(sesion)
                        .param("username", usuario)
                        .param("password", clave)
                        .param("_csrf", tokenDe(pagina)))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        return (MockHttpSession) resultado.getRequest().getSession(false);
    }

    protected MockHttpSession nuevaSesion() throws Exception {
        MvcResult pagina = mockMvc.perform(get("/login")).andReturn();
        return (MockHttpSession) pagina.getRequest().getSession(false);
    }

    /**
     * Obtiene el token CSRF que la pagina presenta en el formulario. Debe leerse
     * del atributo de la peticion (valor enmascarado) y no del almacen interno.
     */
    protected String tokenCsrfPara(MockHttpSession sesion, String ruta) throws Exception {
        MvcResult pagina = mockMvc.perform(get(ruta).session(sesion)).andReturn();
        return tokenDe(pagina);
    }

    protected String tokenDe(MvcResult resultado) {
        CsrfToken token = (CsrfToken) resultado.getRequest().getAttribute("_csrf");
        return token.getToken();
    }
}
