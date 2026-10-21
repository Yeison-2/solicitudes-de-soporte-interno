package com.jdc.web2026i.repository;

import com.jdc.web2026i.entities.NotificacionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificacionRepository extends JpaRepository<NotificacionEntity, Long> {

    List<NotificacionEntity> findByDestinatarioIdUsuarioOrderByFechaDesc(Long idUsuario);

    long countByDestinatarioIdUsuarioAndLeidaFalse(Long idUsuario);
}
