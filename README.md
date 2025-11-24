# 🏗️ Microservicios con Keycloak - Arquitectura JWT-Focused

POC de arquitectura de microservicios con énfasis en **autenticación y autorización** usando Keycloak y JWT.

## 🎯 Objetivo Principal

Demostrar cómo **JWT fluye** a través de una arquitectura de microservicios:
1. Cliente obtiene JWT de Keycloak
2. API Gateway valida JWT
3. Gateway propaga JWT a microservicios
4. Cada microservicio valida JWT independientemente
5. Inter-service communication con JWT propagation

## 🏛️ Arquitectura

```
Keycloak (8080) → API Gateway (8081) → [Eureka Registry]
                         ↓
        ┌────────────────┼────────────────┐
        ▼                ▼                ▼
   User Service    Product Service   Order Service
     (8082)            (8083)            (8084)
```

## 📦 Componentes

### 1. **Config Server** (8888)
- Centraliza configuración de JWT validation
- Almacena `issuer-uri`, `jwk-set-uri`, etc.
- Todos los servicios obtienen su config de aquí

### 2. **Discovery Server - Eureka** (8761)
- Registro de servicios
- Service discovery dinámico
- Health checks

### 3. **API Gateway** (8081)
- **JWT Validation Global**: Valida todos los requests
- **JWT Propagation**: Pasa el token a microservicios
- **Routing**: Enruta a servicios downstream
- **Circuit Breaker**: Resilience4j
- **Rate Limiting**: Control de tráfico

### 4. **User Service** (8082)
- Gestión de perfil de usuario
- **Roles**: USER, ADMIN
- **JWT Validation**: Independiente del gateway
- Endpoints:
  - `GET /api/users/me` - Perfil del usuario autenticado
  - `GET /api/users/{id}` - Usuario por ID (ADMIN)
  - `PUT /api/users/me` - Actualizar perfil

### 5. **Product Service** (8083)
- Catálogo de productos
- **Roles**: USER (read), ADMIN (write)
- **JWT Validation**: Independiente
- Endpoints:
  - `GET /api/products` - Listar productos (público)
  - `GET /api/products/{id}` - Detalle de producto
  - `POST /api/products` - Crear producto (ADMIN)
  - `PUT /api/products/{id}` - Actualizar (ADMIN)

### 6. **Order Service** (8084)
- Gestión de órdenes
- **Roles**: USER (own orders), ADMIN (all)
- **Inter-service**: Llama a User Service para validar usuario
- **JWT Propagation**: Propaga JWT en llamadas internas
- Endpoints:
  - `GET /api/orders` - Mis órdenes (USER)
  - `GET /api/orders/{id}` - Detalle de orden
  - `POST /api/orders` - Crear orden (USER)
  - `GET /api/orders/all` - Todas las órdenes (ADMIN)

### 7. **Common Lib**
- Configuración de seguridad compartida
- DTOs comunes
- Utilities para JWT
- Exception handlers

## 🔐 Flujo de JWT Detallado

### 1. Autenticación Inicial

```bash
# Cliente obtiene token de Keycloak
curl -X POST 'http://localhost:8080/realms/mi-realm/protocol/openid-connect/token' \
  -d 'client_id=microservices-client' \
  -d 'client_secret=secret' \
  -d 'grant_type=password' \
  -d 'username=usuario1' \
  -d 'password=password123'

# Response:
{
  "access_token": "eyJhbGc...",  ← Este JWT contiene: username, roles, exp, etc.
  "token_type": "Bearer"
}
```

### 2. Request a través del Gateway

```bash
# Cliente hace request al Gateway con el token
curl http://localhost:8081/api/users/me \
  -H "Authorization: Bearer eyJhbGc..."

# El Gateway:
# 1. Valida el JWT (firma, expiración, issuer)
# 2. Extrae información (username, roles)
# 3. Propaga el JWT al User Service
# 4. Retorna la respuesta
```

### 3. Validación en el Microservicio

```java
// Cada microservicio valida el JWT independientemente
@GetMapping("/me")
@PreAuthorize("hasRole('USER')")  // ← Spring Security valida el rol del JWT
public UserInfo getCurrentUser(Authentication auth) {
    // auth contiene la info del JWT validado
    String username = auth.getName();
    // ...
}
```

### 4. Inter-Service Communication

```java
// Order Service llama a User Service pasando el JWT
@Service
public class OrderService {

    @Autowired
    private UserServiceClient userClient;

    public Order createOrder(OrderRequest request, String jwt) {
        // Propaga el JWT en la llamada interna
        User user = userClient.getUserById(request.getUserId(), jwt);

        // Valida que el usuario del JWT sea el dueño de la orden
        // ...
    }
}
```

