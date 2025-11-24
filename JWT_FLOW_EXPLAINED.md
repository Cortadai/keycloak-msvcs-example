# 🔐 JWT Flow Explained - Flujo Completo Detallado

Este documento explica **EXACTAMENTE** cómo el JWT viaja desde el cliente hasta los microservicios, con validaciones en cada capa.

---

## 📊 Diagrama General

```
┌─────────────┐
│   Cliente   │
│   (Postman) │
└──────┬──────┘
       │ 1. POST /token (Keycloak)
       │    username=user, password=user
       ↓
┌─────────────────┐
│    Keycloak     │
│   (localhost:   │
│      8080)      │
└──────┬──────────┘
       │ 2. Responde con JWT
       │    access_token=eyJhbGc...
       ↓
┌─────────────┐
│   Cliente   │
│  guarda JWT │
└──────┬──────┘
       │ 3. GET /api/users/me
       │    Authorization: Bearer eyJhbGc...
       ↓
┌───────────────────────────────────────┐
│         API Gateway (8081)            │
│  ┌─────────────────────────────────┐  │
│  │  Spring Security Filter Chain   │  │
│  │  1. Extract JWT from header     │  │
│  │  2. Validate signature (JWKS)   │  │
│  │  3. Validate expiration         │  │
│  │  4. Validate issuer             │  │
│  │  5. Create SecurityContext      │  │
│  └─────────────┬───────────────────┘  │
│                │ JWT VÁLIDO ✅         │
│  ┌─────────────↓───────────────────┐  │
│  │  JWTPropagationFilter           │  │
│  │  1. Get JWT from SecurityContext│  │
│  │  2. Add to request header       │  │
│  └─────────────┬───────────────────┘  │
│                │                       │
│  ┌─────────────↓───────────────────┐  │
│  │  Gateway Routes                 │  │
│  │  - Path /api/users/** →         │  │
│  │    lb://user-service            │  │
│  └─────────────┬───────────────────┘  │
└────────────────┼───────────────────────┘
                 │ Consulta Eureka
                 │ "¿Dónde está user-service?"
                 ↓
         ┌───────────────┐
         │ Eureka (8761) │
         │ Responde:     │
         │ localhost:8082│
         └───────┬───────┘
                 │
                 ↓
         GET http://localhost:8082/users/me
         Authorization: Bearer eyJhbGc...
                 ↓
┌──────────────────────────────────────────┐
│       User Service (8082)                │
│  ┌────────────────────────────────────┐  │
│  │  Spring Security Filter Chain      │  │
│  │  1. Extract JWT from header        │  │
│  │  2. Validate signature (JWKS)      │  │
│  │  3. Validate expiration            │  │
│  │  4. Validate issuer                │  │
│  │  5. Create SecurityContext         │  │
│  └────────────┬───────────────────────┘  │
│               │ JWT VÁLIDO ✅            │
│  ┌────────────↓───────────────────────┐  │
│  │  UserController.getCurrentUser()   │  │
│  │  @AuthenticationPrincipal Jwt jwt  │  │
│  │  - Extract username from JWT       │  │
│  │  - Return user info                │  │
│  └────────────┬───────────────────────┘  │
└───────────────┼──────────────────────────┘
                │
                ↓
         UserInfoDTO (JSON)
                ↓
         Gateway → Cliente
```

---

## 🔍 Flujo Detallado Paso a Paso

### PASO 1: Obtener JWT de Keycloak

**Request:**
```http
POST http://localhost:8080/realms/mi-realm/protocol/openid-connect/token
Content-Type: application/x-www-form-urlencoded

client_id=mi-cliente
&client_secret=tu-secret
&username=user
&password=user
&grant_type=password
```

