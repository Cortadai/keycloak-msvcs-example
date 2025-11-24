# ✅ Mejora Implementada: Configuración CORS

## 📋 Resumen

Se ha implementado la **mejora crítica #3** identificada en el archivo `MEJORAS.md`:

**MEJORA #3: CORS CONFIGURATION** ✅ COMPLETADA

Esta implementación permite que aplicaciones frontend (como Angular en puerto 4200) puedan hacer requests a los microservicios sin ser bloqueadas por las políticas de seguridad del navegador.

---

## 🌍 ¿Qué es CORS?

**CORS (Cross-Origin Resource Sharing)** es un mecanismo de seguridad del navegador que controla si un frontend en un dominio puede hacer requests a un backend en otro dominio.

### Ejemplo del problema sin CORS:

```
Frontend Angular: http://localhost:4200
Backend Gateway:  http://localhost:8081

Sin CORS → Navegador BLOQUEA la request
Con CORS → Navegador PERMITE la request
```

### Error típico sin CORS configurado:

```
Access to XMLHttpRequest at 'http://localhost:8081/api/users'
from origin 'http://localhost:4200' has been blocked by CORS policy:
No 'Access-Control-Allow-Origin' header is present on the requested resource.
```

---

## 🔧 Archivos Creados/Modificados

### 1. Clases CorsConfig Creadas

#### API Gateway (WebFlux)
**Archivo**: `api-gateway/src/main/java/com/example/gateway/config/CorsConfig.java`

- Bean `CorsWebFilter` para programación reactiva
- Configuración específica para WebFlux
- Lee propiedades desde `application.yml` vía variables de entorno

#### User Service (Spring MVC)
**Archivo**: `user-service/src/main/java/com/example/user/config/CorsConfig.java`

- Bean `CorsConfigurationSource` para Spring MVC
- Configuración compartida para servicios tradicionales

#### Product Service (Spring MVC)
**Archivo**: `product-service/src/main/java/com/example/product/config/CorsConfig.java`

#### Order Service (Spring MVC)
**Archivo**: `order-service/src/main/java/com/example/order/config/CorsConfig.java`

---

### 2. SecurityConfig Actualizados

Todos los `SecurityConfig.java` fueron actualizados para habilitar CORS:

#### API Gateway
```java
// WebFlux usa CorsWebFilter bean automáticamente
.cors(cors -> cors.disable());  // Deshabilitado porque usamos CorsWebFilter bean
```

#### Microservicios (User, Product, Order)
```java
@Autowired
private CorsConfigurationSource corsConfigurationSource;

// ...

.cors(cors -> cors.configurationSource(corsConfigurationSource))
```

---

### 3. Configuración Centralizada

**Archivo**: `infrastructure/config-repo/application.yml`

```yaml
# ===============================================
# 🌍 CORS CONFIGURATION
# ===============================================
cors:
  allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:4200,http://localhost:3000,http://localhost:8080}
  allowed-methods: ${CORS_ALLOWED_METHODS:GET,POST,PUT,DELETE,OPTIONS,PATCH}
  allowed-headers: ${CORS_ALLOWED_HEADERS:Authorization,Content-Type,X-Requested-With,Accept,Origin}
  exposed-headers: ${CORS_EXPOSED_HEADERS:Authorization,X-Total-Count,X-Page-Number}
  max-age: ${CORS_MAX_AGE:3600}
  allow-credentials: ${CORS_ALLOW_CREDENTIALS:true}
```

---

### 4. Variables de Entorno

**Archivos actualizados**: `.env` y `.env.example`

Nuevas variables agregadas:

| Variable | Descripción | Valor por defecto (desarrollo) |
|----------|-------------|-------------------------------|
| `CORS_ALLOWED_ORIGINS` | Orígenes permitidos | `http://localhost:4200,http://localhost:3000,http://localhost:8080` |
| `CORS_ALLOWED_METHODS` | Métodos HTTP permitidos | `GET,POST,PUT,DELETE,OPTIONS,PATCH` |
| `CORS_ALLOWED_HEADERS` | Headers permitidos en requests | `Authorization,Content-Type,X-Requested-With,Accept,Origin` |
| `CORS_EXPOSED_HEADERS` | Headers expuestos al frontend | `Authorization,X-Total-Count,X-Page-Number` |
| `CORS_MAX_AGE` | Caché de preflight (segundos) | `3600` |
| `CORS_ALLOW_CREDENTIALS` | Permitir credenciales | `true` |

