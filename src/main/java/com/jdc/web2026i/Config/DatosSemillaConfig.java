package com.jdc.web2026i.Config;

import com.jdc.web2026i.entities.Rol;
import com.jdc.web2026i.entities.UsuarioEntity;
import com.jdc.web2026i.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

/**
 * Crea las cuentas iniciales si no existen. Es idempotente: si el usuario ya
 * esta registrado no se modifica. La clave se toma de la variable de entorno
 * APP_SEED_PASSWORD.
 */
@Configuration
public class DatosSemillaConfig {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${APP_SEED_PASSWORD:Clave#2026}")
    private String claveSemilla;

    public DatosSemillaConfig(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    public CommandLineRunner cargarDatosSemilla() {
        return args -> {
            crearSiNoExiste("coordinador1", "Coordinador Uno", Rol.COORDINADOR);
            crearSiNoExiste("agente1", "Agente Uno", Rol.AGENTE);
            crearSiNoExiste("agente2", "Agente Dos", Rol.AGENTE);
            crearSiNoExiste("solicitante1", "Solicitante Uno", Rol.SOLICITANTE);
            crearSiNoExiste("solicitante2", "Solicitante Dos", Rol.SOLICITANTE);
            crearSiNoExiste("auditor1", "Auditor Uno", Rol.AUDITOR);
        };
    }

    private void crearSiNoExiste(String nombreUsuario, String nombreCompleto, Rol rol) {
        if (usuarioRepository.existsByNombreUsuario(nombreUsuario)) {
            return;
        }
        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setNombreUsuario(nombreUsuario);
        usuario.setNombreCompleto(nombreCompleto);
        usuario.setClaveHash(passwordEncoder.encode(claveSemilla));
        usuario.setRol(rol);
        usuario.setActivo(true);
        usuario.setFechaCreacion(LocalDateTime.now());
        usuarioRepository.save(usuario);
    }
}
