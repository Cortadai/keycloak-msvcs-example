package com.example.discovery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Eureka Discovery Server - Registro de Servicios
 *
 * PROPÓSITO EN ARQUITECTURA JWT:
 * ===============================
 * Aunque no está directamente relacionado con JWT, Eureka es CRUCIAL
 * para que el Gateway pueda enrutar requests (con JWT) dinámicamente.
 *
 * ¿Cómo ayuda al flujo JWT?
 * =========================
 *
 * ESCENARIO:
 * ----------
 * 1. Cliente envía request con JWT al Gateway:
 *    POST http://localhost:8081/api/users/me
 *    Authorization: Bearer eyJhbGc...
 *
 * 2. Gateway valida el JWT ✓
 *
 * 3. Gateway necesita enrutar a user-service
 *    Pregunta: "¿Dónde está user-service?"
 *
 * 4. Eureka responde: "localhost:8082"
 *
 * 5. Gateway propaga JWT:
 *    GET http://localhost:8082/users/me
 *    Authorization: Bearer eyJhbGc...
 *
 * VENTAJAS:
 * =========
 * ✅ Escalado dinámico: Si hay 3 instancias de user-service, Eureka las conoce todas
 * ✅ Load balancing: Gateway distribuye carga entre instancias
 * ✅ Health checks: Si un servicio cae, Eureka lo detecta
 * ✅ No hardcodear IPs: Los servicios se descubren automáticamente
 *
 * SERVICIOS REGISTRADOS:
 * ======================
 * - api-gateway
 * - user-service
 * - product-service
 * - order-service
 *
 * UI DE EUREKA:
 * =============
 * Accede a http://localhost:8761 para ver todos los servicios registrados
 */
@SpringBootApplication
@EnableEurekaServer  // ← Habilita funcionalidad de Eureka Server
public class DiscoveryServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DiscoveryServerApplication.class, args);
        System.out.println("============================================");
        System.out.println("✓ Eureka Discovery Server iniciado");
        System.out.println("============================================");
        System.out.println();
        System.out.println("🌐 Dashboard UI: http://localhost:8761");
        System.out.println();
        System.out.println("Los microservicios se registrarán aquí automáticamente");
        System.out.println("cuando inicien y tengan esta configuración:");
        System.out.println("  eureka.client.serviceUrl.defaultZone=http://localhost:8761/eureka/");
        System.out.println();
        System.out.println("============================================");
    }
}
