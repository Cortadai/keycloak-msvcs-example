package com.example.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Configuración de CORS (Cross-Origin Resource Sharing) para API Gateway
 *
 * ⭐ ¿QUÉ ES CORS? ⭐
 * ==================
 *
 * CORS es un mecanismo de seguridad del navegador que controla si un frontend
 * en un dominio puede hacer requests a un backend en otro dominio.
 *
 * EJEMPLO:
 * --------
 * Frontend Angular: http://localhost:4200
 * Backend Gateway:  http://localhost:8081
 *
 * Sin CORS configurado → El navegador bloquea la request
 * Con CORS configurado → El navegador permite la request
 *
 * ¿POR QUÉ USAR WEBFLUX CORS?
 * ===========================
 *
 * El Gateway usa Spring WebFlux (programación reactiva), por lo tanto:
 * - CorsWebFilter (no CorsFilter)
 * - UrlBasedCorsConfigurationSource (reactive)
 * - No usa HttpServletRequest
 *
 * CONFIGURACIÓN POR VARIABLES DE ENTORNO:
 * ========================================
 *
 * ALLOWED_ORIGINS: Lista de orígenes permitidos separados por coma
 *   Desarrollo:  http://localhost:4200,http://localhost:3000
 *   Producción:  https://app.example.com,https://admin.example.com
 *
 * Si no se configura, usa valores por defecto para desarrollo local.
 *
 * SEGURIDAD:
 * ==========
 *
 * ✅ NUNCA usar "*" (wildcard) en producción con credentials
 * ✅ Especificar orígenes exactos
 * ✅ Limitar métodos HTTP permitidos
 * ✅ Limitar headers permitidos
 * ✅ Configurar Max Age para cachear preflight requests
 *
 * FLUJO DE CORS:
 * ==============
 *
 * 1. Frontend hace request (ej: POST /api/users)
 * 2. Navegador detecta que es cross-origin
 * 3. Navegador envía PREFLIGHT request (OPTIONS /api/users)
 * 4. CorsWebFilter responde con headers CORS
 * 5. Si OK → Navegador envía request real (POST /api/users)
 * 6. Si NO → Navegador bloquea request
 */
@Configuration
public class CorsConfig {

    private static final Logger log = LoggerFactory.getLogger(CorsConfig.class);

    /**
     * Orígenes permitidos (configurables vía variable de entorno)
     *
     * Formato: URLs separadas por coma
     * Ejemplo: http://localhost:4200,http://localhost:3000
     */
    @Value("${cors.allowed-origins:http://localhost:4200,http://localhost:3000,http://localhost:8080}")
    private String allowedOrigins;

    /**
     * Métodos HTTP permitidos (configurables vía variable de entorno)
     *
     * Por defecto: GET, POST, PUT, DELETE, OPTIONS, PATCH
     */
    @Value("${cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS,PATCH}")
    private String allowedMethods;

    /**
     * Headers permitidos (configurables vía variable de entorno)
     *
     * Por defecto: Authorization, Content-Type, X-Requested-With
     */
    @Value("${cors.allowed-headers:Authorization,Content-Type,X-Requested-With,Accept,Origin}")
    private String allowedHeaders;

    /**
     * Headers expuestos al frontend (configurables vía variable de entorno)
     *
     * Estos headers estarán disponibles en el objeto Response del frontend
     */
    @Value("${cors.exposed-headers:Authorization,X-Total-Count,X-Page-Number}")
    private String exposedHeaders;

    /**
     * Tiempo en segundos para cachear la respuesta del preflight
     *
     * 3600 segundos = 1 hora
     * Durante este tiempo, el navegador no enviará preflight requests
     */
    @Value("${cors.max-age:3600}")
    private Long maxAge;

    /**
     * Permitir credenciales (cookies, headers de autenticación)
     *
     * IMPORTANTE: Si es true, NO puedes usar "*" en allowed-origins
     */
    @Value("${cors.allow-credentials:true}")
    private Boolean allowCredentials;

    /**
     * Bean que configura el filtro CORS para WebFlux.
     *
     * Este filtro intercepta TODAS las requests y agrega headers CORS
     * antes de que lleguen a los controladores.
     *
     * @return CorsWebFilter configurado
     */
    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // ==========================================
        // ORÍGENES PERMITIDOS
        // ==========================================
        // Convierte la cadena separada por comas en lista
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        config.setAllowedOrigins(origins);

        log.info("Configuración CORS - API Gateway");
        log.info("Orígenes permitidos: {}", origins);

        // ==========================================
        // MÉTODOS HTTP PERMITIDOS
        // ==========================================
        List<String> methods = Arrays.asList(allowedMethods.split(","));
        config.setAllowedMethods(methods);
        log.info("Métodos permitidos: {}", methods);

        // ==========================================
        // HEADERS PERMITIDOS
        // ==========================================
        // Headers que el frontend puede enviar
        List<String> headers = Arrays.asList(allowedHeaders.split(","));
        config.setAllowedHeaders(headers);
        log.info("Headers permitidos: {}", headers);

        // ==========================================
        // HEADERS EXPUESTOS
        // ==========================================
        // Headers que el frontend puede leer de la respuesta
        List<String> exposed = Arrays.asList(exposedHeaders.split(","));
        config.setExposedHeaders(exposed);
        log.debug("Headers expuestos: {}", exposed);

        // ==========================================
        // CREDENCIALES
        // ==========================================
        config.setAllowCredentials(allowCredentials);
        log.info("Credenciales permitidas: {}", allowCredentials);

        // ==========================================
        // MAX AGE (Caché de preflight)
        // ==========================================
        config.setMaxAge(maxAge);
        log.debug("Max Age (preflight cache): {} segundos", maxAge);

        // ==========================================
        // APLICAR A TODAS LAS RUTAS
        // ==========================================
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsWebFilter(source);
    }

    /**
     * EJEMPLO DE USO EN FRONTEND ANGULAR:
     * ====================================
     *
     * // auth.service.ts
     * login(credentials: Credentials): Observable<TokenResponse> {
     *   return this.http.post<TokenResponse>(
     *     'http://localhost:8081/api/users/login',
     *     credentials,
     *     {
     *       withCredentials: true,  // ← Envía cookies si allowCredentials=true
     *       headers: {
     *         'Content-Type': 'application/json'
     *       }
     *     }
     *   );
     * }
     *
     * // users.service.ts
     * getUsers(token: string): Observable<User[]> {
     *   return this.http.get<User[]>(
     *     'http://localhost:8081/api/users',
     *     {
     *       headers: {
     *         'Authorization': `Bearer ${token}`,  // ← Permitido por CORS
     *         'Content-Type': 'application/json'
     *       }
     *     }
     *   );
     * }
     *
     * TROUBLESHOOTING:
     * ================
     *
     * Error: "Access to XMLHttpRequest has been blocked by CORS policy"
     * Solución:
     * 1. Verificar que el origen del frontend esté en ALLOWED_ORIGINS
     * 2. Verificar que el método HTTP esté en allowed-methods
     * 3. Verificar que los headers estén en allowed-headers
     * 4. Si usas credentials, verificar que allowCredentials=true
     *
     * Error: "The 'Access-Control-Allow-Origin' header contains multiple values"
     * Solución:
     * 1. NO configurar CORS en múltiples lugares
     * 2. Configurar solo en Gateway (no en microservicios si van vía Gateway)
     * 3. O configurar solo en microservicios si se acceden directamente
     */
}
