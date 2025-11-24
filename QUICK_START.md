# 🚀 Quick Start - Microservicios con Keycloak

Esta guía te llevará paso a paso para levantar toda la arquitectura y probar el flujo de JWT.

---

## 📋 Pre-requisitos

✅ Java 17+
✅ Maven 3.6+
✅ Keycloak 23+ corriendo en `http://localhost:8080`
✅ Realm configurado: `mi-realm`
✅ Usuarios creados: `user` (role: user) y `admin` (role: admin)

---

## 🏗️ Arquitectura

```
Cliente
  ↓ (JWT)
API Gateway (8081)
  ↓ (valida JWT + propaga JWT)
  ├─→ User Service (8082)
  ├─→ Product Service (8083)
  └─→ Order Service (8084)
        ├─→ User Service (con JWT propagado)
        └─→ Product Service (con JWT propagado)

Config Server (8888)
Eureka Server (8761)
Keycloak (8080)
```

---

## 📦 Paso 1: Compilar todos los módulos

Desde el directorio `microservices/`:

```bash
mvn clean install -DskipTests
```

Esto compila:
- ✅ config-server
- ✅ discovery-server
- ✅ api-gateway
- ✅ user-service
- ✅ product-service
- ✅ order-service

---

## 🔧 Paso 2: Iniciar los servicios EN ORDEN

### 2.1 Config Server (PRIMERO)

```bash
cd config-server
mvn spring-boot:run
```

✅ **Esperar**: "Config Server running on port 8888"

**¿Por qué primero?** Los demás servicios necesitan obtener configuración del Config Server al iniciar.

---

### 2.2 Eureka Discovery Server (SEGUNDO)

En otra terminal:

```bash
cd discovery-server
mvn spring-boot:run
```

✅ **Esperar**: "Eureka Server running on port 8761"
✅ **Verificar**: http://localhost:8761

**¿Por qué segundo?** El Gateway y los microservicios se registran en Eureka al iniciar.

---

### 2.3 API Gateway (TERCERO)

En otra terminal:

```bash
cd api-gateway
mvn spring-boot:run
```

✅ **Esperar**: "API Gateway iniciado en puerto 8081"
✅ **Verificar logs**: Debe mostrar "JWT Validation: ENABLED"

**¿Qué hace al iniciar?**
1. Se conecta a Config Server
2. Obtiene configuración de JWT (issuer-uri, jwk-set-uri)
3. Descarga claves públicas de Keycloak (JWKS)
4. Se registra en Eureka
5. Configura rutas a microservicios

---

### 2.4 Microservicios (PARALELO)

Ahora puedes iniciar los 3 microservicios en paralelo:

**Terminal 1: User Service**
```bash
cd user-service
mvn spring-boot:run
```
✅ Esperar: "User Service iniciado en puerto 8082"

**Terminal 2: Product Service**
```bash
cd product-service
mvn spring-boot:run
```
✅ Esperar: "Product Service iniciado en puerto 8083"

**Terminal 3: Order Service**
```bash
cd order-service
mvn spring-boot:run
```
✅ Esperar: "Order Service iniciado en puerto 8084"

---

### 2.5 Verificar que todos se registraron en Eureka

Ir a: http://localhost:8761

Deberías ver:
- ✅ USER-SERVICE
- ✅ PRODUCT-SERVICE
- ✅ ORDER-SERVICE
- ✅ API-GATEWAY

**Si alguno falta**: Revisar logs del servicio, probablemente falló al conectar con Config Server o Eureka.

---

## 🔐 Paso 3: Obtener JWT de Keycloak

### 3.1 Token de Usuario Normal

```bash
curl -X POST http://localhost:8080/realms/mi-realm/protocol/openid-connect/token \
  -d "client_id=mi-cliente" \
  -d "client_secret=tu-secret" \
  -d "username=user" \
  -d "password=user" \
  -d "grant_type=password"
```

**Respuesta:**
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIs...",
  "expires_in": 300,
  "token_type": "Bearer"
}
```

**Guardar el token:**
```bash
export USER_TOKEN="eyJhbGciOiJSUzI1NiIs..."
```

---

### 3.2 Token de Admin

```bash
curl -X POST http://localhost:8080/realms/mi-realm/protocol/openid-connect/token \
  -d "client_id=mi-cliente" \
  -d "client_secret=tu-secret" \
  -d "username=admin" \
  -d "password=admin" \
  -d "grant_type=password"
```

**Guardar el token:**
```bash
export ADMIN_TOKEN="eyJhbGciOiJSUzI1NiIs..."
```

---

## 🧪 Paso 4: Probar el flujo de JWT

### 4.1 User Service - Obtener mi información

```bash
curl -H "Authorization: Bearer $USER_TOKEN" \
  http://localhost:8081/api/users/me
