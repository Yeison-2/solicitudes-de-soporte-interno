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
import com.jdc.web2026i.services.SolicitudService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PA-04: cubre CA-001-11 a CA-001-13 (autorizacion, auditoria y ordenamientos).
 * Cada prueba usa usuarios propios y acota sus aserciones a los datos que creo.
 */
class PA04PriorizarTest extends PruebaBase {

    @Autowired
    private SolicitudRepository solicitudRepository;

    @Autowired
    private RegistroAuditoriaRepository registroAuditoriaRepository;

    @Autowired
    private SolicitudService solicitudService;

    private SolicitudEntity persistir(UsuarioEntity solicitante, String titulo, PrioridadSolicitud prioridad,
                                      EstadoSolicitud estado, LocalDateTime creacion) {
        SolicitudEntity solicitud = new SolicitudEntity();
        solicitud.setTitulo(titulo);
        solicitud.setDescripcion("Descripcion de prueba para priorizacion.");
        solicitud.setCategoria(CategoriaSolicitud.RED_CONECTIVIDAD);
        solicitud.setPrioridad(prioridad);
        solicitud.setEstado(estado);
        solicitud.setSolicitante(solicitante);
        solicitud.setFechaCreacion(creacion);
        solicitud.setFechaActualizacion(creacion);
        return solicitudRepository.save(solicitud);
    }

    private PrioridadSolicitud prioridadActual(Long idSolicitud) {
        return solicitudRepository.findById(idSolicitud).orElseThrow().getPrioridad();
    }

    private List<RegistroAuditoriaEntity> auditoriaDe(Long idSolicitud) {
        return registroAuditoriaRepository.findByTipoEntidadAndEntidadId("Solicitud", idSolicitud);
    }

    /** Titulos renderizados de la bandeja, limitados a las solicitudes creadas por la prueba. */
    private List<String> titulosOrdenados(List<SolicitudEntity> bandeja, List<Long> idsCreados) {
        Set<Long> propios = new HashSet<>(idsCreados);
        return bandeja.stream()
                .filter(solicitud -> propios.contains(solicitud.getIdSolicitud()))
                .map(SolicitudEntity::getTitulo)
                .toList();
    }

    @Test
    void soloElCoordinadorCambiaLaPrioridad() throws Exception {
        UsuarioEntity solicitante = crearUsuarioDePrueba("t-pa04-sol-auto", Rol.SOLICITANTE);
        SolicitudEntity solicitud = persistir(solicitante, "t-pa04 autorizacion",
                PrioridadSolicitud.MEDIA, EstadoSolicitud.NUEVO, LocalDateTime.now());

        MockHttpSession sesion = iniciarSesion("t-pa04-sol-auto", CLAVE_PRUEBA);
        mockMvc.perform(post("/coordinacion/solicitudes/" + solicitud.getIdSolicitud() + "/prioridad")
                        .session(sesion)
                        .param("_csrf", tokenCsrfPara(sesion, "/solicitudes"))
                        .param("prioridad", "ALTA"))
                .andExpect(status().isForbidden());

        assertEquals(PrioridadSolicitud.MEDIA, prioridadActual(solicitud.getIdSolicitud()));
    }

    @Test
    void unaPrioridadFueraDelEnumSeRechazaSinGuardar() throws Exception {
        UsuarioEntity solicitante = crearUsuarioDePrueba("t-pa04-sol-invalida", Rol.SOLICITANTE);
        crearUsuarioDePrueba("t-pa04-coord-invalida", Rol.COORDINADOR);
        SolicitudEntity solicitud = persistir(solicitante, "t-pa04 valor invalido",
                PrioridadSolicitud.MEDIA, EstadoSolicitud.NUEVO, LocalDateTime.now());

        MockHttpSession sesion = iniciarSesion("t-pa04-coord-invalida", CLAVE_PRUEBA);
        mockMvc.perform(post("/coordinacion/solicitudes/" + solicitud.getIdSolicitud() + "/prioridad")
                        .session(sesion)
                        .param("_csrf", tokenCsrfPara(sesion, "/coordinacion/solicitudes"))
                        .param("prioridad", "URGENTE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/coordinacion/solicitudes"));

        assertEquals(PrioridadSolicitud.MEDIA, prioridadActual(solicitud.getIdSolicitud()));
        assertEquals(0, auditoriaDe(solicitud.getIdSolicitud()).size(),
                "No deberia registrarse auditoria para la solicitud de prueba");
    }