**Response:**
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6IjEyMzQ1Njc4OTAifQ.eyJzdWIiOiI5ODc2NTQzMjEwIiwicHJlZmVycmVkX3VzZXJuYW1lIjoidXNlciIsImVtYWlsIjoidXNlckBleGFtcGxlLmNvbSIsInJlYWxtX2FjY2VzcyI6eyJyb2xlcyI6WyJ1c2VyIiwib2ZmbGluZV9hY2Nlc3MiXX0sImlzcyI6Imh0dHA6Ly9sb2NhbGhvc3Q6ODA4MC9yZWFsbXMvbWktcmVhbG0iLCJleHAiOjE3MDYwMDAwMDAsImlhdCI6MTcwNTk5OTcwMH0.dGhpcyBpcyBhIHNpZ25hdHVyZSBleGFtcGxl",
  "expires_in": 300,
  "token_type": "Bearer"
}
```

**JWT Decoded (Header):**
```json
{
  "alg": "RS256",
  "typ": "JWT",
  "kid": "1234567890"
}
```

**JWT Decoded (Payload):**
```json
{
  "sub": "9876543210",
  "preferred_username": "user",
  "email": "user@example.com",
  "realm_access": {
    "roles": ["user", "offline_access"]
  },
  "iss": "http://localhost:8080/realms/mi-realm",
  "exp": 1706000000,
  "iat": 1705999700
}
```

**JWT Decoded (Signature):**
```
RSASHA256(
  base64UrlEncode(header) + "." +
  base64UrlEncode(payload),
  private_key_from_keycloak
)
```

**Cliente guarda el token:**
```javascript
const token = response.access_token;
// "eyJhbGciOiJSUzI1NiIsInR..."
```

---

### PASO 2: Cliente llama al Gateway

**Request:**
```http
GET http://localhost:8081/api/users/me
Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR...
```

**Gateway recibe:**
```java
// Spring Security automáticamente extrae el header
String authHeader = request.getHeader("Authorization");
// "Bearer eyJhbGciOiJSUzI1NiIsInR..."

String token = authHeader.substring(7);  // Quita "Bearer "
// "eyJhbGciOiJSUzI1NiIsInR..."
```

---

### PASO 3: Gateway valida JWT (Spring Security)

#### 3.1 Descarga claves públicas de Keycloak (JWKS)

**Gateway hace (automáticamente):**
```http
GET http://localhost:8080/realms/mi-realm/protocol/openid-connect/certs
```

**Keycloak responde:**
```json
{
  "keys": [
    {
      "kid": "1234567890",
      "kty": "RSA",
      "alg": "RS256",
      "use": "sig",
      "n": "xGOr-H7A...",  // Public key modulus
      "e": "AQAB"         // Public key exponent
    }
  ]
}
```

**Gateway guarda en cache** (no descarga en cada request).

---

#### 3.2 Valida firma del JWT

**Código (Spring Security hace esto automáticamente):**
```java
// 1. Parsear JWT
String[] parts = token.split("\\.");
String headerBase64 = parts[0];
String payloadBase64 = parts[1];
String signatureBase64 = parts[2];

// 2. Obtener clave pública de Keycloak (de JWKS)
String kid = parseHeader(headerBase64).get("kid");  // "1234567890"
PublicKey publicKey = getPublicKeyFromJWKS(kid);

// 3. Recalcular firma
String data = headerBase64 + "." + payloadBase64;
byte[] expectedSignature = RSASHA256(data, publicKey);

// 4. Comparar firmas
byte[] actualSignature = Base64.decode(signatureBase64);
if (!Arrays.equals(expectedSignature, actualSignature)) {
    throw new JwtException("Invalid signature");  // → 401
}
```

**Si la firma es inválida:**
```
401 Unauthorized
{
  "error": "invalid_token",
  "error_description": "Invalid signature"
}
```

---

#### 3.3 Valida expiración

**Código:**
```java
// Parsear payload
Map<String, Object> payload = parsePayload(payloadBase64);

// Obtener claim "exp" (Unix timestamp)
long exp = (long) payload.get("exp");  // 1706000000
long now = System.currentTimeMillis() / 1000;  // 1705999800

if (now > exp) {
    throw new JwtException("Token expired");  // → 401
}
```

**Si el token expiró:**
```
401 Unauthorized
{
  "error": "invalid_token",
  "error_description": "Token expired"
}
```

---

#### 3.4 Valida issuer

**Código:**
```java
String issuer = (String) payload.get("iss");
String expectedIssuer = "http://localhost:8080/realms/mi-realm";

if (!issuer.equals(expectedIssuer)) {
    throw new JwtException("Invalid issuer");  // → 401
}
```

**Si el issuer no coincide:**
```
401 Unauthorized
{
  "error": "invalid_token",
  "error_description": "Token issuer does not match"
}
```

---

#### 3.5 Crea SecurityContext

**Código (Spring Security automático):**
```java
// 1. Crear objeto Jwt
Jwt jwt = new Jwt(
    tokenValue,
    issuedAt,
    expiresAt,
    headers,
    claims
);

// 2. Extraer authorities (roles)
List<GrantedAuthority> authorities = extractAuthorities(jwt);
// ["ROLE_user", "ROLE_offline_access"]

// 3. Crear JwtAuthenticationToken
JwtAuthenticationToken authentication = new JwtAuthenticationToken(
    jwt,
    authorities
);