---

## 🚀 Uso desde Angular

### Configuración de Angular (archivo de ejemplo)

**1. Crear un servicio HTTP**

```typescript
// src/app/services/auth.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = 'http://localhost:8081/api/users';  // API Gateway

  constructor(private http: HttpClient) { }

  // Obtener perfil del usuario autenticado
  getProfile(token: string): Observable<any> {
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    });

    return this.http.get(`${this.apiUrl}/me`, { headers });
  }

  // Crear usuario
  createUser(userData: any, token: string): Observable<any> {
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    });

    return this.http.post(this.apiUrl, userData, { headers });
  }
}
```

**2. Usar el servicio en un componente**

```typescript
// src/app/components/profile/profile.component.ts
import { Component, OnInit } from '@angular/core';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-profile',
  templateUrl: './profile.component.html'
})
export class ProfileComponent implements OnInit {
  profile: any;
  token: string = '';  // Obtenido de Keycloak

  constructor(private authService: AuthService) { }

  ngOnInit(): void {
    // Asumiendo que ya tienes el token de Keycloak
    this.token = localStorage.getItem('access_token') || '';

    this.authService.getProfile(this.token).subscribe({
      next: (data) => {
        this.profile = data;
        console.log('Perfil obtenido:', data);
      },
      error: (error) => {
        console.error('Error al obtener perfil:', error);
      }
    });
  }
}
```

**3. Obtener token de Keycloak primero**

```typescript
// src/app/services/keycloak.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class KeycloakService {
  private keycloakUrl = 'http://localhost:8080/realms/mi-realm/protocol/openid-connect/token';

  constructor(private http: HttpClient) { }

  login(username: string, password: string): Observable<any> {
    const body = new URLSearchParams();
    body.set('client_id', 'mi-cliente');
    body.set('username', username);
    body.set('password', password);
    body.set('grant_type', 'password');

    const headers = new HttpHeaders({
      'Content-Type': 'application/x-www-form-urlencoded'
    });

    return this.http.post(this.keycloakUrl, body.toString(), { headers });
  }
}
```

**4. Configurar módulo HTTP**

```typescript
// src/app/app.module.ts
import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { HttpClientModule } from '@angular/common/http';

import { AppComponent } from './app.component';
import { ProfileComponent } from './components/profile/profile.component';

@NgModule({
  declarations: [
    AppComponent,
    ProfileComponent
  ],
  imports: [
    BrowserModule,
    HttpClientModule  // ← Importante para hacer requests HTTP
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }
```

---

## 🔄 Flujo de CORS

### Preflight Request (OPTIONS)

Cuando el frontend hace una request "compleja" (con headers custom o métodos distintos a GET/POST), el navegador primero envía una **preflight request**:

```
1. Frontend Angular quiere hacer: POST http://localhost:8081/api/users
   con header: Authorization: Bearer {token}

2. Navegador detecta que es cross-origin y "compleja"

3. Navegador envía PREFLIGHT:
   OPTIONS http://localhost:8081/api/users
   Origin: http://localhost:4200
   Access-Control-Request-Method: POST
   Access-Control-Request-Headers: Authorization, Content-Type

4. Gateway (CorsWebFilter) responde:
   Access-Control-Allow-Origin: http://localhost:4200
   Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS, PATCH
   Access-Control-Allow-Headers: Authorization, Content-Type, X-Requested-With, Accept, Origin
   Access-Control-Allow-Credentials: true
   Access-Control-Max-Age: 3600

5. Navegador verifica que TODO esté permitido

6. Si OK → Navegador envía request REAL:
   POST http://localhost:8081/api/users
   Authorization: Bearer {token}
   Content-Type: application/json

7. Si NO → Navegador BLOQUEA la request y muestra error CORS
```