    @Test
    void unCambioExitosoRegistraAuditoria() throws Exception {
        UsuarioEntity solicitante = crearUsuarioDePrueba("t-pa04-sol-audita", Rol.SOLICITANTE);
        crearUsuarioDePrueba("t-pa04-coord-audita", Rol.COORDINADOR);
        SolicitudEntity solicitud = persistir(solicitante, "t-pa04 auditoria",
                PrioridadSolicitud.MEDIA, EstadoSolicitud.NUEVO, LocalDateTime.now());

        MockHttpSession sesion = iniciarSesion("t-pa04-coord-audita", CLAVE_PRUEBA);
        mockMvc.perform(post("/coordinacion/solicitudes/" + solicitud.getIdSolicitud() + "/prioridad")
                        .session(sesion)
                        .param("_csrf", tokenCsrfPara(sesion, "/coordinacion/solicitudes"))
                        .param("prioridad", "CRITICA"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/coordinacion/solicitudes"));

        assertEquals(PrioridadSolicitud.CRITICA, prioridadActual(solicitud.getIdSolicitud()));

        List<RegistroAuditoriaEntity> registros = auditoriaDe(solicitud.getIdSolicitud());
        assertEquals(1, registros.size(), "Deberia existir un unico registro de auditoria de la solicitud de prueba");
        RegistroAuditoriaEntity registro = registros.get(0);
        assertEquals("MEDIA", registro.getValorAnterior());
        assertEquals("CRITICA", registro.getValorNuevo());
        assertEquals("prioridad", registro.getCampo());
        assertEquals("CAMBIO_PRIORIDAD", registro.getAccion());
        assertEquals("t-pa04-coord-audita", registro.getActor().getNombreUsuario());
        assertNotNull(registro.getFecha());
    }

    @Test
    void sinCambioDeValorNoRegistraAuditoria() throws Exception {
        UsuarioEntity solicitante = crearUsuarioDePrueba("t-pa04-sol-sincambio", Rol.SOLICITANTE);
        crearUsuarioDePrueba("t-pa04-coord-sincambio", Rol.COORDINADOR);
        SolicitudEntity solicitud = persistir(solicitante, "t-pa04 sin cambios",
                PrioridadSolicitud.ALTA, EstadoSolicitud.NUEVO, LocalDateTime.now());

        MockHttpSession sesion = iniciarSesion("t-pa04-coord-sincambio", CLAVE_PRUEBA);
        mockMvc.perform(post("/coordinacion/solicitudes/" + solicitud.getIdSolicitud() + "/prioridad")
                        .session(sesion)
                        .param("_csrf", tokenCsrfPara(sesion, "/coordinacion/solicitudes"))
                        .param("prioridad", "ALTA"))
                .andExpect(status().is3xxRedirection());

        assertEquals(PrioridadSolicitud.ALTA, prioridadActual(solicitud.getIdSolicitud()));
        assertEquals(0, auditoriaDe(solicitud.getIdSolicitud()).size(),
                "Un cambio igual no deberia auditarse");
    }

    @Test
    void laBandejaOrdenaPorPrioridad() {
        UsuarioEntity solicitante = crearUsuarioDePrueba("t-pa04-sol-prio", Rol.SOLICITANTE);
        LocalDateTime base = LocalDateTime.now();
        SolicitudEntity baja = persistir(solicitante, "t-pa04 prio baja", PrioridadSolicitud.BAJA, EstadoSolicitud.NUEVO, base.minusMinutes(4));
        SolicitudEntity media = persistir(solicitante, "t-pa04 prio media", PrioridadSolicitud.MEDIA, EstadoSolicitud.NUEVO, base.minusMinutes(3));
        SolicitudEntity alta = persistir(solicitante, "t-pa04 prio alta", PrioridadSolicitud.ALTA, EstadoSolicitud.NUEVO, base.minusMinutes(2));
        SolicitudEntity critica = persistir(solicitante, "t-pa04 prio critica", PrioridadSolicitud.CRITICA, EstadoSolicitud.NUEVO, base.minusMinutes(1));

        List<Long> ids = List.of(baja.getIdSolicitud(), media.getIdSolicitud(), alta.getIdSolicitud(), critica.getIdSolicitud());

        List<String> ordenadas = titulosOrdenados(solicitudService.listarParaCoordinacion("prioridad"), ids);

        assertEquals(List.of("t-pa04 prio critica", "t-pa04 prio alta", "t-pa04 prio media", "t-pa04 prio baja"),
                ordenadas);
    }

    @Test
    void laBandejaOrdenaPorEstadoYPorFecha() {
        UsuarioEntity solicitante = crearUsuarioDePrueba("t-pa04-sol-estado", Rol.SOLICITANTE);
        LocalDateTime base = LocalDateTime.now();
        SolicitudEntity nueva = persistir(solicitante, "t-pa04 estado nueva", PrioridadSolicitud.MEDIA, EstadoSolicitud.NUEVO, base.minusMinutes(2));
        SolicitudEntity resuelta = persistir(solicitante, "t-pa04 estado resuelta", PrioridadSolicitud.MEDIA, EstadoSolicitud.RESUELTO, base.minusMinutes(1));
        List<Long> ids = List.of(nueva.getIdSolicitud(), resuelta.getIdSolicitud());

        List<String> porEstado = titulosOrdenados(solicitudService.listarParaCoordinacion("estado"), ids);
        assertEquals(List.of("t-pa04 estado nueva", "t-pa04 estado resuelta"), porEstado);

        List<String> porFecha = titulosOrdenados(solicitudService.listarParaCoordinacion("fecha"), ids);
        assertEquals(List.of("t-pa04 estado resuelta", "t-pa04 estado nueva"), porFecha);
    }

    @Test
    void unOrdenDesconocidoUsaLaFechaDescendente() {
        UsuarioEntity solicitante = crearUsuarioDePrueba("t-pa04-sol-desconocido", Rol.SOLICITANTE);
        LocalDateTime base = LocalDateTime.now();
        SolicitudEntity antigua = persistir(solicitante, "t-pa04 orden antigua", PrioridadSolicitud.MEDIA, EstadoSolicitud.NUEVO, base.minusDays(1));
        SolicitudEntity reciente = persistir(solicitante, "t-pa04 orden reciente", PrioridadSolicitud.BAJA, EstadoSolicitud.NUEVO, base);
        List<Long> ids = List.of(antigua.getIdSolicitud(), reciente.getIdSolicitud());

        List<String> ordenadas = titulosOrdenados(solicitudService.listarParaCoordinacion("inventado"), ids);

        assertEquals(List.of("t-pa04 orden reciente", "t-pa04 orden antigua"), ordenadas);
    }
}
