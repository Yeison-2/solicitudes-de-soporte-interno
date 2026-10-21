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
import com.jdc.web2026i.repository.UsuarioRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class SolicitudServiceImpl implements SolicitudService {

    private static final String TIPO_ENTIDAD = "Solicitud";
    private static final String CAMPO_PRIORIDAD = "prioridad";
    private static final String CAMPO_AGENTE = "agente";
    private static final String CAMPO_ESTADO = "estado";
    private static final String ACCION_CAMBIO_PRIORIDAD = "CAMBIO_PRIORIDAD";
    private static final String ACCION_ASIGNACION = "ASIGNACION";
    private static final String ACCION_CAMBIO_ESTADO = "CAMBIO_ESTADO";
    private static final String ACCION_CONFIRMACION = "CONFIRMACION";
    private static final String ACCION_REAPERTURA = "REAPERTURA";
    private static final String CAMPO_MOTIVO_REAPERTURA = "motivo_reapertura";
    private static final int LONGITUD_MAXIMA_TEXTO = 300;

    private final SolicitudRepository solicitudRepository;
    private final RegistroAuditoriaRepository registroAuditoriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificacionService notificacionService;

    public SolicitudServiceImpl(SolicitudRepository solicitudRepository,
                                RegistroAuditoriaRepository registroAuditoriaRepository,
                                UsuarioRepository usuarioRepository,
                                NotificacionService notificacionService) {
        this.solicitudRepository = solicitudRepository;
        this.registroAuditoriaRepository = registroAuditoriaRepository;
        this.usuarioRepository = usuarioRepository;
        this.notificacionService = notificacionService;
    }

    @Override
    @Transactional
    public SolicitudEntity crear(SolicitudFormularioDTO formulario, UsuarioEntity solicitante) {
        if (formulario.getCategoria() == null) {
            throw new IllegalArgumentException("La categoria es obligatoria.");
        }
        PrioridadSolicitud prioridad = normalizarPrioridadInicial(formulario.getPrioridad());
        String justificacion = null;
        LocalDate fechaObjetivo = null;
        if (prioridad == PrioridadSolicitud.ALTA) {
            justificacion = validarJustificacion(formulario.getJustificacionPrioridad());
            fechaObjetivo = validarFechaObjetivo(formulario.getFechaObjetivo());
        }
        LocalDateTime ahora = LocalDateTime.now();
        SolicitudEntity solicitud = new SolicitudEntity();
        solicitud.setTitulo(formulario.getTitulo().trim());
        solicitud.setDescripcion(formulario.getDescripcion().trim());
        solicitud.setCategoria(formulario.getCategoria());
        solicitud.setPrioridad(prioridad);
        solicitud.setJustificacionPrioridad(justificacion);
        solicitud.setFechaObjetivo(fechaObjetivo);
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
    public List<SolicitudEntity> listarAsignadas(UsuarioEntity agente) {
        return solicitudRepository.findByAgenteAsignadoIdUsuarioOrderByFechaActualizacionDesc(
                agente.getIdUsuario());
    }

    @Override
    @Transactional(readOnly = true)
    public SolicitudEntity obtenerParaUsuario(Long idSolicitud, UsuarioEntity usuario) {
        SolicitudEntity solicitud = solicitudRepository.findById(idSolicitud)
                .orElseThrow(SolicitudNoEncontradaException::new);
        boolean esPropietario = solicitud.getSolicitante().getIdUsuario().equals(usuario.getIdUsuario());
        boolean esCoordinador = usuario.getRol() == Rol.COORDINADOR;
        boolean esAgenteAsignado = solicitud.getAgenteAsignado() != null
                && solicitud.getAgenteAsignado().getIdUsuario().equals(usuario.getIdUsuario());
        if (!esPropietario && !esCoordinador && !esAgenteAsignado) {
            throw new AccesoDenegadoException();
        }
        return solicitud;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RegistroAuditoriaEntity> historial(Long idSolicitud, UsuarioEntity usuario) {
        obtenerParaUsuario(idSolicitud, usuario);
        List<RegistroAuditoriaEntity> registros = new ArrayList<>(
                registroAuditoriaRepository.findByTipoEntidadAndEntidadId(TIPO_ENTIDAD, idSolicitud));
        registros.sort(Comparator.comparing(RegistroAuditoriaEntity::getFecha)
                .thenComparing(RegistroAuditoriaEntity::getIdRegistro)
                .reversed());
        return registros;
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
    public void asignar(Long idSolicitud, Long idAgente, UsuarioEntity coordinador) {
        if (coordinador.getRol() != Rol.COORDINADOR) {
            throw new AccesoDenegadoException();
        }
        SolicitudEntity solicitud = solicitudRepository.findById(idSolicitud)
                .orElseThrow(SolicitudNoEncontradaException::new);
        EstadoSolicitud estadoActual = solicitud.getEstado();
        if (estadoActual != EstadoSolicitud.NUEVO
                && estadoActual != EstadoSolicitud.ASIGNADO
                && estadoActual != EstadoSolicitud.REABIERTO) {
            throw new IllegalArgumentException(
                    "No se puede asignar una solicitud en estado " + estadoActual.name() + ".");
        }
        if (idAgente == null) {
            throw new IllegalArgumentException("El agente es obligatorio.");
        }
        UsuarioEntity agente = usuarioRepository.findById(idAgente)
                .orElseThrow(() -> new IllegalArgumentException("El agente indicado no existe."));
        if (agente.getIdUsuario().equals(coordinador.getIdUsuario())) {
            throw new IllegalArgumentException("El coordinador no puede asignarse la solicitud a si mismo.");
        }
        if (agente.getRol() != Rol.AGENTE) {
            throw new IllegalArgumentException("El usuario indicado no tiene rol de agente.");
        }
        if (!agente.isActivo()) {
            throw new IllegalArgumentException("El agente indicado no esta activo.");
        }

        UsuarioEntity agenteAnterior = solicitud.getAgenteAsignado();
        if (agenteAnterior != null && agenteAnterior.getIdUsuario().equals(agente.getIdUsuario())) {
            // Reasignar al mismo agente no aporta informacion: no se duplican avisos ni auditoria.
            return;
        }
        String nombreAnterior = agenteAnterior == null ? null : agenteAnterior.getNombreUsuario();

        LocalDateTime ahora = LocalDateTime.now();
        solicitud.setAgenteAsignado(agente);
        solicitud.setFechaAsignacion(ahora);
        solicitud.setAsignadoPor(coordinador);
        solicitud.setEstado(EstadoSolicitud.ASIGNADO);
        solicitud.setFechaActualizacion(ahora);
        solicitudRepository.save(solicitud);

        registrarAuditoria(solicitud, CAMPO_AGENTE, nombreAnterior, agente.getNombreUsuario(),
                ACCION_ASIGNACION, coordinador);
        notificacionService.crearAsignacion(solicitud, agente);
    }

    @Override
    @Transactional
    public void cambiarEstado(Long idSolicitud, String accion, UsuarioEntity actor) {
        SolicitudEntity solicitud = solicitudRepository.findById(idSolicitud)
                .orElseThrow(SolicitudNoEncontradaException::new);
        UsuarioEntity agenteAsignado = solicitud.getAgenteAsignado();
        if (agenteAsignado == null || !agenteAsignado.getIdUsuario().equals(actor.getIdUsuario())) {
            throw new AccesoDenegadoException();
        }
        EstadoSolicitud estadoActual = solicitud.getEstado();
        EstadoSolicitud nuevoEstado = resolverTransicion(estadoActual, accion);

        solicitud.setEstado(nuevoEstado);
        solicitud.setFechaActualizacion(LocalDateTime.now());
        solicitudRepository.save(solicitud);
        registrarAuditoria(solicitud, CAMPO_ESTADO, estadoActual.name(), nuevoEstado.name(),
                ACCION_CAMBIO_ESTADO, actor);
    }

    @Override
    @Transactional
    public void confirmar(Long idSolicitud, UsuarioEntity solicitante) {
        SolicitudEntity solicitud = solicitudRepository.findById(idSolicitud)
                .orElseThrow(SolicitudNoEncontradaException::new);
        if (!solicitud.getSolicitante().getIdUsuario().equals(solicitante.getIdUsuario())) {
            throw new AccesoDenegadoException();
        }
        if (solicitud.getEstado() != EstadoSolicitud.RESUELTO) {
            throw new IllegalArgumentException("Solo se puede confirmar una solicitud en estado RESUELTO.");
        }
        solicitud.setEstado(EstadoSolicitud.CERRADO);
        solicitud.setMotivoReapertura(null);
        solicitud.setFechaActualizacion(LocalDateTime.now());
        solicitudRepository.save(solicitud);
        registrarAuditoria(solicitud, CAMPO_ESTADO, EstadoSolicitud.RESUELTO.name(),
                EstadoSolicitud.CERRADO.name(), ACCION_CONFIRMACION, solicitante);
    }

    @Override
    @Transactional
    public void reabrir(Long idSolicitud, String motivo, UsuarioEntity solicitante) {
        SolicitudEntity solicitud = solicitudRepository.findById(idSolicitud)
                .orElseThrow(SolicitudNoEncontradaException::new);
        if (!solicitud.getSolicitante().getIdUsuario().equals(solicitante.getIdUsuario())) {
            throw new AccesoDenegadoException();
        }
        if (solicitud.getEstado() != EstadoSolicitud.RESUELTO) {
            throw new IllegalArgumentException("Solo se puede reabrir una solicitud en estado RESUELTO.");
        }
        String motivoLimpio = motivo == null ? "" : motivo.trim();
        if (motivoLimpio.isEmpty()) {
            throw new IllegalArgumentException("El motivo de reapertura es obligatorio.");
        }
        if (motivoLimpio.length() > LONGITUD_MAXIMA_TEXTO) {
            throw new IllegalArgumentException("El motivo de reapertura no puede superar los 300 caracteres.");
        }
        solicitud.setEstado(EstadoSolicitud.REABIERTO);
        solicitud.setMotivoReapertura(motivoLimpio);
        solicitud.setFechaActualizacion(LocalDateTime.now());
        solicitudRepository.save(solicitud);
        registrarAuditoria(solicitud, CAMPO_ESTADO, EstadoSolicitud.RESUELTO.name(),
                EstadoSolicitud.REABIERTO.name(), ACCION_REAPERTURA, solicitante);
        registrarAuditoria(solicitud, CAMPO_MOTIVO_REAPERTURA, null, motivoLimpio,
                ACCION_REAPERTURA, solicitante);
    }

    @Override
    @Transactional
    public void cambiarPrioridad(Long idSolicitud, PrioridadSolicitud nuevaPrioridad, String justificacion,
                                 LocalDate fechaObjetivo, UsuarioEntity actor) {
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
        if (nuevaPrioridad == PrioridadSolicitud.ALTA) {
            solicitud.setJustificacionPrioridad(validarJustificacion(justificacion));
            solicitud.setFechaObjetivo(validarFechaObjetivo(fechaObjetivo));
        } else {
            solicitud.setJustificacionPrioridad(null);
            solicitud.setFechaObjetivo(null);
        }
        solicitud.setPrioridad(nuevaPrioridad);
        solicitud.setFechaActualizacion(LocalDateTime.now());
        solicitudRepository.save(solicitud);
        registrarAuditoria(solicitud, CAMPO_PRIORIDAD, anterior.name(), nuevaPrioridad.name(),
                ACCION_CAMBIO_PRIORIDAD, actor);
    }

    private EstadoSolicitud resolverTransicion(EstadoSolicitud estadoActual, String accion) {
        String clave = accion == null ? "" : accion.trim().toLowerCase();
        return switch (clave) {
            case "iniciar" -> {
                if (estadoActual != EstadoSolicitud.ASIGNADO && estadoActual != EstadoSolicitud.REABIERTO) {
                    throw new IllegalArgumentException(
                            "La accion iniciar no es valida desde el estado " + estadoActual.name() + ".");
                }
                yield EstadoSolicitud.EN_PROCESO;
            }
            case "resolver" -> {
                if (estadoActual != EstadoSolicitud.EN_PROCESO) {
                    throw new IllegalArgumentException(
                            "La accion resolver no es valida desde el estado " + estadoActual.name() + ".");
                }
                yield EstadoSolicitud.RESUELTO;
            }
            default -> throw new IllegalArgumentException("La accion indicada no es valida.");
        };
    }

    private PrioridadSolicitud normalizarPrioridadInicial(PrioridadSolicitud prioridad) {
        if (prioridad == null || prioridad == PrioridadSolicitud.CRITICA) {
            return PrioridadSolicitud.MEDIA;
        }
        return prioridad;
    }

    private String validarJustificacion(String justificacion) {
        String limpia = justificacion == null ? "" : justificacion.trim();
        if (limpia.isEmpty()) {
            throw new IllegalArgumentException("La prioridad ALTA requiere una justificacion.");
        }
        if (limpia.length() > LONGITUD_MAXIMA_TEXTO) {
            throw new IllegalArgumentException("La justificacion no puede superar los 300 caracteres.");
        }
        return limpia;
    }

    private LocalDate validarFechaObjetivo(LocalDate fechaObjetivo) {
        if (fechaObjetivo == null) {
            throw new IllegalArgumentException("La prioridad ALTA requiere una fecha objetivo.");
        }
        if (fechaObjetivo.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha objetivo no puede ser anterior a hoy.");
        }
        return fechaObjetivo;
    }

    private void registrarAuditoria(SolicitudEntity solicitud, String campo, String valorAnterior,
                                    String valorNuevo, String accion, UsuarioEntity actor) {
        RegistroAuditoriaEntity registro = new RegistroAuditoriaEntity();
        registro.setTipoEntidad(TIPO_ENTIDAD);
        registro.setEntidadId(solicitud.getIdSolicitud());
        registro.setCampo(campo);
        registro.setValorAnterior(valorAnterior);
        registro.setValorNuevo(valorNuevo);
        registro.setActor(actor);
        registro.setFecha(LocalDateTime.now());
        registro.setAccion(accion);
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
