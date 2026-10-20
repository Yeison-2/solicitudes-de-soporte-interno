package com.jdc.web2026i.repository;

import com.jdc.web2026i.entities.RegistroAuditoriaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegistroAuditoriaRepository extends JpaRepository<RegistroAuditoriaEntity, Long> {

    List<RegistroAuditoriaEntity> findByTipoEntidadAndEntidadId(String tipoEntidad, Long entidadId);
}
