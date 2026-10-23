package com.jdc.web2026i.controller;

import com.jdc.web2026i.entities.CategoriaSolicitud;
import com.jdc.web2026i.entities.EstadoSolicitud;
import com.jdc.web2026i.entities.PrioridadSolicitud;
import com.jdc.web2026i.entities.RegistroAuditoriaEntity;
import com.jdc.web2026i.entities.Rol;
import com.jdc.web2026i.entities.SolicitudEntity;
import com.jdc.web2026i.entities.UsuarioEntity;
import com.jdc.web2026i.repository.RegistroAuditoriaRepository;
import com.jdc.web2026i.repository.SolicitudRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PA-11: cubre CA-003-09 a CA-003-11 (acceso restringido, contenido y orden
 * del historial, e inmutabilidad).
 */
class PA11AuditoriaTest extends PruebaBase {

    @Autowired
    private SolicitudRepository solicitudRepository;

    @Autowired
    private RegistroAuditoriaRepository registroAuditoriaRepository;

    private SolicitudEntity persistir(UsuarioEntity solicitante) {
        LocalDateTime ahora = LocalDateTime.now().withNano(0);
        SolicitudEntity solicitud = new SolicitudEntity();
        solicitud.setTitulo("t-pa11 solicitud auditada");
        solicitud.setDescripcion("Descripcion de prueba para el historial.");
        solicitud.setCategoria(CategoriaSolicitud.SOFTWARE);
        solicitud.setPrioridad(PrioridadSolicitud.MEDIA);
        solicitud.setEstado(EstadoSolicitud.NUEVO);
        solicitud.setSolicitante(solicitante);
        solicitud.setFechaCreacion(ahora);
        solicitud.setFechaActualizacion(ahora);
        return solicitudRepository.save(solicitud);
    }

    private SolicitudEntity persistirAsignada(UsuarioEntity solicitante, UsuarioEntity agente) {
        SolicitudEntity solicitud = persistir(solicitante);
        solicitud.setEstado(EstadoSolicitud.ASIGNADO);
        solicitud.setAgenteAsignado(agente);
        solicitud.setFechaAsignacion(LocalDateTime.now().withNano(0));
        return solicitudRepository.save(solicitud);
    }

    private RegistroAuditoriaEntity registrar(Long entidadId, UsuarioEntity actor, LocalDateTime fecha,
                                              String campo, String anterior, String nuevo) {
        RegistroAuditoriaEntity registro = new RegistroAuditoriaEntity();
        registro.setTipoEntidad("Solicitud");
        registro.setEntidadId(entidadId);
        registro.setCampo(campo);
        registro.setValorAnterior(anterior);
        registro.setValorNuevo(nuevo);
        registro.setAccion("CAMBIO_ESTADO");
        registro.setActor(actor);
        registro.setFecha(fecha);
        return registroAuditoriaRepository.save(registro);
    }

