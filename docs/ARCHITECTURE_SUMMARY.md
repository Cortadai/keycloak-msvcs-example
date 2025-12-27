# 🏗️ Arquitectura de Microservicios con Keycloak - Resumen

## 📦 Componentes Implementados

### 1. Config Server (puerto 8888)
- **Ubicación:** `config-server/`
- **Propósito:** Centraliza configuración de todos los servicios
- **Configuración clave:** `infrastructure/config-repo/application.yml`
  - `issuer-uri`: URL de Keycloak para validación
  - `jwk-set-uri`: Endpoint de claves públicas de Keycloak
  - Configuración de Eureka compartida

**¿Por qué es importante?**
- Un solo lugar para cambiar configuración de JWT
- Todos los servicios obtienen la misma configuración
- Consistencia garantizada

---

### 2. Eureka Discovery Server (puerto 8761)
- **Ubicación:** `discovery-server/`
- **Propósito:** Service registry - todos los servicios se registran aquí
- **UI:** http://localhost:8761

**¿Por qué es importante?**
- Gateway descubre IPs de microservicios dinámicamente
- Load balancing automático
- Health checks
- Feign clients usan service discovery

---

### 3. API Gateway (puerto 8081)
- **Ubicación:** `api-gateway/`
- **Propósito:** Single entry point - primera capa de validación JWT

**Componentes clave:**
- `SecurityConfig.java` - Configura validación de JWT
- `JWTPropagationFilter.java` - Propaga JWT a microservicios
- `bootstrap.yml` - Conexión a Config Server

**Rutas configuradas:**
- `/api/users/**` → user-service
- `/api/products/**` → product-service
- `/api/orders/**` → order-service

**¿Qué hace?**
1. Recibe request con JWT
2. Valida JWT (firma, expiración, issuer)
3. Propaga JWT al microservicio
4. Forwarde response al cliente

---

### 4. User Service (puerto 8082)
- **Ubicación:** `user-service/`
- **Propósito:** Gestión de usuarios

**Endpoints:**
- `GET /users/me` - Info del usuario actual (cualquier usuario)
- `GET /users/{id}` - Info de usuario específico (ADMIN only)
- `POST /users` - Crear usuario (ADMIN only)

**Componentes clave:**
- `SecurityConfig.java` - Valida JWT (segunda capa)
- `UserController.java` - Extrae info del JWT con `@AuthenticationPrincipal`
- `UserInfoDTO.java` - DTO con claims del JWT

**¿Qué demuestra?**
- Validación de JWT en microservicio (defense in depth)
- Extracción de claims del JWT
- Control de acceso por roles (`@PreAuthorize`)

---

### 5. Product Service (puerto 8083)
- **Ubicación:** `product-service/`
- **Propósito:** Gestión de productos

**Endpoints:**
- `GET /products` - Listar productos (cualquier usuario)
- `GET /products/{id}` - Obtener producto (cualquier usuario)
- `POST /products` - Crear producto (ADMIN only)
- `PUT /products/{id}` - Actualizar producto (ADMIN only)
- `DELETE /products/{id}` - Eliminar producto (ADMIN only)

**¿Qué demuestra?**
- Control de acceso granular por role
- Read: todos, Write: solo admins
- Patrón común en APIs REST

---

### 6. Order Service (puerto 8084)
- **Ubicación:** `order-service/`
- **Propósito:** Gestión de órdenes + orquestación inter-service

**Endpoints:**
- `GET /orders` - Mis órdenes
- `GET /orders/{id}` - Orden específica
- `POST /orders` - Crear orden (llama a User + Product Service)

**Componentes clave:**
- `SecurityConfig.java` - Valida JWT entrante
- `FeignClientInterceptor.java` - Propaga JWT en llamadas Feign
- `UserServiceClient.java` - Cliente Feign para User Service
- `ProductServiceClient.java` - Cliente Feign para Product Service

