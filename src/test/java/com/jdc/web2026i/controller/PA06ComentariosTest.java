package com.jdc.web2026i.controller;

import com.jdc.web2026i.entities.CategoriaSolicitud;
import com.jdc.web2026i.entities.ComentarioEntity;
import com.jdc.web2026i.entities.EstadoSolicitud;
import com.jdc.web2026i.entities.PrioridadSolicitud;
import com.jdc.web2026i.entities.Rol;
import com.jdc.web2026i.entities.SolicitudEntity;
import com.jdc.web2026i.entities.UsuarioEntity;
import com.jdc.web2026i.repository.ComentarioRepository;
import com.jdc.web2026i.repository.SolicitudRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PA-06: cubre CA-002-04 a CA-002-06 (comentarios validos, inmutables y visibles por rol).
 */
class PA06ComentariosTest extends PruebaBase {

    private static final String TEXTO_VALIDO = "Se reviso el equipo y se reinstalo el controlador.";

    @Autowired
    private SolicitudRepository solicitudRepository;

    @Autowired
    private ComentarioRepository comentarioRepository;

    private SolicitudEntity persistir(UsuarioEntity solicitante, String titulo, EstadoSolicitud estado,
                                      UsuarioEntity agente) {
        LocalDateTime ahora = LocalDateTime.now();
        SolicitudEntity solicitud = new SolicitudEntity();
        solicitud.setTitulo(titulo);
        solicitud.setDescripcion("Descripcion de prueba para los comentarios.");
        solicitud.setCategoria(CategoriaSolicitud.HARDWARE);
        solicitud.setPrioridad(PrioridadSolicitud.MEDIA);
        solicitud.setEstado(estado);
        solicitud.setSolicitante(solicitante);
        solicitud.setAgenteAsignado(agente);
        solicitud.setFechaCreacion(ahora);
        solicitud.setFechaActualizacion(ahora);
        return solicitudRepository.save(solicitud);
    }

    private List<ComentarioEntity> comentariosDe(Long idSolicitud) {
        return comentarioRepository.findBySolicitudIdSolicitudOrderByFechaAsc(idSolicitud);
    }

    private void comentar(MockHttpSession sesion, Long idSolicitud, String texto) throws Exception {
        mockMvc.perform(post("/solicitudes/" + idSolicitud + "/comentarios")
                        .session(sesion)
                        .param("_csrf", tokenCsrfPara(sesion, "/solicitudes/" + idSolicitud))
                        .param("texto", texto))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/solicitudes/" + idSolicitud));
    }

    @Test
    void unComentarioVacioOSoloConEspaciosSeRechaza() throws Exception {
        UsuarioEntity solicitante = crearUsuarioDePrueba("t-pa06-sol-vacio", Rol.SOLICITANTE);
        UsuarioEntity agente = crearUsuarioDePrueba("t-pa06-agente-vacio", Rol.AGENTE);
        SolicitudEntity solicitud = persistir(solicitante, "t-pa06 comentario vacio",
                EstadoSolicitud.EN_PROCESO, agente);

        MockHttpSession sesion = iniciarSesion("t-pa06-agente-vacio", CLAVE_PRUEBA);
        comentar(sesion, solicitud.getIdSolicitud(), "");
        comentar(sesion, solicitud.getIdSolicitud(), "   ");

        assertEquals(0, comentariosDe(solicitud.getIdSolicitud()).size(),
                "Un comentario vacio no debe guardarse");
    }

