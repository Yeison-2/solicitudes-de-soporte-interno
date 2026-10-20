package com.jdc.web2026i.entities;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UsuarioEntityTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private UsuarioEntity usuarioValido() {
        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setNombreUsuario("usuario1");
        usuario.setClaveHash("$2a$10$abcdefghijklmnopqrstuv");
        usuario.setNombreCompleto("Usuario de Prueba");
        usuario.setRol(Rol.SOLICITANTE);
        usuario.setActivo(true);
        usuario.setFechaCreacion(LocalDateTime.now());
        return usuario;
    }

    @Test
    void usuarioValidoSinViolaciones() {
        assertTrue(validator.validate(usuarioValido()).isEmpty(),
                "Un usuario completo no deberia tener violaciones");
    }

    @Test
    void nombreUsuarioVacioEsInvalido() {
        UsuarioEntity usuario = usuarioValido();
        usuario.setNombreUsuario("");
        var violaciones = validator.validate(usuario);
        assertEquals(1, violaciones.size(), "Deberia haber 1 violacion por nombre de usuario vacio");
        assertEquals("nombreUsuario", violaciones.iterator().next().getPropertyPath().toString());
    }

    @Test
    void rolNuloEsInvalido() {
        UsuarioEntity usuario = usuarioValido();
        usuario.setRol(null);
        var violaciones = validator.validate(usuario);
        assertEquals(1, violaciones.size(), "Deberia haber 1 violacion por rol nulo");
        assertEquals("rol", violaciones.iterator().next().getPropertyPath().toString());
    }

    @Test
    void gettersYSetters() {
        UsuarioEntity usuario = usuarioValido();
        assertEquals("usuario1", usuario.getNombreUsuario());
        assertEquals(Rol.SOLICITANTE, usuario.getRol());
        assertTrue(usuario.isActivo());
    }
}
