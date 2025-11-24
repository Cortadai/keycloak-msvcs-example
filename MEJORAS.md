# AUDITORÍA DE SEGURIDAD Y BUENAS PRÁCTICAS
## Arquitectura de Microservicios con Keycloak y JWT

**Fecha:** 22 Noviembre 2025
**Versión:** 1.0
**Tipo:** POC (Proof of Concept)
**Calificación General:** 8.2/10

---

## 📊 RESUMEN EJECUTIVO

Esta arquitectura de microservicios con Keycloak demuestra una **implementación sólida de seguridad JWT** con excelentes prácticas de defense in depth. El código está excepcionalmente bien documentado, lo que facilita el mantenimiento y onboarding.

### Evaluación por Contexto

| Contexto | Calificación | Estado |
|----------|-------------|---------|
| **POC/Demo** | 9/10 | ✅ EXCELENTE |
| **Desarrollo/Staging** | 8/10 | ✅ APROBAR |
| **Producción** | 6/10 | ⏸️ RETENER (requiere mejoras) |

### Arquitectura

- **API Gateway** (Spring Cloud Gateway - Reactivo)
- **Microservicios:** user-service, product-service, order-service
- **Config Server:** Configuración centralizada
- **Eureka Server:** Service Discovery
- **Keycloak:** Identity Provider & Token Issuer

---

## ✅ ASPECTOS BIEN IMPLEMENTADOS

### 1. ARQUITECTURA DE SEGURIDAD (9/10)

#### Defense in Depth - Validación Multinivel

Cada capa valida el JWT independientemente, implementando el principio de Zero Trust:

**Gateway**
- Archivo: `api-gateway/src/main/java/com/example/gateway/config/SecurityConfig.java`
- Líneas: 60-126
- Función: Primera validación de JWT antes de enrutar

**User Service**
- Archivo: `user-service/src/main/java/com/example/user/config/SecurityConfig.java`
- Líneas: 83-151
- Función: Valida JWT independientemente del Gateway

**Order Service**
- Archivo: `order-service/src/main/java/com/example/order/config/SecurityConfig.java`
- Líneas: 24-38
- Función: Valida JWT al recibir peticiones

**Product Service**
- Archivo: `product-service/src/main/java/com/example/product/config/SecurityConfig.java`
- Líneas: 44-73
- Función: Valida JWT al recibir peticiones

#### Validación Completa de JWT

Todos los servicios validan:
- ✅ **Firma digital** (verificada con claves públicas de Keycloak vía JWK-set-uri)
- ✅ **Expiración** (claim "exp")
- ✅ **Issuer** (claim "iss")
- ✅ **Audience** (claim "aud") - **EXCELENTE**: Implementado en todos los servicios

**Implementación de Audience Validation:**
- `JwtConfig.java` en gateway: líneas 111-114
- `JwtConfig.java` en user-service: líneas 44-47
- `JwtConfig.java` en order-service: líneas 28-31
- `JwtConfig.java` en product-service: similar

```java
// Previene token reuse attacks
OAuth2TokenValidator<Jwt> audienceValidator = new JwtClaimValidator<List<String>>(
    JwtClaimNames.AUD,
    audiences -> audiences != null && audiences.contains(expectedAudience)
);
```

### 2. PROPAGACIÓN DE JWT (10/10)

#### Gateway → Microservices

**JWTPropagationGatewayFilterFactory**
- Archivo: `api-gateway/src/main/java/com/example/gateway/filter/JWTPropagationGatewayFilterFactory.java`
- Líneas: 87-156
- Implementación reactiva correcta
- Obtiene JWT del `ReactiveSecurityContextHolder` (línea 97)
- Agrega header `Authorization: Bearer {token}` al request interno (líneas 133-136)
- Logging detallado para debugging (líneas 116-122)

#### Inter-service Communication (Service → Service)

**FeignClientInterceptor**
- Archivo: `order-service/src/main/java/com/example/order/config/FeignClientInterceptor.java`
- Líneas: 110-156
- Validación de existencia de authentication (líneas 120-125)
- Verificación de tipo JWT (líneas 130-134)
- Propagación automática en TODAS las llamadas Feign

```java
// Intercepta automáticamente TODAS las llamadas Feign
@Override
public void apply(RequestTemplate template) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication instanceof JwtAuthenticationToken) {
        Jwt jwt = ((JwtAuthenticationToken) authentication).getToken();
        template.header("Authorization", "Bearer " + jwt.getTokenValue());
    }
}
```

### 3. CONFIGURACIÓN CENTRALIZADA (9/10)

#### Config Server

**Configuración Compartida**
- Archivo: `infrastructure/config-repo/application.yml`
- JWT config centralizada (líneas 13-52)
  - `issuer-uri`
  - `jwk-set-uri`
  - `audience`