    @Test
    void unComentarioValidoGuardaAutorYFechaDelServidor() throws Exception {
        UsuarioEntity solicitante = crearUsuarioDePrueba("t-pa06-sol-valido", Rol.SOLICITANTE);
        UsuarioEntity agente = crearUsuarioDePrueba("t-pa06-agente-valido", Rol.AGENTE);
        SolicitudEntity solicitud = persistir(solicitante, "t-pa06 comentario valido",
                EstadoSolicitud.EN_PROCESO, agente);
        LocalDateTime antes = LocalDateTime.now();

        MockHttpSession sesion = iniciarSesion("t-pa06-agente-valido", CLAVE_PRUEBA);
        comentar(sesion, solicitud.getIdSolicitud(), "  " + TEXTO_VALIDO + "  ");

        List<ComentarioEntity> registrados = comentariosDe(solicitud.getIdSolicitud());
        assertEquals(1, registrados.size());
        ComentarioEntity comentario = registrados.get(0);
        assertEquals(agente.getIdUsuario(), comentario.getAutor().getIdUsuario(),
                "El autor es el agente en sesion, no un dato del formulario");
        assertEquals(TEXTO_VALIDO, comentario.getTexto(), "El texto se guarda sin espacios sobrantes");
        assertNotNull(comentario.getFecha(), "La fecha la fija el servidor");
        assertFalse(comentario.getFecha().isBefore(antes), "La fecha no puede ser anterior a la peticion");
    }

    @Test
    void unAgenteNoAsignadoNoPuedeComentar() throws Exception {
        UsuarioEntity solicitante = crearUsuarioDePrueba("t-pa06-sol-ajeno", Rol.SOLICITANTE);
        UsuarioEntity agente = crearUsuarioDePrueba("t-pa06-agente-asignado", Rol.AGENTE);
        crearUsuarioDePrueba("t-pa06-agente-ajeno", Rol.AGENTE);
        SolicitudEntity solicitud = persistir(solicitante, "t-pa06 agente no asignado",
                EstadoSolicitud.EN_PROCESO, agente);

        MockHttpSession sesion = iniciarSesion("t-pa06-agente-ajeno", CLAVE_PRUEBA);
        comentar(sesion, solicitud.getIdSolicitud(), "Intento de comentario sin asignacion.");

        assertEquals(0, comentariosDe(solicitud.getIdSolicitud()).size(),
                "Solo el agente asignado puede comentar");
    }

    @Test
    void elSolicitanteNoPuedeComentar() throws Exception {
        UsuarioEntity solicitante = crearUsuarioDePrueba("t-pa06-sol-comenta", Rol.SOLICITANTE);
        UsuarioEntity agente = crearUsuarioDePrueba("t-pa06-agente-comenta", Rol.AGENTE);
        SolicitudEntity solicitud = persistir(solicitante, "t-pa06 solicitante comenta",
                EstadoSolicitud.EN_PROCESO, agente);

        MockHttpSession sesion = iniciarSesion("t-pa06-sol-comenta", CLAVE_PRUEBA);
        comentar(sesion, solicitud.getIdSolicitud(), "Intento de comentario del solicitante.");

        assertEquals(0, comentariosDe(solicitud.getIdSolicitud()).size(),
                "El solicitante no comenta en este sprint");
    }

