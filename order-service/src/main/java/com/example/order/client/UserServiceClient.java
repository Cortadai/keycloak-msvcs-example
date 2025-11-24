package com.example.order.client;

import com.example.order.dto.UserInfoDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Feign Client para User Service
 *
 * ⭐ CLIENTE DECLARATIVO PARA LLAMAR A OTRO MICROSERVICIO ⭐
 *
 * ¿QUÉ ES FEIGN?
 * ==============
 *
 * Feign es un cliente HTTP declarativo.
 * En vez de escribir código manual con RestTemplate:
 *
 *   RestTemplate rest = new RestTemplate();
 *   String url = "http://user-service/users/me";
 *   UserInfo user = rest.getForObject(url, UserInfo.class);
 *
 * Usas una interfaz:
 *
 *   @FeignClient("user-service")
 *   interface UserServiceClient {
 *       @GetMapping("/users/me")
 *       UserInfo getCurrentUser();
 *   }
 *
 *   // Uso:
 *   UserInfo user = userServiceClient.getCurrentUser();
 *
 * MÁS LIMPIO, MÁS DECLARATIVO.
 *
 * @FeignClient PARÁMETROS:
 * ========================
 *
 * name = "user-service"
 * - Nombre del servicio en Eureka
 * - Feign consulta Eureka: "¿Dónde está user-service?"
 * - Eureka: "localhost:8082"
 * - Feign llama a: http://localhost:8082/users/me
 *
 * Si tienes múltiples instancias de user-service:
 * - user-service-1: localhost:8082
 * - user-service-2: localhost:8092
 * - user-service-3: localhost:8102
 *
 * Feign automáticamente:
 * - Load balancing (round-robin)
 * - Failover (si una instancia cae)
 * - Retry (con configuración)
 *
 * PROPAGACIÓN DE JWT:
 * ===================
 *
 * FeignClientInterceptor automáticamente agrega el JWT:
 * - Intercepta TODAS las llamadas Feign
 * - Obtiene JWT del SecurityContext
 * - Agrega header: Authorization: Bearer {token}
 *
 * Entonces, cuando llamas:
 *   userServiceClient.getCurrentUser()
 *
 * Feign envía:
 *   GET http://user-service/users/me
 *   Authorization: Bearer eyJhbGc...  ← Agregado por el interceptor
 *
 * VENTAJAS:
 * =========
 *
 * ✅ Código limpio y declarativo
 * ✅ Service discovery automático (Eureka)
 * ✅ Load balancing automático
 * ✅ JWT propagado automáticamente (FeignClientInterceptor)
 * ✅ Retry y circuit breaker (con Resilience4j)
 * ✅ Logging de requests/responses
 *
 * ALTERNATIVAS:
 * =============
 *
 * 1. RestTemplate (antiguo, no recomendado)
 * 2. WebClient (reactivo, recomendado para WebFlux)
 * 3. Feign (declarativo, recomendado para Spring MVC)
 */
@FeignClient(name = "user-service", path = "/api")  // ← Nombre en Eureka + base path
public interface UserServiceClient {

    /**
     * Obtiene información del usuario actual.
     *
     * Llama a: GET http://user-service/api/users/me
     *
     * El JWT se agrega automáticamente por FeignClientInterceptor.
     *
     * @return Información del usuario
     */
    @GetMapping("/users/me")
    UserInfoDTO getCurrentUser();

    /**
     * NOTA: Podrías agregar más métodos aquí:
     *
     * @GetMapping("/users/{id}")
     * UserInfo getUserById(@PathVariable String id);
     *
     * @PostMapping("/users")
     * UserInfo createUser(@RequestBody UserInfo user);
     *
     * etc.
     *
     * Todos automáticamente:
     * - Usan service discovery
     * - Propagan JWT
     * - Load balancing
     */
}