## 🚀 Cómo Ejecutar

### Prerequisitos
- Java 17+
- Maven 3.6+
- Docker & Docker Compose

### Paso 1: Levantar Infraestructura

```bash
cd microservices/infrastructure
docker-compose up -d

# Esto levanta:
# - Keycloak (8080)
# - PostgreSQL (5432)
```

### Paso 2: Configurar Keycloak

```bash
# 1. Accede a http://localhost:8080
# 2. Login: admin / admin
# 3. Crea realm: mi-realm
# 4. Crea client: microservices-client
# 5. Crea usuarios: usuario1 (USER), admin1 (ADMIN)
```

### Paso 3: Levantar Servicios (en orden)

```bash
# 1. Config Server
cd config-server
./mvnw spring-boot:run

# 2. Discovery Server
cd discovery-server
./mvnw spring-boot:run

# 3. API Gateway
cd api-gateway
./mvnw spring-boot:run

# 4. Microservicios (en paralelo)
cd user-service && ./mvnw spring-boot:run &
cd product-service && ./mvnw spring-boot:run &
cd order-service && ./mvnw spring-boot:run &
```

### Paso 4: Probar

```bash
# Obtener token
TOKEN=$(curl -s -X POST 'http://localhost:8080/realms/mi-realm/protocol/openid-connect/token' \
  -d 'client_id=microservices-client' \
  -d 'client_secret=secret' \
  -d 'grant_type=password' \
  -d 'username=usuario1' \
  -d 'password=password123' | jq -r '.access_token')

# Probar endpoints
curl http://localhost:8081/api/users/me -H "Authorization: Bearer $TOKEN"
curl http://localhost:8081/api/products -H "Authorization: Bearer $TOKEN"
curl http://localhost:8081/api/orders -H "Authorization: Bearer $TOKEN"
```

## 📚 Documentación Detallada

- [ARCHITECTURE.md](docs/ARCHITECTURE.md) - Arquitectura detallada
- [JWT_FLOW.md](docs/JWT_FLOW.md) - Flujo completo del JWT
- [SECURITY_CONFIG.md](docs/SECURITY_CONFIG.md) - Configuración de seguridad
- [TESTING_GUIDE.md](docs/TESTING_GUIDE.md) - Guía de testing
- [DEPLOYMENT.md](docs/DEPLOYMENT.md) - Guía de deployment

## 🔑 Conceptos Clave Demostrados

### 1. JWT Validation en Gateway
- Validación centralizada
- Reduce carga en microservicios
- Single point of authentication

### 2. JWT Propagation
- Header forwarding
- Context propagation
- Security context transfer

### 3. JWT Validation en Microservicios
- Defense in depth (defensa en profundidad)
- Cada servicio es independiente
- Zero trust architecture

### 4. Role-Based Access Control (RBAC)
- Roles en JWT
- @PreAuthorize annotations
- Method-level security

### 5. Inter-Service Security
- JWT propagation between services
- Service-to-service authentication
- Context preservation

## 🎓 Comparación: Monolito vs Microservicios

| Aspecto | Monolito (POC original) | Microservicios (Esta POC) |
|---------|------------------------|---------------------------|
| **JWT Validation** | Una vez en la app | Gateway + cada servicio |
| **Escalabilidad** | Vertical | Horizontal (por servicio) |
| **Deployment** | Todo junto | Independiente |
| **Failure** | Toda la app cae | Solo el servicio afectado |
| **Complexity** | Baja | Media (worth it) |

## 🛠️ Tecnologías Utilizadas

- **Spring Boot** 3.2.0
- **Spring Cloud** 2023.0.0
  - Spring Cloud Gateway
  - Spring Cloud Config
  - Spring Cloud Netflix Eureka
- **Spring Security OAuth2** Resource Server
- **Resilience4j** (Circuit Breaker)
- **Keycloak** 23.x
- **Docker & Docker Compose**

## 📊 Puertos

| Servicio | Puerto |
|----------|--------|
| Keycloak | 8080 |
| API Gateway | 8081 |
| User Service | 8082 |
| Product Service | 8083 |
| Order Service | 8084 |
| Eureka Server | 8761 |
| Config Server | 8888 |

## ⚠️ Notas Importantes

### Seguridad
- En producción, usa HTTPS
- Secrets en variables de entorno
- Rotate client secrets regularmente

### Performance
- Gateway puede ser bottleneck: considerar múltiples instancias
- Cada microservicio valida JWT: impacto en performance (mitigado con cache)
- Connection pooling en inter-service calls

### Observability
- Cada servicio expone `/actuator/health`
- Logs estructurados en JSON
- Correlation IDs para tracing

---

**Siguiente**: Comienza por [Config Server](config-server/README.md)
