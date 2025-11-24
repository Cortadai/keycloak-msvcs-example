package com.example.order.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;

import java.util.List;

/**
 * Configuración de JWT con validación de Audience
 */
@Configuration
public class JwtConfig {

    private static final Logger log = LoggerFactory.getLogger(JwtConfig.class);

    @Value("${jwt.audience:spring-boot-client}")
    private String expectedAudience;

    @Bean
    public JwtDecoder jwtDecoder(
        @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri
    ) {
        NimbusJwtDecoder jwtDecoder = JwtDecoders.fromIssuerLocation(issuerUri);

        OAuth2TokenValidator<Jwt> defaultValidators = JwtValidators.createDefaultWithIssuer(issuerUri);
        OAuth2TokenValidator<Jwt> audienceValidator = new JwtClaimValidator<List<String>>(
            JwtClaimNames.AUD,
            audiences -> audiences != null && audiences.contains(expectedAudience)
        );
        OAuth2TokenValidator<Jwt> combinedValidator = new DelegatingOAuth2TokenValidator<>(
            defaultValidators,
            audienceValidator
        );

        jwtDecoder.setJwtValidator(combinedValidator);

        // ⭐ WRAPPER PARA LOGGING - Ver tokens siendo validados
        return token -> {
            String tokenPreview = token.substring(0, Math.min(50, token.length())) + "...";
            log.debug("Validando JWT en Order Service - Token: {}", tokenPreview);

            try {
                Jwt jwt = jwtDecoder.decode(token);

                String username = jwt.getClaimAsString("preferred_username");

                if (log.isDebugEnabled()) {
                    log.debug("Token válido en Order Service - Usuario: {}, Issuer: {}, Audience: {}, Expira: {}",
                        username, jwt.getIssuer(), jwt.getAudience(), jwt.getExpiresAt());
                } else {
                    log.info("Token válido en Order Service - Usuario: {}", username);
                }

                return jwt;
            } catch (Exception e) {
                log.error("Token inválido en Order Service: {}", e.getMessage());
                throw e;
            }
        };
    }
}
