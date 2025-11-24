package com.example.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Configuración de Seguridad del Gateway
 *
 * ⭐ ESTE ES EL CÓDIGO QUE VALIDA EL JWT ⭐
 *
 * ¿QUÉ HACE ESTE CÓDIGO?
 * =======================
 * Configura Spring Security para:
 * 1. Validar TODOS los JWT que llegan al Gateway
 * 2. Extraer información del JWT (username, roles)
 * 3. Permitir/denegar acceso basado en el JWT
 *
 * IMPORTANTE: Gateway usa WebFlux (programación reactiva)
 * Por eso usamos ServerHttpSecurity en vez de HttpSecurity
 *
 * FLUJO DE VALIDACIÓN:
 * ====================
 *
 * Request llega → SecurityWebFilterChain
 *                        ↓
 *        ¿Tiene header Authorization: Bearer {token}?
 *                        ↓
 *                  Sí          No
 *                  ↓            ↓
 *        Validar JWT       401 Unauthorized
 *                  ↓
 *     ¿JWT válido? (firma, exp, issuer)
 *                  ↓
 *            Sí          No
 *            ↓            ↓
 *     Permitir acceso   401 Unauthorized
 *            ↓
 *   Extraer roles y crear SecurityContext
 *            ↓
 *   Continuar con filtros (routing, etc.)
 */
@Configuration
@EnableWebFluxSecurity  // ← WebFlux = reactive (no bloqueante)
public class SecurityConfig {

    /**
     * Configuración principal de seguridad.
     *
     * Este bean define:
     * - Qué rutas requieren autenticación
     * - Cómo validar JWT
     * - Qué hacer si falla la validación
     *
     * @param http Configurador de seguridad
     * @return Cadena de filtros de seguridad
     */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
            // ==========================================
            // AUTORIZACIÓN - Qué rutas requieren JWT
            // ==========================================
            .authorizeExchange(exchange -> exchange
                // Rutas públicas (sin JWT)
                .pathMatchers("/actuator/**").permitAll()  // Health checks
                .pathMatchers("/eureka/**").permitAll()    // Eureka (si expuesto)

                // TODAS las demás rutas requieren JWT válido
                .anyExchange().authenticated()
            )

            // ==========================================
            // 🔐 VALIDACIÓN DE JWT
            // ==========================================
            // Esta es la configuración MÁS IMPORTANTE
            //
            // oauth2ResourceServer(): Configura el Gateway como Resource Server
            // - Resource Server = servidor que acepta y valida tokens OAuth2
            // - NO genera tokens (eso lo hace Keycloak)
            // - SOLO valida tokens
            //
            // jwt(): Especifica que los tokens son JWT
            // - No SAML, no opaque tokens
            // - JWT = JSON Web Token
            //
            // Spring Security automáticamente:
            // 1. Extrae JWT del header "Authorization: Bearer {token}"
            // 2. Descarga claves públicas de Keycloak (jwk-set-uri)
            // 3. Valida firma usando clave pública
            // 4. Valida expiración (claim "exp")
            // 5. Valida issuer (claim "iss")
            // 6. Si TODO OK → request continúa
            // 7. Si ALGO falla → 401 Unauthorized
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> {
                    // Configuración adicional del JWT (si es necesario)
                    // Por defecto usa la config de application.yml:
                    //   spring.security.oauth2.resourceserver.jwt.issuer-uri
                    //   spring.security.oauth2.resourceserver.jwt.jwk-set-uri

                    // Aquí podrías agregar:
                    // - Custom JWT decoder
                    // - Custom JWT converter
                    // - Audience validation
                    // - etc.
                })
            )

            // ==========================================
            // CSRF - Deshabilitado para API REST
            // ==========================================
            // CSRF (Cross-Site Request Forgery) es una protección
            // para formularios HTML, no para APIs REST
            .csrf(csrf -> csrf.disable())

            // ==========================================
            // CORS - Permitir llamadas desde frontend
            // ==========================================
            // CORS está configurado en CorsConfig.java
            // El bean CorsWebFilter se aplica automáticamente
            //
            // Para WebFlux, NO necesitamos configurar CORS aquí
            // El CorsWebFilter bean se encarga de todo
            //
            // Orígenes permitidos: configurables vía CORS_ALLOWED_ORIGINS
            // Por defecto: http://localhost:4200 (Angular)
            .cors(cors -> cors.disable());  // Deshabilitado porque usamos CorsWebFilter bean

        return http.build();
    }

    /**
     * NOTA: ¿Por qué no veo aquí el jwk-set-uri o issuer-uri?
     * =========================================================
     *
     * Esos parámetros están en application.yml (o en Config Server):
     *
     * spring:
     *   security:
     *     oauth2:
     *       resourceserver:
     *         jwt:
     *           issuer-uri: http://localhost:8080/realms/mi-realm
     *           jwk-set-uri: http://localhost:8080/realms/mi-realm/protocol/openid-connect/certs
     *
     * Spring Boot los lee automáticamente y configura el JWT decoder.
     *
     * Cuando haces .oauth2ResourceServer(oauth2 -> oauth2.jwt()),
     * Spring usa esos valores de application.yml automáticamente.
     */

    /**
     * VENTAJAS DE VALIDAR JWT EN EL GATEWAY:
     * ========================================
     *
     * ✅ Validación centralizada:
     *    - Un solo punto donde validar
     *    - Más fácil de auditar
     *    - Logs centralizados
     *
     * ✅ Protección de microservicios:
     *    - Microservicios NUNCA reciben requests sin JWT válido
     *    - Reduce carga en microservicios (no tienen que validar)
     *
     * ✅ Fail fast:
     *    - Si el JWT es inválido, se rechaza aquí
     *    - No se propaga a servicios downstream
     *
     * ✅ Consistencia:
     *    - Todos los servicios protegidos de la misma manera
     *
     * DESVENTAJAS / CONSIDERACIONES:
     * ===============================
     *
     * ⚠️ Single point of failure:
     *    - Si el Gateway cae, nada funciona
     *    - Solución: Múltiples instancias del Gateway + Load Balancer
     *
     * ⚠️ Performance bottleneck:
     *    - TODO el tráfico pasa por aquí
     *    - Solución: Gateway es liviano y escalable horizontalmente
     *
     * ⚠️ Aún necesitas validar en microservicios (defense in depth):
     *    - Por si alguien llama directamente al microservicio
     *    - Por si el Gateway es comprometido
     *    - Zero trust architecture
     */
}
