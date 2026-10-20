package com.jdc.web2026i.controller;

import com.jdc.web2026i.entities.EstadoSolicitud;
import com.jdc.web2026i.entities.PrioridadSolicitud;
import com.jdc.web2026i.entities.Rol;
import com.jdc.web2026i.entities.SolicitudEntity;
import com.jdc.web2026i.entities.UsuarioEntity;
import com.jdc.web2026i.repository.SolicitudRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.mock.web.MockHttpSession;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * PA-02: cubre CA-001-05 a CA-001-07 (creacion valida, validaciones y campos forzados).
 */
class PA02CrearSolicitudTest extends PruebaBase {

    @Autowired
    private SolicitudRepository solicitudRepository;

    private List<SolicitudEntity> solicitudesDe(UsuarioEntity solicitante) {
        return solicitudRepository.findBySolicitante(solicitante, Sort.unsorted());
    }

    @Test
    void creacionValidaAsignaCamposDelSistema() throws Exception {
        UsuarioEntity solicitante = crearUsuarioDePrueba("t-pa02-crear", Rol.SOLICITANTE);
        MockHttpSession sesion = iniciarSesion("t-pa02-crear", CLAVE_PRUEBA);

        mockMvc.perform(post("/solicitudes/nueva")
                        .session(sesion)
                        .param("_csrf", tokenCsrfPara(sesion, "/solicitudes/nueva"))
                        .param("titulo", "Proyector sin imagen")
                        .param("descripcion", "El proyector de la sala 2 no muestra imagen.")
                        .param("categoria", "HARDWARE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/solicitudes"));

        List<SolicitudEntity> propias = solicitudesDe(solicitante);
        assertEquals(1, propias.size(), "El solicitante de prueba deberia tener exactamente una solicitud");
        SolicitudEntity creada = propias.get(0);
        assertNotNull(creada.getIdSolicitud());
        assertNotNull(creada.getFechaCreacion());
        assertEquals(EstadoSolicitud.NUEVO, creada.getEstado());
        assertEquals(PrioridadSolicitud.MEDIA, creada.getPrioridad());
        assertEquals(solicitante.getIdUsuario(), creada.getSolicitante().getIdUsuario());
    }

    @Test
    void camposObligatoriosFaltantesNoGuardanYConservanLaVista() throws Exception {
        UsuarioEntity solicitante = crearUsuarioDePrueba("t-pa02-faltantes", Rol.SOLICITANTE);
        MockHttpSession sesion = iniciarSesion("t-pa02-faltantes", CLAVE_PRUEBA);

        mockMvc.perform(post("/solicitudes/nueva")
                        .session(sesion)
                        .param("_csrf", tokenCsrfPara(sesion, "/solicitudes/nueva"))
                        .param("titulo", "")
                        .param("descripcion", "")
                        .param("categoria", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("solicitudes/nueva"))
                .andExpect(model().attributeHasFieldErrors("solicitud", "titulo", "descripcion", "categoria"));

        assertTrue(solicitudesDe(solicitante).isEmpty(),
                "No deberia guardarse ninguna solicitud para el solicitante de prueba");
    }

    @Test
    void elClienteNoPuedeFijarCamposDelSistema() throws Exception {
        UsuarioEntity solicitante = crearUsuarioDePrueba("t-pa02-forzados", Rol.SOLICITANTE);
        MockHttpSession sesion = iniciarSesion("t-pa02-forzados", CLAVE_PRUEBA);

        mockMvc.perform(post("/solicitudes/nueva")
                        .session(sesion)
                        .param("_csrf", tokenCsrfPara(sesion, "/solicitudes/nueva"))
                        .param("titulo", "Solicitud con datos forzados")
                        .param("descripcion", "Intento de fijar campos del sistema.")
                        .param("categoria", "SOFTWARE")
                        .param("idSolicitud", "9999")
                        .param("estado", "CERRADO")
                        .param("prioridad", "CRITICA")
                        .param("solicitante", "otro"))
                .andExpect(status().is3xxRedirection());

        List<SolicitudEntity> propias = solicitudesDe(solicitante);
        assertEquals(1, propias.size(), "El solicitante de prueba deberia tener exactamente una solicitud");
        SolicitudEntity creada = propias.get(0);
        assertNotEquals(9999L, creada.getIdSolicitud());
        assertEquals(EstadoSolicitud.NUEVO, creada.getEstado());
        assertEquals(PrioridadSolicitud.MEDIA, creada.getPrioridad());
        assertEquals(solicitante.getIdUsuario(), creada.getSolicitante().getIdUsuario());
    }
}
