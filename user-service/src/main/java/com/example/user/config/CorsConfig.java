package com.example.user.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Configuración de CORS (Cross-Origin Resource Sharing) para User Service
 *
 * ⭐ ¿CUÁNDO SE USA ESTA CONFIGURACIÓN? ⭐
 * =======================================
 *
 * CASO 1: Frontend llama directamente al microservicio (sin Gateway)
 *   Frontend (4200) → User Service (8082)
 *   En este caso, NECESITAS esta configuración CORS
 *
 * CASO 2: Frontend llama vía Gateway
 *   Frontend (4200) → Gateway (8081) → User Service (8082)
 *   En este caso, CORS se maneja en el Gateway
 *   Pero NO hace daño tener CORS aquí también (defense in depth)
 *
 * RECOMENDACIÓN:
 * ==============
 * - Configurar CORS en el Gateway (ya lo hicimos)
 * - TAMBIÉN configurar CORS en microservicios (por si se acceden directamente)
 * - En producción, los microservicios NO deberían ser accesibles directamente
 *   (solo vía Gateway)
 *
 * DIFERENCIAS CON GATEWAY:
 * ========================
 *
 * Gateway (WebFlux):
 *   - CorsWebFilter
 *   - UrlBasedCorsConfigurationSource (reactive)
 *
 * Microservicio (Spring MVC):
 *   - CorsConfigurationSource
 *   - UrlBasedCorsConfigurationSource (servlet)
 *
 * CONFIGURACIÓN POR VARIABLES DE ENTORNO:
 * ========================================
 *
 * CORS_ALLOWED_ORIGINS: Lista de orígenes permitidos separados por coma
 *   Desarrollo:  http://localhost:4200,http://localhost:3000
 *   Producción:  https://app.example.com
 *
 * Si no se configura, usa valores por defecto para desarrollo local.
 */
@Configuration
public class CorsConfig {

    private static final Logger log = LoggerFactory.getLogger(CorsConfig.class);

    /**
     * Orígenes permitidos (configurables vía variable de entorno)
     */
    @Value("${cors.allowed-origins:http://localhost:4200,http://localhost:3000,http://localhost:8080}")
    private String allowedOrigins;

    /**
     * Métodos HTTP permitidos
     */
    @Value("${cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS,PATCH}")
    private String allowedMethods;

    /**
     * Headers permitidos
     */
    @Value("${cors.allowed-headers:Authorization,Content-Type,X-Requested-With,Accept,Origin}")
    private String allowedHeaders;

    /**
     * Headers expuestos al frontend
     */
    @Value("${cors.exposed-headers:Authorization,X-Total-Count,X-Page-Number}")
    private String exposedHeaders;

    /**
     * Tiempo de caché para preflight requests (1 hora)
     */
    @Value("${cors.max-age:3600}")
    private Long maxAge;

    /**
     * Permitir credenciales (cookies, headers de autenticación)
     */
    @Value("${cors.allow-credentials:true}")
    private Boolean allowCredentials;

    /**
     * Bean que configura CORS para Spring MVC.
     *
     * Este bean será usado por SecurityConfig:
     *   .cors(cors -> cors.configurationSource(corsConfigurationSource()))
     *
     * @return CorsConfigurationSource configurado
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Orígenes permitidos
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        config.setAllowedOrigins(origins);

        log.info("Configuración CORS - User Service");
        log.info("Orígenes permitidos: {}", origins);

        // Métodos HTTP permitidos
        List<String> methods = Arrays.asList(allowedMethods.split(","));
        config.setAllowedMethods(methods);
        log.debug("Métodos permitidos: {}", methods);

        // Headers permitidos
        List<String> headers = Arrays.asList(allowedHeaders.split(","));
        config.setAllowedHeaders(headers);
        log.debug("Headers permitidos: {}", headers);

        // Headers expuestos
        List<String> exposed = Arrays.asList(exposedHeaders.split(","));
        config.setExposedHeaders(exposed);
        log.debug("Headers expuestos: {}", exposed);

        // Credenciales
        config.setAllowCredentials(allowCredentials);
        log.debug("Credenciales permitidas: {}", allowCredentials);

        // Max Age
        config.setMaxAge(maxAge);
        log.debug("Max Age (preflight cache): {} segundos", maxAge);

        // Aplicar a todas las rutas
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }

    /**
     * EJEMPLO DE LLAMADA DIRECTA DESDE ANGULAR:
     * ==========================================
     *
     * // users.service.ts
     * getMyProfile(token: string): Observable<UserProfile> {
     *   return this.http.get<UserProfile>(
     *     'http://localhost:8082/api/users/me',  // ← Directo al microservicio
     *     {
     *       headers: {
     *         'Authorization': `Bearer ${token}`
     *       }
     *     }
     *   );
     * }
     *
     * RECOMENDACIÓN EN PRODUCCIÓN:
     * =============================
     *
     * ❌ NO llamar directamente a microservicios desde frontend
     * ✅ Llamar siempre vía Gateway
     *
     * Razones:
     * 1. Gateway maneja autenticación centralizada
     * 2. Gateway maneja rate limiting
     * 3. Gateway maneja circuit breakers
     * 4. Gateway oculta la arquitectura interna
     * 5. Más fácil cambiar microservicios sin afectar frontend
     *
     * PERO:
     * - En desarrollo, puede ser útil llamar directamente para debugging
     * - Por eso mantenemos CORS configurado en microservicios también
     */
}
