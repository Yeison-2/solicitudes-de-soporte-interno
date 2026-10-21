package com.jdc.web2026i.services;

import com.jdc.web2026i.entities.ComentarioEntity;
import com.jdc.web2026i.entities.RegistroAuditoriaEntity;
import com.jdc.web2026i.entities.Rol;
import com.jdc.web2026i.entities.SolicitudEntity;
import com.jdc.web2026i.entities.UsuarioEntity;
import com.jdc.web2026i.repository.ComentarioRepository;
import com.jdc.web2026i.repository.RegistroAuditoriaRepository;
import com.jdc.web2026i.repository.SolicitudRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ComentarioServiceImpl implements ComentarioService {

    private static final String TIPO_ENTIDAD = "Solicitud";
    private static final String CAMPO_COMENTARIO = "comentario";
    private static final String ACCION_COMENTARIO = "COMENTARIO";
    private static final int LONGITUD_MAXIMA = 1000;

    private final ComentarioRepository comentarioRepository;
    private final SolicitudRepository solicitudRepository;
    private final RegistroAuditoriaRepository registroAuditoriaRepository;
    private final SolicitudService solicitudService;

    public ComentarioServiceImpl(ComentarioRepository comentarioRepository,
                                 SolicitudRepository solicitudRepository,
                                 RegistroAuditoriaRepository registroAuditoriaRepository,
                                 SolicitudService solicitudService) {
        this.comentarioRepository = comentarioRepository;
        this.solicitudRepository = solicitudRepository;
        this.registroAuditoriaRepository = registroAuditoriaRepository;
        this.solicitudService = solicitudService;
    }

    @Override
    @Transactional
    public ComentarioEntity registrar(Long idSolicitud, String texto, UsuarioEntity autor) {
        SolicitudEntity solicitud = solicitudRepository.findById(idSolicitud)
                .orElseThrow(SolicitudNoEncontradaException::new);
        if (autor.getRol() != Rol.AGENTE) {
            throw new AccesoDenegadoException();
        }
        UsuarioEntity agenteAsignado = solicitud.getAgenteAsignado();
        if (agenteAsignado == null || !agenteAsignado.getIdUsuario().equals(autor.getIdUsuario())) {
            throw new AccesoDenegadoException();
        }
        String limpio = texto == null ? "" : texto.trim();
        if (limpio.isEmpty()) {
            throw new IllegalArgumentException("El comentario no puede estar vacio.");
        }
        if (limpio.length() > LONGITUD_MAXIMA) {
            throw new IllegalArgumentException("El comentario no puede superar los 1000 caracteres.");
        }
        ComentarioEntity comentario = new ComentarioEntity();
        comentario.setSolicitud(solicitud);
        comentario.setAutor(autor);
        comentario.setTexto(limpio);
        comentario.setFecha(LocalDateTime.now());
        ComentarioEntity guardado = comentarioRepository.save(comentario);

        RegistroAuditoriaEntity registro = new RegistroAuditoriaEntity();
        registro.setTipoEntidad(TIPO_ENTIDAD);
        registro.setEntidadId(solicitud.getIdSolicitud());
        registro.setCampo(CAMPO_COMENTARIO);
        registro.setValorAnterior(null);
        registro.setValorNuevo("Comentario #" + guardado.getIdComentario());
        registro.setActor(autor);
        registro.setFecha(LocalDateTime.now());
        registro.setAccion(ACCION_COMENTARIO);
        registroAuditoriaRepository.save(registro);
        return guardado;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComentarioEntity> listar(Long idSolicitud, UsuarioEntity usuario) {
        solicitudService.obtenerParaUsuario(idSolicitud, usuario);
        return comentarioRepository.findBySolicitudIdSolicitudOrderByFechaAsc(idSolicitud);
    }
}
