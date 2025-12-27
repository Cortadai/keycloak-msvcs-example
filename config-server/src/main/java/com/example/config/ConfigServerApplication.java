package com.example.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Config Server - Servidor de Configuración Centralizada
 *
 * PROPÓSITO EN ARQUITECTURA JWT:
 * ===============================
 * Este servidor centraliza la configuración de validación de JWT
 * para TODOS los microservicios.
 *
 * ¿Por qué es importante?
 * - Todos los servicios necesitan la misma configuración de JWT:
 *   * issuer-uri (quién emitió el token)
 *   * jwk-set-uri (dónde están las claves públicas)
 *   * audience, etc.
 * - Si cambia algo en Keycloak (ej: nuevo realm), solo actualizas aquí
 * - Secrets centralizados y seguros
 *
 * FLUJO:
 * ======
 * 1. Microservicio inicia
 * 2. Se conecta a Config Server (http://localhost:8888)
 * 3. Obtiene su configuración (incluida la de JWT)
 * 4. Configura Spring Security con esos parámetros
 * 5. Ya puede validar tokens JWT
 *
 * CONFIGURACIÓN QUE SIRVE:
 * ========================
 * - application.yml (común a todos)
 * - gateway.yml (específico del Gateway)
 * - user-service.yml
 * - product-service.yml
 * - order-service.yml
 *
 * Todos estos archivos están en: infrastructure/config-repo/
 */
@SpringBootApplication
@EnableConfigServer  // ← Habilita funcionalidad de Config Server
public class ConfigServerApplication {

    private static final Logger log = LoggerFactory.getLogger(ConfigServerApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
        log.info("=========================================");
        log.info("✓ Config Server iniciado en puerto 8888");
        log.info("=========================================");
        System.out.println();
        log.info("📁 Sirviendo configuraciones desde: file:../infrastructure/config-repo");
        System.out.println();
        log.info("Endpoints disponibles:");
        log.info("  http://localhost:8888/application/default");
        log.info("  http://localhost:8888/gateway/default");
        log.info("  http://localhost:8888/user-service/default");
        log.info("=========================================");
    }
}