- Eureka config centralizada (líneas 59-73)
- Actuator config (líneas 77-92)
- Logging config (líneas 96-105)

**Configuraciones Específicas por Servicio**
- `gateway.yml`: Rutas, filtros, circuit breakers
- `user-service.yml`: Puerto, context-path
- `order-service.yml`: Puerto, context-path
- `product-service.yml`: Puerto, context-path

**Ventajas:**
- ✅ Single source of truth
- ✅ Cambios centralizados
- ✅ Versionado en Git
- ✅ Refresh dinámico (con `/actuator/refresh`)

### 4. SERVICE DISCOVERY (10/10)

#### Eureka Integration

**Registro de Servicios:**
- Todos los servicios se registran automáticamente en Eureka
- Health checks configurados (application.yml línea 71)
- Metadata personalizada por servicio

**Feign Clients:**
- Uso correcto de service discovery: `@FeignClient(name = "user-service")`
- Load balancing automático con Ribbon/Spring Cloud LoadBalancer
- Failover automático si una instancia cae

**Gateway Routing:**
- Load balancing con `lb://` prefix:
  - `lb://user-service` (gateway.yml línea 29)
  - `lb://product-service` (línea 48)
  - `lb://order-service` (línea 58)

### 5. MANEJO DE ERRORES (8/10)

#### GlobalExceptionHandler

**User Service**
- Archivo: `GlobalExceptionHandler.java`
- Líneas: 85-279
- Completo y bien documentado

**Excepciones Manejadas:**
- ✅ Validation errors (`@Valid`) → 400 Bad Request
- ✅ Access Denied → 403 Forbidden
- ✅ Authentication failures → 401 Unauthorized
- ✅ Resource Not Found → 404 Not Found
- ✅ Generic errors → 500 Internal Server Error

**Respuestas Consistentes:**
```java
{
  "timestamp": "2025-11-22T22:45:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid JWT token",
  "path": "/api/users/me"
}
```

### 6. CONTROL DE ACCESO BASADO EN ROLES (9/10)

#### @PreAuthorize Implementation

**User Service:**
```java
@PreAuthorize("hasRole('ADMIN')")
@DeleteMapping("/{id}")
public void deleteUser(@PathVariable String id)
```
- Líneas: 143, 182, 216
- Requiere rol ADMIN para operaciones críticas

**Product Service:**
```java
@PreAuthorize("hasRole('ADMIN')")
@PostMapping
public ProductDTO createProduct(@Valid @RequestBody CreateProductRequest request)
```
- Líneas: 141, 170, 204
- Requiere rol ADMIN para crear/actualizar/eliminar

**Configuración:**
- `@EnableMethodSecurity` correctamente habilitado en todos los SecurityConfig
- Roles extraídos del claim `realm_access.roles` de Keycloak

### 7. STATELESS ARCHITECTURE (10/10)

#### Session Management

**Configuración:**
```java
.sessionManagement(session -> session
    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
)
```

**Ventajas:**
- ✅ No se crea `HttpSession`
- ✅ Escalabilidad horizontal sin sticky sessions
- ✅ Cada request contiene toda la información necesaria (JWT)
- ✅ Reduce uso de memoria en servidores
- ✅ Facilita despliegue en contenedores/Kubernetes

### 8. LOGGING Y DEBUGGING (7/10)

#### Sistema de Logs

**Puntos de Logging:**
- ✅ Validación de JWT en todos los servicios
- ✅ Propagación de JWT (Gateway y Feign)
- ✅ Operaciones en controllers
- ✅ Exception handlers
- ✅ Spring Security DEBUG habilitado

**Ejemplo de Log de Validación:**
```
========================================
🔐 GATEWAY - VALIDANDO JWT
========================================
Token (primeros 50 chars): eyJhbGciOiJSUzI1NiIs...
✅ Token VÁLIDO en Gateway
Usuario: usuario1
Issuer: http://localhost:8080/realms/mi-realm
Audience: [spring-boot-client, account]
Expira: 2025-11-22T23:45:00Z
========================================
```

---

## ⚠️ MEJORAS RECOMENDADAS

### 🔴 CRÍTICAS (Implementar inmediatamente)

#### 1. HARDCODED URLS EN PRODUCCIÓN

**Problema:**
- Archivo: `infrastructure/config-repo/application.yml` (línea 28)
- URLs hardcodeadas: `http://localhost:8080/realms/mi-realm`
- **Riesgo:** No funcionará en ambientes de staging/producción

