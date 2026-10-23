package com.jdc.web2026i.controller;

import com.jdc.web2026i.entities.CategoriaSolicitud;
import com.jdc.web2026i.entities.EstadoSolicitud;
import com.jdc.web2026i.entities.PrioridadSolicitud;
import com.jdc.web2026i.entities.Rol;
import com.jdc.web2026i.entities.SolicitudEntity;
import com.jdc.web2026i.entities.UsuarioEntity;
import com.jdc.web2026i.repository.SolicitudRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PA-09: cubre CA-003-01 a CA-003-04 (busqueda por texto, filtros combinados,
 * alcance por rol y filtros desconocidos).
 */
class PA09BusquedaTest extends PruebaBase {

    private static final String PREFIJO_FILTROS = "t-pa09filtro";

    @Autowired
    private SolicitudRepository solicitudRepository;

    private SolicitudEntity persistir(UsuarioEntity solicitante, String titulo, String descripcion,
                                      EstadoSolicitud estado, PrioridadSolicitud prioridad,
                                      CategoriaSolicitud categoria, UsuarioEntity agente) {
        LocalDateTime ahora = LocalDateTime.now().withNano(0);
        SolicitudEntity solicitud = new SolicitudEntity();
        solicitud.setTitulo(titulo);
        solicitud.setDescripcion(descripcion);
        solicitud.setCategoria(categoria);
        solicitud.setPrioridad(prioridad);
        solicitud.setEstado(estado);
        solicitud.setSolicitante(solicitante);
        solicitud.setAgenteAsignado(agente);
        solicitud.setFechaCreacion(ahora);
        solicitud.setFechaActualizacion(ahora);
        return solicitudRepository.save(solicitud);
    }

