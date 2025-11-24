package com.example.product.controller;

import com.example.product.dto.ProductDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Product Controller - Endpoints de Productos
 *
 * ⭐ DEMUESTRA CONTROL DE ACCESO POR ROLES ⭐
 *
 * PERMISOS:
 * =========
 *
 * LECTURA (GET):
 * - Cualquier usuario autenticado puede leer productos
 * - No requiere role específico
 * - Solo necesita JWT válido
 *
 * ESCRITURA (POST/PUT/DELETE):
 * - Solo ADMIN puede crear/modificar/eliminar productos
 * - @PreAuthorize("hasRole('ADMIN')") valida esto
 * - Si no eres admin → 403 Forbidden
 *
 * ESTO DEMUESTRA UN PATRÓN COMÚN:
 * ================================
 *
 * READ: Usuarios normales
 * WRITE: Solo administradores
 *
 * Ejemplos reales:
 * - E-commerce: Todos ven productos, solo admins los crean
 * - Blog: Todos leen artículos, solo autores los escriben
 * - API pública: Todos consultan, solo partners crean
 */
@RestController
@RequestMapping("/products")
public class ProductController {

    private static final Logger log = LoggerFactory.getLogger(ProductController.class);

    // Mock database (en memoria)
    private final Map<Long, ProductDTO> products = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    // Inicializar con algunos productos
    public ProductController() {
        products.put(1L, ProductDTO.builder()
            .id(1L)
            .name("Laptop")
            .description("High-performance laptop")
            .price(new BigDecimal("999.99"))
            .stock(10)
            .build());

        products.put(2L, ProductDTO.builder()
            .id(2L)
            .name("Mouse")
            .description("Wireless mouse")
            .price(new BigDecimal("29.99"))
            .stock(50)
            .build());

        idGenerator.set(3L);
    }

    /**
     * GET /products
     *
     * Lista todos los productos.
     *
     * PERMISOS:
     * - Cualquier usuario autenticado
     * - No requiere role específico
     *
     * @param jwt JWT del usuario
     * @return Lista de productos
     */
    @GetMapping
    public List<ProductDTO> getAllProducts(@AuthenticationPrincipal Jwt jwt) {
        log.info("GET /products - Usuario: {}, Total: {}",
            jwt.getClaimAsString("preferred_username"), products.size());

        return new ArrayList<>(products.values());
    }

    /**
     * GET /products/{id}
     *
     * Obtiene un producto específico.
     *
     * PERMISOS:
     * - Cualquier usuario autenticado
     *
     * @param id ID del producto
     * @param jwt JWT del usuario
     * @return Producto solicitado
     */
    @GetMapping("/{id}")
    public ProductDTO getProductById(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        log.info("GET /products/{} - Usuario: {}", id, jwt.getClaimAsString("preferred_username"));

        ProductDTO product = products.get(id);
        if (product == null) {
            throw new RuntimeException("Product not found: " + id);
        }
        return product;
    }

    /**
     * POST /products
     *
     * Crea un nuevo producto.
     *
     * ⭐ REQUIERE ROLE ADMIN ⭐
     *
     * PERMISOS:
     * - Solo usuarios con role "admin"
     * - Si no eres admin → 403 Forbidden
     *
     * @param product Producto a crear
     * @param jwt JWT del admin
     * @return Producto creado
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")  // ← Solo admins
    public ProductDTO createProduct(@RequestBody ProductDTO product, @AuthenticationPrincipal Jwt jwt) {
        log.info("POST /products - Admin: {}, Producto: {}",
            jwt.getClaimAsString("preferred_username"), product.getName());

        Long id = idGenerator.getAndIncrement();
        product.setId(id);
        products.put(id, product);

        return product;
    }

    /**
     * PUT /products/{id}
     *
     * Actualiza un producto existente.
     *
     * ⭐ REQUIERE ROLE ADMIN ⭐
     *
     * @param id ID del producto
     * @param product Datos actualizados
     * @param jwt JWT del admin
     * @return Producto actualizado
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")  // ← Solo admins
    public ProductDTO updateProduct(
        @PathVariable Long id,
        @RequestBody ProductDTO product,
        @AuthenticationPrincipal Jwt jwt
    ) {
        log.info("PUT /products/{} - Admin: {}", id, jwt.getClaimAsString("preferred_username"));

        if (!products.containsKey(id)) {
            throw new RuntimeException("Product not found: " + id);
        }

        product.setId(id);
        products.put(id, product);

        return product;
    }

    /**
     * DELETE /products/{id}
     *
     * Elimina un producto.
     *
     * ⭐ REQUIERE ROLE ADMIN ⭐
     *
     * @param id ID del producto
     * @param jwt JWT del admin
     * @return Mensaje de confirmación
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")  // ← Solo admins
    public Map<String, String> deleteProduct(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        log.info("DELETE /products/{} - Admin: {}", id, jwt.getClaimAsString("preferred_username"));

        if (!products.containsKey(id)) {
            throw new RuntimeException("Product not found: " + id);
        }

        products.remove(id);

        return Map.of(
            "message", "Product deleted successfully",
            "id", id.toString()
        );
    }

    /**
     * TESTING:
     * ========
     *
     * 1. Obtener token de usuario normal:
     *    curl -X POST http://localhost:8080/realms/mi-realm/protocol/openid-connect/token \
     *      -d "client_id=mi-cliente" \
     *      -d "username=user" \
     *      -d "password=user" \
     *      -d "grant_type=password"
     *
     * 2. Listar productos (usuario normal):
     *    curl -H "Authorization: Bearer $USER_TOKEN" http://localhost:8081/api/products
     *    → 200 OK ✓
     *
     * 3. Intentar crear producto (usuario normal):
     *    curl -X POST -H "Authorization: Bearer $USER_TOKEN" \
     *      -H "Content-Type: application/json" \
     *      -d '{"name":"Test","price":10.0}' \
     *      http://localhost:8081/api/products
     *    → 403 Forbidden ✗ (no es admin)
     *
     * 4. Obtener token de admin:
     *    curl -X POST http://localhost:8080/realms/mi-realm/protocol/openid-connect/token \
     *      -d "client_id=mi-cliente" \
     *      -d "username=admin" \
     *      -d "password=admin" \
     *      -d "grant_type=password"
     *
     * 5. Crear producto (admin):
     *    curl -X POST -H "Authorization: Bearer $ADMIN_TOKEN" \
     *      -H "Content-Type: application/json" \
     *      -d '{"name":"Test","price":10.0}' \
     *      http://localhost:8081/api/products
     *    → 200 OK ✓
     */
}