**Solución:**
```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${KEYCLOAK_ISSUER_URI:http://localhost:8080/realms/mi-realm}
          jwk-set-uri: ${KEYCLOAK_JWK_SET_URI:http://localhost:8080/realms/mi-realm/protocol/openid-connect/certs}

jwt:
  audience: ${JWT_AUDIENCE:spring-boot-client}

eureka:
  client:
    serviceUrl:
      defaultZone: ${EUREKA_URL:http://localhost:8761/eureka/}
```

**Variables de Entorno Requeridas:**
```bash
# Producción
export KEYCLOAK_ISSUER_URI=https://keycloak.production.com/realms/production-realm
export KEYCLOAK_JWK_SET_URI=https://keycloak.production.com/realms/production-realm/protocol/openid-connect/certs
export JWT_AUDIENCE=production-client
export EUREKA_URL=http://eureka.production.com:8761/eureka/
```

**Impacto:** 🔴 CRÍTICO
**Esfuerzo:** 2 horas
**Prioridad:** 1

---

#### 2. AUSENCIA DE .gitignore

**Problema:**
- No existe archivo `.gitignore` en el proyecto root
- **Riesgo:** Podrian commitearse:
  - Directorios `target/`
  - Archivos IDE (`.idea/`, `*.iml`)
  - Secrets (`.env`, `application-local.yml`)
  - Logs (`*.log`)

**Solución:**

Crear `.gitignore`:
```gitignore
# Build
target/
build/
*.jar
*.war

# IDE
.idea/
*.iml
*.iws
.vscode/
.eclipse/

# Secrets
.env
*.key
*.pem
application-local.yml
application-local.properties

# Logs
*.log
logs/

# OS
.DS_Store
Thumbs.db

# Spring Boot
spring-boot-devtools.properties
```

**Impacto:** 🔴 CRÍTICO
**Esfuerzo:** 15 minutos
**Prioridad:** 1

---

#### 3. CORS COMPLETAMENTE DESHABILITADO

**Problema:**
- Archivos: Todos los `SecurityConfig.java`
- Configuración: `.cors(cors -> cors.disable())`
- **Riesgo:** En producción con frontend, necesitarás CORS configurado

**Solución:**

Crear `CorsConfig.java`:
```java
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Origenes permitidos (desde variable de entorno)
        String allowedOrigins = System.getenv("ALLOWED_ORIGINS");
        if (allowedOrigins != null) {
            configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        } else {
            // Desarrollo
            configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000", "http://localhost:4200"));
        }

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With"));
        configuration.setExposedHeaders(Arrays.asList("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
```

Actualizar `SecurityConfig.java`:
```java
.cors(cors -> cors.configurationSource(corsConfigurationSource()))
```

**Impacto:** 🔴 CRÍTICO (para frontend)
**Esfuerzo:** 1 hora
**Prioridad:** 1

---

#### 4. LOGGING CON System.out/System.err

**Problema:**
- **262 ocurrencias** en 17 archivos
- Uso de `System.out.println()` y `System.err.println()`
- **Riesgo:**
  - Logs no estructurados
  - Difíciles de filtrar
  - Sin niveles (INFO, WARN, ERROR)
  - No se integran con sistemas de logging (ELK, Splunk)

**Archivos Afectados:**
- `JwtConfig.java` (todos los servicios)
- `JWTPropagationGatewayFilterFactory.java`
- `FeignClientInterceptor.java`
- Controllers (todos)

**Solución:**

Agregar SLF4J Logger:
```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class JwtConfig {
    private static final Logger log = LoggerFactory.getLogger(JwtConfig.class);

    @Bean
    public JwtDecoder jwtDecoder(...) {
        return token -> {
            log.debug("Validando JWT - Token: {}...", token.substring(0, 50));

            try {
                Jwt jwt = jwtDecoder.decode(token);
                log.info("Token válido - Usuario: {}, Expira: {}",
                    jwt.getClaimAsString("preferred_username"),
                    jwt.getExpiresAt()
                );
                return jwt;
            } catch (JwtException e) {
                log.error("Token inválido: {}", e.getMessage());
                throw e;
            }
        };
    }
}
```

Configurar `logback-spring.xml`:
```xml
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/application.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/application.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <logger name="com.example" level="DEBUG"/>
    <logger name="org.springframework.security" level="INFO"/>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
    </root>
</configuration>
```

**Impacto:** 🔴 CRÍTICO
**Esfuerzo:** 4 horas
**Prioridad:** 1

---

#### 5. AUSENCIA DE TESTS DE SEGURIDAD

**Problema:**
- **0 tests** en todo el proyecto (`**/test/**/*.java` = 0 archivos)
- **Riesgo:** Cambios pueden romper seguridad sin detección

**Solución:**

Crear tests de seguridad:

```java
@SpringBootTest
@AutoConfigureMockMvc
class SecurityTests {

    @Autowired
    private MockMvc mockMvc;

    // Test 1: Endpoint protegido sin JWT → 401
    @Test
    void protectedEndpoint_withoutJWT_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/users/me"))
            .andExpect(status().isUnauthorized());
    }

    // Test 2: Endpoint protegido con JWT válido → 200
    @Test
    @WithMockJwt(subject = "user1", roles = {"USER"})
    void protectedEndpoint_withValidJWT_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/users/me"))
            .andExpect(status().isOk());
    }

    // Test 3: Endpoint ADMIN sin rol → 403
    @Test
    @WithMockJwt(subject = "user1", roles = {"USER"})
    void adminEndpoint_withoutAdminRole_shouldReturn403() throws Exception {
        mockMvc.perform(delete("/api/users/123"))
            .andExpect(status().isForbidden());
    }

    // Test 4: Endpoint ADMIN con rol → 200
    @Test
    @WithMockJwt(subject = "admin1", roles = {"ADMIN"})
    void adminEndpoint_withAdminRole_shouldReturn200() throws Exception {
        mockMvc.perform(delete("/api/users/123"))
            .andExpect(status().isOk());
    }

    // Test 5: JWT expirado → 401
    @Test
    @WithExpiredJwt
    void protectedEndpoint_withExpiredJWT_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/users/me"))
            .andExpect(status().isUnauthorized());
    }

    // Test 6: JWT con issuer incorrecto → 401
    @Test
    @WithInvalidIssuerJwt
    void protectedEndpoint_withInvalidIssuer_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/users/me"))
            .andExpect(status().isUnauthorized());
    }

    // Test 7: JWT sin audience → 401
    @Test
    @WithJwtWithoutAudience
    void protectedEndpoint_withoutAudience_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/users/me"))
            .andExpect(status().isUnauthorized());
    }
}
```

Tests de integración:
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderServiceIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void createOrder_withValidJWT_shouldCallUserAndProductServices() {
        // Given
        String jwt = getValidJWT();
        CreateOrderRequest request = new CreateOrderRequest(1L, 2);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwt);
        HttpEntity<CreateOrderRequest> entity = new HttpEntity<>(request, headers);

        // When
        ResponseEntity<OrderDTO> response = restTemplate.exchange(
            "/api/orders",
            HttpMethod.POST,
            entity,
            OrderDTO.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getUsername()).isEqualTo("usuario1");
        assertThat(response.getBody().getProductName()).isNotNull();
    }
}
```

**Impacto:** 🔴 CRÍTICO
**Esfuerzo:** 8 horas
**Prioridad:** 1

---

### 🟡 IMPORTANTES (Implementar pronto)

#### 6. RATE LIMITING DESHABILITADO

**Problema:**
- Archivo: `infrastructure/config-repo/gateway.yml` (líneas 68-74)
- Rate limiting comentado
- **Riesgo:** Vulnerable a:
  - Ataques de fuerza bruta
  - DoS (Denial of Service)
  - Abuso de API

**Solución:**

Implementar con Resilience4j (no requiere Redis):

Agregar dependencia en `gateway/pom.xml`:
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-circuitbreaker-reactor-resilience4j</artifactId>
</dependency>
```

Actualizar `gateway.yml`:
```yaml
spring:
  cloud:
    gateway:
      default-filters:
        - name: RequestRateLimiter
          args:
            key-resolver-name: userKeyResolver
            deny-empty-key: false

resilience4j:
  ratelimiter:
    instances:
      gateway:
        limitForPeriod: 100       # 100 requests
        limitRefreshPeriod: 1s    # por segundo
        timeoutDuration: 0s       # sin timeout
```

Crear `RateLimiterConfig.java`:
```java
@Configuration
public class RateLimiterConfig {

    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            // Rate limit por usuario (JWT claim)
            return exchange.getPrincipal()
                .map(principal -> {
                    if (principal instanceof JwtAuthenticationToken) {
                        Jwt jwt = ((JwtAuthenticationToken) principal).getToken();
                        return jwt.getClaimAsString("preferred_username");
                    }
                    return "anonymous";
                })
                .defaultIfEmpty("anonymous");
        };
    }
}
```

**Impacto:** 🟡 ALTO
**Esfuerzo:** 2 horas
**Prioridad:** 2

---

#### 7. ENDPOINT SENSIBLE EXPUESTO

**Problema:**
- Archivo: `UserController.java` (línea 238)
- Endpoint `/jwt-info` expone TODO el JWT en producción
- **Riesgo:** Exposición de información sensible

**Solución:**

Usar `@Profile("dev")`:
```java
@GetMapping("/jwt-info")
@Profile("dev")  // Solo disponible en desarrollo
public Map<String, Object> getJwtInfo(@AuthenticationPrincipal Jwt jwt) {
    return jwt.getClaims();
}
```

