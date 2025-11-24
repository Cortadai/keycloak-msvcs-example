package com.example.product.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Configuración de Seguridad del Product Service
 *
 * ⭐ IDÉNTICA AL USER SERVICE ⭐
 *
 * Este SecurityConfig es prácticamente idéntico al del User Service.
 *
 * ESTO DEMUESTRA:
 * ===============
 *
 * 🎯 PATRÓN REPETIBLE:
 *    - La configuración de JWT es la misma en todos los microservicios
 *    - Solo cambian los endpoints específicos de cada servicio
 *    - Puedes copiar este SecurityConfig a cualquier microservicio nuevo
 *
 * 🎯 CONSISTENCIA:
 *    - Todos usan oauth2ResourceServer()
 *    - Todos usan SessionCreationPolicy.STATELESS
 *    - Todos deshabilitan CSRF
 *    - Todos obtienen JWT config del Config Server
 *
 * 🎯 OPPORTUNITY FOR REFACTORING:
 *    - En el futuro, podríamos mover este SecurityConfig a common-lib
 *    - Todos los microservicios lo importarían
 *    - Un solo lugar para mantener la configuración
 *    - Pero por ahora, lo dejamos separado para claridad educativa
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // ← Habilita @PreAuthorize en controllers
public class SecurityConfig {

    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Endpoints públicos vs protegidos
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .anyRequest().authenticated()
            )

            // 🔐 Validación de JWT
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> {
                    // Config viene de application.yml (Config Server):
                    // - spring.security.oauth2.resourceserver.jwt.issuer-uri
                    // - spring.security.oauth2.resourceserver.jwt.jwk-set-uri
                })
            )

            // Stateless - sin sesiones HTTP
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // CSRF deshabilitado (API REST)
            .csrf(csrf -> csrf.disable())

            // CORS - Configurado en CorsConfig.java
            .cors(cors -> cors.configurationSource(corsConfigurationSource));

        return http.build();
    }

    /**
     * NOTA: Este código es IDÉNTICO al de User Service
     * ===================================================
     *
     * Esto es intencional y demuestra:
     * 1. Consistencia de seguridad en todos los servicios
     * 2. Patrón repetible y fácil de mantener
     * 3. Configuración centralizada en Config Server
     *
     * En el futuro, podrías:
     * - Mover esto a common-lib
     * - Crear un @Configuration compartido
     * - Todos los microservicios lo importan
     * - Un solo lugar para cambios de seguridad
     */
}
