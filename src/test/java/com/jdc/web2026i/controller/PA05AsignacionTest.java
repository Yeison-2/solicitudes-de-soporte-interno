package com.jdc.web2026i.controller;

import com.jdc.web2026i.entities.CategoriaSolicitud;
import com.jdc.web2026i.entities.EstadoSolicitud;
import com.jdc.web2026i.entities.NotificacionEntity;
import com.jdc.web2026i.entities.PrioridadSolicitud;
import com.jdc.web2026i.entities.RegistroAuditoriaEntity;
import com.jdc.web2026i.entities.Rol;
import com.jdc.web2026i.entities.SolicitudEntity;
import com.jdc.web2026i.entities.UsuarioEntity;
import com.jdc.web2026i.repository.NotificacionRepository;
import com.jdc.web2026i.repository.RegistroAuditoriaRepository;
import com.jdc.web2026i.repository.SolicitudRepository;
import com.jdc.web2026i.services.SolicitudService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PA-05: cubre CA-002-01 a CA-002-03 (asignacion valida e invalida y aviso al agente).
 */
class PA05AsignacionTest extends PruebaBase {

    @Autowired
    private SolicitudRepository solicitudRepository;

    @Autowired
    private RegistroAuditoriaRepository registroAuditoriaRepository;

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Autowired
    private SolicitudService solicitudService;

    private SolicitudEntity persistir(UsuarioEntity solicitante, String titulo) {
        LocalDateTime ahora = LocalDateTime.now();
        SolicitudEntity solicitud = new SolicitudEntity();
        solicitud.setTitulo(titulo);
        solicitud.setDescripcion("Descripcion de prueba para la asignacion.");
        solicitud.setCategoria(CategoriaSolicitud.SOFTWARE);
        solicitud.setPrioridad(PrioridadSolicitud.MEDIA);
        solicitud.setEstado(EstadoSolicitud.NUEVO);
        solicitud.setSolicitante(solicitante);
        solicitud.setFechaCreacion(ahora);
        solicitud.setFechaActualizacion(ahora);
        return solicitudRepository.save(solicitud);
    }

    private SolicitudEntity recargar(Long idSolicitud) {
        return solicitudRepository.findById(idSolicitud).orElseThrow();
    }

    private List<RegistroAuditoriaEntity> auditoriaDe(Long idSolicitud) {
        return registroAuditoriaRepository.findByTipoEntidadAndEntidadId("Solicitud", idSolicitud);
    }

    private List<NotificacionEntity> avisosDe(UsuarioEntity destinatario) {
        return notificacionRepository.findByDestinatarioIdUsuarioOrderByFechaDesc(destinatario.getIdUsuario());
    }

    @Test
    void asignacionValidaFijaAgenteFechaYAsignadoPor() {
        UsuarioEntity solicitante = crearUsuarioDePrueba("t-pa05-sol-valida", Rol.SOLICITANTE);
        UsuarioEntity agente = crearUsuarioDePrueba("t-pa05-agente-valida", Rol.AGENTE);
        UsuarioEntity coordinador = crearUsuarioDePrueba("t-pa05-coord-valida", Rol.COORDINADOR);
        SolicitudEntity solicitud = persistir(solicitante, "t-pa05 asignacion valida");

        solicitudService.asignar(solicitud.getIdSolicitud(), agente.getIdUsuario(), coordinador);

        SolicitudEntity actualizada = recargar(solicitud.getIdSolicitud());
        assertEquals(EstadoSolicitud.ASIGNADO, actualizada.getEstado());
        assertEquals(agente.getIdUsuario(), actualizada.getAgenteAsignado().getIdUsuario());
        assertEquals(coordinador.getIdUsuario(), actualizada.getAsignadoPor().getIdUsuario());
        assertNotNull(actualizada.getFechaAsignacion(), "La asignacion debe registrar la fecha");

        List<RegistroAuditoriaEntity> registros = auditoriaDe(solicitud.getIdSolicitud());
        assertEquals(1, registros.size(), "La asignacion debe dejar un unico registro de auditoria");
        RegistroAuditoriaEntity registro = registros.get(0);
        assertEquals("ASIGNACION", registro.getAccion());
        assertEquals("agente", registro.getCampo());
        assertNull(registro.getValorAnterior());
        assertEquals("t-pa05-agente-valida", registro.getValorNuevo());
        assertEquals("t-pa05-coord-valida", registro.getActor().getNombreUsuario());

        List<NotificacionEntity> avisos = avisosDe(agente);
        assertEquals(1, avisos.size(), "El agente debe recibir una notificacion");
        assertEquals(solicitud.getIdSolicitud(), avisos.get(0).getSolicitud().getIdSolicitud());
        assertFalse(avisos.get(0).isLeida(), "El aviso recien creado no esta leido");
    }

