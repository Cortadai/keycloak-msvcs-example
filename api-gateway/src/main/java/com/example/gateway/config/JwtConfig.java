package com.example.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Configuración de JWT con validación de Audience
 *
 * ⭐ AGREGA VALIDACIÓN DEL CLAIM "aud" (AUDIENCE) ⭐
 *
 * ¿QUÉ ES AUDIENCE?
 * =================
 *
 * El claim "aud" (audience) indica para QUÉ aplicación/servicio
 * fue generado el token.
 *
 * EJEMPLO:
 * --------
 * Un JWT puede contener:
 * {
 *   "iss": "http://localhost:8080/realms/mi-realm",
 *   "sub": "usuario1",
 *   "aud": "microservices-client",  ← Este claim
 *   "exp": 1234567890
 * }
 *
 * ¿POR QUÉ VALIDAR AUDIENCE?
 * ===========================
 *
 * PROBLEMA SIN VALIDACIÓN:
 * ------------------------
 * 1. Atacante obtiene token para "mobile-app"
 * 2. Usa ese token en tu API de microservicios
 * 3. Tu API acepta el token (firma válida, issuer correcto)
 * 4. Token Reuse Attack ❌
 *
 * SOLUCIÓN CON VALIDACIÓN:
 * ------------------------
 * 1. Atacante obtiene token para "mobile-app"
 * 2. Usa ese token en tu API de microservicios
 * 3. Tu API valida audience: "mobile-app" != "microservices-client"
 * 4. Token rechazado ✅
 *
 * CONFIGURACIÓN EN KEYCLOAK:
 * ==========================
 *
 * Para que Keycloak incluya el claim "aud":
 * 1. Client Settings → "microservices-client"
 * 2. Settings → Client ID = "microservices-client"
 * 3. El claim "aud" será el Client ID automáticamente
 *
 * O puedes agregar manualmente:
 * 1. Client Scopes → Create
 * 2. Mapper → Audience
 * 3. Included Client Audience = "microservices-client"
 */
@Configuration
public class JwtConfig {

    private static final Logger log = LoggerFactory.getLogger(JwtConfig.class);

    /**
     * Audience esperado (desde application.yml o variable de entorno)
     *
     * En application.yml:
     * jwt:
     *   audience: microservices-client
     *
     * En producción, usa variable de entorno:
     * JWT_AUDIENCE=microservices-client
     */
    @Value("${jwt.audience:spring-boot-client}")
    private String expectedAudience;

    /**
     * Configuración del decoder de JWT con validación de audience.
     *
     * IMPORTANTE: Spring Cloud Gateway es REACTIVO (WebFlux)
     * Por eso usamos ReactiveJwtDecoder en lugar de JwtDecoder
     *
     * Spring Security usa este decoder para:
     * 1. Decodificar el JWT
     * 2. Validar firma
     * 3. Validar expiración
     * 4. Validar issuer
     * 5. Validar audience (NUEVO)
     *
     * @param issuerUri URI del emisor de JWT (Keycloak)
     * @return Decoder reactivo configurado con validación de audience
     */
    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder(
        @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri
    ) {
        // Crear decoder no reactivo primero (para configurar validators)
        NimbusJwtDecoder jwtDecoder = JwtDecoders.fromIssuerLocation(issuerUri);

        // ==========================================
        // VALIDATORS
        // ==========================================

        // 1. Validator por defecto (firma, expiración, issuer)
        OAuth2TokenValidator<Jwt> defaultValidators = JwtValidators.createDefaultWithIssuer(issuerUri);

        // 2. Validator de audience (custom)
        OAuth2TokenValidator<Jwt> audienceValidator = new JwtClaimValidator<List<String>>(
            JwtClaimNames.AUD,
            audiences -> audiences != null && audiences.contains(expectedAudience)
        );

        // 3. Combinar validators
        OAuth2TokenValidator<Jwt> combinedValidator = new DelegatingOAuth2TokenValidator<>(
            defaultValidators,
            audienceValidator
        );

        // Aplicar validators al decoder
        jwtDecoder.setJwtValidator(combinedValidator);

        // Convertir a ReactiveJwtDecoder con logging para ver la validación
        return token -> Mono.fromCallable(() -> {
            log.debug("Validando JWT en Gateway - Token: {}...", token.substring(0, Math.min(50, token.length())));

            try {
                Jwt jwt = jwtDecoder.decode(token);

                if (log.isDebugEnabled()) {
                    log.debug("Token válido en Gateway - Usuario: {}, Issuer: {}, Audience: {}, Expira: {}, Roles: {}",
                        jwt.getClaimAsString("preferred_username"),
                        jwt.getIssuer(),
                        jwt.getAudience(),
                        jwt.getExpiresAt(),
                        jwt.getClaimAsStringList("realm_access.roles"));
                } else {
                    log.info("Token válido en Gateway - Usuario: {}", jwt.getClaimAsString("preferred_username"));
                }

                return jwt;
            } catch (Exception e) {
                log.error("Token inválido en Gateway: {}", e.getMessage());
                throw e;
            }
        });
    }

    /**
     * TESTING:
     * ========
     *
     * 1. Token CON audience correcto:
     *    {
     *      "aud": "microservices-client"
     *    }
     *    → ✅ Aceptado
     *
     * 2. Token SIN audience:
     *    {
     *      // no tiene "aud"
     *    }
     *    → ❌ Rechazado (401)
     *
     * 3. Token con audience diferente:
     *    {
     *      "aud": "mobile-app"
     *    }
     *    → ❌ Rechazado (401)
     *
     * 4. Token con múltiples audiences (array):
     *    {
     *      "aud": ["microservices-client", "mobile-app"]
     *    }
     *    → ✅ Aceptado (contiene el audience esperado)
     *
     * NOTA IMPORTANTE:
     * ================
     *
     * Si estás probando y no tienes "aud" en el JWT,
     * puedes desactivar temporalmente esta validación
     * comentando el bean, pero en producción SIEMPRE
     * debe estar activo.
     */
}
