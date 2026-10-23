package com.jdc.web2026i.controller;

import com.jdc.web2026i.DTO.IndicadoresDTO;
import com.jdc.web2026i.entities.CategoriaSolicitud;
import com.jdc.web2026i.entities.EstadoSolicitud;
import com.jdc.web2026i.entities.PrioridadSolicitud;
import com.jdc.web2026i.entities.RegistroAuditoriaEntity;
import com.jdc.web2026i.entities.Rol;
import com.jdc.web2026i.entities.SolicitudEntity;
import com.jdc.web2026i.entities.UsuarioEntity;
import com.jdc.web2026i.repository.RegistroAuditoriaRepository;
import com.jdc.web2026i.repository.SolicitudRepository;
import com.jdc.web2026i.services.IndicadorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PA-10: cubre CA-003-05 a CA-003-08 (volumen con ceros, mediana, filtros
 * reproducibles y ausencia de agrupacion por persona).
 */
class PA10IndicadoresTest extends PruebaBase {

    @Autowired
    private IndicadorService indicadorService;

    @Autowired
    private SolicitudRepository solicitudRepository;

    @Autowired
    private RegistroAuditoriaRepository registroAuditoriaRepository;

    private SolicitudEntity persistir(UsuarioEntity solicitante, String titulo, LocalDateTime creacion,
                                      EstadoSolicitud estado, PrioridadSolicitud prioridad,
                                      CategoriaSolicitud categoria, UsuarioEntity agente) {
        SolicitudEntity solicitud = new SolicitudEntity();
        solicitud.setTitulo(titulo);
        solicitud.setDescripcion("Descripcion de prueba para los indicadores.");
        solicitud.setCategoria(categoria);
        solicitud.setPrioridad(prioridad);
        solicitud.setEstado(estado);
        solicitud.setSolicitante(solicitante);
        solicitud.setAgenteAsignado(agente);
        solicitud.setFechaCreacion(creacion);
        solicitud.setFechaActualizacion(creacion);
        return solicitudRepository.save(solicitud);
    }

    private SolicitudEntity cerrar(SolicitudEntity solicitud, UsuarioEntity actor, LocalDateTime fechaCierre) {
        RegistroAuditoriaEntity registro = new RegistroAuditoriaEntity();
        registro.setTipoEntidad("Solicitud");
        registro.setEntidadId(solicitud.getIdSolicitud());
        registro.setCampo("estado");
        registro.setValorAnterior("RESUELTO");
        registro.setValorNuevo("CERRADO");
        registro.setAccion("CONFIRMACION");
        registro.setActor(actor);
        registro.setFecha(fechaCierre);
        registroAuditoriaRepository.save(registro);
        return solicitud;
    }

    @Test
    void elVolumenIncluyeLosSeisEstadosConCeros() {
        UsuarioEntity solicitante = crearUsuarioDePrueba("t-pa10-sol-volumen", Rol.SOLICITANTE);
        persistir(solicitante, "t-pa10 volumen", LocalDateTime.of(2015, 1, 10, 9, 0),
                EstadoSolicitud.NUEVO, PrioridadSolicitud.MEDIA, CategoriaSolicitud.SOFTWARE, null);

        IndicadoresDTO indicadores = indicadorService.calcular(null, null, null,
                LocalDate.of(2015, 1, 1), LocalDate.of(2015, 1, 31));

        assertEquals(new LinkedHashSet<>(List.of(EstadoSolicitud.values())),
                indicadores.getVolumen().keySet(), "El volumen debe traer los seis estados");
        assertEquals(6, indicadores.getVolumen().size());
        assertEquals(1L, indicadores.getVolumen().get(EstadoSolicitud.NUEVO));
        assertEquals(0L, indicadores.getVolumen().get(EstadoSolicitud.ASIGNADO));
        assertEquals(0L, indicadores.getVolumen().get(EstadoSolicitud.EN_PROCESO));
        assertEquals(0L, indicadores.getVolumen().get(EstadoSolicitud.RESUELTO));
        assertEquals(0L, indicadores.getVolumen().get(EstadoSolicitud.CERRADO));
        assertEquals(0L, indicadores.getVolumen().get(EstadoSolicitud.REABIERTO));
        assertEquals(1L, indicadores.getTotal());
    }