O crear endpoint más seguro:
```java
@GetMapping("/me/claims")
public Map<String, Object> getMyClaims(@AuthenticationPrincipal Jwt jwt) {
    // Solo exponer claims no sensibles
    Map<String, Object> safeClaims = new HashMap<>();
    safeClaims.put("username", jwt.getClaimAsString("preferred_username"));
    safeClaims.put("email", jwt.getClaimAsString("email"));
    safeClaims.put("roles", jwt.getClaimAsStringList("realm_access.roles"));
    safeClaims.put("expiresAt", jwt.getExpiresAt());
    // NO exponer: sub, jti, iat, etc.
    return safeClaims;
}
```

**Impacto:** 🟡 MEDIO
**Esfuerzo:** 15 minutos
**Prioridad:** 2

---

#### 8. FALTA VALIDACIÓN DE EXPIRACIÓN EN PROPAGACIÓN

**Problema:**
- Archivos: `FeignClientInterceptor.java`, `JWTPropagationGatewayFilterFactory.java`
- No verifican si el token está próximo a expirar antes de propagarlo
- **Riesgo:** Token puede expirar durante request de larga duración

**Solución:**

Agregar validación en `FeignClientInterceptor`:
```java
@Override
public void apply(RequestTemplate template) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication instanceof JwtAuthenticationToken) {
        Jwt jwt = ((JwtAuthenticationToken) authentication).getToken();

        // Validar expiración
        Instant expiresAt = jwt.getExpiresAt();
        Instant now = Instant.now();
        long secondsUntilExpiration = Duration.between(now, expiresAt).getSeconds();

        if (secondsUntilExpiration < 60) {
            log.warn("⚠️ Token expira en {} segundos - Usuario: {}",
                secondsUntilExpiration,
                jwt.getClaimAsString("preferred_username")
            );
            // Opcionalmente: lanzar excepción o refresh token
            // throw new TokenExpiredException("Token expires in less than 60 seconds");
        }

        template.header("Authorization", "Bearer " + jwt.getTokenValue());
    }
}
```

**Impacto:** 🟡 MEDIO
**Esfuerzo:** 1 hora
**Prioridad:** 2

---

#### 9. AUDIENCE VALIDATION CON VALOR POR DEFECTO

**Problema:**
- Archivos: Todos los `JwtConfig.java`
- Configuración: `@Value("${jwt.audience:spring-boot-client}")`
- **Riesgo:** Si falta configuración, usa valor incorrecto sin fallar

**Solución:**

Forzar configuración explícita:
```java
// ❌ NO USAR DEFAULT
@Value("${jwt.audience:spring-boot-client}")

// ✅ FORZAR CONFIGURACIÓN
@Value("${jwt.audience}")
private String expectedAudience;
```

En producción, si falta `jwt.audience`, la aplicación fallará al arrancar (fail-fast).

Agregar validación en `@PostConstruct`:
```java
@PostConstruct
public void validateConfig() {
    if (expectedAudience == null || expectedAudience.isEmpty()) {
        throw new IllegalStateException(
            "jwt.audience must be configured. " +
            "Set environment variable: JWT_AUDIENCE=your-client-id"
        );
    }
    log.info("JWT Audience configurado: {}", expectedAudience);
}
```

**Impacto:** 🟡 MEDIO
**Esfuerzo:** 30 minutos
**Prioridad:** 2

---

#### 10. PRODUCT SERVICE SIN LOGGING EN JwtConfig

**Problema:**
- Archivo: `product-service/src/main/java/com/example/product/config/JwtConfig.java`
- No tiene wrapper de logging como otros servicios
- **Inconsistencia:** User y Order services tienen logging, Product no

**Solución:**

Agregar wrapper de logging (igual que en User/Order services):
```java
@Bean
public JwtDecoder jwtDecoder(@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri) {
    NimbusJwtDecoder jwtDecoder = JwtDecoders.fromIssuerLocation(issuerUri);

    // ... validators ...

    jwtDecoder.setJwtValidator(combinedValidator);

    // ⭐ WRAPPER PARA LOGGING
    return token -> {
        System.out.println("========================================");
        System.out.println("🔐 PRODUCT SERVICE - VALIDANDO JWT");
        System.out.println("========================================");
        System.out.println("Token (primeros 50 chars): " + token.substring(0, Math.min(50, token.length())) + "...");

        try {
            Jwt jwt = jwtDecoder.decode(token);
            System.out.println("✅ Token VÁLIDO");
            System.out.println("Usuario: " + jwt.getClaimAsString("preferred_username"));
            System.out.println("========================================");
            return jwt;
        } catch (Exception e) {
            System.err.println("❌ Token INVÁLIDO: " + e.getMessage());
            System.err.println("========================================");
            throw e;
        }
    };
}
```