    @Test
    void elComentarioEsVisibleParaDuenioAgenteYCoordinador() throws Exception {
        UsuarioEntity solicitante = crearUsuarioDePrueba("t-pa06-sol-visible", Rol.SOLICITANTE);
        UsuarioEntity agente = crearUsuarioDePrueba("t-pa06-agente-visible", Rol.AGENTE);
        crearUsuarioDePrueba("t-pa06-coord-visible", Rol.COORDINADOR);
        crearUsuarioDePrueba("t-pa06-sol-intruso", Rol.SOLICITANTE);
        SolicitudEntity solicitud = persistir(solicitante, "t-pa06 visibilidad",
                EstadoSolicitud.EN_PROCESO, agente);

        MockHttpSession sesionAgente = iniciarSesion("t-pa06-agente-visible", CLAVE_PRUEBA);
        comentar(sesionAgente, solicitud.getIdSolicitud(), TEXTO_VALIDO);

        String ruta = "/solicitudes/" + solicitud.getIdSolicitud();
        for (String usuario : List.of("t-pa06-sol-visible", "t-pa06-agente-visible", "t-pa06-coord-visible")) {
            MockHttpSession sesion = iniciarSesion(usuario, CLAVE_PRUEBA);
            mockMvc.perform(get(ruta).session(sesion))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString(TEXTO_VALIDO)));
        }

        MockHttpSession sesionIntruso = iniciarSesion("t-pa06-sol-intruso", CLAVE_PRUEBA);
        mockMvc.perform(get(ruta).session(sesionIntruso))
                .andExpect(status().isForbidden());
    }

    @Test
    void elCoordinadorNoPuedeComentar() throws Exception {
        UsuarioEntity solicitante = crearUsuarioDePrueba("t-pa06-sol-coord", Rol.SOLICITANTE);
        UsuarioEntity agente = crearUsuarioDePrueba("t-pa06-agente-coord", Rol.AGENTE);
        crearUsuarioDePrueba("t-pa06-coord-comenta", Rol.COORDINADOR);
        SolicitudEntity solicitud = persistir(solicitante, "t-pa06 coordinador comenta",
                EstadoSolicitud.EN_PROCESO, agente);

        MockHttpSession sesion = iniciarSesion("t-pa06-coord-comenta", CLAVE_PRUEBA);
        comentar(sesion, solicitud.getIdSolicitud(), "Intento de comentario del coordinador.");

        assertEquals(0, comentariosDe(solicitud.getIdSolicitud()).size(),
                "El coordinador no escribe comentarios de trabajo");
    }

    @Test
    void unComentarioSinTokenCsrfSeRechaza() throws Exception {
        UsuarioEntity solicitante = crearUsuarioDePrueba("t-pa06-sol-sincsrf", Rol.SOLICITANTE);
        UsuarioEntity agente = crearUsuarioDePrueba("t-pa06-agente-sincsrf", Rol.AGENTE);
        SolicitudEntity solicitud = persistir(solicitante, "t-pa06 comentario sin csrf",
                EstadoSolicitud.EN_PROCESO, agente);

        MockHttpSession sesion = iniciarSesion("t-pa06-agente-sincsrf", CLAVE_PRUEBA);
        mockMvc.perform(post("/solicitudes/" + solicitud.getIdSolicitud() + "/comentarios")
                        .session(sesion)
                        .param("texto", "Comentario enviado sin token."))
                .andExpect(status().isForbidden());

        assertEquals(0, comentariosDe(solicitud.getIdSolicitud()).size(),
                "Sin token CSRF no se guarda ningun comentario");
    }

    @Test
    void noExisteRutaParaEditarNiBorrarUnComentario() throws Exception {
        UsuarioEntity solicitante = crearUsuarioDePrueba("t-pa06-sol-inmutable", Rol.SOLICITANTE);
        UsuarioEntity agente = crearUsuarioDePrueba("t-pa06-agente-inmutable", Rol.AGENTE);
        SolicitudEntity solicitud = persistir(solicitante, "t-pa06 inmutable",
                EstadoSolicitud.EN_PROCESO, agente);

        MockHttpSession sesion = iniciarSesion("t-pa06-agente-inmutable", CLAVE_PRUEBA);
        comentar(sesion, solicitud.getIdSolicitud(), TEXTO_VALIDO);
        ComentarioEntity comentario = comentariosDe(solicitud.getIdSolicitud()).get(0);
        String rutaComentario = "/solicitudes/" + solicitud.getIdSolicitud()
                + "/comentarios/" + comentario.getIdComentario();

        mockMvc.perform(post(rutaComentario)
                        .session(sesion)
                        .param("_csrf", tokenCsrfPara(sesion, "/solicitudes/" + solicitud.getIdSolicitud()))
                        .param("texto", "Texto reemplazado"))
                .andExpect(status().is4xxClientError());

        mockMvc.perform(delete(rutaComentario)
                        .session(sesion)
                        .header("X-CSRF-TOKEN", tokenCsrfPara(sesion, "/solicitudes/" + solicitud.getIdSolicitud())))
                .andExpect(status().is4xxClientError());

        List<ComentarioEntity> restantes = comentariosDe(solicitud.getIdSolicitud());
        assertEquals(1, restantes.size(), "El comentario no se puede borrar");
        assertTrue(restantes.get(0).getTexto().equals(TEXTO_VALIDO),
                "El comentario no se puede editar");
    }
}
