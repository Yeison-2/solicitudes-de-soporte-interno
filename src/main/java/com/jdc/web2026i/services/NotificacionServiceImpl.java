package com.jdc.web2026i.services;

import com.jdc.web2026i.entities.NotificacionEntity;
import com.jdc.web2026i.entities.SolicitudEntity;
import com.jdc.web2026i.entities.UsuarioEntity;
import com.jdc.web2026i.repository.NotificacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class NotificacionServiceImpl implements NotificacionService {

    private final NotificacionRepository notificacionRepository;

    public NotificacionServiceImpl(NotificacionRepository notificacionRepository) {
        this.notificacionRepository = notificacionRepository;
    }

    @Override
    @Transactional
    public NotificacionEntity crearAsignacion(SolicitudEntity solicitud, UsuarioEntity destinatario) {
        NotificacionEntity notificacion = new NotificacionEntity();
        notificacion.setDestinatario(destinatario);
        notificacion.setSolicitud(solicitud);
        notificacion.setMensaje("Se le asigno la solicitud #" + solicitud.getIdSolicitud()
                + ": " + solicitud.getTitulo());
        notificacion.setLeida(false);
        notificacion.setFecha(LocalDateTime.now());
        return notificacionRepository.save(notificacion);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificacionEntity> listar(UsuarioEntity destinatario) {
        return notificacionRepository.findByDestinatarioIdUsuarioOrderByFechaDesc(destinatario.getIdUsuario());
    }

    @Override
    @Transactional
    public void marcarLeidas(UsuarioEntity destinatario) {
        List<NotificacionEntity> noLeidas =
                notificacionRepository.findByDestinatarioIdUsuarioOrderByFechaDesc(destinatario.getIdUsuario());
        List<NotificacionEntity> pendientes = new ArrayList<>();
        for (NotificacionEntity notificacion : noLeidas) {
            if (!notificacion.isLeida()) {
                notificacion.setLeida(true);
                pendientes.add(notificacion);
            }
        }
        if (!pendientes.isEmpty()) {
            notificacionRepository.saveAll(pendientes);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long contarNoLeidas(UsuarioEntity destinatario) {
        return notificacionRepository.countByDestinatarioIdUsuarioAndLeidaFalse(destinatario.getIdUsuario());
    }
}
