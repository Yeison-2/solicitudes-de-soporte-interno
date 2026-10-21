package com.jdc.web2026i.controller;

import com.jdc.web2026i.DTO.SolicitudFormularioDTO;
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
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PA-08: cubre CA-002-10 a CA-002-14 (cierre, reapertura y cambio 1 de prioridad alta).
 */
class PA08CierreTest extends PruebaBase {

    private static final String MOTIVO = "El problema continua despues de la revision.";

    @Autowired
    private SolicitudRepository solicitudRepository;

    @Autowired
    private RegistroAuditoriaRepository registroAuditoriaRepository;

    @Autowired
    private SolicitudService solicitudService;

    @Autowired
    private EntityManager entityManager;

    private SolicitudEntity persistir(UsuarioEntity solicitante, String titulo, EstadoSolicitud estado,
                                      PrioridadSolicitud prioridad, UsuarioEntity agente) {
        LocalDateTime ahora = LocalDateTime.now();
        SolicitudEntity solicitud = new SolicitudEntity();
        solicitud.setTitulo(titulo);
        solicitud.setDescripcion("Descripcion de prueba para el cierre.");
        solicitud.setCategoria(CategoriaSolicitud.SOFTWARE);
        solicitud.setPrioridad(prioridad);
        solicitud.setEstado(estado);
        solicitud.setSolicitante(solicitante);
        solicitud.setAgenteAsignado(agente);
        solicitud.setFechaCreacion(ahora);
        solicitud.setFechaActualizacion(ahora);
        return solicitudRepository.save(solicitud);
    }

    private SolicitudFormularioDTO formulario(String titulo, PrioridadSolicitud prioridad, String justificacion,
                                              LocalDate fechaObjetivo) {
        SolicitudFormularioDTO formulario = new SolicitudFormularioDTO();
        formulario.setTitulo(titulo);
        formulario.setDescripcion("Descripcion de prueba para el cambio 1.");
        formulario.setCategoria(CategoriaSolicitud.SOFTWARE);
        formulario.setPrioridad(prioridad);
        formulario.setJustificacionPrioridad(justificacion);
        formulario.setFechaObjetivo(fechaObjetivo);
        return formulario;
    }

    private SolicitudEntity recargar(Long idSolicitud) {
        return solicitudRepository.findById(idSolicitud).orElseThrow();
    }

    private List<RegistroAuditoriaEntity> auditoriaDe(Long idSolicitud) {
        return registroAuditoriaRepository.findByTipoEntidadAndEntidadId("Solicitud", idSolicitud);
    }

    private List<SolicitudEntity> solicitudesDe(UsuarioEntity solicitante) {
        return solicitudRepository.findBySolicitante(solicitante, Sort.unsorted());
    }

    @Test
    void elDuenioConfirmaDesdeResueltoYQuedaAuditado() {
        UsuarioEntity solicitante = crearUsuarioDePrueba("t-pa08-sol-confirma", Rol.SOLICITANTE);
        UsuarioEntity agente = crearUsuarioDePrueba("t-pa08-agente-confirma", Rol.AGENTE);
        SolicitudEntity solicitud = persistir(solicitante, "t-pa08 confirmar",
                EstadoSolicitud.RESUELTO, PrioridadSolicitud.MEDIA, agente);

        solicitudService.confirmar(solicitud.getIdSolicitud(), solicitante);

        assertEquals(EstadoSolicitud.CERRADO, recargar(solicitud.getIdSolicitud()).getEstado());
        List<RegistroAuditoriaEntity> registros = auditoriaDe(solicitud.getIdSolicitud());
        assertEquals(1, registros.size());
        assertEquals("CONFIRMACION", registros.get(0).getAccion());
        assertEquals("RESUELTO", registros.get(0).getValorAnterior());
        assertEquals("CERRADO", registros.get(0).getValorNuevo());
        assertEquals("t-pa08-sol-confirma", registros.get(0).getActor().getNombreUsuario());
    }

