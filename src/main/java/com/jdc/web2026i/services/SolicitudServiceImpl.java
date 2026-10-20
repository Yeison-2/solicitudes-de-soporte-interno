package com.jdc.web2026i.services;

import com.jdc.web2026i.DTO.SolicitudFormularioDTO;
import com.jdc.web2026i.entities.EstadoSolicitud;
import com.jdc.web2026i.entities.PrioridadSolicitud;
import com.jdc.web2026i.entities.RegistroAuditoriaEntity;
import com.jdc.web2026i.entities.Rol;
import com.jdc.web2026i.entities.SolicitudEntity;
import com.jdc.web2026i.entities.UsuarioEntity;
import com.jdc.web2026i.repository.RegistroAuditoriaRepository;
import com.jdc.web2026i.repository.SolicitudRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SolicitudServiceImpl implements SolicitudService {

    private static final String TIPO_ENTIDAD = "Solicitud";
    private static final String CAMPO_PRIORIDAD = "prioridad";
    private static final String ACCION_CAMBIO_PRIORIDAD = "CAMBIO_PRIORIDAD";

    private final SolicitudRepository solicitudRepository;
    private final RegistroAuditoriaRepository registroAuditoriaRepository;

    public SolicitudServiceImpl(SolicitudRepository solicitudRepository,
                                RegistroAuditoriaRepository registroAuditoriaRepository) {
        this.solicitudRepository = solicitudRepository;
        this.registroAuditoriaRepository = registroAuditoriaRepository;
    }

    @Override
    @Transactional
    public SolicitudEntity crear(SolicitudFormularioDTO formulario, UsuarioEntity solicitante) {
        if (formulario.getCategoria() == null) {
            throw new IllegalArgumentException("La categoria es obligatoria.");
        }
        LocalDateTime ahora = LocalDateTime.now();
        SolicitudEntity solicitud = new SolicitudEntity();
        solicitud.setTitulo(formulario.getTitulo().trim());
        solicitud.setDescripcion(formulario.getDescripcion().trim());
        solicitud.setCategoria(formulario.getCategoria());
        solicitud.setPrioridad(PrioridadSolicitud.MEDIA);
        solicitud.setEstado(EstadoSolicitud.NUEVO);
        solicitud.setSolicitante(solicitante);
        solicitud.setFechaCreacion(ahora);
        solicitud.setFechaActualizacion(ahora);
        return solicitudRepository.save(solicitud);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SolicitudEntity> listarPropias(UsuarioEntity solicitante) {
        return solicitudRepository.findBySolicitante(solicitante,
                Sort.by(Sort.Direction.DESC, "fechaActualizacion"));
    }

    @Override
    @Transactional(readOnly = true)
    public SolicitudEntity obtenerParaUsuario(Long idSolicitud, UsuarioEntity usuario) {
        SolicitudEntity solicitud = solicitudRepository.findById(idSolicitud)
                .orElseThrow(SolicitudNoEncontradaException::new);
        boolean esPropietario = solicitud.getSolicitante().getIdUsuario().equals(usuario.getIdUsuario());
        boolean esCoordinador = usuario.getRol() == Rol.COORDINADOR;
        if (!esPropietario && !esCoordinador) {
            throw new AccesoDenegadoException();
        }
        return solicitud;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SolicitudEntity> listarParaCoordinacion(String orden) {
        List<SolicitudEntity> solicitudes = solicitudRepository.findAll(
                Sort.by(Sort.Direction.DESC, "fechaCreacion"));
        String clave = orden == null ? "" : orden.trim().toLowerCase();
        switch (clave) {
            case "prioridad" -> solicitudes.sort(
                    (a, b) -> Integer.compare(pesoPrioridad(a.getPrioridad()), pesoPrioridad(b.getPrioridad())));
            case "estado" -> solicitudes.sort(
                    (a, b) -> Integer.compare(a.getEstado().ordinal(), b.getEstado().ordinal()));
            case "fecha" -> {
                // El orden por defecto ya es fecha descendente.
            }
            default -> {
                // Valor no reconocido: se mantiene el orden por defecto (fecha descendente).
            }
        }
        return solicitudes;
    }

    @Override
    @Transactional
    public void cambiarPrioridad(Long idSolicitud, PrioridadSolicitud nuevaPrioridad, UsuarioEntity actor) {
        if (nuevaPrioridad == null) {
            throw new IllegalArgumentException("La prioridad es obligatoria.");
        }
        if (actor.getRol() != Rol.COORDINADOR) {
            throw new AccesoDenegadoException();
        }
        SolicitudEntity solicitud = solicitudRepository.findById(idSolicitud)
                .orElseThrow(SolicitudNoEncontradaException::new);
        PrioridadSolicitud anterior = solicitud.getPrioridad();
        if (anterior == nuevaPrioridad) {
            return;
        }
        solicitud.setPrioridad(nuevaPrioridad);
        solicitud.setFechaActualizacion(LocalDateTime.now());
        solicitudRepository.save(solicitud);

        RegistroAuditoriaEntity registro = new RegistroAuditoriaEntity();
        registro.setTipoEntidad(TIPO_ENTIDAD);
        registro.setEntidadId(solicitud.getIdSolicitud());
        registro.setCampo(CAMPO_PRIORIDAD);
        registro.setValorAnterior(anterior.name());
        registro.setValorNuevo(nuevaPrioridad.name());
        registro.setActor(actor);
        registro.setFecha(LocalDateTime.now());
        registro.setAccion(ACCION_CAMBIO_PRIORIDAD);
        registroAuditoriaRepository.save(registro);
    }

    private int pesoPrioridad(PrioridadSolicitud prioridad) {
        return switch (prioridad) {
            case CRITICA -> 0;
            case ALTA -> 1;
            case MEDIA -> 2;
            case BAJA -> 3;
        };
    }
}
