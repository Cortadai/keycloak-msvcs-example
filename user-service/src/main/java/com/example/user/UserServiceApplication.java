package com.example.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * User Service - Microservicio de Usuarios
 *
 * ⭐ ESTE SERVICIO DEMUESTRA DEFENSE IN DEPTH ⭐
 *
 * FLUJO COMPLETO DEL JWT:
 * =======================
 *
 * 1. CLIENTE → GATEWAY
 *    POST http://localhost:8081/api/users/me
 *    Header: Authorization: Bearer eyJhbGc...
 *
 * 2. GATEWAY VALIDA JWT (primera validación)
 *    ✓ Firma válida
 *    ✓ No expirado
 *    ✓ Issuer correcto
 *
 * 3. GATEWAY PROPAGA JWT (JWTPropagationFilter)
 *    GET http://localhost:8082/users/me
 *    Header: Authorization: Bearer eyJhbGc...
 *
 * 4. USER SERVICE VALIDA JWT (segunda validación) ← AQUÍ ESTAMOS
 *    ✓ Firma válida
 *    ✓ No expirado
 *    ✓ Issuer correcto
 *
 * 5. USER SERVICE PROCESA REQUEST
 *    - Extrae username del JWT
 *    - Devuelve información del usuario
 *
 * ¿POR QUÉ VALIDAR DE NUEVO SI EL GATEWAY YA VALIDÓ?
 * ===================================================
 *
 * DEFENSE IN DEPTH (Defensa en Profundidad):
 * ------------------------------------------
 *
 * 1. PROTECCIÓN CONTRA BYPASS DEL GATEWAY:
 *    - ¿Qué pasa si alguien llama directamente al microservicio?
 *    - Sin validación aquí → cualquiera puede acceder
 *    - Con validación aquí → necesitan JWT válido de Keycloak
 *
 * 2. ZERO TRUST ARCHITECTURE:
 *    - No confíes en nadie, ni siquiera en tu propio Gateway
 *    - Gateway puede ser comprometido
 *    - Gateway puede tener bugs
 *    - Gateway puede ser mal configurado
 *
 * 3. COMPLIANCE Y AUDITORÍA:
 *    - Algunas regulaciones (PCI-DSS, HIPAA) requieren validación en cada capa
 *    - Logs independientes por servicio
 *    - Trazabilidad completa
 *
 * 4. RESILIENCIA:
 *    - Si Gateway falla y deja pasar JWT inválido → servicio lo rechaza
 *    - Cada servicio es autónomo en seguridad
 *
 * EJEMPLO DE ATAQUE SIN VALIDACIÓN EN MICROSERVICIO:
 * ===================================================
 *
 * Atacante descubre que User Service está en localhost:8082
 * (por ejemplo, leyendo configuración expuesta, o por Eureka sin auth)
 *
 * Sin validación en microservicio:
 *   curl http://localhost:8082/users/me
 *   → 200 OK (¡SIN JWT!)
 *
 * Con validación en microservicio:
 *   curl http://localhost:8082/users/me
 *   → 401 Unauthorized
 *
 *   curl -H "Authorization: Bearer token-falso" http://localhost:8082/users/me
 *   → 401 Unauthorized (firma inválida)
 *
 *   Solo funciona con JWT válido de Keycloak ✓
 *
 * CONFIGURACIÓN DE SEGURIDAD:
 * ===========================
 *
 * La validación de JWT está en SecurityConfig.java (similar al Gateway)
 * La configuración de issuer-uri y jwk-set-uri viene del Config Server
 */
@SpringBootApplication
@EnableDiscoveryClient  // ← Se registra en Eureka
public class UserServiceApplication {

    private static final Logger log = LoggerFactory.getLogger(UserServiceApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);

        log.info("User Service iniciado en puerto 8082");
        log.info("JWT Validation: ENABLED - Validando contra: http://localhost:8080/realms/mi-realm");
        log.info("Service Discovery: ENABLED - Registrado en Eureka: http://localhost:8761");
        log.info("Endpoints disponibles:");
        log.info("  GET  /users/me        -> Información del usuario actual");
        log.info("  GET  /users/{{id}}      -> Información de usuario específico");
        log.info("  POST /users           -> Crear usuario (admin only)");
        log.info("IMPORTANTE: Todos los endpoints requieren JWT válido");
    }
}