    @Test
    void reabrirSinMotivoSeRechazaSinEfectos() {
        UsuarioEntity solicitante = crearUsuarioDePrueba("t-pa08-sol-sinmotivo", Rol.SOLICITANTE);
        UsuarioEntity agente = crearUsuarioDePrueba("t-pa08-agente-sinmotivo", Rol.AGENTE);
        SolicitudEntity solicitud = persistir(solicitante, "t-pa08 reabrir sin motivo",
                EstadoSolicitud.RESUELTO, PrioridadSolicitud.MEDIA, agente);

        assertThrows(IllegalArgumentException.class,
                () -> solicitudService.reabrir(solicitud.getIdSolicitud(), "   ", solicitante));

        SolicitudEntity actualizada = recargar(solicitud.getIdSolicitud());
        assertEquals(EstadoSolicitud.RESUELTO, actualizada.getEstado());
        assertNull(actualizada.getMotivoReapertura());
        assertEquals(0, auditoriaDe(solicitud.getIdSolicitud()).size(),
                "Una reapertura rechazada no debe escribir auditoria");
    }

    @Test
    void reabrirConMotivoQuedaAuditadoConElMotivo() {
        UsuarioEntity solicitante = crearUsuarioDePrueba("t-pa08-sol-reabre", Rol.SOLICITANTE);
        UsuarioEntity agente = crearUsuarioDePrueba("t-pa08-agente-reabre", Rol.AGENTE);
        SolicitudEntity solicitud = persistir(solicitante, "t-pa08 reabrir",
                EstadoSolicitud.RESUELTO, PrioridadSolicitud.MEDIA, agente);

        solicitudService.reabrir(solicitud.getIdSolicitud(), MOTIVO, solicitante);

        SolicitudEntity actualizada = recargar(solicitud.getIdSolicitud());
        assertEquals(EstadoSolicitud.REABIERTO, actualizada.getEstado());
        assertEquals(MOTIVO, actualizada.getMotivoReapertura());
        List<RegistroAuditoriaEntity> registros = auditoriaDe(solicitud.getIdSolicitud());
        assertEquals(2, registros.size(), "La reapertura deja rastro del estado y del motivo");
        RegistroAuditoriaEntity cambioEstado = registros.stream()
                .filter(registro -> "estado".equals(registro.getCampo()))
                .findFirst().orElseThrow();
        assertEquals("REAPERTURA", cambioEstado.getAccion());
        assertEquals("RESUELTO", cambioEstado.getValorAnterior());
        assertEquals("REABIERTO", cambioEstado.getValorNuevo());
        RegistroAuditoriaEntity cambioMotivo = registros.stream()
                .filter(registro -> "motivo_reapertura".equals(registro.getCampo()))
                .findFirst().orElseThrow();
        assertEquals("REAPERTURA", cambioMotivo.getAccion());
        assertNull(cambioMotivo.getValorAnterior());
        assertEquals(MOTIVO, cambioMotivo.getValorNuevo());
    }

    @Test
    void soloElDuenioConfirmaOReabre() {
        UsuarioEntity solicitante = crearUsuarioDePrueba("t-pa08-sol-duenio", Rol.SOLICITANTE);
        UsuarioEntity otroSolicitante = crearUsuarioDePrueba("t-pa08-sol-ajeno", Rol.SOLICITANTE);
        UsuarioEntity agente = crearUsuarioDePrueba("t-pa08-agente-duenio", Rol.AGENTE);
        UsuarioEntity coordinador = crearUsuarioDePrueba("t-pa08-coord-duenio", Rol.COORDINADOR);
        SolicitudEntity solicitud = persistir(solicitante, "t-pa08 autorizacion cierre",
                EstadoSolicitud.RESUELTO, PrioridadSolicitud.MEDIA, agente);

        assertThrows(AccesoDenegadoException.class,
                () -> solicitudService.confirmar(solicitud.getIdSolicitud(), otroSolicitante));
        assertThrows(AccesoDenegadoException.class,
                () -> solicitudService.confirmar(solicitud.getIdSolicitud(), agente));
        assertThrows(AccesoDenegadoException.class,
                () -> solicitudService.reabrir(solicitud.getIdSolicitud(), MOTIVO, coordinador));
        assertThrows(AccesoDenegadoException.class,
                () -> solicitudService.reabrir(solicitud.getIdSolicitud(), MOTIVO, otroSolicitante));

        assertEquals(EstadoSolicitud.RESUELTO, recargar(solicitud.getIdSolicitud()).getEstado());
        assertEquals(0, auditoriaDe(solicitud.getIdSolicitud()).size(),
                "Ningun intento no autorizado debe escribir auditoria");
    }