    @Test
    void elTiempoMedianoDeCicloEsLaMedianaDeLasCerradas() {
        UsuarioEntity solicitante = crearUsuarioDePrueba("t-pa10-sol-mediana", Rol.SOLICITANTE);
        LocalDateTime base = LocalDateTime.of(2014, 2, 1, 8, 0);
        cerrar(persistir(solicitante, "t-pa10 mediana dos horas", base,
                        EstadoSolicitud.CERRADO, PrioridadSolicitud.MEDIA, CategoriaSolicitud.SOFTWARE, null),
                solicitante, base.plusHours(2));
        cerrar(persistir(solicitante, "t-pa10 mediana cuatro horas", base,
                        EstadoSolicitud.CERRADO, PrioridadSolicitud.MEDIA, CategoriaSolicitud.SOFTWARE, null),
                solicitante, base.plusHours(4));
        cerrar(persistir(solicitante, "t-pa10 mediana diez horas", base,
                        EstadoSolicitud.CERRADO, PrioridadSolicitud.MEDIA, CategoriaSolicitud.SOFTWARE, null),
                solicitante, base.plusHours(10));

        IndicadoresDTO indicadores = indicadorService.calcular(EstadoSolicitud.CERRADO, null, null,
                LocalDate.of(2014, 2, 1), LocalDate.of(2014, 2, 28));

        assertEquals(3L, indicadores.getTotal());
        assertNotNull(indicadores.getMedianaHorasCiclo());
        assertEquals(4.0, indicadores.getMedianaHorasCiclo(), 0.01,
                "La mediana de 2, 4 y 10 horas es 4 horas");
    }

    @Test
    void repetirLaMismaUrlProduceElMismoResultadoYVuelveALosFiltros() throws Exception {
        UsuarioEntity solicitante = crearUsuarioDePrueba("t-pa10-sol-repite", Rol.SOLICITANTE);
        crearUsuarioDePrueba("t-pa10-coord-repite", Rol.COORDINADOR);
        LocalDateTime base = LocalDateTime.of(2013, 3, 5, 10, 0);
        cerrar(persistir(solicitante, "t-pa10 reproducible", base,
                        EstadoSolicitud.CERRADO, PrioridadSolicitud.MEDIA, CategoriaSolicitud.SOFTWARE, null),
                solicitante, base.plusHours(5));
        MockHttpSession sesion = iniciarSesion("t-pa10-coord-repite", CLAVE_PRUEBA);

        String primera = pedirIndicadores(sesion);
        String segunda = pedirIndicadores(sesion);

        assertEquals(recortarSeccionIndicadores(primera), recortarSeccionIndicadores(segunda),
                "La misma URL de indicadores debe producir el mismo resultado");
        assertTrue(primera.contains("Filtros aplicados: desde=2013-03-01; hasta=2013-03-31"),
                "La vista debe mostrar los filtros aplicados");
        assertTrue(primera.contains("Solicitudes en el conjunto: 1"));
        for (EstadoSolicitud estado : EstadoSolicitud.values()) {
            assertTrue(primera.contains(estado.name()), "La tabla de volumen debe incluir " + estado.name());
        }
    }

    @Test
    void losIndicadoresNoMuestranAgentesNiAgrupacionPorPersona() throws Exception {
        UsuarioEntity solicitante = crearUsuarioDePrueba("t-pa10-sol-sinagente", Rol.SOLICITANTE);
        UsuarioEntity agente = crearUsuarioDePrueba("t-pa10-agente-oculto", Rol.AGENTE);
        crearUsuarioDePrueba("t-pa10-coord-sinagente", Rol.COORDINADOR);
        persistir(solicitante, "t-pa10 sin agrupacion", LocalDateTime.of(2012, 4, 4, 9, 0),
                EstadoSolicitud.ASIGNADO, PrioridadSolicitud.MEDIA, CategoriaSolicitud.SOFTWARE, agente);
        MockHttpSession sesion = iniciarSesion("t-pa10-coord-sinagente", CLAVE_PRUEBA);

        String html = pedirIndicadores(sesion);

        assertFalse(html.contains("t-pa10-agente-oculto"),
                "Los indicadores no deben nombrar agentes");
        assertFalse(html.toLowerCase().contains("por agente"),
                "Los indicadores no deben agrupar por agente");
    }

    private String pedirIndicadores(MockHttpSession sesion) throws Exception {
        return mockMvc.perform(get("/coordinacion/indicadores").session(sesion)
                        .param("desde", "2013-03-01")
                        .param("hasta", "2013-03-31"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * Recorta la parte de la vista con los indicadores: la cabecera incluye el
     * token CSRF del formulario de cierre de sesion, que cambia en cada
     * respuesta y no forma parte del resultado.
     */
    private String recortarSeccionIndicadores(String html) {
        int inicio = html.indexOf("Filtros aplicados");
        int fin = html.indexOf("Exportar CSV");
        assertTrue(inicio >= 0 && fin > inicio, "La vista debe contener la seccion de indicadores");
        return html.substring(inicio, fin);
    }
}
