package com.example.user.controller;

import com.example.user.dto.UserInfoDTO;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * User Controller - Endpoints de Usuario
 *
 * ⭐ ESTE CONTROLLER DEMUESTRA CÓMO USAR EL JWT ⭐
 *
 * EXTRACCIÓN DE INFORMACIÓN DEL JWT:
 * ===================================
 *
 * Spring Security automáticamente:
 * 1. Valida el JWT (SecurityConfig)
 * 2. Extrae los claims del JWT
 * 3. Crea un objeto Jwt
 * 4. Lo inyecta con @AuthenticationPrincipal
 *
 * Entonces puedes acceder a:
 * - jwt.getTokenValue() → Token completo
 * - jwt.getClaim("sub") → Subject (username)
 * - jwt.getClaim("email") → Email
 * - jwt.getClaim("realm_access") → Roles
 * - jwt.getClaimAsString("preferred_username") → Username preferido
 * - etc.
 *
 * ROLES Y PERMISOS:
 * =================
 *
 * @PreAuthorize permite validar roles ANTES de ejecutar el método:
 *
 * - @PreAuthorize("hasRole('ADMIN')")
 *   → Solo usuarios con role "admin" en Keycloak
 *
 * - @PreAuthorize("hasRole('USER')")
 *   → Solo usuarios con role "user"
 *
 * - @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
 *   → Usuarios con "admin" O "user"
 *
 * - @PreAuthorize("authentication.principal.claims['email_verified'] == true")
 *   → Custom claim validation
 *
 * Si el usuario NO tiene el role → 403 Forbidden
 *
 * FLUJO COMPLETO DE UNA REQUEST:
 * ===============================
 *
 * 1. Cliente → Gateway con JWT
 * 2. Gateway valida JWT ✓
 * 3. Gateway propaga JWT al microservicio
 * 4. Microservicio recibe request con JWT
 * 5. SecurityConfig valida JWT ✓
 * 6. Spring extrae claims y crea objeto Jwt
 * 7. @PreAuthorize valida roles ✓
 * 8. Método del controller se ejecuta
 * 9. Método accede a información del JWT
 * 10. Método devuelve respuesta
 */