// 4. Guardar en SecurityContext
SecurityContextHolder.getContext().setAuthentication(authentication);
```

**Ahora el request está autenticado ✅**

---

### PASO 4: Gateway propaga JWT (JWTPropagationFilter)

**Código:**
```java
// JWTPropagationFilter.java

// 1. Obtener SecurityContext
Authentication auth = SecurityContextHolder.getContext().getAuthentication();

// 2. Extraer JWT
JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) auth;
Jwt jwt = jwtAuth.getToken();
String tokenValue = jwt.getTokenValue();

// 3. Modificar request para agregar header
ServerHttpRequest modifiedRequest = exchange.getRequest()
    .mutate()
    .header("Authorization", "Bearer " + tokenValue)
    .build();

// 4. Continuar con request modificado
ServerWebExchange modifiedExchange = exchange.mutate()
    .request(modifiedRequest)
    .build();

return chain.filter(modifiedExchange);
```

**Request original:**
```http
GET http://localhost:8081/api/users/me
Authorization: Bearer eyJhbGc...
```

**Request modificado (interno, Gateway → Microservicio):**
```http
GET http://localhost:8082/users/me
Authorization: Bearer eyJhbGc...
```

**Nota:** El header `Authorization` ya existía, pero el filtro lo garantiza.

---

### PASO 5: Gateway enruta al microservicio

#### 5.1 Rewrite Path

**Configuración (gateway.yml):**
```yaml
filters:
  - RewritePath=/api/users/(?<segment>.*), /${segment}
```

**Path original:** `/api/users/me`
**Path reescrito:** `/users/me`

---

#### 5.2 Service Discovery

**Gateway consulta Eureka:**
```
¿Dónde está user-service?
```

**Eureka responde:**
```
user-service:
  - localhost:8082 (weight: 100)
```

**Si hubiera múltiples instancias:**
```
user-service:
  - localhost:8082 (weight: 100)
  - localhost:8092 (weight: 100)
  - localhost:8102 (weight: 100)
```

**Gateway hace load balancing** (round-robin por defecto).

---

#### 5.3 Gateway envía request

**Request enviado:**
```http
GET http://localhost:8082/users/me
Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR...
Host: localhost:8082
X-Forwarded-For: 192.168.1.100
X-Forwarded-Proto: http
X-Forwarded-Host: localhost:8081
```

---

### PASO 6: User Service valida JWT (de nuevo)

**Spring Security en User Service hace EXACTAMENTE lo mismo que el Gateway:**

1. ✅ Extrae JWT del header
2. ✅ Descarga JWKS de Keycloak (cached)
3. ✅ Valida firma
4. ✅ Valida expiración
5. ✅ Valida issuer
6. ✅ Crea SecurityContext

**¿Por qué validar de nuevo?**
- Defense in Depth
- Zero Trust Architecture
- Por si alguien llama directamente al microservicio (bypassing Gateway)

**Si el JWT es inválido aquí:**
```
401 Unauthorized
```

**Si el JWT es válido:**
Request continúa al controller ✅

---

### PASO 7: Controller procesa request

**Código:**
```java
@GetMapping("/users/me")
public UserInfoDTO getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
    // Spring inyecta el JWT automáticamente
    // Viene del SecurityContext

    // Extraer claims
    String username = jwt.getClaimAsString("preferred_username");
    String email = jwt.getClaimAsString("email");
    List<String> roles = extractRoles(jwt);

    // Crear DTO
    return UserInfoDTO.builder()
        .username(username)
        .email(email)
        .roles(roles)
        .build();
}
```

**Response:**
```json
{
  "username": "user",
  "email": "user@example.com",
  "roles": ["user"]
}
```

---

### PASO 8: Response viaja de regreso

```
User Service → Gateway → Cliente
```

**User Service responde:**
```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "username": "user",
  "email": "user@example.com"
}
```

**Gateway forwarde la respuesta al cliente sin modificarla.**

---

## 🔗 Flujo Inter-Service (Order Service → User Service)

Cuando Order Service llama a User Service:

```
┌──────────────────┐
│  Order Service   │
│  (8084)          │
└────────┬─────────┘
         │ userServiceClient.getCurrentUser()
         ↓
┌───────────────────────────────────────┐
│  FeignClientInterceptor               │
│  1. Get SecurityContext               │
│  2. Extract JWT                       │
│  3. Add to Feign request header       │
└────────┬──────────────────────────────┘
         │ GET http://user-service/users/me
         │ Authorization: Bearer eyJhbGc...
         ↓
      Eureka
         ↓
