package com.example.user.config;

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
 * Configuración de Seguridad del User Service
 *
 * ⭐ SEGUNDA CAPA DE VALIDACIÓN DE JWT ⭐
 *
 * DIFERENCIAS CON EL GATEWAY:
 * ===========================
 *
 * 1. GATEWAY:
 *    - Usa WebFlux (reactivo)
 *    - ServerHttpSecurity
 *    - authorizeExchange()
 *
 * 2. MICROSERVICIO:
 *    - Usa Spring MVC (tradicional)
 *    - HttpSecurity
 *    - authorizeHttpRequests()
 *
 * PERO EL CONCEPTO ES EL MISMO:
 * - oauth2ResourceServer() → validar JWT
 * - jwt() → tipo de token
 * - issuer-uri y jwk-set-uri vienen de Config Server
 *
 * VALIDACIÓN DE JWT:
 * ==================
 *
 * Spring Security automáticamente:
 * 1. Extrae JWT del header Authorization
 * 2. Descarga claves públicas de Keycloak (JWKS)
 * 3. Valida firma
 * 4. Valida expiración
 * 5. Valida issuer
 * 6. Crea SecurityContext con la información del usuario
 *
 * Si CUALQUIERA de estas validaciones falla → 401 Unauthorized
 *
 * ROLES Y PERMISOS:
 * =================
 *
 * @EnableMethodSecurity permite usar anotaciones en los controllers:
 * - @PreAuthorize("hasRole('ADMIN')")
 * - @PreAuthorize("hasAuthority('SCOPE_read')")
 * - @Secured("ROLE_ADMIN")
 *
 * Los roles vienen del JWT:
 * - Keycloak los pone en: realm_access.roles
 * - Spring Security los extrae automáticamente
 * - Se convierten a GrantedAuthority
 *
 * STATELESS:
 * ==========
 *
 * SessionCreationPolicy.STATELESS:
 * - No crear sesiones HTTP
 * - Cada request debe incluir JWT
 * - Microservicios son stateless por naturaleza
 * - Escalabilidad horizontal sin problemas
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // ← Habilita @PreAuthorize, @Secured, etc.
public class SecurityConfig {

    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    /**
     * Configuración principal de seguridad.
     *
     * Este filtro intercepta TODAS las requests y valida el JWT.
     *
     * @param http Configurador de seguridad
     * @return Cadena de filtros de seguridad
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // ==========================================
            // AUTORIZACIÓN - Qué endpoints requieren JWT
            // ==========================================
            .authorizeHttpRequests(auth -> auth
                // Endpoints públicos (sin JWT)
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()

                // TODOS los demás endpoints requieren JWT válido
                .anyRequest().authenticated()
            )

            // ==========================================
            // 🔐 VALIDACIÓN DE JWT
            // ==========================================
            //
            // Misma configuración que en el Gateway:
            // - oauth2ResourceServer(): Este es un Resource Server
            // - jwt(): Los tokens son JWT
            //
            // Spring Security lee de application.yml:
            //   spring.security.oauth2.resourceserver.jwt.issuer-uri
            //   spring.security.oauth2.resourceserver.jwt.jwk-set-uri
            //
            // Estos valores vienen del Config Server (application.yml compartido)
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> {
                    // Aquí podrías agregar configuración custom:
                    // - Custom JWT decoder
                    // - Custom JWT converter (para roles)
                    // - Audience validation
                    // - etc.

                    // Por defecto, Spring Security:
                    // 1. Convierte realm_access.roles a GrantedAuthority
                    // 2. Prefija roles con "ROLE_"
                    //    Ejemplo: "admin" → "ROLE_admin"
                    // 3. Crea JwtAuthenticationToken
                })
            )

            // ==========================================
            // STATELESS - Sin sesiones HTTP
            // ==========================================
            //
            // IMPORTANTE para microservicios:
            // - No crear HttpSession
            // - Cada request incluye JWT
            // - Escalabilidad horizontal sin sticky sessions
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // ==========================================
            // CSRF - Deshabilitado para API REST
            // ==========================================
            //
            // CSRF solo es necesario para formularios HTML
            // APIs REST con JWT no necesitan CSRF
            .csrf(csrf -> csrf.disable())

            // ==========================================
            // CORS - Configurado en CorsConfig.java
            // ==========================================
            //
            // CORS está habilitado con configuración de CorsConfig.java
            // Orígenes permitidos: configurables vía CORS_ALLOWED_ORIGINS
            // Por defecto: http://localhost:4200 (Angular)
            //
            // IMPORTANTE:
            // - Si el frontend llama vía Gateway, CORS se maneja en Gateway
            // - Si el frontend llama directo al servicio, CORS se maneja aquí
            // - Defense in depth: configurar en ambos lugares
            .cors(cors -> cors.configurationSource(corsConfigurationSource));

        return http.build();
    }

    /**
     * NOTA: Conversión de Roles
     * ==========================
     *
     * Por defecto, Spring Security espera roles con prefijo "ROLE_".
     *
     * Si en Keycloak tienes role "admin", Spring lo convierte a "ROLE_admin".
     *
     * Luego puedes usar:
     * - hasRole("admin")         → busca "ROLE_admin"
     * - hasAuthority("ROLE_admin") → busca "ROLE_admin" exacto
     *
     * Si quieres cambiar esta conversión, puedes crear un custom JwtAuthenticationConverter:
     *
     * @Bean
     * public JwtAuthenticationConverter jwtAuthenticationConverter() {
     *     JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
     *     grantedAuthoritiesConverter.setAuthoritiesClaimName("realm_access.roles");
     *     grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");
     *
     *     JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
     *     jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
     *     return jwtAuthenticationConverter;
     * }
     *
     * Pero por defecto, Spring ya hace esto correctamente para Keycloak.
     */

    /**
     * TESTING:
     * ========
     *
     * Para probar la validación de JWT:
     *
     * 1. Sin JWT:
     *    curl http://localhost:8082/users/me
     *    → 401 Unauthorized
     *
     * 2. Con JWT inválido:
     *    curl -H "Authorization: Bearer fake-token" http://localhost:8082/users/me
     *    → 401 Unauthorized
     *
     * 3. Con JWT válido:
     *    curl -H "Authorization: Bearer {token-de-keycloak}" http://localhost:8082/users/me
     *    → 200 OK
     *
     * 4. Con JWT válido pero expirado:
     *    → 401 Unauthorized
     *
     * 5. Con JWT válido pero de otro realm:
     *    → 401 Unauthorized (issuer no coincide)
     */
}