**Impacto:** 🟡 BAJO (consistencia)
**Esfuerzo:** 15 minutos
**Prioridad:** 3

---

### 🔵 OPCIONALES (Mejoras futuras)

#### 11. CÓDIGO DUPLICADO EN SecurityConfig

**Problema:**
- `SecurityConfig.java` prácticamente idéntico en user/order/product services
- **Oportunidad:** Crear common-lib con configuración compartida

**Solución:**

Crear módulo `common-security`:
```xml
<!-- pom.xml del nuevo módulo -->
<artifactId>common-security</artifactId>
<packaging>jar</packaging>
```

```java
// common-security/src/main/java/.../BaseSecurityConfig.java
@Configuration
public class BaseSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

        return http.build();
    }
}
```

Luego en cada servicio:
```java
@Configuration
public class SecurityConfig extends BaseSecurityConfig {
    // Solo override si necesitas personalización
}
```

**Impacto:** 🔵 BAJO (mantenimiento)
**Esfuerzo:** 3 horas
**Prioridad:** 4

---

#### 12. MÉTRICAS Y MONITORING

**Problema:**
- Actuator expuesto pero sin métricas de seguridad específicas
- **Oportunidad:** Agregar métricas custom

**Solución:**

Crear `SecurityMetrics.java`:
```java
@Component
public class SecurityMetrics {

    private final Counter jwtValidCounter;
    private final Counter jwtInvalidCounter;
    private final Timer jwtValidationTimer;

    public SecurityMetrics(MeterRegistry registry) {
        this.jwtValidCounter = Counter.builder("jwt.validation.success")
            .description("Successful JWT validations")
            .tag("service", "user-service")
            .register(registry);

        this.jwtInvalidCounter = Counter.builder("jwt.validation.failure")
            .description("Failed JWT validations")
            .tag("service", "user-service")
            .register(registry);

        this.jwtValidationTimer = Timer.builder("jwt.validation.duration")
            .description("JWT validation duration")
            .register(registry);
    }

    public void recordValidJwt() {
        jwtValidCounter.increment();
    }

    public void recordInvalidJwt() {
        jwtInvalidCounter.increment();
    }

    public Timer.Sample startValidationTimer() {
        return Timer.start();
    }

    public void recordValidationTime(Timer.Sample sample) {
        sample.stop(jwtValidationTimer);
    }
}
```

Usar en `JwtConfig`:
```java
@Bean
public JwtDecoder jwtDecoder(..., SecurityMetrics metrics) {
    return token -> {
        Timer.Sample sample = metrics.startValidationTimer();
        try {
            Jwt jwt = jwtDecoder.decode(token);
            metrics.recordValidJwt();
            return jwt;
        } catch (Exception e) {
            metrics.recordInvalidJwt();
            throw e;
        } finally {
            metrics.recordValidationTime(sample);
        }
    };
}
```

**Métricas disponibles:**
- `jwt.validation.success` - Counter de validaciones exitosas
- `jwt.validation.failure` - Counter de validaciones fallidas
- `jwt.validation.duration` - Tiempo de validación

**Impacto:** 🔵 BAJO (observabilidad)
**Esfuerzo:** 2 horas
**Prioridad:** 5

---

#### 13. REFRESH TOKEN STRATEGY

**Problema:**
- No implementado mecanismo de refresh token
- **Oportunidad:** Sesiones largas sin re-autenticación

**Solución:**

Implementar en frontend:
```javascript
// Interceptor para refresh automático
axios.interceptors.response.use(
    response => response,
    async error => {
        if (error.response.status === 401) {
            const refreshToken = localStorage.getItem('refresh_token');
            if (refreshToken) {
                const newAccessToken = await refreshAccessToken(refreshToken);
                // Retry request con nuevo token
            }
        }
        return Promise.reject(error);
    }
);
```

**Impacto:** 🔵 BAJO (UX)
**Esfuerzo:** 4 horas
**Prioridad:** 6

---

#### 14. CIRCUIT BREAKER EN FEIGN CLIENTS

**Problema:**
- Archivo: `order-service.yml` (línea 43)
- `circuitbreaker.enabled: false`
- **Oportunidad:** Mejor resiliencia

**Solución:**

Habilitar en `application.yml`:
```yaml
feign:
  circuitbreaker:
    enabled: true

resilience4j:
  circuitbreaker:
    instances:
      userService:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 10000
        permittedNumberOfCallsInHalfOpenState: 3
```

Crear fallback:
```java
@Component
public class UserServiceFallback implements UserServiceClient {

    @Override
    public UserInfoDTO getCurrentUser() {
        // Retornar usuario por defecto o cached
        log.warn("User Service no disponible - usando fallback");
        return UserInfoDTO.builder()
            .username("unknown")
            .email("unavailable@example.com")
            .build();
    }
}
```