### Request Simple (sin preflight)

Requests simples como GET sin headers custom NO requieren preflight:

```
1. Frontend: GET http://localhost:8081/api/users/me

2. Gateway responde con headers CORS en la respuesta:
   Access-Control-Allow-Origin: http://localhost:4200
   Access-Control-Allow-Credentials: true

3. Navegador permite que el frontend lea la respuesta
```

---

## ⚙️ Configuración por Ambiente

### Desarrollo Local (Angular en puerto 4200)

```bash
# .env
CORS_ALLOWED_ORIGINS=http://localhost:4200,http://localhost:3000,http://localhost:8080
```

### Staging

```bash
# .env
CORS_ALLOWED_ORIGINS=https://app-staging.example.com,https://admin-staging.example.com
```

### Producción

```bash
# .env o variables de sistema
CORS_ALLOWED_ORIGINS=https://app.example.com,https://admin.example.com
CORS_ALLOW_CREDENTIALS=true
```

**⚠️ NUNCA usar `*` en producción con credentials:**

```yaml
# ❌ PELIGROSO en producción
cors:
  allowed-origins: "*"
  allow-credentials: true  # ← ERROR: No se puede combinar * con credentials

# ✅ CORRECTO en producción
cors:
  allowed-origins: "https://app.example.com,https://admin.example.com"
  allow-credentials: true
```

---

## 🔒 Seguridad

### Mejores Prácticas Implementadas

✅ **Orígenes específicos**: No usamos `*` wildcard
✅ **Métodos limitados**: Solo los métodos HTTP necesarios
✅ **Headers específicos**: Solo los headers permitidos
✅ **Max Age configurado**: Reduce preflight requests innecesarias
✅ **Credentials controladas**: Solo si es necesario
✅ **Configurable por ambiente**: Diferentes orígenes en dev/staging/prod

### Validaciones de Seguridad

1. **Validación de Origen**: Solo requests de orígenes en `CORS_ALLOWED_ORIGINS`
2. **Validación de Método**: Solo métodos en `CORS_ALLOWED_METHODS`
3. **Validación de Headers**: Solo headers en `CORS_ALLOWED_HEADERS`
4. **JWT Validation**: CORS NO reemplaza la validación de JWT
   - CORS permite la request desde el navegador
   - JWT valida que el usuario esté autenticado
   - Ambos trabajan juntos

---

## 🎯 Defense in Depth

### Doble Configuración CORS

CORS está configurado tanto en el **Gateway** como en los **microservicios**:

#### Escenario 1: Frontend → Gateway → Microservicio (RECOMENDADO)

```
Frontend (4200) → Gateway (8081) → User Service (8082)
                   ↑
                   CORS validado aquí
```

CORS se maneja en el Gateway. Los microservicios también tienen CORS configurado como segunda línea de defensa.

#### Escenario 2: Frontend → Microservicio directo (SOLO DESARROLLO)

```
Frontend (4200) → User Service (8082)
                   ↑
                   CORS validado aquí
```

En desarrollo, puedes llamar directo al microservicio. CORS está configurado y funcionará.

**En producción**: Los microservicios NO deberían ser accesibles directamente desde internet (solo vía Gateway).

---

## 📊 Comparación Antes/Después

| Aspecto | Antes | Después |
|---------|-------|---------|
| **CORS** | Completamente deshabilitado | Habilitado y configurado |
| **Frontend Angular** | ❌ Bloqueado por navegador | ✅ Puede hacer requests |
| **Configuración** | Hardcoded `cors.disable()` | Variables de entorno |
| **Seguridad** | ⚠️ Sin protección CORS | ✅ Orígenes específicos |
| **Flexibilidad** | ❌ Un solo valor | ✅ Configurable por ambiente |
| **Production-ready** | ❌ No funcional | ✅ Listo para producción |

---

## 🧪 Pruebas

### 1. Probar desde navegador

Abre la consola de Angular y haz una request:

