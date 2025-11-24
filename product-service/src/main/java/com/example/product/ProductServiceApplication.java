package com.example.product;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Product Service - Microservicio de Productos
 *
 * ⭐ SEGUNDO MICROSERVICIO CON JWT VALIDATION ⭐
 *
 * Este servicio es prácticamente idéntico al User Service en cuanto a seguridad:
 * - Valida JWT (SecurityConfig)
 * - Extrae información del JWT (@AuthenticationPrincipal)
 * - Controla acceso por roles (@PreAuthorize)
 *
 * DIFERENCIAS CON USER SERVICE:
 * =============================
 *
 * 1. PUERTO: 8083 (vs 8082)
 * 2. ENDPOINTS: /products (vs /users)
 * 3. LÓGICA DE NEGOCIO: Productos (vs Usuarios)
 *
 * PERO LA SEGURIDAD ES IDÉNTICA:
 * ===============================
 *
 * - Mismo SecurityConfig
 * - Mismas validaciones de JWT
 * - Misma configuración de issuer-uri y jwk-set-uri
 * - Mismo mecanismo de propagación desde el Gateway
 *
 * ESTO DEMUESTRA:
 * ===============
 *
 * 🎯 CONSISTENCIA DE SEGURIDAD:
 *    - Todos los microservicios usan la MISMA configuración de JWT
 *    - Config Server asegura consistencia
 *    - Si cambias issuer-uri → todos los servicios se actualizan
 *
 * 🎯 DEFENSE IN DEPTH:
 *    - Gateway valida JWT ✓
 *    - User Service valida JWT ✓
 *    - Product Service valida JWT ✓
 *    - Order Service valida JWT ✓
 *    - Cada servicio es autónomo en seguridad
 *
 * 🎯 ZERO TRUST:
 *    - No confías en que el Gateway validó correctamente
 *    - No confías en que otros servicios validaron
 *    - Cada servicio valida independientemente
 *
 * FLUJO DEL JWT:
 * ==============
 *
 * 1. Cliente → Gateway con JWT
 * 2. Gateway valida JWT
 * 3. Gateway → Product Service con JWT (JWTPropagationFilter)
 * 4. Product Service valida JWT (SecurityConfig)
 * 5. Product Service procesa request
 * 6. Product Service devuelve respuesta
 *
 * PERMISOS:
 * =========
 *
 * - GET /products → Cualquier usuario autenticado
 * - POST /products → Solo ADMIN
 * - PUT /products/{id} → Solo ADMIN
 * - DELETE /products/{id} → Solo ADMIN
 */
@SpringBootApplication
@EnableDiscoveryClient
public class ProductServiceApplication {

    private static final Logger log = LoggerFactory.getLogger(ProductServiceApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(ProductServiceApplication.class, args);

        log.info("Product Service iniciado en puerto 8083");
        log.info("JWT Validation: ENABLED - Validando contra: http://localhost:8080/realms/mi-realm");
        log.info("Service Discovery: ENABLED - Registrado en Eureka: http://localhost:8761");
        log.info("Endpoints disponibles:");
        log.info("  GET    /products          -> Listar productos (cualquier usuario)");
        log.info("  GET    /products/{{id}}     -> Obtener producto (cualquier usuario)");
        log.info("  POST   /products          -> Crear producto (admin only)");
        log.info("  PUT    /products/{{id}}     -> Actualizar producto (admin only)");
        log.info("  DELETE /products/{{id}}     -> Eliminar producto (admin only)");
        log.info("IMPORTANTE: Todos los endpoints requieren JWT válido");
    }
}