┌──────────────────┐
│  User Service    │
│  (8082)          │
│  - Valida JWT ✅ │
│  - Procesa       │
│  - Responde      │
└────────┬─────────┘
         │
         ↓
    UserInfoDTO
         ↓
   Order Service
```

**Código del interceptor:**
```java
@Component
public class FeignClientInterceptor implements RequestInterceptor {
    @Override
    public void apply(RequestTemplate requestTemplate) {
        // 1. Obtener SecurityContext
        Authentication auth = SecurityContextHolder
            .getContext()
            .getAuthentication();

        // 2. Extraer JWT
        JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) auth;
        Jwt jwt = jwtAuth.getToken();
        String token = jwt.getTokenValue();

        // 3. Agregar header
        requestTemplate.header("Authorization", "Bearer " + token);
    }
}
```

**Sin este interceptor:**
```
Order Service → User Service (SIN JWT)
User Service → 401 Unauthorized ❌
```

**Con este interceptor:**
```
Order Service → User Service (CON JWT)
User Service → valida JWT ✅
User Service → procesa ✅
User Service → responde ✅
```

---

## 📋 Resumen del Flujo Completo

| Paso | Componente | Acción | JWT Presente |
|------|-----------|--------|--------------|
| 1 | Cliente | Obtiene JWT de Keycloak | ✅ Genera |
| 2 | Cliente | Llama Gateway con JWT | ✅ Header |
| 3 | Gateway | Valida JWT (firma, exp, issuer) | ✅ Válido |
| 4 | Gateway | Propaga JWT a microservicio | ✅ Header |
| 5 | User Service | Valida JWT (de nuevo) | ✅ Válido |
| 6 | User Service | Procesa request | ✅ Claims |
| 7 | User Service | Responde | - |
| 8 | Gateway | Forwarde response | - |
| 9 | Cliente | Recibe response | - |

**Con inter-service:**

| Paso | Componente | Acción | JWT Presente |
|------|-----------|--------|--------------|
| 1-4 | ... | (igual que arriba) | ✅ |
| 5 | Order Service | Valida JWT | ✅ Válido |
| 6 | Order Service | Llama User Service (Feign) | ✅ Propagado |
| 7 | User Service | Valida JWT | ✅ Válido |
| 8 | User Service | Responde | - |
| 9 | Order Service | Llama Product Service (Feign) | ✅ Propagado |
| 10 | Product Service | Valida JWT | ✅ Válido |
| 11 | Product Service | Responde | - |
| 12 | Order Service | Combina + responde | - |

---

## 🎯 Validaciones en cada capa

```
┌─────────────────────────────────────────────────┐
│ LAYER 1: Gateway                                │
│ ✅ Signature validation                         │
│ ✅ Expiration validation                        │
│ ✅ Issuer validation                            │
│ ✅ Rate limiting                                │
│ ✅ Circuit breaker                              │
└─────────────┬───────────────────────────────────┘
              │ JWT propagated
              ↓
┌─────────────────────────────────────────────────┐
│ LAYER 2: Microservicio (User/Product/Order)    │
│ ✅ Signature validation                         │
│ ✅ Expiration validation                        │
│ ✅ Issuer validation                            │
│ ✅ Role-based access control                    │
└─────────────┬───────────────────────────────────┘
              │ JWT propagated (inter-service)
              ↓
┌─────────────────────────────────────────────────┐
│ LAYER 3: Otro Microservicio                    │
│ ✅ Signature validation                         │
│ ✅ Expiration validation                        │
│ ✅ Issuer validation                            │
│ ✅ Role-based access control                    │
└─────────────────────────────────────────────────┘
```

**Resultado:**
- ✅ 3 capas de validación
- ✅ Defense in Depth
- ✅ Zero Trust
- ✅ Seguridad robusta

---

## 🚀 ¿Qué has aprendido?

✅ Cómo Keycloak genera JWT
✅ Cómo Spring Security valida JWT (firma, expiración, issuer)
✅ Cómo el Gateway propaga JWT a microservicios
✅ Cómo los microservicios validan JWT independientemente
✅ Cómo Feign propaga JWT en llamadas inter-service
✅ Defense in Depth en microservicios
✅ Zero Trust Architecture
✅ JWKS (JSON Web Key Set) para validación de firmas

**Este conocimiento es aplicable a:**
- Arquitecturas reales de microservicios
- OAuth2 / OpenID Connect
- Spring Security
- Service Mesh (Istio, Linkerd)
- API Gateways (Kong, Traefik, AWS API Gateway)

🎉 **¡Ahora eres un experto en JWT flow en microservicios!**
