package com.example.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * API Gateway - Punto de Entrada Único
 *
 * FLUJO COMPLETO DE JWT:
 * ======================
 *
 * 1. CLIENTE ENVÍA REQUEST CON JWT
 *    ↓
 *    POST http://localhost:8081/api/users/me
 *    Header: Authorization: Bearer eyJhbGciOiJSUzI1NiIs...
 *
 * 2. GATEWAY RECIBE (SecurityConfig.java)
 *    ↓
 *    - Spring Security intercepta
 *    - Extrae JWT del header Authorization
 *
 * 3. JWT VALIDATION (automático por Spring Security)
 *    ↓
 *    a) Descarga claves públicas de Keycloak (JWKS):
 *       GET http://localhost:8080/realms/mi-realm/protocol/openid-connect/certs
 *
 *    b) Valida firma del JWT:
 *       - JWT tiene 3 partes: header.payload.signature
 *       - Usa clave pública para verificar que la firma es válida
 *       - Si la firma no coincide → JWT fue modificado → RECHAZAR (401)
 *
 *    c) Valida expiración:
 *       - Extrae claim "exp" del JWT
 *       - Compara con timestamp actual
 *       - Si expiró → RECHAZAR (401)
 *
 *    d) Valida issuer:
 *       - Extrae claim "iss" del JWT
 *       - Compara con issuer-uri configurado
 *       - Si no coincide → No vino de Keycloak → RECHAZAR (401)
 *
 *    e) Si TODO está OK → Continúa
 *
 * 4. EXTRAE INFORMACIÓN DEL JWT
 *    ↓
 *    - Username (claim: preferred_username)
 *    - Roles (claim: realm_access.roles)
 *    - Email, name, etc.
 *    - Crea SecurityContext con esta info
 *
 * 5. APLICA FILTROS (JWTPropagationFilter.java)
 *    ↓
 *    - Agrega JWT al header del request interno
 *    - Request modificado: ahora incluye el token
 *
 * 6. CONSULTA EUREKA
 *    ↓
 *    - "¿Dónde está user-service?"
 *    - Eureka: "localhost:8082"
 *
 * 7. ENRUTA AL MICROSERVICIO
 *    ↓
 *    GET http://localhost:8082/users/me
 *    Header: Authorization: Bearer eyJhbGciOiJSUzI1NiIs...
 *
 * 8. MICROSERVICIO VALIDA (de nuevo, defense in depth)
 *    ↓
 *    - User Service TAMBIÉN valida el JWT
 *    - Doble validación = más seguro
 *
 * 9. RESPUESTA
 *    ↓
 *    Microservicio → Gateway → Cliente
 *
 * COMPONENTES CLAVE:
 * ==================
 * - SecurityConfig.java: Configura validación de JWT
 * - JWTPropagationFilter.java: Propaga JWT a microservicios
 * - RouteConfig.java: Define rutas (opcional, si no usamos application.yml)
 */
@SpringBootApplication
@EnableDiscoveryClient  // ← Habilita integración con Eureka
public class GatewayApplication {

    private static final Logger log = LoggerFactory.getLogger(GatewayApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);

        log.info("API Gateway iniciado en puerto 8081");
        log.info("JWT Validation: ENABLED - Validando contra: http://localhost:8080/realms/mi-realm");
        log.info("Service Discovery: ENABLED - Eureka Server: http://localhost:8761");
        log.info("Rutas configuradas:");
        log.info("  /api/users/**    -> user-service");
        log.info("  /api/products/** -> product-service");
        log.info("  /api/orders/**   -> order-service");
    }
}