    @Test
    void noSeAsignaAUnAgenteInactivo() {
        UsuarioEntity solicitante = crearUsuarioDePrueba("t-pa05-sol-inactivo", Rol.SOLICITANTE);
        UsuarioEntity agente = crearUsuarioDePrueba("t-pa05-agente-inactivo", Rol.AGENTE);
        agente.setActivo(false);
        usuarioRepository.save(agente);
        UsuarioEntity coordinador = crearUsuarioDePrueba("t-pa05-coord-inactivo", Rol.COORDINADOR);
        SolicitudEntity solicitud = persistir(solicitante, "t-pa05 agente inactivo");

        assertThrows(IllegalArgumentException.class,
                () -> solicitudService.asignar(solicitud.getIdSolicitud(), agente.getIdUsuario(), coordinador));

        assertSinEfectos(solicitud.getIdSolicitud(), agente);
    }

    @Test
    void noSeAsignaAUnUsuarioSinRolDeAgente() {
        UsuarioEntity solicitante = crearUsuarioDePrueba("t-pa05-sol-noagente", Rol.SOLICITANTE);
        UsuarioEntity otroSolicitante = crearUsuarioDePrueba("t-pa05-noagente-destino", Rol.SOLICITANTE);
        UsuarioEntity coordinador = crearUsuarioDePrueba("t-pa05-coord-noagente", Rol.COORDINADOR);
        SolicitudEntity solicitud = persistir(solicitante, "t-pa05 destino sin rol");

        assertThrows(IllegalArgumentException.class,
                () -> solicitudService.asignar(solicitud.getIdSolicitud(), otroSolicitante.getIdUsuario(), coordinador));

        assertSinEfectos(solicitud.getIdSolicitud(), otroSolicitante);
    }

    @Test
    void noSeAsignaAUnUsuarioInexistente() {
        UsuarioEntity solicitante = crearUsuarioDePrueba("t-pa05-sol-inexistente", Rol.SOLICITANTE);
        UsuarioEntity coordinador = crearUsuarioDePrueba("t-pa05-coord-inexistente", Rol.COORDINADOR);
        SolicitudEntity solicitud = persistir(solicitante, "t-pa05 destino inexistente");

        assertThrows(IllegalArgumentException.class,
                () -> solicitudService.asignar(solicitud.getIdSolicitud(), 999_999L, coordinador));

        SolicitudEntity actualizada = recargar(solicitud.getIdSolicitud());
        assertEquals(EstadoSolicitud.NUEVO, actualizada.getEstado());
        assertNull(actualizada.getAgenteAsignado());
        assertNull(actualizada.getFechaAsignacion());
        assertEquals(0, auditoriaDe(solicitud.getIdSolicitud()).size(),
                "Un intento rechazado no debe escribir auditoria");
    }

    @Test
    void elCoordinadorNoSeAsignaASiMismo() {
        UsuarioEntity solicitante = crearUsuarioDePrueba("t-pa05-sol-propio", Rol.SOLICITANTE);
        UsuarioEntity coordinador = crearUsuarioDePrueba("t-pa05-coord-propio", Rol.COORDINADOR);
        SolicitudEntity solicitud = persistir(solicitante, "t-pa05 coordinador destino");

        assertThrows(IllegalArgumentException.class,
                () -> solicitudService.asignar(solicitud.getIdSolicitud(), coordinador.getIdUsuario(), coordinador));

        assertSinEfectos(solicitud.getIdSolicitud(), coordinador);
    }

