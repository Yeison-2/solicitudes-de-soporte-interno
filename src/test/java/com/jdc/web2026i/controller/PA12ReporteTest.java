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
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PA-12: cubre CA-003-12 a CA-003-15 (filtros del CSV, columnas permitidas,
 * exclusiones, registro de la exportacion y neutralizacion antiformula).
 */
class PA12ReporteTest extends PruebaBase {

    private static final List<String> COLUMNAS = List.of("ID", "Categoria", "Prioridad", "Estado",
            "FechaCreacion", "FechaActualizacion", "AgenteAsignado", "FechaCierre");

    @Autowired
    private SolicitudRepository solicitudRepository;

    @Autowired
    private RegistroAuditoriaRepository registroAuditoriaRepository;

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
        solicitud.setJustificacionPrioridad("justificacion-secreta-t-pa12");
        solicitud.setMotivoReapertura("motivo-secreto-t-pa12");
        return solicitudRepository.save(solicitud);
    }

    private MvcResult descargar(MockHttpSession sesion, String... parametros) throws Exception {
        var peticion = get("/coordinacion/reporte.csv").session(sesion);
        for (int indice = 0; indice + 1 < parametros.length; indice += 2) {
            peticion.param(parametros[indice], parametros[indice + 1]);
        }
        return mockMvc.perform(peticion).andExpect(status().isOk()).andReturn();
    }

    private String csv(MvcResult resultado) throws Exception {
        return resultado.getResponse().getContentAsString(StandardCharsets.UTF_8).replace("\uFEFF", "");
    }

    private List<String> celdas(String linea) {
        List<String> celdas = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        boolean enComillas = false;
        for (int indice = 0; indice < linea.length(); indice++) {
            char caracter = linea.charAt(indice);
            if (enComillas) {
                if (caracter == '"') {
                    if (indice + 1 < linea.length() && linea.charAt(indice + 1) == '"') {
                        actual.append('"');
                        indice++;
                    } else {
                        enComillas = false;
                    }
                } else {
                    actual.append(caracter);
                }
            } else if (caracter == '"') {
                enComillas = true;
            } else if (caracter == ',') {
                celdas.add(actual.toString());
                actual.setLength(0);
            } else {
                actual.append(caracter);
            }
        }
        celdas.add(actual.toString());
        return celdas;
    }

    private long exportacionesDe(UsuarioEntity actor) {        return registroAuditoriaRepository.findAll().stream()
                .filter(registro -> "EXPORTACION_REPORTE".equals(registro.getAccion()))
                .filter(registro -> registro.getActor() != null
                        && registro.getActor().getIdUsuario().equals(actor.getIdUsuario()))
                .count();
    }

    @Test
    void elCsvRespetaLosFiltrosYSoloTieneColumnasPermitidas() throws Exception {
        UsuarioEntity solicitante = crearUsuarioDePrueba("t-pa12-sol-filtros", Rol.SOLICITANTE);
        UsuarioEntity agente = crearUsuarioDePrueba("t-pa12-agente-filtros", Rol.AGENTE);
        crearUsuarioDePrueba("t-pa12-coord-filtros", Rol.COORDINADOR);
        SolicitudEntity nuevoMedia = persistir(solicitante, "t-pa12 nuevo media", "Descripcion de prueba del reporte.",
                EstadoSolicitud.NUEVO, PrioridadSolicitud.MEDIA, CategoriaSolicitud.SOFTWARE, agente);
        SolicitudEntity nuevoBaja = persistir(solicitante, "t-pa12 nuevo baja", "Descripcion de prueba del reporte.",
                EstadoSolicitud.NUEVO, PrioridadSolicitud.BAJA, CategoriaSolicitud.SOFTWARE, agente);
        SolicitudEntity cerradoMedia = persistir(solicitante, "t-pa12 cerrado media", "Descripcion de prueba del reporte.",
                EstadoSolicitud.CERRADO, PrioridadSolicitud.MEDIA, CategoriaSolicitud.SOFTWARE, agente);
        MockHttpSession sesion = iniciarSesion("t-pa12-coord-filtros", CLAVE_PRUEBA);

        String porEstado = csv(descargar(sesion, "estado", "NUEVO"));
        assertTrue(identificadores(porEstado).contains(String.valueOf(nuevoMedia.getIdSolicitud())));
        assertTrue(identificadores(porEstado).contains(String.valueOf(nuevoBaja.getIdSolicitud())));
        assertFalse(identificadores(porEstado).contains(String.valueOf(cerradoMedia.getIdSolicitud())),
                "El CSV debe respetar el filtro de estado");

        String estadoYPrioridad = csv(descargar(sesion, "estado", "NUEVO", "prioridad", "MEDIA"));
        assertTrue(identificadores(estadoYPrioridad).contains(String.valueOf(nuevoMedia.getIdSolicitud())));
        assertFalse(identificadores(estadoYPrioridad).contains(String.valueOf(nuevoBaja.getIdSolicitud())),
                "Los filtros del CSV se combinan con AND");
        assertFalse(identificadores(estadoYPrioridad).contains(String.valueOf(cerradoMedia.getIdSolicitud())));

        String cerrados = csv(descargar(sesion, "estado", "CERRADO"));
        assertTrue(identificadores(cerrados).contains(String.valueOf(cerradoMedia.getIdSolicitud())));
        assertFalse(identificadores(cerrados).contains(String.valueOf(nuevoMedia.getIdSolicitud())));

        String[] lineas = porEstado.split("\r\n");
        assertEquals(COLUMNAS, celdas(lineas[0]), "La cabecera debe tener exactamente las 8 columnas");
        boolean encontroFila = false;
        for (int indice = 1; indice < lineas.length; indice++) {
            if (lineas[indice].startsWith("\"" + nuevoMedia.getIdSolicitud() + "\"")) {
                List<String> fila = celdas(lineas[indice]);
                assertEquals(8, fila.size(), "Cada fila debe tener exactamente 8 columnas");
                assertEquals("SOFTWARE", fila.get(1));
                assertEquals("MEDIA", fila.get(2));
                assertEquals("NUEVO", fila.get(3));
                assertEquals("t-pa12-agente-filtros", fila.get(6));
                encontroFila = true;
            }
        }
        assertTrue(encontroFila, "La fila de la solicitud creada debe estar en el CSV");
    }

    @Test
    void elCsvExcluyeTextoLibreYCredenciales() throws Exception {
        UsuarioEntity solicitante = crearUsuarioDePrueba("t-pa12-sol-exclusiones", Rol.SOLICITANTE);
        UsuarioEntity coordinador = crearUsuarioDePrueba("t-pa12-coord-exclusiones", Rol.COORDINADOR);
        persistir(solicitante, "t-pa12 exclusiones", "descripcion-secreta-t-pa12",
                EstadoSolicitud.NUEVO, PrioridadSolicitud.MEDIA, CategoriaSolicitud.SOFTWARE, null);
        MockHttpSession sesion = iniciarSesion("t-pa12-coord-exclusiones", CLAVE_PRUEBA);

        String contenido = csv(descargar(sesion));

        assertFalse(contenido.contains("t-pa12 exclusiones"),
                "El CSV no debe incluir el titulo (texto libre)");
        assertFalse(contenido.contains("descripcion-secreta-t-pa12"),
                "El CSV no debe incluir la descripcion");
        assertFalse(contenido.contains("justificacion-secreta-t-pa12"),
                "El CSV no debe incluir la justificacion de prioridad");
        assertFalse(contenido.contains("motivo-secreto-t-pa12"),
                "El CSV no debe incluir el motivo de reapertura");
        assertFalse(contenido.contains(coordinador.getClaveHash()),
                "El CSV no debe incluir hashes de credenciales");
        assertFalse(contenido.toLowerCase().contains("clave"),
                "El CSV no debe incluir columnas de credenciales");
        assertFalse(contenido.toLowerCase().contains("contraseña")
                || contenido.toLowerCase().contains("contrasena"));
        assertFalse(contenido.toLowerCase().contains("descripcion"));
        assertFalse(contenido.toLowerCase().contains("comentario"));
    }

    @Test
    void cadaDescargaRegistraLaExportacionConActorYFiltro() throws Exception {
        UsuarioEntity solicitante = crearUsuarioDePrueba("t-pa12-sol-exportacion", Rol.SOLICITANTE);
        UsuarioEntity coordinador = crearUsuarioDePrueba("t-pa12-coord-exportacion", Rol.COORDINADOR);
        persistir(solicitante, "t-pa12 exportacion", "Descripcion de prueba del reporte.",
                EstadoSolicitud.NUEVO, PrioridadSolicitud.MEDIA, CategoriaSolicitud.SOFTWARE, null);
        MockHttpSession sesion = iniciarSesion("t-pa12-coord-exportacion", CLAVE_PRUEBA);

        long antes = exportacionesDe(coordinador);
        descargar(sesion, "estado", "NUEVO", "prioridad", "MEDIA");
        long trasPrimera = exportacionesDe(coordinador);
        RegistroAuditoriaEntity primeraExportacion = registroAuditoriaRepository.findAll().stream()
                .filter(registro -> "EXPORTACION_REPORTE".equals(registro.getAccion()))
                .filter(registro -> registro.getActor() != null
                        && registro.getActor().getIdUsuario().equals(coordinador.getIdUsuario()))
                .max((uno, otro) -> uno.getIdRegistro().compareTo(otro.getIdRegistro()))
                .orElseThrow();
        descargar(sesion);
        long trasSegunda = exportacionesDe(coordinador);

        assertEquals(antes + 1, trasPrimera, "Cada descarga debe crear un registro de exportacion");
        assertEquals(trasPrimera + 1, trasSegunda);

        assertTrue(primeraExportacion.getValorNuevo().contains("estado=NUEVO"),
                "El registro debe guardar el filtro aplicado");
        assertTrue(primeraExportacion.getValorNuevo().contains("prioridad=MEDIA"));
        assertTrue(primeraExportacion.getFecha() != null, "El registro debe guardar la fecha");
        assertEquals(coordinador.getIdUsuario(), primeraExportacion.getActor().getIdUsuario());
    }

    @Test
    void losValoresConFormulaSalenNeutralizadosYLaDescargaEsCsvUtf8() throws Exception {
        UsuarioEntity solicitante = crearUsuarioDePrueba("t-pa12-sol-formula", Rol.SOLICITANTE);
        crearUsuarioDePrueba("t-pa12-coord-formula", Rol.COORDINADOR);
        UsuarioEntity agente = crearUsuarioDePrueba("=t-pa12-formula", Rol.AGENTE);
        persistir(solicitante, "t-pa12 formula", "Descripcion de prueba del reporte.",
                EstadoSolicitud.NUEVO, PrioridadSolicitud.MEDIA, CategoriaSolicitud.SOFTWARE, agente);
        MockHttpSession sesion = iniciarSesion("t-pa12-coord-formula", CLAVE_PRUEBA);

        MvcResult resultado = descargar(sesion, "estado", "NUEVO");
        String contenido = csv(resultado);

        assertTrue(contenido.contains("\"'=t-pa12-formula\""),
                "Un valor que empieza por = debe salir neutralizado");
        for (String linea : contenido.split("\r\n")) {
            for (String celda : celdas(linea)) {
                assertFalse(celda.startsWith("=") || celda.startsWith("+")
                                || celda.startsWith("-") || celda.startsWith("@"),
                        "Ninguna celda debe empezar por un caracter de formula: " + celda);
            }
        }

        byte[] cuerpo = resultado.getResponse().getContentAsByteArray();
        assertTrue(cuerpo.length > 3);
        assertEquals((byte) 0xEF, cuerpo[0], "El CSV debe empezar con BOM UTF-8");
        assertEquals((byte) 0xBB, cuerpo[1]);
        assertEquals((byte) 0xBF, cuerpo[2]);
        String disposicion = resultado.getResponse().getHeader("Content-Disposition");
        assertTrue(disposicion != null && disposicion.contains("reporte-solicitudes-")
                        && disposicion.contains(".csv"),
                "La descarga debe anunciar el nombre reporte-solicitudes-*.csv");
    }

    @Test
    void unValorConComaYComillaCumpleRfc4180YElEncabezadoVaEntrecomillado() throws Exception {
        UsuarioEntity solicitante = crearUsuarioDePrueba("t-pa12-sol-rfc", Rol.SOLICITANTE);
        crearUsuarioDePrueba("t-pa12-coord-rfc", Rol.COORDINADOR);
        UsuarioEntity agente = crearUsuarioDePrueba("t-pa12, \"a\"", Rol.AGENTE);
        persistir(solicitante, "t-pa12 rfc", "Descripcion de prueba del reporte.",
                EstadoSolicitud.NUEVO, PrioridadSolicitud.MEDIA, CategoriaSolicitud.SOFTWARE, agente);
        MockHttpSession sesion = iniciarSesion("t-pa12-coord-rfc", CLAVE_PRUEBA);

        String contenido = csv(descargar(sesion, "estado", "NUEVO"));

        String encabezadoEsperado = "\"ID\",\"Categoria\",\"Prioridad\",\"Estado\","
                + "\"FechaCreacion\",\"FechaActualizacion\",\"AgenteAsignado\",\"FechaCierre\"";
        assertTrue(contenido.startsWith(encabezadoEsperado),
                "El encabezado debe ir entrecomillado columna a columna");
        assertTrue(contenido.contains("\"t-pa12, \"\"a\"\"\""),
                "El campo debe entrecomillarse, duplicar comillas y conservar la coma");

        int filasConAgente = 0;
        for (List<String> fila : filas(contenido)) {
            if (fila.size() > 6 && fila.get(6).equals("t-pa12, \"a\"")) {
                filasConAgente++;
                assertEquals(8, fila.size(), "La coma no debe partir la fila logica");
                assertEquals("SOFTWARE", fila.get(1));
            }
        }
        assertEquals(1, filasConAgente, "El valor debe ocupar exactamente una fila logica");
    }

    /** Identificadores de las solicitudes listadas en el CSV (sin cabecera). */
    private List<String> identificadores(String contenido) {
        return filas(contenido).stream()
                .skip(1)
                .filter(fila -> !fila.isEmpty())
                .map(fila -> fila.get(0))
                .toList();
    }


    /**
     * Parser CSV minimo que respeta comillas y saltos de linea dentro de un
     * campo, para contar filas logicas y no fisicas.
     */
    private List<List<String>> filas(String contenido) {
        List<List<String>> filas = new ArrayList<>();
        List<String> fila = new ArrayList<>();
        StringBuilder celda = new StringBuilder();
        boolean enComillas = false;
        for (int indice = 0; indice < contenido.length(); indice++) {
            char caracter = contenido.charAt(indice);
            if (enComillas) {
                if (caracter == '"') {
                    if (indice + 1 < contenido.length() && contenido.charAt(indice + 1) == '"') {
                        celda.append('"');
                        indice++;
                    } else {
                        enComillas = false;
                    }
                } else {
                    celda.append(caracter);
                }
            } else if (caracter == '"') {
                enComillas = true;
            } else if (caracter == ',') {
                fila.add(celda.toString());
                celda.setLength(0);
            } else if (caracter == '\r' || caracter == '\n') {
                fila.add(celda.toString());
                celda.setLength(0);
                filas.add(fila);
                fila = new ArrayList<>();
                if (caracter == '\r' && indice + 1 < contenido.length()
                        && contenido.charAt(indice + 1) == '\n') {
                    indice++;
                }
            } else {
                celda.append(caracter);
            }
        }
        if (celda.length() > 0 || !fila.isEmpty()) {
            fila.add(celda.toString());
            filas.add(fila);
        }
        return filas;
    }
}