**¿Qué demuestra? (⭐ LO MÁS IMPORTANTE)**
- Comunicación inter-service con JWT
- Propagación de JWT en cadena:
  - Cliente → Gateway → Order Service → User Service
  - Cliente → Gateway → Order Service → Product Service
- Validación de JWT en TODAS las capas
- Service orchestration

---

## 🔐 Flujo de JWT - Vista de Alto Nivel

```
1. OBTENER JWT
   Cliente → Keycloak
   POST /token (username + password)
   ← JWT

2. LLAMAR API
   Cliente → Gateway
   GET /api/users/me (Authorization: Bearer JWT)

3. GATEWAY VALIDA
   Gateway → Keycloak JWKS
   Valida firma ✅
   Valida expiración ✅
   Valida issuer ✅

4. GATEWAY PROPAGA
   Gateway → User Service
   GET /users/me (Authorization: Bearer JWT)

5. MICROSERVICIO VALIDA
   User Service → Keycloak JWKS
   Valida firma ✅
   Valida expiración ✅
   Valida issuer ✅

6. MICROSERVICIO PROCESA
   UserController.getCurrentUser()
   Extrae username del JWT
   Devuelve UserInfoDTO

7. RESPONSE
   User Service → Gateway → Cliente
```

---

## 🎯 Conceptos Clave Implementados

### 1. Defense in Depth
**¿Qué es?**
Seguridad en múltiples capas.

**Implementación:**
- Gateway valida JWT
- Cada microservicio valida JWT
- Si Gateway falla, microservicios siguen seguros

**Archivos:**
- `api-gateway/config/SecurityConfig.java`
- `user-service/config/SecurityConfig.java`
- `product-service/config/SecurityConfig.java`
- `order-service/config/SecurityConfig.java`

---

### 2. Zero Trust Architecture
**¿Qué es?**
No confíes en nadie, valida siempre.

**Implementación:**
- Microservicios NO confían en que Gateway validó
- Order Service NO confía en que otros servicios validaron
- Cada servicio valida independientemente

---

### 3. JWT Propagation (Gateway → Microservicio)
**¿Qué es?**
Pasar el JWT del Gateway al microservicio.

**Implementación:**
- `JWTPropagationFilter.java` en Gateway
- Extrae JWT del SecurityContext
- Agrega header `Authorization: Bearer {token}`

**¿Por qué?**
- Microservicio necesita JWT para validarlo
- Defense in depth

---

### 4. JWT Propagation (Microservicio → Microservicio)
**¿Qué es?**
Pasar el JWT cuando un microservicio llama a otro.

**Implementación:**
- `FeignClientInterceptor.java` en Order Service
- Intercepta requests de Feign
- Agrega header `Authorization: Bearer {token}`

**¿Por qué?**
- User/Product Service necesitan JWT para validarlo
- Mantener contexto de seguridad en toda la cadena

---

### 5. Service Discovery
**¿Qué es?**
Descubrir dinámicamente dónde están los servicios.

**Implementación:**
- Eureka Server (8761)
- Todos los servicios se registran con `@EnableDiscoveryClient`
- Gateway usa `lb://user-service` (load balanced)
- Feign usa `@FeignClient("user-service")`

**¿Por qué?**
- No hardcodear IPs
- Múltiples instancias → load balancing
- Health checks automáticos

---

### 6. Centralized Configuration
**¿Qué es?**
Un solo lugar para configuración compartida.

**Implementación:**
- Config Server (8888)
- `infrastructure/config-repo/application.yml` compartido
- Todos los servicios lo obtienen al iniciar

**¿Por qué?**
- Cambiar `issuer-uri` en un solo lugar
- Consistencia garantizada
- Refresh dinámico (con Spring Cloud Bus)

---

### 7. Role-Based Access Control (RBAC)
**¿Qué es?**
Controlar acceso basado en roles del usuario.

**Implementación:**
- `@PreAuthorize("hasRole('ADMIN')")` en controllers
- Roles vienen del JWT (claim `realm_access.roles`)
- Spring Security valida automáticamente