```typescript
// En la consola del navegador (F12)
fetch('http://localhost:8081/api/users/me', {
  method: 'GET',
  headers: {
    'Authorization': 'Bearer YOUR_JWT_TOKEN_HERE',
    'Content-Type': 'application/json'
  }
})
.then(res => res.json())
.then(data => console.log(data))
.catch(err => console.error(err));
```

**Resultado esperado**:
- ✅ Sin errores CORS
- ✅ Respuesta del servidor (200 OK o 401 si token inválido)

### 2. Verificar headers CORS en response

```bash
curl -I -X OPTIONS http://localhost:8081/api/users \
  -H "Origin: http://localhost:4200" \
  -H "Access-Control-Request-Method: POST" \
  -H "Access-Control-Request-Headers: Authorization, Content-Type"
```

**Resultado esperado**:
```
HTTP/1.1 200 OK
Access-Control-Allow-Origin: http://localhost:4200
Access-Control-Allow-Methods: GET,POST,PUT,DELETE,OPTIONS,PATCH
Access-Control-Allow-Headers: Authorization,Content-Type,X-Requested-With,Accept,Origin
Access-Control-Allow-Credentials: true
Access-Control-Max-Age: 3600
```

### 3. Verificar logs de inicio

Al iniciar los servicios, deberías ver en los logs:

```
========================================
🌍 CONFIGURACIÓN CORS - API GATEWAY
========================================
✅ Orígenes permitidos: [http://localhost:4200, http://localhost:3000, http://localhost:8080]
✅ Métodos permitidos: [GET, POST, PUT, DELETE, OPTIONS, PATCH]
✅ Headers permitidos: [Authorization, Content-Type, X-Requested-With, Accept, Origin]
✅ Headers expuestos: [Authorization, X-Total-Count, X-Page-Number]
✅ Credenciales permitidas: true
✅ Max Age (preflight cache): 3600 segundos
========================================
```

---

## 🔍 Troubleshooting

### Error: "CORS policy: No 'Access-Control-Allow-Origin' header"

**Causa**: El origen del frontend no está en `CORS_ALLOWED_ORIGINS`

**Solución**:
```bash
# En .env, agregar el origen
CORS_ALLOWED_ORIGINS=http://localhost:4200,http://localhost:3000
```

### Error: "CORS policy: Request header not allowed"

**Causa**: Estás enviando un header que no está en `CORS_ALLOWED_HEADERS`

**Solución**:
```bash
# Agregar el header necesario
CORS_ALLOWED_HEADERS=Authorization,Content-Type,X-Custom-Header
```

### Error: "The 'Access-Control-Allow-Origin' header contains multiple values"

**Causa**: CORS configurado en múltiples lugares (Gateway + microservicio)

**Solución**:
- Si llamas vía Gateway, desactiva CORS en microservicios
- O configura solo en un lugar

### Preflight request tarda mucho

**Solución**: Aumentar `CORS_MAX_AGE` para cachear preflight más tiempo:

```bash
CORS_MAX_AGE=7200  # 2 horas
```

---

## 📝 Checklist de Configuración

Antes de desplegar con frontend:

- [ ] Archivo `.env` configurado con orígenes correctos
- [ ] Variable `CORS_ALLOWED_ORIGINS` incluye el dominio del frontend
- [ ] HTTPS habilitado en producción
- [ ] No usar `*` wildcard con credentials en producción
- [ ] Probar preflight request con curl
- [ ] Verificar logs muestran configuración CORS
- [ ] Frontend puede hacer requests sin errores CORS
- [ ] JWT validation funciona correctamente (CORS no lo reemplaza)

---

## 📚 Referencias

- [MDN - CORS](https://developer.mozilla.org/es/docs/Web/HTTP/CORS)
- [Spring Security CORS](https://docs.spring.io/spring-security/reference/servlet/integrations/cors.html)
- [Spring WebFlux CORS](https://docs.spring.io/spring-framework/reference/web/webflux-cors.html)
- [Angular HttpClient](https://angular.io/guide/http)

---

**Implementado**: 23 Noviembre 2025
**Estado**: ✅ COMPLETADO
**Impacto**: 🔴 CRÍTICO (para frontend)
**Esfuerzo**: 1.5 horas
**Prioridad**: 1
