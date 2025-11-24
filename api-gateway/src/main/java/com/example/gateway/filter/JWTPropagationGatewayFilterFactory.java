package com.example.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * JWT Propagation Filter
 *
 * ⭐ ESTE FILTRO ES CLAVE PARA ENTENDER CÓMO EL JWT VIAJA ⭐
 *
 * PROPÓSITO:
 * ==========
 * Agregar el JWT al header "Authorization" del request que va
 * del Gateway → Microservicio
 *
 * ¿POR QUÉ ES NECESARIO?
 * =======================
 *
 * PROBLEMA SIN ESTE FILTRO:
 * --------------------------
 * 1. Cliente → Gateway con JWT ✅
 * 2. Gateway valida JWT ✅
 * 3. Gateway → Microservicio... ❌ SIN JWT
 * 4. Microservicio: "No tengo JWT, rechazar" → 401
 *
 * SOLUCIÓN CON ESTE FILTRO:
 * --------------------------
 * 1. Cliente → Gateway con JWT ✅
 * 2. Gateway valida JWT ✅
 * 3. ESTE FILTRO agrega JWT al request interno
 * 4. Gateway → Microservicio CON JWT ✅
 * 5. Microservicio valida JWT ✅
 * 6. Microservicio procesa request ✅
 *
 * FLUJO DETALLADO:
 * ================
 *
 * A) ANTES DEL FILTRO:
 *    Request original del cliente:
 *    ---------------------------
 *    GET http://localhost:8081/api/users/me
 *    Authorization: Bearer eyJhbGc...
 *
 * B) GATEWAY VALIDA (SecurityConfig.java)
 *    - JWT válido ✓
 *    - SecurityContext creado con info del usuario
 *
 * C) ESTE FILTRO SE EJECUTA:
 *    1. Obtiene SecurityContext (contiene el JWT validado)
 *    2. Extrae el JWT token
 *    3. Agrega header "Authorization: Bearer {token}" al request interno
 *
 * D) DESPUÉS DEL FILTRO:
 *    Request modificado que va al microservicio:
 *    ------------------------------------------
 *    GET http://localhost:8082/users/me
 *    Authorization: Bearer eyJhbGc...  ← AGREGADO POR ESTE FILTRO
 *
 * E) MICROSERVICIO RECIBE:
 *    - Tiene el JWT en el header
 *    - Puede validarlo independientemente
 *    - Defense in depth ✓
 */
@Component
public class JWTPropagationGatewayFilterFactory extends AbstractGatewayFilterFactory<JWTPropagationGatewayFilterFactory.Config> {

    private static final Logger log = LoggerFactory.getLogger(JWTPropagationGatewayFilterFactory.class);

    public JWTPropagationGatewayFilterFactory() {
        super(Config.class);
    }

    /**
     * Aplica el filtro al request.
     *
     * Este método se ejecuta ANTES de que el request sea enrutado
     * al microservicio downstream.
     *
     * @param config Configuración del filtro
     * @return GatewayFilter configurado
     */
    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            // ==========================================
            // 1. OBTENER EL SECURITY CONTEXT
            // ==========================================
            // ReactiveSecurityContextHolder contiene la información
            // del usuario autenticado (incluido el JWT)
            //
            // Esto es reactivo (Mono), no bloqueante
            return ReactiveSecurityContextHolder.getContext()

                // ==========================================
                // 2. EXTRAER LA AUTENTICACIÓN
                // ==========================================
                .map(securityContext -> securityContext.getAuthentication())

                // ==========================================
                // 3. EXTRAER EL JWT TOKEN
                // ==========================================
                .filter(authentication -> authentication instanceof JwtAuthenticationToken)
                .cast(JwtAuthenticationToken.class)
                .map(authentication -> {
                    // El JwtAuthenticationToken contiene el JWT original
                    Jwt jwt = authentication.getToken();

                    // Obtener el token como string
                    String tokenValue = jwt.getTokenValue();

                    if (log.isDebugEnabled()) {
                        log.debug("JWT Propagation Filter - Usuario: {}, Token: {}..., Destino: {}",
                            jwt.getClaimAsString("preferred_username"),
                            tokenValue.substring(0, Math.min(50, tokenValue.length())),
                            exchange.getRequest().getURI());
                    } else {
                        log.info("JWT Propagation Filter - Usuario: {}", jwt.getClaimAsString("preferred_username"));
                    }

                    return tokenValue;
                })

                // ==========================================
                // 4. AGREGAR JWT AL HEADER DEL REQUEST
                // ==========================================
                .flatMap(token -> {
                    // Modificar el request para incluir el header Authorization
                    // IMPORTANTE: Crear el request modificado y asignarlo al exchange
                    var modifiedRequest = exchange.getRequest()
                        .mutate()
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .build();

                    // Crear un nuevo exchange con el request modificado
                    var modifiedExchange = exchange.mutate()
                        .request(modifiedRequest)
                        .build();

                    // Continuar con la cadena de filtros usando el exchange modificado
                    // El request ahora tiene el JWT en el header
                    return chain.filter(modifiedExchange);
                })

                // ==========================================
                // 5. CASO SIN AUTENTICACIÓN (fallback)
                // ==========================================
                // En caso de que no haya SecurityContext (edge case en el pipeline reactivo),
                // continuar sin agregar el header. El microservicio validará el JWT original
                // que viene en el request del cliente.
                .switchIfEmpty(chain.filter(exchange));
        };
    }

    /**
     * Configuración del filtro (vacía en este caso).
     *
     * Podrías agregar configuración como:
     * - Qué header usar (por si no quieres "Authorization")
     * - Prefijo del token (por si no quieres "Bearer")
     * - etc.
     */
    public static class Config {
        // Configuración vacía por ahora
        // Ejemplo de lo que podrías agregar:
        // private String headerName = "Authorization";
        // private String tokenPrefix = "Bearer ";
    }

    /**
     * IMPORTANTE: ¿Cómo se usa este filtro?
     * ======================================
     *
     * Este filtro se aplica en application.yml (o gateway.yml):
     *
     * spring:
     *   cloud:
     *     gateway:
     *       routes:
     *         - id: user-service
     *           uri: lb://user-service
     *           predicates:
     *             - Path=/api/users/**
     *           filters:
     *             - name: JWTPropagation  ← AQUÍ SE USA
     *
     * Cuando defines "- name: JWTPropagation", Spring busca un bean
     * con nombre "JWTPropagationGatewayFilterFactory" y lo aplica al route.
     *
     * Por eso esta clase se llama "JWTPropagationGatewayFilterFactory" y tiene @Component.
     */

    /**
     * ALTERNATIVA: Global Filter
     * ===========================
     *
     * Si quieres que este filtro se aplique a TODAS las rutas
     * automáticamente (sin tener que agregarlo en cada route),
     * puedes implementar GlobalFilter en vez de GatewayFilterFactory:
     *
     * @Component
     * public class JWTPropagationGlobalFilter implements GlobalFilter, Ordered {
     *     @Override
     *     public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
     *         // Mismo código de arriba
     *     }
     *
     *     @Override
     *     public int getOrder() {
     *         return -1;  // Ejecutar antes de otros filtros
     *     }
     * }
     *
     * Ventaja: No necesitas agregarlo en application.yml
     * Desventaja: Se aplica a TODOS los routes (incluso los que no quieres)
     */
}
