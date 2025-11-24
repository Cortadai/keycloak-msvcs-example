package com.example.order.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Feign Client Interceptor - Propaga JWT a otros microservicios
 *
 * ⭐ ESTE ES EL CORAZÓN DE LA PROPAGACIÓN INTER-SERVICE ⭐
 *
 * ¿QUÉ HACE?
 * ==========
 *
 * Cuando Order Service llama a otro microservicio usando Feign:
 * 1. Intercepta la request antes de enviarla
 * 2. Obtiene el JWT del SecurityContext
 * 3. Agrega el JWT al header Authorization
 * 4. La request se envía con el JWT
 *
 * FLUJO DETALLADO:
 * ================
 *
 * SIN ESTE INTERCEPTOR:
 * ---------------------
 * 1. Cliente → Gateway con JWT
 * 2. Gateway → Order Service con JWT
 * 3. Order Service valida JWT ✓
 * 4. Order Service → User Service... ❌ SIN JWT
 * 5. User Service: "No JWT → 401 Unauthorized"
 *
 * CON ESTE INTERCEPTOR:
 * ---------------------
 * 1. Cliente → Gateway con JWT
 * 2. Gateway → Order Service con JWT
 * 3. Order Service valida JWT ✓
 * 4. Order Service → User Service
 *    ↑
 *    ESTE INTERCEPTOR agrega JWT aquí
 * 5. User Service recibe JWT ✓
 * 6. User Service valida JWT ✓
 * 7. User Service procesa request ✓
 *
 * COMPARACIÓN CON GATEWAY:
 * ========================
 *
 * GATEWAY (JWTPropagationFilter):
 * - Propaga JWT de Cliente → Microservicio
 * - Usa reactive (WebFlux)
 * - GatewayFilter
 *
 * ORDER SERVICE (FeignClientInterceptor):
 * - Propaga JWT de Microservicio → Microservicio
 * - Usa Spring MVC tradicional
 * - RequestInterceptor de Feign
 *
 * PERO EL CONCEPTO ES EL MISMO:
 * - Obtener JWT del SecurityContext
 * - Agregarlo al header Authorization
 * - Request continúa con JWT
 *
 * CÓDIGO PASO A PASO:
 * ===================
 *
 * 1. SecurityContextHolder.getContext()
 *    → Obtiene el SecurityContext (thread-local)
 *
 * 2. .getAuthentication()
 *    → Obtiene la Authentication (contiene el JWT)
 *
 * 3. if (authentication instanceof JwtAuthenticationToken)
 *    → Verifica que es JWT (no otro tipo de auth)
 *
 * 4. ((JwtAuthenticationToken) authentication).getToken()
 *    → Obtiene el Jwt object
 *
 * 5. jwt.getTokenValue()
 *    → Obtiene el token como String
 *
 * 6. requestTemplate.header("Authorization", "Bearer " + token)
 *    → Agrega el header a la request de Feign
 *
 * IMPORTANTE:
 * ===========
 *
 * Este interceptor se aplica a TODAS las llamadas Feign.
 *
 * Eso significa:
 * - UserServiceClient → automáticamente con JWT
 * - ProductServiceClient → automáticamente con JWT
 * - Cualquier otro FeignClient → automáticamente con JWT
 *
 * No necesitas agregar manualmente el header en cada llamada.
 */
@Component
public class FeignClientInterceptor implements RequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(FeignClientInterceptor.class);

    /**
     * Intercepta TODAS las requests de Feign y agrega JWT.
     *
     * Este método se ejecuta ANTES de que la request se envíe.
     *
     * @param requestTemplate Template de la request de Feign
     */
    @Override
    public void apply(RequestTemplate requestTemplate) {
        // ==========================================
        // 1. OBTENER EL SECURITY CONTEXT
        // ==========================================
        // SecurityContextHolder es thread-local, contiene la info del usuario actual
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // ==========================================
        // 2. VERIFICAR QUE HAY AUTENTICACIÓN
        // ==========================================
        if (authentication == null) {
            log.warn("No authentication found in SecurityContext - Feign request will be sent WITHOUT JWT - Target: {}",
                requestTemplate.url());
            return;
        }

        // ==========================================
        // 3. VERIFICAR QUE ES JWT
        // ==========================================
        if (!(authentication instanceof JwtAuthenticationToken)) {
            log.warn("Authentication is not JWT - Type: {}", authentication.getClass().getName());
            return;
        }

        // ==========================================
        // 4. EXTRAER EL JWT TOKEN
        // ==========================================
        JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) authentication;
        Jwt jwt = jwtAuth.getToken();
        String tokenValue = jwt.getTokenValue();

        // ==========================================
        // 5. AGREGAR JWT AL HEADER
        // ==========================================
        requestTemplate.header("Authorization", "Bearer " + tokenValue);

        // Log para debugging
        String tokenPreview = tokenValue.substring(0, Math.min(20, tokenValue.length())) + "...";
        log.debug("Feign Client Interceptor - Usuario: {}, Destino: {}, JWT: Bearer {}",
            jwt.getClaimAsString("preferred_username"), requestTemplate.url(), tokenPreview);
    }

    /**
     * TESTING:
     * ========
     *
     * Para verificar que el interceptor funciona:
     *
     * 1. Crear orden (esto llama internamente a User y Product Service):
     *    curl -X POST -H "Authorization: Bearer $TOKEN" \
     *      -H "Content-Type: application/json" \
     *      -d '{"productId":1,"quantity":2}' \
     *      http://localhost:8081/api/orders
     *
     * 2. Observar los logs del Order Service:
     *    - Verás "🔗 Feign Client Interceptor"
     *    - Verás "Usuario: user"
     *    - Verás "Destino: http://user-service/users/me"
     *    - Verás "JWT agregado: Bearer ey..."
     *
     * 3. Observar los logs de User Service y Product Service:
     *    - Verás "📋 GET /users/me"
     *    - Verás "Usuario autenticado: user"
     *    - Esto confirma que recibieron el JWT
     *
     * Si quitas este interceptor:
     * - Order Service → User Service SIN JWT
     * - User Service → 401 Unauthorized
     * - Order Service → Error al crear orden
     */
}
