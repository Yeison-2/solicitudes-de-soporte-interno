package com.jdc.web2026i.controller;

import com.jdc.web2026i.DTO.SolicitudFormularioDTO;
import com.jdc.web2026i.entities.CategoriaSolicitud;
import com.jdc.web2026i.entities.Rol;
import com.jdc.web2026i.entities.SolicitudEntity;
import com.jdc.web2026i.entities.UsuarioEntity;
import com.jdc.web2026i.services.SolicitudService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PA-03: cubre CA-001-08 a CA-001-10 (lista propia, detalle y detalle ajeno).
 */
class PA03ConsultaSolicitudTest extends PruebaBase {

    @Autowired
    private SolicitudService solicitudService;

    private SolicitudEntity crear(UsuarioEntity solicitante, String titulo) {
        SolicitudFormularioDTO formulario = new SolicitudFormularioDTO();
        formulario.setTitulo(titulo);
        formulario.setDescripcion("Descripcion de prueba para la consulta.");
        formulario.setCategoria(CategoriaSolicitud.SOFTWARE);
        return solicitudService.crear(formulario, solicitante);
    }

    @Test
    void listaSoloLasSolicitudesPropias() throws Exception {
        UsuarioEntity propietario = crearUsuarioDePrueba("t-pa03-propio", Rol.SOLICITANTE);
        UsuarioEntity tercero = crearUsuarioDePrueba("t-pa03-ajeno", Rol.SOLICITANTE);
        crear(propietario, "Solicitud propia de la prueba");
        crear(tercero, "Solicitud ajena de la prueba");

        MockHttpSession sesion = iniciarSesion("t-pa03-propio", CLAVE_PRUEBA);
        mockMvc.perform(get("/solicitudes").session(sesion))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Solicitud propia de la prueba")))
                .andExpect(content().string(not(containsString("Solicitud ajena de la prueba"))));
    }

    @Test
    void detallePropioMuestraEstadoYUltimaActualizacion() throws Exception {
        UsuarioEntity propietario = crearUsuarioDePrueba("t-pa03-detalle", Rol.SOLICITANTE);
        SolicitudEntity propia = crear(propietario, "Solicitud con detalle de la prueba");

        MockHttpSession sesion = iniciarSesion("t-pa03-detalle", CLAVE_PRUEBA);
        mockMvc.perform(get("/solicitudes/" + propia.getIdSolicitud()).session(sesion))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Solicitud con detalle de la prueba")))
                .andExpect(content().string(containsString("NUEVO")))
                .andExpect(content().string(containsString("Ultima actualizacion")));
    }

    @Test
    void detalleAjenoNoEsAccesible() throws Exception {
        UsuarioEntity propietario = crearUsuarioDePrueba("t-pa03-duenio", Rol.SOLICITANTE);
        crearUsuarioDePrueba("t-pa03-intruso", Rol.SOLICITANTE);
        SolicitudEntity ajena = crear(propietario, "Solicitud reservada de la prueba");

        MockHttpSession sesion = iniciarSesion("t-pa03-intruso", CLAVE_PRUEBA);
        mockMvc.perform(get("/solicitudes/" + ajena.getIdSolicitud()).session(sesion))
                .andExpect(status().isForbidden());
    }
}