    @Test
    void laNotificacionSeVeEnLaBandejaYQuedaLeida() throws Exception {
        UsuarioEntity solicitante = crearUsuarioDePrueba("t-pa05-sol-aviso", Rol.SOLICITANTE);
        UsuarioEntity agente = crearUsuarioDePrueba("t-pa05-agente-aviso", Rol.AGENTE);
        UsuarioEntity coordinador = crearUsuarioDePrueba("t-pa05-coord-aviso", Rol.COORDINADOR);
        SolicitudEntity solicitud = persistir(solicitante, "t-pa05 aviso al agente");

        solicitudService.asignar(solicitud.getIdSolicitud(), agente.getIdUsuario(), coordinador);

        MockHttpSession sesion = iniciarSesion("t-pa05-agente-aviso", CLAVE_PRUEBA);
        mockMvc.perform(get("/notificaciones").session(sesion))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "Se le asigno la solicitud #" + solicitud.getIdSolicitud())));

        List<NotificacionEntity> avisos = avisosDe(agente);
        assertEquals(1, avisos.size());
        assertTrue(avisos.get(0).isLeida(), "Abrir la bandeja debe marcar el aviso como leido");
    }

    @Test
    void unUsuarioQueNoEsCoordinadorNoPuedeAsignarPorHttp() throws Exception {
        UsuarioEntity solicitante = crearUsuarioDePrueba("t-pa05-sol-http", Rol.SOLICITANTE);
        UsuarioEntity agente = crearUsuarioDePrueba("t-pa05-agente-http", Rol.AGENTE);
        SolicitudEntity solicitud = persistir(solicitante, "t-pa05 asignacion por http");

        MockHttpSession sesion = iniciarSesion("t-pa05-agente-http", CLAVE_PRUEBA);
        mockMvc.perform(post("/coordinacion/solicitudes/" + solicitud.getIdSolicitud() + "/asignar")
                        .session(sesion)
                        .param("_csrf", tokenCsrfPara(sesion, "/solicitudes/mis-asignadas"))
                        .param("agenteId", String.valueOf(agente.getIdUsuario())))
                .andExpect(status().isForbidden());

        assertSinEfectos(solicitud.getIdSolicitud(), agente);
    }

    @Test
    void unAgenteNoAsignadoNoVeElDetalle() throws Exception {
        UsuarioEntity solicitante = crearUsuarioDePrueba("t-pa05-sol-detalle-ajeno", Rol.SOLICITANTE);
        crearUsuarioDePrueba("t-pa05-agente-detalle-ajeno", Rol.AGENTE);
        crearUsuarioDePrueba("t-pa05-agente-detalle-otro", Rol.AGENTE);
        SolicitudEntity solicitud = persistir(solicitante, "t-pa05 detalle de agente ajeno");

        MockHttpSession sesion = iniciarSesion("t-pa05-agente-detalle-otro", CLAVE_PRUEBA);
        mockMvc.perform(get("/solicitudes/" + solicitud.getIdSolicitud()).session(sesion))
                .andExpect(status().isForbidden());
    }

    private void assertSinEfectos(Long idSolicitud, UsuarioEntity destino) {
        SolicitudEntity actualizada = recargar(idSolicitud);
        assertEquals(EstadoSolicitud.NUEVO, actualizada.getEstado());
        assertNull(actualizada.getAgenteAsignado());
        assertNull(actualizada.getAsignadoPor());
        assertEquals(0, auditoriaDe(idSolicitud).size(), "Un intento rechazado no debe escribir auditoria");
        assertEquals(0, avisosDe(destino).size(), "Un intento rechazado no debe crear notificaciones");
    }
}
