package com.example.product.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Configuración de CORS (Cross-Origin Resource Sharing) para Product Service
 *
 * Ver CorsConfig en user-service para documentación completa.
 */
@Configuration
public class CorsConfig {

    private static final Logger log = LoggerFactory.getLogger(CorsConfig.class);

    @Value("${cors.allowed-origins:http://localhost:4200,http://localhost:3000,http://localhost:8080}")
    private String allowedOrigins;

    @Value("${cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS,PATCH}")
    private String allowedMethods;

    @Value("${cors.allowed-headers:Authorization,Content-Type,X-Requested-With,Accept,Origin}")
    private String allowedHeaders;

    @Value("${cors.exposed-headers:Authorization,X-Total-Count,X-Page-Number}")
    private String exposedHeaders;

    @Value("${cors.max-age:3600}")
    private Long maxAge;

    @Value("${cors.allow-credentials:true}")
    private Boolean allowCredentials;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        config.setAllowedOrigins(origins);

        log.info("Configuración CORS - Product Service");
        log.info("Orígenes permitidos: {}", origins);

        List<String> methods = Arrays.asList(allowedMethods.split(","));
        config.setAllowedMethods(methods);
        log.debug("Métodos permitidos: {}", methods);

        List<String> headers = Arrays.asList(allowedHeaders.split(","));
        config.setAllowedHeaders(headers);
        log.debug("Headers permitidos: {}", headers);

        List<String> exposed = Arrays.asList(exposedHeaders.split(","));
        config.setExposedHeaders(exposed);
        log.debug("Headers expuestos: {}", exposed);

        config.setAllowCredentials(allowCredentials);
        log.debug("Credenciales permitidas: {}", allowCredentials);

        config.setMaxAge(maxAge);
        log.debug("Max Age (preflight cache): {} segundos", maxAge);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}