```

**Flujo:**
1. Cliente → Gateway con JWT
2. Gateway valida JWT ✅
3. Gateway → User Service con JWT (propagado)
4. User Service valida JWT ✅
5. User Service devuelve info del usuario

**Respuesta esperada:**
```json
{
  "username": "user",
  "email": "user@example.com",
  "name": "User Name",
  "roles": ["user"]
}
```

**Logs a observar:**
- **Gateway**: "🔐 JWT Propagation Filter"
- **User Service**: "📋 GET /users/me"

---

### 4.2 Product Service - Listar productos

```bash
curl -H "Authorization: Bearer $USER_TOKEN" \
  http://localhost:8081/api/products
```

**Respuesta esperada:**
```json
[
  {
    "id": 1,
    "name": "Laptop",
    "price": 999.99,
    "stock": 10
  },
  {
    "id": 2,
    "name": "Mouse",
    "price": 29.99,
    "stock": 50
  }
]
```

---

### 4.3 Product Service - Crear producto (REQUIERE ADMIN)

**Con usuario normal (debería fallar):**
```bash
curl -X POST \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Keyboard","price":79.99,"stock":25}' \
  http://localhost:8081/api/products
```

**Respuesta esperada:**
```
403 Forbidden
```

**Con admin (debería funcionar):**
```bash
curl -X POST \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Keyboard","price":79.99,"stock":25}' \
  http://localhost:8081/api/products