    private String buscarComo(String nombreUsuario, String... parametros) throws Exception {
        MockHttpSession sesion = iniciarSesion(nombreUsuario, CLAVE_PRUEBA);
        MockHttpServletRequestBuilder peticion = get("/solicitudes/buscar").session(sesion);
        for (int indice = 0; indice + 1 < parametros.length; indice += 2) {
            peticion.param(parametros[indice], parametros[indice + 1]);
        }
        return mockMvc.perform(peticion)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private void assertContiene(String html, String... textos) {
        for (String texto : textos) {
            assertTrue(html.contains(texto), "El resultado deberia contener: " + texto);
        }
    }

    private void assertNoContiene(String html, String... textos) {
        for (String texto : textos) {
            assertFalse(html.contains(texto), "El resultado no deberia contener: " + texto);
        }
    }

    @Test
    void encuentraPorTituloYDescripcionSinDistinguirMayusculas() throws Exception {
        UsuarioEntity solicitante = crearUsuarioDePrueba("t-pa09-sol-texto", Rol.SOLICITANTE);
        UsuarioEntity coordinador = crearUsuarioDePrueba("t-pa09-coord-texto", Rol.COORDINADOR);
        persistir(solicitante, "t-pa09 Impresora SIN TONER", "El equipo no imprime desde ayer.",
                EstadoSolicitud.NUEVO, PrioridadSolicitud.MEDIA, CategoriaSolicitud.HARDWARE, null);
        persistir(solicitante, "t-pa09 otro asunto", "Fallo de RED WiFi en el ala norte.",
                EstadoSolicitud.NUEVO, PrioridadSolicitud.MEDIA, CategoriaSolicitud.RED_CONECTIVIDAD, null);

        String porTitulo = buscarComo("t-pa09-coord-texto", "q", "impresora sin toner");
        assertContiene(porTitulo, "t-pa09 Impresora SIN TONER");
        assertNoContiene(porTitulo, "t-pa09 otro asunto");

        String porDescripcion = buscarComo("t-pa09-coord-texto", "q", "red wifi");
        assertContiene(porDescripcion, "t-pa09 otro asunto");
        assertNoContiene(porDescripcion, "t-pa09 Impresora SIN TONER");
    }

    @Test
    void cadaFiltroFuncionaSoloYCombinadosConAnd() throws Exception {
        UsuarioEntity solicitante = crearUsuarioDePrueba("t-pa09-sol-filtros", Rol.SOLICITANTE);
        crearUsuarioDePrueba("t-pa09-coord-filtros", Rol.COORDINADOR);
        String altaHardware = PREFIJO_FILTROS + " alta hardware";
        String cerradaSoftware = PREFIJO_FILTROS + " cerrada software";
        String nuevoSoftware = PREFIJO_FILTROS + " nuevo software";
        String nuevoRed = PREFIJO_FILTROS + " nuevo red";
        persistir(solicitante, altaHardware, "Descripcion de la prueba de filtros.",
                EstadoSolicitud.NUEVO, PrioridadSolicitud.ALTA, CategoriaSolicitud.HARDWARE, null);
        persistir(solicitante, cerradaSoftware, "Descripcion de la prueba de filtros.",
                EstadoSolicitud.CERRADO, PrioridadSolicitud.MEDIA, CategoriaSolicitud.SOFTWARE, null);
        persistir(solicitante, nuevoSoftware, "Descripcion de la prueba de filtros.",
                EstadoSolicitud.NUEVO, PrioridadSolicitud.MEDIA, CategoriaSolicitud.SOFTWARE, null);
        persistir(solicitante, nuevoRed, "Descripcion de la prueba de filtros.",
                EstadoSolicitud.NUEVO, PrioridadSolicitud.MEDIA, CategoriaSolicitud.RED_CONECTIVIDAD, null);

        String soloEstado = buscarComo("t-pa09-coord-filtros", "q", PREFIJO_FILTROS, "estado", "CERRADO");
        assertContiene(soloEstado, cerradaSoftware);
        assertNoContiene(soloEstado, altaHardware, nuevoSoftware, nuevoRed);

        String soloPrioridad = buscarComo("t-pa09-coord-filtros", "q", PREFIJO_FILTROS, "prioridad", "ALTA");
        assertContiene(soloPrioridad, altaHardware);
        assertNoContiene(soloPrioridad, cerradaSoftware, nuevoSoftware, nuevoRed);

        String soloCategoria = buscarComo("t-pa09-coord-filtros", "q", PREFIJO_FILTROS,
                "categoria", "RED_CONECTIVIDAD");
        assertContiene(soloCategoria, nuevoRed);
        assertNoContiene(soloCategoria, altaHardware, cerradaSoftware, nuevoSoftware);

        String combinados = buscarComo("t-pa09-coord-filtros", "q", PREFIJO_FILTROS,
                "estado", "NUEVO", "prioridad", "MEDIA", "categoria", "SOFTWARE");
        assertContiene(combinados, nuevoSoftware);
        assertNoContiene(combinados, altaHardware, cerradaSoftware, nuevoRed);
    }

    @Test
    void elAlcanceDeLaBusquedaDependeDelRol() throws Exception {
        UsuarioEntity solA = crearUsuarioDePrueba("t-pa09-sol-a", Rol.SOLICITANTE);
        UsuarioEntity solB = crearUsuarioDePrueba("t-pa09-sol-b", Rol.SOLICITANTE);
        UsuarioEntity agenteA = crearUsuarioDePrueba("t-pa09-agente-a", Rol.AGENTE);
        UsuarioEntity agenteB = crearUsuarioDePrueba("t-pa09-agente-b", Rol.AGENTE);
        crearUsuarioDePrueba("t-pa09-coord-alcance", Rol.COORDINADOR);
        crearUsuarioDePrueba("t-pa09-auditor-alcance", Rol.AUDITOR);
        String tituloA = "t-pa09alcance caso de A";
        String tituloB = "t-pa09alcance caso de B";
        persistir(solA, tituloA, "Descripcion del alcance por rol.",
                EstadoSolicitud.ASIGNADO, PrioridadSolicitud.MEDIA, CategoriaSolicitud.SOFTWARE, agenteA);
        persistir(solB, tituloB, "Descripcion del alcance por rol.",
                EstadoSolicitud.ASIGNADO, PrioridadSolicitud.MEDIA, CategoriaSolicitud.SOFTWARE, agenteB);

        String comoSolicitante = buscarComo("t-pa09-sol-a", "q", "t-pa09alcance");
        assertContiene(comoSolicitante, tituloA);
        assertNoContiene(comoSolicitante, tituloB);

        String comoAgente = buscarComo("t-pa09-agente-a", "q", "t-pa09alcance");
        assertContiene(comoAgente, tituloA);
        assertNoContiene(comoAgente, tituloB);

        String comoCoordinador = buscarComo("t-pa09-coord-alcance", "q", "t-pa09alcance");
        assertContiene(comoCoordinador, tituloA, tituloB);

        String comoAuditor = buscarComo("t-pa09-auditor-alcance", "q", "t-pa09alcance");
        assertContiene(comoAuditor, tituloA, tituloB);
    }

    @Test
    void unFiltroDesconocidoSeIgnoraSinAlterarLaConsulta() throws Exception {
        UsuarioEntity solicitante = crearUsuarioDePrueba("t-pa09-sol-desconocido", Rol.SOLICITANTE);
        crearUsuarioDePrueba("t-pa09-coord-desconocido", Rol.COORDINADOR);
        String titulo = PREFIJO_FILTROS + " valor desconocido";
        persistir(solicitante, titulo, "Descripcion del filtro desconocido.",
                EstadoSolicitud.NUEVO, PrioridadSolicitud.MEDIA, CategoriaSolicitud.OTROS, null);

        String soloTexto = buscarComo("t-pa09-coord-desconocido", "q", PREFIJO_FILTROS);
        String conDesconocidos = buscarComo("t-pa09-coord-desconocido", "q", PREFIJO_FILTROS,
                "estado", "NO_EXISTE", "prioridad", "INVENTADA", "categoria", "TODAS");

        assertContiene(conDesconocidos, titulo);
        assertContiene(conDesconocidos, "Filtros aplicados: texto=" + PREFIJO_FILTROS);
        assertNoContiene(conDesconocidos, "estado=NO_EXISTE", "prioridad=INVENTADA", "categoria=TODAS");
        int filasSoloTexto = contarOcurrencias(soloTexto, "Ver detalle");
        int filasConDesconocidos = contarOcurrencias(conDesconocidos, "Ver detalle");
        assertTrue(filasSoloTexto > 0, "La consulta base deberia devolver al menos una solicitud");
        assertTrue(filasSoloTexto == filasConDesconocidos,
                "Un filtro desconocido no debe alterar el resto de la consulta");
    }

    @Test
    void losComodinesDelUsuarioNoActuanComoComodin() throws Exception {
        UsuarioEntity solicitante = crearUsuarioDePrueba("t-pa09-sol-comodin", Rol.SOLICITANTE);
        crearUsuarioDePrueba("t-pa09-coord-comodin", Rol.COORDINADOR);
        persistir(solicitante, "t-pa09 avance del 100% confirmado", "Descripcion del comodin literal.",
                EstadoSolicitud.NUEVO, PrioridadSolicitud.MEDIA, CategoriaSolicitud.SOFTWARE, null);
        persistir(solicitante, "t-pa09 archivo_config del servidor", "Descripcion del comodin literal.",
                EstadoSolicitud.NUEVO, PrioridadSolicitud.MEDIA, CategoriaSolicitud.SOFTWARE, null);
        persistir(solicitante, "t-pa09 sin comodines", "Descripcion del comodin literal.",
                EstadoSolicitud.NUEVO, PrioridadSolicitud.MEDIA, CategoriaSolicitud.SOFTWARE, null);

        String porcentaje = buscarComo("t-pa09-coord-comodin", "q", "%");
        assertContiene(porcentaje, "t-pa09 avance del 100% confirmado");
        assertNoContiene(porcentaje, "t-pa09 archivo_config del servidor", "t-pa09 sin comodines");

        String guionBajo = buscarComo("t-pa09-coord-comodin", "q", "_");
        assertContiene(guionBajo, "t-pa09 archivo_config del servidor");
        assertNoContiene(guionBajo, "t-pa09 avance del 100% confirmado", "t-pa09 sin comodines");
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
}
