package com.example.order;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Order Service - Microservicio de Órdenes
 *
 * ⭐ DEMUESTRA COMUNICACIÓN INTER-SERVICE CON JWT ⭐
 *
 * Este servicio es especial porque:
 * 1. Recibe JWT del Gateway (como User y Product Service)
 * 2. PERO TAMBIÉN llama a otros microservicios (User y Product)
 * 3. Propaga el JWT cuando llama a otros servicios
 *
 * FLUJO COMPLETO:
 * ===============
 *
 * ESCENARIO: Usuario crea una orden
 *
 * 1. CLIENTE → GATEWAY
 *    POST http://localhost:8081/api/orders
 *    Authorization: Bearer {jwt}
 *    Body: { productId: 1, quantity: 2 }
 *
 * 2. GATEWAY VALIDA JWT
 *    ✓ JWT válido
 *
 * 3. GATEWAY → ORDER SERVICE
 *    POST http://localhost:8084/orders
 *    Authorization: Bearer {jwt}  ← Propagado por JWTPropagationFilter
 *
 * 4. ORDER SERVICE VALIDA JWT
 *    ✓ JWT válido (defense in depth)
 *
 * 5. ORDER SERVICE → USER SERVICE (usando Feign)
 *    GET http://user-service/users/me
 *    Authorization: Bearer {jwt}  ← Propagado por FeignClientInterceptor
 *
 * 6. USER SERVICE VALIDA JWT
 *    ✓ JWT válido
 *    → Devuelve info del usuario
 *
 * 7. ORDER SERVICE → PRODUCT SERVICE (usando Feign)
 *    GET http://product-service/products/1
 *    Authorization: Bearer {jwt}  ← Propagado por FeignClientInterceptor
 *
 * 8. PRODUCT SERVICE VALIDA JWT
 *    ✓ JWT válido
 *    → Devuelve info del producto
 *
 * 9. ORDER SERVICE CREA LA ORDEN
 *    - Combina info de usuario + producto
 *    - Crea orden
 *    - Devuelve respuesta
 *
 * 10. ORDEN → GATEWAY → CLIENTE
 *
 * ⭐ ESTO DEMUESTRA: ⭐
 * ====================
 *
 * 🎯 JWT PROPAGATION EN CADENA:
 *    Cliente → Gateway → Order Service → User Service
 *                                     → Product Service
 *
 * 🎯 DEFENSE IN DEPTH COMPLETA:
 *    - Gateway valida JWT
 *    - Order Service valida JWT
 *    - User Service valida JWT
 *    - Product Service valida JWT
 *    ¡4 capas de validación!
 *
 * 🎯 ZERO TRUST:
 *    - Order Service NO confía en que Gateway validó
 *    - User Service NO confía en que Order Service validó
 *    - Cada uno valida independientemente
 *
 * 🎯 MICROSERVICIOS REALES:
 *    - Este patrón es común en arquitecturas reales
 *    - Un servicio orquestador (Order) llama a otros servicios
 *    - JWT viaja por toda la cadena
 *
 * COMPONENTES CLAVE:
 * ==================
 *
 * 1. FeignClient: Cliente HTTP declarativo
 *    - Llama a otros microservicios
 *    - Service discovery con Eureka
 *    - Load balancing automático
 *
 * 2. FeignClientInterceptor: Propaga JWT
 *    - Intercepta requests de Feign
 *    - Agrega header Authorization
 *    - Similar a JWTPropagationFilter del Gateway
 *
 * 3. SecurityConfig: Valida JWT entrante
 *    - Como en User y Product Service
 */
@SpringBootApplication
@EnableDiscoveryClient  // ← Registrarse en Eureka
@EnableFeignClients     // ← Habilitar Feign para llamar otros servicios
public class OrderServiceApplication {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);

        log.info("Order Service iniciado en puerto 8084");
        log.info("JWT Validation: ENABLED - Validando contra: http://localhost:8080/realms/mi-realm");
        log.info("Service Discovery: ENABLED - Registrado en Eureka: http://localhost:8761");
        log.info("Feign Clients: ENABLED");
        log.info("  - UserServiceClient -> user-service");
        log.info("  - ProductServiceClient -> product-service");
        log.info("Endpoints disponibles:");
        log.info("  GET  /orders           -> Listar órdenes del usuario");
        log.info("  GET  /orders/{{id}}      -> Obtener orden específica");
        log.info("  POST /orders           -> Crear nueva orden");
        log.info("ESPECIAL: Este servicio llama a otros microservicios");
        log.info("  - Llama a User Service para obtener info del usuario");
        log.info("  - Llama a Product Service para obtener info del producto");
        log.info("  - Propaga JWT en todas las llamadas");
    }
}