@RestController
@RequestMapping("/users")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    /**
     * GET /users/me
     *
     * Devuelve información del usuario actual (extraída del JWT).
     *
     * ⭐ ENDPOINT MÁS IMPORTANTE ⭐
     *
     * Este endpoint demuestra:
     * - Cómo extraer información del JWT
     * - Cómo acceder a claims
     * - Cómo crear DTOs con info del usuario
     *
     * NO REQUIERE ROLE ESPECÍFICO:
     * - Cualquier usuario autenticado puede llamar este endpoint
     * - Solo necesita JWT válido
     *
     * @param jwt JWT inyectado automáticamente por Spring Security
     * @return Información del usuario actual
     */
    @GetMapping("/me")
    public UserInfoDTO getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");

        if (log.isDebugEnabled()) {
            log.debug("GET /users/me - Usuario: {}, Subject: {}, Issuer: {}, Expira: {}",
                username, jwt.getSubject(), jwt.getIssuer(), jwt.getExpiresAt());
        } else {
            log.info("GET /users/me - Usuario: {}", username);
        }

        // Extraer información del JWT
        String email = jwt.getClaimAsString("email");
        String name = jwt.getClaimAsString("name");
        String givenName = jwt.getClaimAsString("given_name");
        String familyName = jwt.getClaimAsString("family_name");

        // Extraer roles
        // Keycloak pone los roles en: realm_access.roles
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        List<String> roles = realmAccess != null
            ? (List<String>) realmAccess.get("roles")
            : List.of();

        // Crear DTO con la información
        return UserInfoDTO.builder()
            .username(username)
            .email(email)
            .name(name)
            .givenName(givenName)
            .familyName(familyName)
            .roles(roles)
            .emailVerified(jwt.getClaimAsBoolean("email_verified"))
            .build();
    }

    /**
     * GET /users/{id}
     *
     * Devuelve información de un usuario específico.
     *
     * REQUIERE ROLE ADMIN:
     * - Solo administradores pueden ver información de otros usuarios
     * - Si no tienes role "admin" → 403 Forbidden
     *
     * @param id ID del usuario a consultar
     * @param jwt JWT del usuario que hace la request
     * @return Información del usuario solicitado
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")  // ← Solo admins
    public UserInfoDTO getUserById(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        log.info("GET /users/{} - Admin: {}", id, jwt.getClaimAsString("preferred_username"));

        // En una app real, aquí consultarías la base de datos
        // Por ahora, devolvemos mock data

        return UserInfoDTO.builder()
            .username(id)
            .email(id + "@example.com")
            .name("User " + id)
            .roles(List.of("user"))
            .emailVerified(true)
            .build();
    }

    /**
     * POST /users
     *
     * Crea un nuevo usuario.
     *
     * REQUIERE ROLE ADMIN:
     * - Solo administradores pueden crear usuarios
     * - Si no tienes role "admin" → 403 Forbidden
     *
     * NOTA: En una app real, esto crearía el usuario en:
     * - Base de datos local (para info adicional)
     * - Keycloak (para autenticación)
     *
     * @param userInfo Información del usuario a crear
     * @param jwt JWT del admin que crea el usuario
     * @return Usuario creado
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")  // ← Solo admins
    public UserInfoDTO createUser(
        @Validated(UserInfoDTO.CreateUser.class) @RequestBody UserInfoDTO userInfo,
        @AuthenticationPrincipal Jwt jwt
    ) {
        log.info("POST /users - Admin: {}, Usuario a crear: {}",
            jwt.getClaimAsString("preferred_username"), userInfo.getUsername());

        // En una app real, aquí:
        // 1. Validarías los datos
        // 2. Crearías el usuario en la BD
        // 3. Crearías el usuario en Keycloak usando Admin API
        // 4. Devolverías el usuario creado

        return userInfo;
    }

    /**
     * GET /users/admin-only
     *
     * Endpoint solo para demostrar restricción por role.
     *
     * REQUIERE ROLE ADMIN:
     * - Si tienes role "admin" → 200 OK
     * - Si NO tienes role "admin" → 403 Forbidden
     *
     * @param jwt JWT del usuario
     * @return Mensaje de éxito
     */
    @GetMapping("/admin-only")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, String> adminOnlyEndpoint(@AuthenticationPrincipal Jwt jwt) {
        return Map.of(
            "message", "¡Eres admin!",
            "username", jwt.getClaimAsString("preferred_username")
        );
    }

    /**
     * GET /users/jwt-info
     *
     * Devuelve TODA la información del JWT (para debugging).
     *
     * ⚠️ SOLO PARA DESARROLLO ⚠️
     *
     * En producción, NO expongas todo el JWT:
     * - Puede contener información sensible
     * - Solo expone lo que el frontend necesita
     *
     * @param jwt JWT del usuario
     * @return Todos los claims del JWT
     */
    @GetMapping("/jwt-info")
    public Map<String, Object> getJwtInfo(@AuthenticationPrincipal Jwt jwt) {
        return jwt.getClaims();
    }

    /**
     * TESTING:
     * ========
     *
     * 1. Obtener token de Keycloak:
     *    curl -X POST http://localhost:8080/realms/mi-realm/protocol/openid-connect/token \
     *      -d "client_id=mi-cliente" \
     *      -d "client_secret=tu-secret" \
     *      -d "username=user" \
     *      -d "password=user" \
     *      -d "grant_type=password"
     *
     * 2. Llamar endpoint a través del Gateway:
     *    curl -H "Authorization: Bearer $TOKEN" http://localhost:8081/api/users/me
     *
     * 3. Llamar endpoint directamente al microservicio:
     *    curl -H "Authorization: Bearer $TOKEN" http://localhost:8082/users/me
     *
     * Ambas requests DEBEN funcionar (defense in depth).
     *
     * 4. Probar endpoint de admin (con usuario normal):
     *    curl -H "Authorization: Bearer $USER_TOKEN" http://localhost:8081/api/users/admin-only
     *    → 403 Forbidden
     *
     * 5. Probar endpoint de admin (con usuario admin):
     *    curl -H "Authorization: Bearer $ADMIN_TOKEN" http://localhost:8081/api/users/admin-only
     *    → 200 OK
     */
}
