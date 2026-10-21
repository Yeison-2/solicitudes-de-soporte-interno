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
import com.jdc.web2026i.services.AccesoDenegadoException;
import com.jdc.web2026i.services.SolicitudService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PA-07: cubre CA-002-07 a CA-002-09 (matriz de estados, rechazos y autorizacion).
 */
class PA07EstadosTest extends PruebaBase {

    @Autowired
    private SolicitudRepository solicitudRepository;

    @Autowired
    private RegistroAuditoriaRepository registroAuditoriaRepository;

    @Autowired
    private SolicitudService solicitudService;

    private SolicitudEntity persistir(UsuarioEntity solicitante, String titulo, EstadoSolicitud estado,
                                      UsuarioEntity agente) {
        LocalDateTime ahora = LocalDateTime.now();
        SolicitudEntity solicitud = new SolicitudEntity();
        solicitud.setTitulo(titulo);
        solicitud.setDescripcion("Descripcion de prueba para las transiciones.");
        solicitud.setCategoria(CategoriaSolicitud.RED_CONECTIVIDAD);
        solicitud.setPrioridad(PrioridadSolicitud.MEDIA);
        solicitud.setEstado(estado);
        solicitud.setSolicitante(solicitante);
        solicitud.setAgenteAsignado(agente);
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

    private RegistroAuditoriaEntity registrarEvento(Long idSolicitud, UsuarioEntity actor, String anterior,
                                                    String nuevo, LocalDateTime fecha) {
        RegistroAuditoriaEntity registro = new RegistroAuditoriaEntity();
        registro.setTipoEntidad("Solicitud");
        registro.setEntidadId(idSolicitud);
        registro.setCampo("estado");
        registro.setValorAnterior(anterior);
        registro.setValorNuevo(nuevo);
        registro.setAccion("CAMBIO_ESTADO");
        registro.setActor(actor);
        registro.setFecha(fecha);
        return registroAuditoriaRepository.save(registro);
    }

    @Test
    void iniciarDesdeAsignadoDejaAuditoriaConAnteriorYNuevo() {
        UsuarioEntity solicitante = crearUsuarioDePrueba("t-pa07-sol-iniciar", Rol.SOLICITANTE);
        UsuarioEntity agente = crearUsuarioDePrueba("t-pa07-agente-iniciar", Rol.AGENTE);
        SolicitudEntity solicitud = persistir(solicitante, "t-pa07 iniciar", EstadoSolicitud.ASIGNADO, agente);

        solicitudService.cambiarEstado(solicitud.getIdSolicitud(), "iniciar", agente);

        assertEquals(EstadoSolicitud.EN_PROCESO, recargar(solicitud.getIdSolicitud()).getEstado());
        List<RegistroAuditoriaEntity> registros = auditoriaDe(solicitud.getIdSolicitud());
        assertEquals(1, registros.size());
        assertEquals("CAMBIO_ESTADO", registros.get(0).getAccion());
        assertEquals("estado", registros.get(0).getCampo());
        assertEquals("ASIGNADO", registros.get(0).getValorAnterior());
        assertEquals("EN_PROCESO", registros.get(0).getValorNuevo());
        assertEquals("t-pa07-agente-iniciar", registros.get(0).getActor().getNombreUsuario());
    }

    @Test
    void resolverDesdeEnProcesoDejaAuditoriaConAnteriorYNuevo() {
        UsuarioEntity solicitante = crearUsuarioDePrueba("t-pa07-sol-resolver", Rol.SOLICITANTE);
        UsuarioEntity agente = crearUsuarioDePrueba("t-pa07-agente-resolver", Rol.AGENTE);
        SolicitudEntity solicitud = persistir(solicitante, "t-pa07 resolver", EstadoSolicitud.EN_PROCESO, agente);

        solicitudService.cambiarEstado(solicitud.getIdSolicitud(), "resolver", agente);

        assertEquals(EstadoSolicitud.RESUELTO, recargar(solicitud.getIdSolicitud()).getEstado());
        List<RegistroAuditoriaEntity> registros = auditoriaDe(solicitud.getIdSolicitud());
        assertEquals(1, registros.size());
        assertEquals("EN_PROCESO", registros.get(0).getValorAnterior());
        assertEquals("RESUELTO", registros.get(0).getValorNuevo());
    }

    @Test
    void iniciarDesdeReabiertoTambienEsValido() {
        UsuarioEntity solicitante = crearUsuarioDePrueba("t-pa07-sol-reabierto", Rol.SOLICITANTE);
        UsuarioEntity agente = crearUsuarioDePrueba("t-pa07-agente-reabierto", Rol.AGENTE);
        SolicitudEntity solicitud = persistir(solicitante, "t-pa07 reabierto",
                EstadoSolicitud.REABIERTO, agente);

        solicitudService.cambiarEstado(solicitud.getIdSolicitud(), "iniciar", agente);

        assertEquals(EstadoSolicitud.EN_PROCESO, recargar(solicitud.getIdSolicitud()).getEstado());
        List<RegistroAuditoriaEntity> registros = auditoriaDe(solicitud.getIdSolicitud());
        assertEquals("REABIERTO", registros.get(0).getValorAnterior());
        assertEquals("EN_PROCESO", registros.get(0).getValorNuevo());
    }

    @Test
    void reasignarDesdeAsignadoYDesdeReabiertoLlevaAAsignado() {
        UsuarioEntity solicitante = crearUsuarioDePrueba("t-pa07-sol-reasignar", Rol.SOLICITANTE);
        UsuarioEntity agenteAnterior = crearUsuarioDePrueba("t-pa07-agente-anterior", Rol.AGENTE);
        UsuarioEntity agenteNuevo = crearUsuarioDePrueba("t-pa07-agente-nuevo", Rol.AGENTE);
        UsuarioEntity coordinador = crearUsuarioDePrueba("t-pa07-coord-reasignar", Rol.COORDINADOR);

        SolicitudEntity desdeAsignado = persistir(solicitante, "t-pa07 reasignar",
                EstadoSolicitud.ASIGNADO, agenteAnterior);
        solicitudService.asignar(desdeAsignado.getIdSolicitud(), agenteNuevo.getIdUsuario(), coordinador);

        SolicitudEntity actualizada = recargar(desdeAsignado.getIdSolicitud());
        assertEquals(EstadoSolicitud.ASIGNADO, actualizada.getEstado());
        assertEquals(agenteNuevo.getIdUsuario(), actualizada.getAgenteAsignado().getIdUsuario());
        List<RegistroAuditoriaEntity> registros = auditoriaDe(desdeAsignado.getIdSolicitud());
        assertEquals(1, registros.size());
        assertEquals("ASIGNACION", registros.get(0).getAccion());
        assertEquals("t-pa07-agente-anterior", registros.get(0).getValorAnterior());
        assertEquals("t-pa07-agente-nuevo", registros.get(0).getValorNuevo());

        SolicitudEntity desdeReabierto = persistir(solicitante, "t-pa07 reasignar reabierto",
                EstadoSolicitud.REABIERTO, agenteAnterior);
        solicitudService.asignar(desdeReabierto.getIdSolicitud(), agenteNuevo.getIdUsuario(), coordinador);
        assertEquals(EstadoSolicitud.ASIGNADO, recargar(desdeReabierto.getIdSolicitud()).getEstado());
    }

    @Test
    void resolverDesdeAsignadoSeRechazaSinEfectos() {
        UsuarioEntity solicitante = crearUsuarioDePrueba("t-pa07-sol-fuera-matriz", Rol.SOLICITANTE);
        UsuarioEntity agente = crearUsuarioDePrueba("t-pa07-agente-fuera-matriz", Rol.AGENTE);
        SolicitudEntity solicitud = persistir(solicitante, "t-pa07 fuera de matriz",
                EstadoSolicitud.ASIGNADO, agente);

        assertThrows(IllegalArgumentException.class,
                () -> solicitudService.cambiarEstado(solicitud.getIdSolicitud(), "resolver", agente));

        assertEquals(EstadoSolicitud.ASIGNADO, recargar(solicitud.getIdSolicitud()).getEstado());
        assertEquals(0, auditoriaDe(solicitud.getIdSolicitud()).size(),
                "Una transicion invalida no debe escribir auditoria");
    }

    @Test
    void iniciarDesdeEnProcesoSeRechazaSinEfectos() {
        UsuarioEntity solicitante = crearUsuarioDePrueba("t-pa07-sol-repetido", Rol.SOLICITANTE);
        UsuarioEntity agente = crearUsuarioDePrueba("t-pa07-agente-repetido", Rol.AGENTE);
        SolicitudEntity solicitud = persistir(solicitante, "t-pa07 iniciar repetido",
                EstadoSolicitud.EN_PROCESO, agente);

        assertThrows(IllegalArgumentException.class,
                () -> solicitudService.cambiarEstado(solicitud.getIdSolicitud(), "iniciar", agente));

        assertEquals(EstadoSolicitud.EN_PROCESO, recargar(solicitud.getIdSolicitud()).getEstado());
        assertEquals(0, auditoriaDe(solicitud.getIdSolicitud()).size());
    }

    @Test
    void elEstadoCerradoEsFinal() {
        UsuarioEntity solicitante = crearUsuarioDePrueba("t-pa07-sol-cerrado", Rol.SOLICITANTE);
        UsuarioEntity agente = crearUsuarioDePrueba("t-pa07-agente-cerrado", Rol.AGENTE);
        SolicitudEntity solicitud = persistir(solicitante, "t-pa07 estado final",
                EstadoSolicitud.CERRADO, agente);

        assertThrows(IllegalArgumentException.class,
                () -> solicitudService.cambiarEstado(solicitud.getIdSolicitud(), "iniciar", agente));

        assertEquals(EstadoSolicitud.CERRADO, recargar(solicitud.getIdSolicitud()).getEstado());
        assertEquals(0, auditoriaDe(solicitud.getIdSolicitud()).size());
    }

    @Test
    void elSolicitanteYElAgenteNoAsignadoNoCambianElEstado() {
        UsuarioEntity solicitante = crearUsuarioDePrueba("t-pa07-sol-noautorizado", Rol.SOLICITANTE);
        UsuarioEntity agente = crearUsuarioDePrueba("t-pa07-agente-asignado", Rol.AGENTE);
        crearUsuarioDePrueba("t-pa07-agente-otro", Rol.AGENTE);
        SolicitudEntity solicitud = persistir(solicitante, "t-pa07 autorizacion",
                EstadoSolicitud.ASIGNADO, agente);

        assertThrows(AccesoDenegadoException.class,
                () -> solicitudService.cambiarEstado(solicitud.getIdSolicitud(), "iniciar", solicitante));

        assertEquals(EstadoSolicitud.ASIGNADO, recargar(solicitud.getIdSolicitud()).getEstado());
        assertEquals(0, auditoriaDe(solicitud.getIdSolicitud()).size());

        UsuarioEntity agenteAjeno = usuarioRepository.findByNombreUsuario("t-pa07-agente-otro").orElseThrow();
        assertThrows(AccesoDenegadoException.class,
                () -> solicitudService.cambiarEstado(solicitud.getIdSolicitud(), "iniciar", agenteAjeno));

        assertEquals(EstadoSolicitud.ASIGNADO, recargar(solicitud.getIdSolicitud()).getEstado());
        assertEquals(0, auditoriaDe(solicitud.getIdSolicitud()).size());
    }

    @Test
    void elDetalleMuestraElHistorialEnOrdenDescendente() throws Exception {
        UsuarioEntity solicitante = crearUsuarioDePrueba("t-pa07-sol-historial", Rol.SOLICITANTE);
        UsuarioEntity actorAntiguo = crearUsuarioDePrueba("t-pa07-actor-antiguo", Rol.AGENTE);
        UsuarioEntity actorReciente = crearUsuarioDePrueba("t-pa07-actor-reciente", Rol.AGENTE);
        UsuarioEntity agenteAsignado = crearUsuarioDePrueba("t-pa07-agente-asignado-historial", Rol.AGENTE);
        crearUsuarioDePrueba("t-pa07-sol-ajeno-historial", Rol.SOLICITANTE);
        SolicitudEntity solicitud = persistir(solicitante, "t-pa07 historial",
                EstadoSolicitud.EN_PROCESO, agenteAsignado);
        LocalDateTime base = LocalDateTime.now();
        registrarEvento(solicitud.getIdSolicitud(), actorAntiguo, "NUEVO", "ASIGNADO", base.minusHours(2));
        registrarEvento(solicitud.getIdSolicitud(), actorReciente, "ASIGNADO", "EN_PROCESO", base.minusHours(1));

        List<RegistroAuditoriaEntity> historial = solicitudService.historial(solicitud.getIdSolicitud(), solicitante);
        assertEquals(2, historial.size(), "El historial debe incluir todos los eventos");
        assertEquals("t-pa07-actor-reciente", historial.get(0).getActor().getNombreUsuario(),
                "El evento mas reciente debe aparecer primero");

        MockHttpSession sesion = iniciarSesion("t-pa07-sol-historial", CLAVE_PRUEBA);
        String pagina = mockMvc.perform(get("/solicitudes/" + solicitud.getIdSolicitud()).session(sesion))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Historial")))
                .andExpect(content().string(containsString("CAMBIO_ESTADO")))
                .andReturn().getResponse().getContentAsString();

        int posicionReciente = pagina.indexOf("t-pa07-actor-reciente");
        int posicionAntigua = pagina.indexOf("t-pa07-actor-antiguo");
        assertTrue(posicionReciente >= 0 && posicionAntigua >= 0,
                "El historial debe mostrar a los actores por su identificador");
        assertTrue(posicionReciente < posicionAntigua,
                "El historial visible debe ir de la fecha mas reciente a la mas antigua");

        UsuarioEntity ajeno = usuarioRepository.findByNombreUsuario("t-pa07-sol-ajeno-historial").orElseThrow();
        assertThrows(AccesoDenegadoException.class,
                () -> solicitudService.historial(solicitud.getIdSolicitud(), ajeno));
    }
}