    @Test
    void crearAltaSinJustificacionOConFechaPasadaNoGuarda() {
        UsuarioEntity solicitante = crearUsuarioDePrueba("t-pa08-sol-alta-incompleta", Rol.SOLICITANTE);

        assertThrows(IllegalArgumentException.class, () -> solicitudService.crear(
                formulario("t-pa08 alta sin datos", PrioridadSolicitud.ALTA, null, null), solicitante));
        assertThrows(IllegalArgumentException.class, () -> solicitudService.crear(
                formulario("t-pa08 alta sin fecha", PrioridadSolicitud.ALTA, "Falla critica", null), solicitante));
        assertThrows(IllegalArgumentException.class, () -> solicitudService.crear(
                formulario("t-pa08 alta fecha pasada", PrioridadSolicitud.ALTA, "Falla critica",
                        LocalDate.now().minusDays(1)), solicitante));

        assertTrue(solicitudesDe(solicitante).isEmpty(),
                "Sin justificacion y fecha futura no se guarda ninguna solicitud");
    }

    @Test
    void crearAltaConJustificacionYFechaGuardaAmbosCampos() {
        UsuarioEntity solicitante = crearUsuarioDePrueba("t-pa08-sol-alta-completa", Rol.SOLICITANTE);
        LocalDate objetivo = LocalDate.now().plusDays(2);

        SolicitudEntity creada = solicitudService.crear(
                formulario("t-pa08 alta completa", PrioridadSolicitud.ALTA, "Afecta a toda el area", objetivo),
                solicitante);

        SolicitudEntity guardada = recargar(creada.getIdSolicitud());
        assertEquals(PrioridadSolicitud.ALTA, guardada.getPrioridad());
        assertEquals("Afecta a toda el area", guardada.getJustificacionPrioridad());
        assertEquals(objetivo, guardada.getFechaObjetivo());
        assertNotNull(guardada.getFechaCreacion());
    }

    @Test
    void priorizarAltaSinJustificacionSeRechaza() {
        UsuarioEntity solicitante = crearUsuarioDePrueba("t-pa08-sol-priorizar-sinjust", Rol.SOLICITANTE);
        UsuarioEntity coordinador = crearUsuarioDePrueba("t-pa08-coord-priorizar-sinjust", Rol.COORDINADOR);
        SolicitudEntity solicitud = persistir(solicitante, "t-pa08 priorizar sin justificacion",
                EstadoSolicitud.NUEVO, PrioridadSolicitud.MEDIA, null);

        assertThrows(IllegalArgumentException.class, () -> solicitudService.cambiarPrioridad(
                solicitud.getIdSolicitud(), PrioridadSolicitud.ALTA, "   ", LocalDate.now().plusDays(3), coordinador));

        entityManager.clear();
        assertPrioridadSinCambios(solicitud.getIdSolicitud());
    }

    @Test
    void priorizarAltaSinFechaObjetivoSeRechaza() {
        UsuarioEntity solicitante = crearUsuarioDePrueba("t-pa08-sol-priorizar-sinfecha", Rol.SOLICITANTE);
        UsuarioEntity coordinador = crearUsuarioDePrueba("t-pa08-coord-priorizar-sinfecha", Rol.COORDINADOR);
        SolicitudEntity solicitud = persistir(solicitante, "t-pa08 priorizar sin fecha",
                EstadoSolicitud.NUEVO, PrioridadSolicitud.MEDIA, null);

        assertThrows(IllegalArgumentException.class, () -> solicitudService.cambiarPrioridad(
                solicitud.getIdSolicitud(), PrioridadSolicitud.ALTA, "Motivo valido", null, coordinador));

        entityManager.clear();
        assertPrioridadSinCambios(solicitud.getIdSolicitud());
    }

