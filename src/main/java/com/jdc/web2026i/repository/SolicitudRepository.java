package com.jdc.web2026i.repository;

import com.jdc.web2026i.entities.SolicitudEntity;
import com.jdc.web2026i.entities.UsuarioEntity;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SolicitudRepository extends JpaRepository<SolicitudEntity, Long> {

    List<SolicitudEntity> findBySolicitante(UsuarioEntity solicitante, Sort sort);

    boolean existsBySolicitanteIdUsuario(Long idUsuario);

    List<SolicitudEntity> findByAgenteAsignadoIdUsuarioOrderByFechaActualizacionDesc(Long idUsuario);
}
