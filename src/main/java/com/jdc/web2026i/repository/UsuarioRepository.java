package com.jdc.web2026i.repository;

import com.jdc.web2026i.entities.Rol;
import com.jdc.web2026i.entities.UsuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<UsuarioEntity, Long> {

    Optional<UsuarioEntity> findByNombreUsuario(String nombreUsuario);

    boolean existsByNombreUsuario(String nombreUsuario);

    List<UsuarioEntity> findByRolAndActivoTrue(Rol rol);
}
