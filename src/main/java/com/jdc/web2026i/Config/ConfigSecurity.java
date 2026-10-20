package com.jdc.web2026i.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Reglas de acceso por rol. La autorizacion se aplica en el servidor:
 * ocultar enlaces en las vistas es solo cosmetico.
 */
@Configuration
@EnableWebSecurity
public class ConfigSecurity {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(autorizacion -> autorizacion
                        .requestMatchers("/login", "/css/**", "/error", "/favicon.ico").permitAll()
                        .requestMatchers("/solicitudes/nueva").hasRole("SOLICITANTE")
                        .requestMatchers(HttpMethod.GET, "/solicitudes").hasRole("SOLICITANTE")
                        .requestMatchers("/coordinacion/**").hasRole("COORDINADOR")
                        .requestMatchers("/solicitudes/**").authenticated()
                        .anyRequest().authenticated())
                .formLogin(formulario -> formulario
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/inicio", true)
                        .failureUrl("/login?error")
                        .permitAll())
                .logout(cierre -> cierre
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll())
                .csrf(Customizer.withDefaults());
        return http.build();
    }
}
