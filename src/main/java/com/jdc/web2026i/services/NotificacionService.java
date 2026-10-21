package com.jdc.web2026i.services;

import com.jdc.web2026i.entities.NotificacionEntity;
import com.jdc.web2026i.entities.SolicitudEntity;
import com.jdc.web2026i.entities.UsuarioEntity;

import java.util.List;

/**
 * Avisos en la aplicacion para los usuarios. Las notificaciones no se borran:
 * solo se marcan como leidas.
 */
public interface NotificacionService {

    NotificacionEntity crearAsignacion(SolicitudEntity solicitud, UsuarioEntity destinatario);

    List<NotificacionEntity> listar(UsuarioEntity destinatario);

    void marcarLeidas(UsuarioEntity destinatario);

    long contarNoLeidas(UsuarioEntity destinatario);
}
