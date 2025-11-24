package com.example.order.controller;

import com.example.order.client.ProductServiceClient;
import com.example.order.client.UserServiceClient;
import com.example.order.dto.*;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Order Controller - Endpoints de Órdenes
 *
 * ⭐ DEMUESTRA COMUNICACIÓN INTER-SERVICE CON JWT ⭐
 *
 * FLUJO COMPLETO DE createOrder():
 * ================================
 *
 * 1. Cliente → Gateway con JWT
 * 2. Gateway valida JWT
 * 3. Gateway → Order Service con JWT (JWTPropagationFilter)
 * 4. Order Service valida JWT (SecurityConfig)
 * 5. Controller recibe request
 * 6. Controller → User Service (Feign + FeignClientInterceptor)
 *    - FeignClientInterceptor agrega JWT al request
 *    - User Service valida JWT
 *    - User Service devuelve info del usuario
 * 7. Controller → Product Service (Feign + FeignClientInterceptor)
 *    - FeignClientInterceptor agrega JWT al request
 *    - Product Service valida JWT
 *    - Product Service devuelve info del producto
 * 8. Controller combina información y crea orden
 * 9. Controller devuelve orden creada
 * 10. Orden → Gateway → Cliente
 *
 * ESTO DEMUESTRA:
 * ===============
 *
 * 🎯 JWT VIAJA POR TODA LA CADENA:
 *    Cliente → Gateway → Order → User Service
 *                              → Product Service
 *
 * 🎯 CADA SERVICIO VALIDA JWT:
 *    Gateway ✓
 *    Order Service ✓
 *    User Service ✓
 *    Product Service ✓
 *
 * 🎯 SERVICE ORCHESTRATION:
 *    Order Service orquesta llamadas a otros servicios
 *    Patrón común en microservicios
 */
