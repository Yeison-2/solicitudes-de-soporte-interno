package com.jdc.web2026i.repository;

import com.jdc.web2026i.entities.RegistroAuditoriaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RegistroAuditoriaRepository extends JpaRepository<RegistroAuditoriaEntity, Long> {

    List<RegistroAuditoriaEntity> findByTipoEntidadAndEntidadId(String tipoEntidad, Long entidadId);

    List<RegistroAuditoriaEntity> findByTipoEntidadAndEntidadIdAndAccionOrderByFechaDesc(
            String tipoEntidad, Long entidadId, String accion);

    /**
     * Historial completo con filtros opcionales por solicitud y rango de
     * fechas, en orden descendente y paginado. Solo lectura. Cada filtro usa
     * su bandera porque PostgreSQL no deduce el tipo de un parametro que solo
     * aparece en un {@code is null}.
     */
    @Query("""
            select r from RegistroAuditoriaEntity r
            where (:sinSolicitud = true or r.entidadId = :solicitudId)
              and (:sinDesde = true or r.fecha >= :desde)
              and (:sinHasta = true or r.fecha < :hasta)
            order by r.fecha desc, r.idRegistro desc
            """)
    Page<RegistroAuditoriaEntity> buscar(@Param("solicitudId") Long solicitudId,
                                        @Param("sinSolicitud") boolean sinSolicitud,
                                        @Param("desde") LocalDateTime desde,
                                        @Param("sinDesde") boolean sinDesde,
                                        @Param("hasta") LocalDateTime hasta,
                                        @Param("sinHasta") boolean sinHasta,
                                        Pageable pageable);
}
