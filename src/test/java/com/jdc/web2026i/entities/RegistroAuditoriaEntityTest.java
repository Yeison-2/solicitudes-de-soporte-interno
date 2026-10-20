package com.jdc.web2026i.entities;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistroAuditoriaEntityTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private RegistroAuditoriaEntity registroValido() {
        UsuarioEntity actor = new UsuarioEntity();
        actor.setNombreUsuario("coordinador1");
        actor.setClaveHash("hash");
        actor.setNombreCompleto("Coordinador Uno");
        actor.setRol(Rol.COORDINADOR);
        actor.setFechaCreacion(LocalDateTime.now());

        RegistroAuditoriaEntity registro = new RegistroAuditoriaEntity();
        registro.setTipoEntidad("Solicitud");
        registro.setEntidadId(1L);
        registro.setCampo("prioridad");
        registro.setValorAnterior("MEDIA");
        registro.setValorNuevo("ALTA");
        registro.setActor(actor);
        registro.setFecha(LocalDateTime.now());
        registro.setAccion("CAMBIO_PRIORIDAD");
        return registro;
    }

    @Test
    void registroValidoSinViolaciones() {
        assertTrue(validator.validate(registroValido()).isEmpty(),
                "Un registro completo no deberia tener violaciones");
    }

    @Test
    void accionVaciaEsInvalida() {
        RegistroAuditoriaEntity registro = registroValido();
        registro.setAccion("");
        var violaciones = validator.validate(registro);
        assertEquals(1, violaciones.size(), "Deberia haber 1 violacion por accion vacia");
        assertEquals("accion", violaciones.iterator().next().getPropertyPath().toString());
    }

    @Test
    void actorNuloEsInvalido() {
        RegistroAuditoriaEntity registro = registroValido();
        registro.setActor(null);
        var violaciones = validator.validate(registro);
        assertEquals(1, violaciones.size(), "Deberia haber 1 violacion por actor nulo");
        assertEquals("actor", violaciones.iterator().next().getPropertyPath().toString());
    }
}