    @Test
    void priorizarAltaConFechaPasadaSeRechaza() {
        UsuarioEntity solicitante = crearUsuarioDePrueba("t-pa08-sol-priorizar-pasada", Rol.SOLICITANTE);
        UsuarioEntity coordinador = crearUsuarioDePrueba("t-pa08-coord-priorizar-pasada", Rol.COORDINADOR);
        SolicitudEntity solicitud = persistir(solicitante, "t-pa08 priorizar fecha pasada",
                EstadoSolicitud.NUEVO, PrioridadSolicitud.MEDIA, null);

        assertThrows(IllegalArgumentException.class, () -> solicitudService.cambiarPrioridad(
                solicitud.getIdSolicitud(), PrioridadSolicitud.ALTA, "Motivo valido",
                LocalDate.now().minusDays(1), coordinador));

        entityManager.clear();
        assertPrioridadSinCambios(solicitud.getIdSolicitud());
    }

    private void assertPrioridadSinCambios(Long idSolicitud) {
        SolicitudEntity actualizada = recargar(idSolicitud);
        assertEquals(PrioridadSolicitud.MEDIA, actualizada.getPrioridad());
        assertNull(actualizada.getJustificacionPrioridad());
        assertNull(actualizada.getFechaObjetivo());
        assertEquals(0, auditoriaDe(idSolicitud).size(),
                "Una priorizacion rechazada no debe escribir auditoria");
    }

    @Test
    void priorizarAltaConJustificacionYFechaGuardaYAudita() {
        UsuarioEntity solicitante = crearUsuarioDePrueba("t-pa08-sol-priorizar-completa", Rol.SOLICITANTE);
        UsuarioEntity coordinador = crearUsuarioDePrueba("t-pa08-coord-priorizar-completa", Rol.COORDINADOR);
        SolicitudEntity solicitud = persistir(solicitante, "t-pa08 priorizar completa",
                EstadoSolicitud.NUEVO, PrioridadSolicitud.MEDIA, null);
        LocalDate futuro = LocalDate.now().plusDays(3);

        solicitudService.cambiarPrioridad(solicitud.getIdSolicitud(), PrioridadSolicitud.ALTA,
                "Atencion urgente solicitada", futuro, coordinador);

        SolicitudEntity actualizada = recargar(solicitud.getIdSolicitud());
        assertEquals(PrioridadSolicitud.ALTA, actualizada.getPrioridad());
        assertEquals("Atencion urgente solicitada", actualizada.getJustificacionPrioridad());
        assertEquals(futuro, actualizada.getFechaObjetivo());
        List<RegistroAuditoriaEntity> registros = auditoriaDe(solicitud.getIdSolicitud());
        assertEquals(1, registros.size());
        assertEquals("CAMBIO_PRIORIDAD", registros.get(0).getAccion());
        assertEquals("MEDIA", registros.get(0).getValorAnterior());
        assertEquals("ALTA", registros.get(0).getValorNuevo());
    }

    @Test
    void bajarDeAltaLimpiaJustificacionYFechaYAudita() {
        UsuarioEntity solicitante = crearUsuarioDePrueba("t-pa08-sol-bajar", Rol.SOLICITANTE);
        UsuarioEntity coordinador = crearUsuarioDePrueba("t-pa08-coord-bajar", Rol.COORDINADOR);
        SolicitudEntity solicitud = persistir(solicitante, "t-pa08 bajar prioridad",
                EstadoSolicitud.NUEVO, PrioridadSolicitud.ALTA, null);
        solicitud.setJustificacionPrioridad("Justificacion original");
        solicitud.setFechaObjetivo(LocalDate.now().plusDays(1));
        solicitudRepository.save(solicitud);

        solicitudService.cambiarPrioridad(solicitud.getIdSolicitud(), PrioridadSolicitud.MEDIA,
                null, null, coordinador);

        SolicitudEntity actualizada = recargar(solicitud.getIdSolicitud());
        assertEquals(PrioridadSolicitud.MEDIA, actualizada.getPrioridad());
        assertNull(actualizada.getJustificacionPrioridad(), "Al bajar de ALTA se limpia la justificacion");
        assertNull(actualizada.getFechaObjetivo(), "Al bajar de ALTA se limpia la fecha objetivo");
        List<RegistroAuditoriaEntity> registros = auditoriaDe(solicitud.getIdSolicitud());
        assertEquals(1, registros.size());
        assertEquals("CAMBIO_PRIORIDAD", registros.get(0).getAccion());
        assertEquals("ALTA", registros.get(0).getValorAnterior());
        assertEquals("MEDIA", registros.get(0).getValorNuevo());
    }
}