**Ejemplos:**
- User normal → puede leer productos ✅
- User normal → NO puede crear productos ❌ (403)
- Admin → puede crear productos ✅

---

## 📁 Estructura de Archivos Clave

```
microservices/
├── infrastructure/
│   └── config-repo/               ← Configuración centralizada
│       ├── application.yml        ← JWT config compartida ⭐
│       ├── gateway.yml            ← Rutas del Gateway
│       ├── user-service.yml
│       ├── product-service.yml
│       └── order-service.yml
│
├── config-server/                 ← Config Server (8888)
│   └── src/main/resources/
│       └── application.yml        ← Apunta a config-repo
│
├── discovery-server/              ← Eureka (8761)
│
├── api-gateway/                   ← Gateway (8081)
│   ├── config/
│   │   └── SecurityConfig.java   ← Valida JWT ⭐
│   ├── filter/
│   │   └── JWTPropagationFilter.java ← Propaga JWT ⭐
│   └── resources/
│       └── bootstrap.yml          ← Conecta a Config Server
│
├── user-service/                  ← User Service (8082)
│   ├── config/
│   │   └── SecurityConfig.java   ← Valida JWT ⭐
│   ├── controller/
│   │   └── UserController.java   ← Extrae JWT claims ⭐
│   └── dto/
│       └── UserInfoDTO.java
│
├── product-service/               ← Product Service (8083)
│   ├── config/
│   │   └── SecurityConfig.java   ← Valida JWT
│   ├── controller/
│   │   └── ProductController.java ← RBAC con @PreAuthorize ⭐
│   └── dto/
│       └── ProductDTO.java
│
└── order-service/                 ← Order Service (8084)
    ├── config/
    │   ├── SecurityConfig.java   ← Valida JWT
    │   └── FeignClientInterceptor.java ← Propaga JWT inter-service ⭐
    ├── client/
    │   ├── UserServiceClient.java    ← Feign client ⭐
    │   └── ProductServiceClient.java ← Feign client ⭐
    ├── controller/
    │   └── OrderController.java   ← Orquesta llamadas ⭐
    └── dto/
        ├── OrderDTO.java
        ├── UserInfoDTO.java
        └── ProductDTO.java
```

**Archivos marcados con ⭐ son los más importantes para entender el flujo de JWT.**

---

## 🧪 Testing Scenarios

### Escenario 1: Usuario normal lista productos
```bash
# ✅ Debería funcionar
curl -H "Authorization: Bearer $USER_TOKEN" \
  http://localhost:8081/api/products

# Gateway valida JWT ✅
# Product Service valida JWT ✅
# Devuelve lista de productos ✅
```

---

### Escenario 2: Usuario normal intenta crear producto
```bash
# ❌ Debería fallar (403)
curl -X POST \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Test","price":10.0}' \
  http://localhost:8081/api/products

# Gateway valida JWT ✅
# Product Service valida JWT ✅
# @PreAuthorize("hasRole('ADMIN')") falla ❌
# Respuesta: 403 Forbidden
```

---

### Escenario 3: Admin crea producto
```bash
# ✅ Debería funcionar
curl -X POST \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Test","price":10.0}' \
  http://localhost:8081/api/products

# Gateway valida JWT ✅
# Product Service valida JWT ✅
# @PreAuthorize("hasRole('ADMIN')") OK ✅
# Producto creado ✅
```

---

### Escenario 4: Crear orden (inter-service communication)
```bash
# ✅ Flujo completo inter-service
curl -X POST \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"productId":1,"quantity":2}' \
  http://localhost:8081/api/orders

# Flujo:
# 1. Gateway valida JWT ✅
# 2. Order Service valida JWT ✅
# 3. Order Service → User Service (Feign + JWT) ✅
#    - FeignClientInterceptor agrega JWT
#    - User Service valida JWT ✅
# 4. Order Service → Product Service (Feign + JWT) ✅
#    - FeignClientInterceptor agrega JWT
#    - Product Service valida JWT ✅
# 5. Order Service combina info y crea orden ✅
```

---