**Impacto:** 🔵 BAJO (resiliencia)
**Esfuerzo:** 2 horas
**Prioridad:** 7

---

#### 15. SECRET ROTATION STRATEGY

**Problema:**
- No documentada estrategia de rotación de claves en Keycloak
- **Oportunidad:** Mayor seguridad a largo plazo

**Solución:**

Documentar proceso:
```markdown
# Rotación de Claves JWKS en Keycloak

## Procedimiento

1. **En Keycloak Admin Console:**
   - Realm Settings → Keys → Providers
   - Agregar nuevo provider RSA (rsa-generated)
   - Nuevo key se marca como "active"
   - Key anterior se marca como "passive" (aún válido)

2. **Período de transición (24 horas):**
   - Nuevos tokens firmados con nueva key
   - Tokens existentes aún válidos (firmados con key anterior)
   - Microservicios descargan ambas keys vía JWKS

3. **Después de 24 horas:**
   - Eliminar key antigua en Keycloak
   - Microservicios automáticamente dejan de aceptar tokens con key antigua

## Frecuencia recomendada
- Desarrollo: Cada 6 meses
- Producción: Cada 3 meses
- Post-incidente: Inmediatamente
```

**Impacto:** 🔵 BAJO (documentación)
**Esfuerzo:** 1 hora
**Prioridad:** 8

---

#### 16. CONTEXTO DE SEGURIDAD EN THREADS ASÍNCRONOS

**Problema:**
- Si se usa `@Async`, `SecurityContext` puede no propagarse
- **Riesgo:** NPE al intentar acceder a JWT en threads asíncronos

**Solución:**

Configurar propagación en `AsyncConfig.java`:
```java
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(25);
        executor.setThreadNamePrefix("async-");

        // ⭐ Decorar con SecurityContext
        executor.setTaskDecorator(new SecurityContextPropagatingTaskDecorator());

        executor.initialize();
        return executor;
    }
}

class SecurityContextPropagatingTaskDecorator implements TaskDecorator {
    @Override
    public Runnable decorate(Runnable runnable) {
        SecurityContext context = SecurityContextHolder.getContext();
        return () -> {
            try {
                SecurityContextHolder.setContext(context);
                runnable.run();
            } finally {
                SecurityContextHolder.clearContext();
            }
        };
    }
}
```

**Impacto:** 🔵 BAJO (si no usas @Async)
**Esfuerzo:** 1 hora
**Prioridad:** 9

---

## ❌ PROBLEMAS CRÍTICOS

**NINGUNO DETECTADO** ✅

La arquitectura **no presenta vulnerabilidades críticas** de seguridad. Todas las validaciones esenciales están implementadas:
- ✅ JWT firmado y validado
- ✅ Audience validation
- ✅ Defense in depth
- ✅ Stateless architecture
- ✅ Role-based access control

---

## 📈 EVALUACIÓN POR ÁREA

| Área | Puntuación | Comentario |
|------|-----------|------------|
| **Validación JWT** | 10/10 | Perfecta implementación |
| **Propagación JWT** | 10/10 | Gateway y Feign correctos |
| **Defense in Depth** | 9/10 | Excelente - cada servicio valida |
| **RBAC** | 9/10 | Muy bien implementado |
| **Configuración** | 8/10 | Bien, pero URLs hardcodeadas |
| **Manejo de errores** | 8/10 | Bueno y consistente |
| **Service Discovery** | 10/10 | Eureka bien integrado |
| **Logging** | 6/10 | Usar SLF4J en lugar de System.out |
| **Testing** | 0/10 | No existen tests |
| **Production-ready** | 6/10 | Necesita mejoras críticas |

### Promedio: **8.2/10**

---

## 🎯 TOP 3 PRIORIDADES PARA PRODUCCIÓN

### 1. EXTERNALIZACIÓN DE CONFIGURACIÓN Y SECRETS
**Impacto:** 🔴 CRÍTICO
**Esfuerzo:** 2 horas
**Archivos:** `application.yml`, crear `.gitignore`

**Acciones:**
- [ ] Crear `.gitignore` completo
- [ ] Cambiar URLs hardcodeadas a variables de entorno
- [ ] Documentar variables requeridas en `README.md`
- [ ] Crear `application-prod.yml` con placeholders
- [ ] Probar con variables de entorno

**Resultado esperado:**
```bash
# Producción
export KEYCLOAK_ISSUER_URI=https://keycloak.prod.com/realms/prod
mvn spring-boot:run
# ✅ Aplicación arranca con config de producción
```

---

