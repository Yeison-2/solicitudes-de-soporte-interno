package com.jdc.web2026i.repository;

import com.jdc.web2026i.entities.CategoriaSolicitud;
import com.jdc.web2026i.entities.EstadoSolicitud;
import com.jdc.web2026i.entities.PrioridadSolicitud;
import com.jdc.web2026i.entities.SolicitudEntity;
import com.jdc.web2026i.entities.UsuarioEntity;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SolicitudRepository extends JpaRepository<SolicitudEntity, Long> {

    List<SolicitudEntity> findBySolicitante(UsuarioEntity solicitante, Sort sort);

    boolean existsBySolicitanteIdUsuario(Long idUsuario);

    List<SolicitudEntity> findByAgenteAsignadoIdUsuarioOrderByFechaActualizacionDesc(Long idUsuario);

    /**
     * Busqueda con filtros combinables. Cada filtro lleva su bandera "sin
     * filtro": PostgreSQL no puede deducir el tipo de un parametro que solo
     * aparece en un {@code is null}, asi que el parametro real siempre viaja
     * con un valor tipado y la bandera decide si se aplica. El texto nunca se
     * concatena en la consulta. El orden es fijo (fecha de creacion
     * descendente) para que la misma combinacion devuelva el mismo resultado.
     */
    @Query("""
            select s from SolicitudEntity s
            where (:sinTexto = true
                   or lower(s.titulo) like :texto escape '!'
                   or lower(s.descripcion) like :texto escape '!')
              and (:sinEstado = true or s.estado = :estado)
              and (:sinPrioridad = true or s.prioridad = :prioridad)
              and (:sinCategoria = true or s.categoria = :categoria)
              and (:sinSolicitante = true or s.solicitante.idUsuario = :solicitanteId)
              and (:sinAgente = true
                   or (s.agenteAsignado is not null and s.agenteAsignado.idUsuario = :agenteId))
              and (:sinDesde = true or s.fechaCreacion >= :desde)
              and (:sinHasta = true or s.fechaCreacion < :hasta)
            order by s.fechaCreacion desc
            """)
    List<SolicitudEntity> buscar(@Param("texto") String texto,
                                 @Param("sinTexto") boolean sinTexto,
                                 @Param("estado") EstadoSolicitud estado,
                                 @Param("sinEstado") boolean sinEstado,
                                 @Param("prioridad") PrioridadSolicitud prioridad,
                                 @Param("sinPrioridad") boolean sinPrioridad,
                                 @Param("categoria") CategoriaSolicitud categoria,
                                 @Param("sinCategoria") boolean sinCategoria,
                                 @Param("solicitanteId") Long solicitanteId,
                                 @Param("sinSolicitante") boolean sinSolicitante,
                                 @Param("agenteId") Long agenteId,
                                 @Param("sinAgente") boolean sinAgente,
                                 @Param("desde") LocalDateTime desde,
                                 @Param("sinDesde") boolean sinDesde,
                                 @Param("hasta") LocalDateTime hasta,
                                 @Param("sinHasta") boolean sinHasta);
}
