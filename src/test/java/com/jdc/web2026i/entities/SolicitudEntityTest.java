package com.jdc.web2026i.entities;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SolicitudEntityTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private SolicitudEntity solicitudValida() {
        UsuarioEntity solicitante = new UsuarioEntity();
        solicitante.setNombreUsuario("solicitante1");
        solicitante.setClaveHash("hash");
        solicitante.setNombreCompleto("Solicitante Uno");
        solicitante.setRol(Rol.SOLICITANTE);
        solicitante.setFechaCreacion(LocalDateTime.now());

        LocalDateTime ahora = LocalDateTime.now();
        SolicitudEntity solicitud = new SolicitudEntity();
        solicitud.setTitulo("No enciende el equipo");
        solicitud.setDescripcion("El equipo de la sala 3 no enciende desde hoy.");
        solicitud.setCategoria(CategoriaSolicitud.HARDWARE);
        solicitud.setPrioridad(PrioridadSolicitud.MEDIA);
        solicitud.setEstado(EstadoSolicitud.NUEVO);
        solicitud.setSolicitante(solicitante);
        solicitud.setFechaCreacion(ahora);
        solicitud.setFechaActualizacion(ahora);
        return solicitud;
    }

    @Test
    void solicitudValidaSinViolaciones() {
        assertTrue(validator.validate(solicitudValida()).isEmpty(),
                "Una solicitud completa no deberia tener violaciones");
    }

    @Test
    void tituloVacioEsInvalido() {
        SolicitudEntity solicitud = solicitudValida();
        solicitud.setTitulo("");
        var violaciones = validator.validate(solicitud);
        assertEquals(1, violaciones.size(), "Deberia haber 1 violacion por titulo vacio");
        assertEquals("titulo", violaciones.iterator().next().getPropertyPath().toString());
    }

    @Test
    void descripcionVaciaEsInvalida() {
        SolicitudEntity solicitud = solicitudValida();
        solicitud.setDescripcion("");
        var violaciones = validator.validate(solicitud);
        assertEquals(1, violaciones.size(), "Deberia haber 1 violacion por descripcion vacia");
        assertEquals("descripcion", violaciones.iterator().next().getPropertyPath().toString());
    }

    @Test
    void categoriaNulaEsInvalida() {
        SolicitudEntity solicitud = solicitudValida();
        solicitud.setCategoria(null);
        var violaciones = validator.validate(solicitud);
        assertEquals(1, violaciones.size(), "Deberia haber 1 violacion por categoria nula");
        assertEquals("categoria", violaciones.iterator().next().getPropertyPath().toString());
    }
}
