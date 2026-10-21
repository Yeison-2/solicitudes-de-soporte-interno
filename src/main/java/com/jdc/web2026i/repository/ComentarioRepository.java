package com.jdc.web2026i.repository;

import com.jdc.web2026i.entities.ComentarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComentarioRepository extends JpaRepository<ComentarioEntity, Long> {

    List<ComentarioEntity> findBySolicitudIdSolicitudOrderByFechaAsc(Long idSolicitud);
}