    private String pedirHistorial(MockHttpSession sesion, Long solicitudId) throws Exception {
        return mockMvc.perform(get("/auditoria").session(sesion).param("solicitudId", solicitudId.toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private int contarOcurrencias(String texto, String fragmento) {
        int total = 0;
        int desde = texto.indexOf(fragmento);
        while (desde >= 0) {
            total++;
            desde = texto.indexOf(fragmento, desde + fragmento.length());
        }
        return total;
    }

    @Test
    void soloAuditorYCoordinadorAccedenAlHistorial() throws Exception {
        UsuarioEntity solicitante = crearUsuarioDePrueba("t-pa11-sol-acceso", Rol.SOLICITANTE);
        UsuarioEntity agente = crearUsuarioDePrueba("t-pa11-agente-acceso", Rol.AGENTE);
        crearUsuarioDePrueba("t-pa11-auditor-acceso", Rol.AUDITOR);
        crearUsuarioDePrueba("t-pa11-coord-acceso", Rol.COORDINADOR);
        persistir(solicitante);

        MockHttpSession sesionSolicitante = iniciarSesion("t-pa11-sol-acceso", CLAVE_PRUEBA);
        mockMvc.perform(get("/auditoria").session(sesionSolicitante))
                .andExpect(status().isForbidden());

        MockHttpSession sesionAgente = iniciarSesion("t-pa11-agente-acceso", CLAVE_PRUEBA);
        mockMvc.perform(get("/auditoria").session(sesionAgente))
                .andExpect(status().isForbidden());

        MockHttpSession sesionAuditor = iniciarSesion("t-pa11-auditor-acceso", CLAVE_PRUEBA);
        mockMvc.perform(get("/auditoria").session(sesionAuditor))
                .andExpect(status().isOk());

        MockHttpSession sesionCoordinador = iniciarSesion("t-pa11-coord-acceso", CLAVE_PRUEBA);
        mockMvc.perform(get("/auditoria").session(sesionCoordinador))
                .andExpect(status().isOk());
    }

    @Test
    void elHistorialMuestraFechaActorCodificadoCampoYValoresEnOrdenDescendente() throws Exception {
        UsuarioEntity solicitante = crearUsuarioDePrueba("t-pa11-sol-contenido", Rol.SOLICITANTE);
        crearUsuarioDePrueba("t-pa11-auditor-contenido", Rol.AUDITOR);
        UsuarioEntity actor = crearUsuarioDePrueba("t-pa11-actor-contenido", Rol.COORDINADOR);
        actor.setNombreCompleto("Nombre Completo Reservado");
        usuarioRepository.save(actor);
        SolicitudEntity solicitud = persistir(solicitante);
        LocalDateTime base = LocalDateTime.of(2019, 6, 1, 9, 0);
        registrar(solicitud.getIdSolicitud(), actor, base, "campo-antiguo", "valor-inicial-uno", "valor-medio-dos");
        registrar(solicitud.getIdSolicitud(), actor, base.plusHours(1), "campo-medio",
                "valor-medio-dos", "valor-final-tres");

        String html = pedirHistorial(iniciarSesion("t-pa11-auditor-contenido", CLAVE_PRUEBA),
                solicitud.getIdSolicitud());

        assertTrue(html.contains("2019-06-01 09:00"), "El historial debe mostrar la fecha del registro");
        assertTrue(html.contains("t-pa11-actor-contenido"),
                "El historial debe mostrar el identificador del actor");
        assertFalse(html.contains("Nombre Completo Reservado"),
                "El historial no debe exponer el nombre completo del actor");
        assertTrue(html.contains("campo-medio"));
        assertTrue(html.contains("valor-medio-dos"));
        assertTrue(html.contains("valor-final-tres"),
                "El historial debe mostrar el valor anterior y el nuevo");
        int posicionReciente = html.indexOf("valor-final-tres");
        int posicionAntigua = html.indexOf("campo-antiguo");
        assertTrue(posicionReciente >= 0 && posicionAntigua >= 0);
        assertTrue(posicionReciente < posicionAntigua,
                "El registro mas reciente debe aparecer primero");
    }

    @Test
    void elHistorialSePaginaDeCincuentaEnCincuenta() throws Exception {
        UsuarioEntity solicitante = crearUsuarioDePrueba("t-pa11-sol-pagina", Rol.SOLICITANTE);
        UsuarioEntity auditor = crearUsuarioDePrueba("t-pa11-auditor-pagina", Rol.AUDITOR);
        SolicitudEntity solicitud = persistir(solicitante);
        LocalDateTime base = LocalDateTime.of(2018, 5, 1, 8, 0);
        for (int indice = 1; indice <= 55; indice++) {
            registrar(solicitud.getIdSolicitud(), auditor, base.plusMinutes(indice),
                    "campo" + indice, "anterior" + indice, "marca t-pa11-pag " + indice);
        }
        MockHttpSession sesion = iniciarSesion("t-pa11-auditor-pagina", CLAVE_PRUEBA);

        String primeraPagina = pedirHistorial(sesion, solicitud.getIdSolicitud());
        assertEquals(50, contarOcurrencias(primeraPagina, "marca t-pa11-pag"),
                "La primera pagina debe listar 50 registros");
        assertTrue(primeraPagina.contains("Registros encontrados: 55"));

        String segundaPagina = mockMvc.perform(get("/auditoria").session(sesion)
                        .param("solicitudId", solicitud.getIdSolicitud().toString())
                        .param("page", "1"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertEquals(5, contarOcurrencias(segundaPagina, "marca t-pa11-pag"),
                "La segunda pagina debe listar los 5 registros restantes");
    }

    @Test
    void elHistorialNoAdmiteEscrituras() throws Exception {
        UsuarioEntity solicitante = crearUsuarioDePrueba("t-pa11-sol-escritura", Rol.SOLICITANTE);
        UsuarioEntity coordinador = crearUsuarioDePrueba("t-pa11-coord-escritura", Rol.COORDINADOR);
        SolicitudEntity solicitud = persistir(solicitante);
        registrar(solicitud.getIdSolicitud(), coordinador, LocalDateTime.of(2017, 7, 7, 7, 0),
                "estado", "NUEVO", "ASIGNADO");
        long totalAntes = registroAuditoriaRepository.count();
        MockHttpSession sesion = iniciarSesion("t-pa11-coord-escritura", CLAVE_PRUEBA);

        mockMvc.perform(post("/auditoria").session(sesion)
                        .param("_csrf", tokenCsrfPara(sesion, "/auditoria"))
                        .param("campo", "estado")
                        .param("valorNuevo", "BORRADO"))
                .andExpect(status().is4xxClientError());

        assertEquals(totalAntes, registroAuditoriaRepository.count(),
                "Una peticion de escritura no debe modificar el historial");
        List<RegistroAuditoriaEntity> registros =
                registroAuditoriaRepository.findByTipoEntidadAndEntidadId("Solicitud", solicitud.getIdSolicitud());
        assertEquals(1, registros.size());
        assertEquals("ASIGNADO", registros.get(0).getValorNuevo());
    }

    @Test
    void unaPaginaFueraDeRangoNoRompeElHistorial() throws Exception {
        UsuarioEntity solicitante = crearUsuarioDePrueba("t-pa11-sol-rango", Rol.SOLICITANTE);
        UsuarioEntity auditor = crearUsuarioDePrueba("t-pa11-auditor-rango", Rol.AUDITOR);
        SolicitudEntity solicitud = persistir(solicitante);
        LocalDateTime base = LocalDateTime.of(2016, 8, 1, 8, 0);
        registrar(solicitud.getIdSolicitud(), auditor, base, "campo", "antes", "marca t-pa11-rango uno");
        registrar(solicitud.getIdSolicitud(), auditor, base.plusMinutes(1), "campo",
                "antes", "marca t-pa11-rango dos");
        MockHttpSession sesion = iniciarSesion("t-pa11-auditor-rango", CLAVE_PRUEBA);

        mockMvc.perform(get("/auditoria").session(sesion)
                        .param("solicitudId", solicitud.getIdSolicitud().toString())
                        .param("page", "-1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("marca t-pa11-rango uno")))
                .andExpect(content().string(containsString("Registros encontrados: 2")));

        mockMvc.perform(get("/auditoria").session(sesion)
                        .param("solicitudId", solicitud.getIdSolicitud().toString())
                        .param("page", "99999"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("marca t-pa11-rango uno")))
                .andExpect(content().string(containsString("Registros encontrados: 2")));
    }

    @Test
    void unAuditorNoPuedeCambiarElEstadoDeUnaSolicitud() throws Exception {
        UsuarioEntity solicitante = crearUsuarioDePrueba("t-pa11-sol-auditor-escribe", Rol.SOLICITANTE);
        UsuarioEntity agente = crearUsuarioDePrueba("t-pa11-agente-auditor-escribe", Rol.AGENTE);
        crearUsuarioDePrueba("t-pa11-auditor-escribe", Rol.AUDITOR);
        SolicitudEntity solicitud = persistirAsignada(solicitante, agente);
        MockHttpSession sesion = iniciarSesion("t-pa11-auditor-escribe", CLAVE_PRUEBA);

        mockMvc.perform(post("/solicitudes/" + solicitud.getIdSolicitud() + "/estado").session(sesion)
                        .param("_csrf", tokenCsrfPara(sesion, "/solicitudes/" + solicitud.getIdSolicitud()))
                        .param("accion", "iniciar"))
                .andExpect(status().is4xxClientError());

        assertEquals(EstadoSolicitud.ASIGNADO,
                solicitudRepository.findById(solicitud.getIdSolicitud()).orElseThrow().getEstado(),
                "Un auditor no debe poder cambiar el estado");
        assertEquals(0, registroAuditoriaRepository
                        .findByTipoEntidadAndEntidadId("Solicitud", solicitud.getIdSolicitud()).size(),
                "El intento no debe escribir auditoria");
    }
}