### Escenario 5: Sin JWT (debería fallar)
```bash
# ❌ Debería fallar (401)
curl http://localhost:8081/api/users/me

# Gateway: No JWT → 401 Unauthorized ❌
```

---

### Escenario 6: JWT inválido (debería fallar)
```bash
# ❌ Debería fallar (401)
curl -H "Authorization: Bearer token-fake" \
  http://localhost:8081/api/users/me

# Gateway: Valida firma → FALLA → 401 ❌
```

---

### Escenario 7: Bypass Gateway (llamada directa)
```bash
# ❌ Sin JWT
curl http://localhost:8082/users/me
# User Service: No JWT → 401 ❌

# ✅ Con JWT
curl -H "Authorization: Bearer $USER_TOKEN" \
  http://localhost:8082/users/me
# User Service: Valida JWT ✅ → Responde ✅

# ESTO DEMUESTRA DEFENSE IN DEPTH ✅
```

---

## 🚀 Orden de Inicio

**IMPORTANTE:** Iniciar en este orden:

```
1. Keycloak (8080)          ← Ya debe estar corriendo
2. Config Server (8888)     ← mvn spring-boot:run
3. Eureka Server (8761)     ← mvn spring-boot:run
4. API Gateway (8081)       ← mvn spring-boot:run
5. User Service (8082)      ← mvn spring-boot:run (paralelo)
6. Product Service (8083)   ← mvn spring-boot:run (paralelo)
7. Order Service (8084)     ← mvn spring-boot:run (paralelo)
```

**¿Por qué este orden?**
- Config Server primero → los demás obtienen config de aquí
- Eureka segundo → los demás se registran aquí
- Gateway tercero → necesita Eureka para descubrir servicios
- Microservicios último → pueden iniciarse en paralelo

---

## 📚 Documentación Adicional

- `README.md` - Visión general de la arquitectura
- `QUICK_START.md` - Guía paso a paso para iniciar
- `JWT_FLOW_EXPLAINED.md` - Flujo detallado de JWT con diagramas
- `ARCHITECTURE_SUMMARY.md` (este archivo) - Resumen de componentes

---

## 🎓 Conceptos Aprendidos

✅ Arquitectura de microservicios
✅ JWT (JSON Web Tokens)
✅ OAuth2 / OpenID Connect
✅ Spring Security (JWT validation)
✅ Spring Cloud Config (configuración centralizada)
✅ Eureka Service Discovery
✅ Spring Cloud Gateway
✅ OpenFeign (cliente HTTP declarativo)
✅ Defense in Depth
✅ Zero Trust Architecture
✅ RBAC (Role-Based Access Control)
✅ JWKS (JSON Web Key Set)
✅ Service orchestration
✅ Inter-service communication

---

## 💡 Mejoras Futuras

1. **Persistencia:** PostgreSQL para órdenes y productos
2. **Circuit Breaker:** Resilience4j para fault tolerance
3. **Distributed Tracing:** Sleuth + Zipkin
4. **Centralized Logging:** ELK Stack
5. **API Documentation:** Swagger/OpenAPI
6. **Refresh Tokens:** Renovar tokens sin re-autenticar
7. **Docker Compose:** Containerizar servicios
8. **Kubernetes:** Deploy en K8s con Istio
9. **Monitoring:** Prometheus + Grafana
10. **CI/CD:** Jenkins/GitHub Actions

---

## ✅ Checklist de Verificación

Antes de dar por terminada la implementación, verifica:

- [ ] Config Server inicia correctamente
- [ ] Eureka muestra todos los servicios registrados
- [ ] Gateway puede obtener token de Keycloak
- [ ] Gateway valida JWT correctamente
- [ ] User Service responde a `/users/me`
- [ ] Product Service lista productos
- [ ] Admin puede crear productos, user no
- [ ] Order Service puede crear órdenes
- [ ] Logs muestran JWT propagation en cada capa
- [ ] Llamadas directas a microservicios requieren JWT
- [ ] 401 sin JWT, 403 sin role correcto