```

**Respuesta esperada:**
```json
{
  "id": 3,
  "name": "Keyboard",
  "price": 79.99,
  "stock": 25
}
```

**ESTO DEMUESTRA:** Control de acceso basado en roles (`@PreAuthorize("hasRole('ADMIN')`)`)

---

### 4.4 Order Service - Crear orden (⭐ LO MÁS IMPORTANTE)

```bash
curl -X POST \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"productId":1,"quantity":2}' \
  http://localhost:8081/api/orders
```

**Flujo completo:**

```
1. Cliente → Gateway (JWT)
2. Gateway valida JWT ✅
3. Gateway → Order Service (JWT propagado)
4. Order Service valida JWT ✅
5. Order Service → User Service (JWT propagado por Feign)
   ├─ User Service valida JWT ✅
   └─ Devuelve info del usuario
6. Order Service → Product Service (JWT propagado por Feign)
   ├─ Product Service valida JWT ✅
   └─ Devuelve info del producto
7. Order Service crea la orden
8. Order Service → Gateway → Cliente
```

**Respuesta esperada:**
```json
{
  "id": 1,
  "username": "user",
  "productId": 1,
  "productName": "Laptop",
  "productPrice": 999.99,
  "quantity": 2,
  "totalPrice": 1999.98,
  "createdAt": "2025-01-22T10:30:00"
}
```

**Logs a observar (muy importantes):**

**Gateway:**
```
🔐 JWT Propagation Filter
Usuario: user
Destino: http://localhost:8084/orders
```

**Order Service:**
```
📦 POST /orders
Usuario: user
🔗 Llamando a User Service...
🔗 Feign Client Interceptor
Destino: http://user-service/users/me
JWT agregado: Bearer ey...
✓ User Service respondió: user

🔗 Llamando a Product Service...
🔗 Feign Client Interceptor
Destino: http://product-service/products/1
JWT agregado: Bearer ey...
✓ Product Service respondió: Laptop

✓ Orden creada exitosamente
```

**User Service:**
```
📋 GET /users/me
Usuario autenticado: user
```

**Product Service:**
```
📦 GET /products/1
Usuario: user
```

**ESTO DEMUESTRA:**
- ✅ JWT propagado desde Gateway → Order Service
- ✅ JWT propagado desde Order Service → User Service (Feign)
- ✅ JWT propagado desde Order Service → Product Service (Feign)
- ✅ Validación en TODAS las capas (defense in depth)
- ✅ Comunicación inter-service con JWT

---

### 4.5 Order Service - Listar mis órdenes

```bash
curl -H "Authorization: Bearer $USER_TOKEN" \
  http://localhost:8081/api/orders
```

**Respuesta esperada:**
```json
[
  {
    "id": 1,
    "username": "user",
    "productName": "Laptop",
    "quantity": 2,
    "totalPrice": 1999.98
  }
]
```

---

## 🔍 Paso 5: Probar Defense in Depth

### 5.1 Sin JWT (debería fallar)

```bash
curl http://localhost:8081/api/users/me
```

**Respuesta:**
```
401 Unauthorized
```

**Rechazado por:** Gateway (primera capa)

---

### 5.2 JWT inválido (debería fallar)

```bash
curl -H "Authorization: Bearer token-falso" \
  http://localhost:8081/api/users/me
```

**Respuesta:**
```
401 Unauthorized
```

**Rechazado por:** Gateway (validación de firma)

---

### 5.3 JWT expirado (debería fallar)

**Esperar 5 minutos** (los tokens expiran en 300 segundos)

```bash
curl -H "Authorization: Bearer $USER_TOKEN" \
  http://localhost:8081/api/users/me
```

**Respuesta:**
```
401 Unauthorized
```

**Rechazado por:** Gateway (validación de expiración)

---

### 5.4 Llamar directamente al microservicio (bypassing Gateway)

```bash
# Sin JWT
curl http://localhost:8082/users/me
# → 401 Unauthorized ✅

# Con JWT válido
curl -H "Authorization: Bearer $USER_TOKEN" \
  http://localhost:8082/users/me
# → 200 OK ✅
```

**ESTO DEMUESTRA:**
- Microservicios TAMBIÉN validan JWT
- No confían en que el Gateway validó
- Defense in depth funcionando ✅

---

## 🎯 Conceptos Clave Demostrados

### 1. JWT Propagation en Gateway
- **Archivo:** `api-gateway/filter/JWTPropagationFilter.java`
- **Qué hace:** Extrae JWT del SecurityContext y lo agrega al request que va al microservicio
- **Por qué:** Los microservicios necesitan el JWT para validarlo (defense in depth)

### 2. JWT Validation en todos los servicios
- **Archivos:** `*/config/SecurityConfig.java`
- **Qué hace:** `.oauth2ResourceServer(oauth2 -> oauth2.jwt())`
- **Por qué:** Zero trust - cada servicio valida independientemente

### 3. JWT Propagation en Feign (inter-service)
- **Archivo:** `order-service/config/FeignClientInterceptor.java`
- **Qué hace:** Intercepta llamadas Feign y agrega JWT al header
- **Por qué:** Order Service → User/Product Service necesitan JWT

### 4. Configuración centralizada
- **Archivo:** `infrastructure/config-repo/application.yml`
- **Qué hace:** Define `issuer-uri` y `jwk-set-uri` para TODOS los servicios
- **Por qué:** Consistencia - un solo lugar para cambiar config de JWT

### 5. Service Discovery
- **Eureka:** Todos los servicios se registran automáticamente
- **Gateway:** Usa `lb://user-service` (load balanced)
- **Feign:** Usa `@FeignClient("user-service")` (descubre automáticamente)

---

## 🐛 Troubleshooting

### Config Server no inicia
```bash
# Verificar que existe el directorio config-repo
ls infrastructure/config-repo/

# Verificar application.yml en config-server
# Debe apuntar a: file:../infrastructure/config-repo
```

### Gateway no valida JWT (401 en requests válidos)
```bash
# Verificar logs del Gateway
# Debe mostrar: "Downloading keys from Keycloak..."

# Verificar que Keycloak está accesible:
curl http://localhost:8080/realms/mi-realm/protocol/openid-connect/certs

# Verificar issuer-uri en config-repo/application.yml
```

### Servicios no se registran en Eureka
```bash
# Verificar que Eureka está corriendo
curl http://localhost:8761/eureka/apps

# Verificar bootstrap.yml de cada servicio
# Debe conectarse a Config Server primero
```

### Feign no propaga JWT
```bash
# Verificar que existe FeignClientInterceptor
# Verificar logs: "🔗 Feign Client Interceptor"
# Si no aparece, el interceptor no se está ejecutando
```

---

## 📚 Próximos Pasos

### Mejoras sugeridas:
1. **Base de datos**: Agregar PostgreSQL para persistir órdenes
2. **Circuit Breaker**: Habilitar Resilience4j para manejo de fallos
3. **Distributed Tracing**: Agregar Sleuth + Zipkin para tracing
4. **Logging agregado**: Agregar ELK stack (Elasticsearch + Logstash + Kibana)
5. **API Documentation**: Agregar Swagger/OpenAPI a cada servicio
6. **Docker Compose**: Containerizar todos los servicios
7. **Kubernetes**: Deploy en Kubernetes con Istio service mesh

### Explorar:
- JWT con claims custom (agregar tenant_id, permissions, etc.)
- Refresh tokens (renovar tokens sin re-autenticar)
- Token introspection (validar tokens opacos)
- Mutual TLS (mTLS) entre microservicios
- API Gateway con Kong o Traefik

---

## ✅ Resumen

Has implementado con éxito:

✅ **Config Server** - Configuración centralizada
✅ **Eureka** - Service discovery
✅ **API Gateway** - Single entry point con JWT validation
✅ **User Service** - Microservicio con JWT validation
✅ **Product Service** - Control de acceso por roles
✅ **Order Service** - Inter-service communication con JWT propagation

**Conceptos demostrados:**
- Defense in Depth (validación en múltiples capas)
- Zero Trust Architecture (cada servicio valida)
- JWT Propagation (Gateway → Microservicio → Microservicio)
- Service Discovery (Eureka)
- Centralized Configuration (Config Server)
- Role-based Access Control (RBAC)

**Flujo completo de JWT:**
```
Keycloak → Cliente → Gateway → Microservicio → Microservicio
   ↓          ↓         ↓            ↓               ↓
 genera    obtiene   valida       valida         valida
  JWT       JWT       JWT          JWT            JWT
```

🎉 **¡Felicidades! Ahora entiendes cómo fluye el JWT en microservicios.**