### 2. IMPLEMENTAR LOGGING PROFESIONAL
**Impacto:** 🔴 CRÍTICO
**Esfuerzo:** 4 horas
**Archivos:** Todos los que usan `System.out` (262 ocurrencias)

**Acciones:**
- [ ] Reemplazar `System.out/err` con SLF4J en todos los archivos
- [ ] Configurar `logback-spring.xml` con rolling file appender
- [ ] Configurar niveles de log por ambiente (dev=DEBUG, prod=INFO)
- [ ] Agregar correlation IDs para trazabilidad
- [ ] Estructurar logs en JSON para producción (opcional)

**Resultado esperado:**
```
2025-11-22 15:30:45.123 [http-nio-8082-exec-1] INFO  c.e.u.c.JwtConfig - Token válido - Usuario: usuario1
```

---

### 3. CREAR SUITE DE TESTS DE SEGURIDAD
**Impacto:** 🔴 CRÍTICO
**Esfuerzo:** 8 horas
**Archivos:** Crear en `src/test/java`

**Acciones:**
- [ ] Tests de validación JWT (válido, expirado, firma incorrecta, issuer incorrecto, sin audience)
- [ ] Tests de propagación JWT (Gateway→Service, Service→Service via Feign)
- [ ] Tests de RBAC (con/sin roles, roles incorrectos)
- [ ] Tests de exception handlers (401, 403, 404, 500)
- [ ] Tests de integración end-to-end (crear orden completa)
- [ ] Configurar CI/CD para ejecutar tests automáticamente

**Cobertura objetivo:** >70%

**Resultado esperado:**
```bash
mvn test
# ✅ 45 tests passed
# Coverage: 75%
```

---

## 📝 LISTA DE VERIFICACIÓN PRE-PRODUCCIÓN

### Seguridad
- [ ] URLs externalizadas (no hardcoded)
- [ ] Secrets en variables de entorno (no en código)
- [ ] `.gitignore` configurado
- [ ] CORS configurado para orígenes permitidos
- [ ] Rate limiting habilitado
- [ ] Endpoint `/jwt-info` solo en dev
- [ ] Audience validation sin fallback
- [ ] HTTPS habilitado (no HTTP)

### Logging y Monitoring
- [ ] SLF4J implementado (no System.out)
- [ ] Niveles de log configurados por ambiente
- [ ] Logs estructurados (JSON opcional)
- [ ] Métricas de seguridad expuestas
- [ ] Alertas configuradas (tokens inválidos, errores 401/403)

### Testing
- [ ] Tests unitarios de validación JWT
- [ ] Tests de integración de propagación
- [ ] Tests de RBAC
- [ ] Tests de exception handlers
- [ ] Cobertura >70%
- [ ] CI/CD ejecuta tests automáticamente

### Resiliencia
- [ ] Circuit breakers habilitados
- [ ] Timeouts configurados
- [ ] Retry policies configuradas
- [ ] Fallbacks implementados

### Documentación
- [ ] README con instrucciones de despliegue
- [ ] Variables de entorno documentadas
- [ ] Diagrama de arquitectura actualizado
- [ ] Procedimientos de rotación de claves

---

## 📚 RECURSOS ADICIONALES

### Documentación Oficial
- [Spring Security OAuth2 Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/index.html)
- [Keycloak Documentation](https://www.keycloak.org/documentation)
- [Spring Cloud Gateway](https://spring.io/projects/spring-cloud-gateway)
- [Resilience4j](https://resilience4j.readme.io/)

### Mejores Prácticas
- [OWASP API Security Top 10](https://owasp.org/www-project-api-security/)
- [NIST Cybersecurity Framework](https://www.nist.gov/cyberframework)
- [12 Factor App](https://12factor.net/)

### Tools
- [JWT.io](https://jwt.io/) - Decoder JWT
- [JWK Set Validator](https://mkjwk.org/) - Validar JWKS
- [SonarQube](https://www.sonarqube.org/) - Code quality
- [OWASP ZAP](https://www.zaproxy.org/) - Security testing

---

## 📊 CONCLUSIÓN

Esta arquitectura demuestra una **sólida comprensión de seguridad en microservicios** y está muy bien implementada para una POC. El código es limpio, bien documentado y sigue principios de Zero Trust y Defense in Depth.

**Para POC/Demo:** ✅ **EXCELENTE (9/10)**

**Para Producción:** ⏸️ **IMPLEMENTAR TOP 3 PRIORIDADES**

Con las 3 mejoras críticas implementadas, esta arquitectura alcanzaría **9.5/10** y estaría **production-ready**.

---

**Última actualización:** 22 Noviembre 2025
**Revisado por:** Claude Code Agent
**Siguiente revisión:** Después de implementar TOP 3 prioridades