@RestController
@RequestMapping("/orders")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final UserServiceClient userServiceClient;
    private final ProductServiceClient productServiceClient;

    // Mock database
    private final Map<Long, OrderDTO> orders = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public OrderController(UserServiceClient userServiceClient, ProductServiceClient productServiceClient) {
        this.userServiceClient = userServiceClient;
        this.productServiceClient = productServiceClient;
    }

    /**
     * GET /orders
     *
     * Lista todas las órdenes del usuario actual.
     *
     * @param jwt JWT del usuario
     * @return Órdenes del usuario
     */
    @GetMapping
    public List<OrderDTO> getMyOrders(@AuthenticationPrincipal Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");

        List<OrderDTO> userOrders = orders.values().stream()
            .filter(order -> order.getUsername().equals(username))
            .collect(Collectors.toList());

        log.info("GET /orders - Usuario: {}, Total órdenes: {}", username, userOrders.size());

        return userOrders;
    }

    /**
     * GET /orders/{id}
     *
     * Obtiene una orden específica.
     *
     * @param id ID de la orden
     * @param jwt JWT del usuario
     * @return Orden solicitada
     */
    @GetMapping("/{id}")
    public OrderDTO getOrderById(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");

        log.info("GET /orders/{} - Usuario: {}", id, username);

        OrderDTO order = orders.get(id);
        if (order == null) {
            log.warn("Orden no encontrada - ID: {}, Usuario: {}", id, username);
            throw new RuntimeException("Order not found: " + id);
        }

        // Verificar que la orden pertenece al usuario
        if (!order.getUsername().equals(username)) {
            log.warn("Acceso no autorizado a orden - ID: {}, Usuario: {}, Propietario: {}",
                id, username, order.getUsername());
            throw new RuntimeException("Unauthorized: This order belongs to another user");
        }

        return order;
    }

    /**
     * POST /orders
     *
     * Crea una nueva orden.
     *
     * ⭐ ESTE ES EL ENDPOINT MÁS IMPORTANTE ⭐
     *
     * FLUJO:
     * 1. Obtiene info del usuario (llamada a User Service con JWT)
     * 2. Obtiene info del producto (llamada a Product Service con JWT)
     * 3. Combina información y crea orden
     *
     * @param request Request con productId y quantity
     * @param jwt JWT del usuario
     * @return Orden creada
     */
    @PostMapping
    public OrderDTO createOrder(@Valid @RequestBody CreateOrderRequest request, @AuthenticationPrincipal Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");

        log.info("POST /orders - Usuario: {}, Producto ID: {}, Cantidad: {}",
            username, request.getProductId(), request.getQuantity());

        // ==========================================
        // 1. OBTENER INFO DEL USUARIO
        // ==========================================
        log.debug("Llamando a User Service...");
        UserInfoDTO user;
        try {
            // Feign llama a: GET http://user-service/users/me
            // FeignClientInterceptor agrega JWT automáticamente
            user = userServiceClient.getCurrentUser();
            log.debug("User Service respondió: {}", user.getUsername());
        } catch (Exception e) {
            log.error("Error llamando a User Service: {}", e.getMessage(), e);
            throw new RuntimeException("Error obteniendo información del usuario", e);
        }

        // ==========================================
        // 2. OBTENER INFO DEL PRODUCTO
        // ==========================================
        log.debug("Llamando a Product Service...");
        ProductDTO product;
        try {
            // Feign llama a: GET http://product-service/products/{id}
            // FeignClientInterceptor agrega JWT automáticamente
            product = productServiceClient.getProductById(request.getProductId());
            log.debug("Product Service respondió: {}", product.getName());
        } catch (Exception e) {
            log.error("Error llamando a Product Service: {}", e.getMessage(), e);
            throw new RuntimeException("Error obteniendo información del producto", e);
        }

        // ==========================================
        // 3. VALIDAR STOCK
        // ==========================================
        if (product.getStock() < request.getQuantity()) {
            throw new RuntimeException("Stock insuficiente. Disponible: " + product.getStock());
        }

        // ==========================================
        // 4. CALCULAR TOTAL
        // ==========================================
        BigDecimal totalPrice = product.getPrice().multiply(BigDecimal.valueOf(request.getQuantity()));

        // ==========================================
        // 5. CREAR ORDEN
        // ==========================================
        Long orderId = idGenerator.getAndIncrement();
        OrderDTO order = OrderDTO.builder()
            .id(orderId)
            .username(user.getUsername())
            .productId(product.getId())
            .productName(product.getName())
            .productPrice(product.getPrice())
            .quantity(request.getQuantity())
            .totalPrice(totalPrice)
            .createdAt(LocalDateTime.now())
            .build();

        orders.put(orderId, order);

        log.info("Orden creada exitosamente - ID: {}, Usuario: {}, Producto: {}, Cantidad: {}, Total: ${}",
            order.getId(), order.getUsername(), order.getProductName(),
            order.getQuantity(), order.getTotalPrice());

        return order;

        /**
         * IMPORTANTE: En una app real, aquí también:
         * - Descontarías stock en Product Service
         * - Procesarías pago
         * - Enviarías eventos (Kafka/RabbitMQ)
         * - Crearías record en BD
         * - Enviarías email de confirmación
         * - etc.
         */
    }

    /**
     * TESTING:
     * ========
     *
     * 1. Obtener token:
     *    curl -X POST http://localhost:8080/realms/mi-realm/protocol/openid-connect/token \
     *      -d "client_id=mi-cliente" \
     *      -d "username=user" \
     *      -d "password=user" \
     *      -d "grant_type=password"
     *
     * 2. Crear orden (a través del Gateway):
     *    curl -X POST -H "Authorization: Bearer $TOKEN" \
     *      -H "Content-Type: application/json" \
     *      -d '{"productId":1,"quantity":2}' \
     *      http://localhost:8081/api/orders
     *
     * 3. Observar los logs:
     *    - Gateway: "🔐 JWT Propagation Filter" → Order Service
     *    - Order Service: "📦 POST /orders"
     *    - Order Service: "🔗 Feign Client Interceptor" → User Service
     *    - User Service: "📋 GET /users/me"
     *    - Order Service: "🔗 Feign Client Interceptor" → Product Service
     *    - Product Service: "📦 GET /products/1"
     *    - Order Service: "✓ Orden creada exitosamente"
     *
     * 4. Listar mis órdenes:
     *    curl -H "Authorization: Bearer $TOKEN" http://localhost:8081/api/orders
     *
     * ESTO DEMUESTRA EL FLUJO COMPLETO DE JWT EN MICROSERVICIOS ✓
     */
}
