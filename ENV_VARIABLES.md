# 🔧 Guía de Variables de Entorno

## 📋 Índice

1. [Introducción](#introducción)
2. [Variables Disponibles](#variables-disponibles)
3. [Configuración por Ambiente](#configuración-por-ambiente)
4. [Uso con .env](#uso-con-env)
5. [Uso sin .env (variables de sistema)](#uso-sin-env-variables-de-sistema)
6. [Mejores Prácticas](#mejores-prácticas)
7. [Troubleshooting](#troubleshooting)

---

## 📖 Introducción

Este proyecto ahora soporta **configuración mediante variables de entorno**, eliminando las URLs hardcodeadas y permitiendo despliegues en diferentes ambientes sin modificar el código.

### ¿Por qué usar variables de entorno?

✅ **Flexibilidad**: Cambia configuraciones sin editar código
✅ **Seguridad**: Secretos fuera del repositorio
✅ **Multi-ambiente**: Desarrollo, staging, producción con la misma base de código
✅ **Mejores prácticas**: Sigue los principios de [12 Factor App](https://12factor.net/config)

---

## 🔐 Variables Disponibles

### KEYCLOAK_ISSUER_URI

**Descripción**: URL del emisor de tokens JWT (Keycloak realm)

**Formato**: `http(s)://{host}/realms/{realm-name}`

**Valores por ambiente**:
- **Desarrollo**: `http://localhost:8080/realms/mi-realm`
- **Staging**: `https://keycloak.staging.example.com/realms/staging-realm`
- **Producción**: `https://keycloak.production.com/realms/production-realm`

**Uso en código**: Se valida contra el claim `iss` del JWT

---

### KEYCLOAK_JWK_SET_URI

**Descripción**: URL del conjunto de claves públicas JWK para validar firmas JWT

**Formato**: `http(s)://{host}/realms/{realm-name}/protocol/openid-connect/certs`

**Valores por ambiente**:
- **Desarrollo**: `http://localhost:8080/realms/mi-realm/protocol/openid-connect/certs`
- **Staging**: `https://keycloak.staging.example.com/realms/staging-realm/protocol/openid-connect/certs`
- **Producción**: `https://keycloak.production.com/realms/production-realm/protocol/openid-connect/certs`

**Uso en código**: Spring Security descarga las claves públicas para validar firmas

---

### JWT_AUDIENCE

**Descripción**: Audience esperado en el claim `aud` del JWT

**Formato**: `{client-id}`

**Valores por ambiente**:
- **Desarrollo**: `spring-boot-client`
- **Staging**: `staging-client`
- **Producción**: `production-client`

**Uso en código**: Previene ataques de reutilización de tokens (token reuse)

---

### EUREKA_URL

**Descripción**: URL del servidor Eureka para Service Discovery

**Formato**: `http(s)://{host}:{port}/eureka/`

**Valores por ambiente**:
- **Desarrollo**: `http://localhost:8761/eureka/`
- **Staging**: `http://eureka.staging.example.com:8761/eureka/`
- **Producción**: `http://eureka.production.com:8761/eureka/`

**Uso en código**: Los servicios se registran y descubren mediante Eureka

---

### CORS_ALLOWED_ORIGINS

**Descripción**: Orígenes permitidos para requests cross-origin (separados por coma)

**Formato**: `{url1},{url2},{url3}`

**Valores por ambiente**:
- **Desarrollo**: `http://localhost:4200,http://localhost:3000,http://localhost:8080`
- **Staging**: `https://app-staging.example.com,https://admin-staging.example.com`
- **Producción**: `https://app.example.com,https://admin.example.com`

**Uso en código**: Permite que frontends en estos dominios hagan requests al backend

**⚠️ IMPORTANTE**: NUNCA usar `*` (wildcard) en producción con credentials

---

### CORS_ALLOWED_METHODS

**Descripción**: Métodos HTTP permitidos (separados por coma)

**Formato**: `{METHOD1},{METHOD2}`

**Valor por defecto**: `GET,POST,PUT,DELETE,OPTIONS,PATCH`

**Uso en código**: Controla qué métodos HTTP puede usar el frontend

---

### CORS_ALLOWED_HEADERS

**Descripción**: Headers permitidos en requests (separados por coma)

**Formato**: `{Header1},{Header2}`

**Valor por defecto**: `Authorization,Content-Type,X-Requested-With,Accept,Origin`

**Uso en código**: Controla qué headers puede enviar el frontend

---

### CORS_EXPOSED_HEADERS

**Descripción**: Headers expuestos al frontend en responses (separados por coma)

**Formato**: `{Header1},{Header2}`

**Valor por defecto**: `Authorization,X-Total-Count,X-Page-Number`

**Uso en código**: Headers que el frontend puede leer de la respuesta

---

### CORS_MAX_AGE

**Descripción**: Tiempo de caché para preflight requests (en segundos)

**Formato**: `{segundos}`

**Valor por defecto**: `3600` (1 hora)

**Uso en código**: Reduce preflight requests innecesarias

---

### CORS_ALLOW_CREDENTIALS

**Descripción**: Permitir credenciales (cookies, headers de autenticación)

**Formato**: `true` o `false`

**Valor por defecto**: `true`

**Uso en código**: Permite enviar cookies y headers de autenticación

**⚠️ IMPORTANTE**: Si es `true`, NO puedes usar `*` en CORS_ALLOWED_ORIGINS

---

## 🌍 Configuración por Ambiente

### Desarrollo (localhost)

```bash
# Keycloak & JWT
KEYCLOAK_ISSUER_URI=http://localhost:8080/realms/mi-realm
KEYCLOAK_JWK_SET_URI=http://localhost:8080/realms/mi-realm/protocol/openid-connect/certs
JWT_AUDIENCE=spring-boot-client

# Service Discovery
EUREKA_URL=http://localhost:8761/eureka/

# CORS (Angular en 4200, React en 3000)
CORS_ALLOWED_ORIGINS=http://localhost:4200,http://localhost:3000,http://localhost:8080
CORS_ALLOWED_METHODS=GET,POST,PUT,DELETE,OPTIONS,PATCH
CORS_ALLOWED_HEADERS=Authorization,Content-Type,X-Requested-With,Accept,Origin
CORS_EXPOSED_HEADERS=Authorization,X-Total-Count,X-Page-Number
CORS_MAX_AGE=3600
CORS_ALLOW_CREDENTIALS=true
```

### Staging

```bash
KEYCLOAK_ISSUER_URI=https://keycloak.staging.example.com/realms/staging-realm
KEYCLOAK_JWK_SET_URI=https://keycloak.staging.example.com/realms/staging-realm/protocol/openid-connect/certs
JWT_AUDIENCE=staging-client
EUREKA_URL=http://eureka.staging.example.com:8761/eureka/
```

### Producción

```bash
KEYCLOAK_ISSUER_URI=https://keycloak.production.com/realms/production-realm
KEYCLOAK_JWK_SET_URI=https://keycloak.production.com/realms/production-realm/protocol/openid-connect/certs
JWT_AUDIENCE=production-client
EUREKA_URL=http://eureka.production.com:8761/eureka/
```

---

## 📄 Uso con .env

### Opción 1: Modo Recomendado para Desarrollo

1. **Copiar el archivo de ejemplo**:

```bash
cp .env.example .env
```

2. **Editar .env con tus valores**:

```bash
nano .env
# o
vim .env
# o usar tu editor favorito
```

3. **Iniciar servicios con el script mejorado**:

```bash
./start-all-with-env.sh
```

El script automáticamente:
- ✅ Carga las variables del archivo `.env`
- ✅ Valida que todas las variables requeridas estén presentes
- ✅ Exporta las variables al entorno
- ✅ Inicia todos los servicios

### Ventajas del archivo .env

✅ Fácil de editar y versionar (excepto el .env real)
✅ No contamina el entorno del sistema
✅ Perfecto para desarrollo local
✅ Compatible con Docker Compose

---

## 🖥️ Uso sin .env (Variables de Sistema)

### Opción 2: Variables de Sistema

Útil para **producción**, **CI/CD**, **contenedores**.

#### En Linux/Mac:

```bash
# Exportar manualmente
export KEYCLOAK_ISSUER_URI=https://keycloak.production.com/realms/production-realm
export KEYCLOAK_JWK_SET_URI=https://keycloak.production.com/realms/production-realm/protocol/openid-connect/certs
export JWT_AUDIENCE=production-client
export EUREKA_URL=http://eureka.production.com:8761/eureka/

# Iniciar servicios
./start-all.sh
```

#### En Windows (PowerShell):

```powershell
# Exportar manualmente
$env:KEYCLOAK_ISSUER_URI="https://keycloak.production.com/realms/production-realm"
$env:KEYCLOAK_JWK_SET_URI="https://keycloak.production.com/realms/production-realm/protocol/openid-connect/certs"
$env:JWT_AUDIENCE="production-client"
$env:EUREKA_URL="http://eureka.production.com:8761/eureka/"

# Iniciar servicios
mvn spring-boot:run
```

#### En Docker/Kubernetes:

**Docker Compose**:

```yaml
services:
  user-service:
    image: user-service:latest
    environment:
      - KEYCLOAK_ISSUER_URI=https://keycloak.production.com/realms/production-realm
      - KEYCLOAK_JWK_SET_URI=https://keycloak.production.com/realms/production-realm/protocol/openid-connect/certs
      - JWT_AUDIENCE=production-client
      - EUREKA_URL=http://eureka:8761/eureka/
```

**Kubernetes ConfigMap**:

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: app-config
data:
  KEYCLOAK_ISSUER_URI: "https://keycloak.production.com/realms/production-realm"
  KEYCLOAK_JWK_SET_URI: "https://keycloak.production.com/realms/production-realm/protocol/openid-connect/certs"
  JWT_AUDIENCE: "production-client"
  EUREKA_URL: "http://eureka:8761/eureka/"
```

---

## ✅ Mejores Prácticas

### 1. Nunca Commitear el Archivo .env

❌ **NUNCA**:
```bash
git add .env
git commit -m "Agregando configuración"  # ¡PELIGRO!
```

✅ **SIEMPRE**:
```bash
# .env está en .gitignore
git add .env.example
git commit -m "Actualizar plantilla de configuración"
```

### 2. Usar Valores por Defecto para Desarrollo

En `application.yml`, los valores por defecto son para desarrollo local:

```yaml
issuer-uri: ${KEYCLOAK_ISSUER_URI:http://localhost:8080/realms/mi-realm}
#                                  ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
#                                  Valor por defecto si no hay variable
```

### 3. Validar Variables en Producción

En producción, **NO USAR valores por defecto**. Spring Boot puede fallar si faltan variables:

```yaml
# Para producción (sin fallback)
issuer-uri: ${KEYCLOAK_ISSUER_URI}
```

Si falta la variable, la aplicación no iniciará (fail-fast).

### 4. Usar Gestores de Secretos

Para **producción**, usa servicios especializados:

- **AWS**: AWS Secrets Manager, AWS Parameter Store
- **Azure**: Azure Key Vault
- **GCP**: Google Secret Manager
- **Kubernetes**: Secrets, Sealed Secrets
- **Vault**: HashiCorp Vault

### 5. Diferentes .env por Desarrollador

Cada desarrollador puede tener su propio `.env` con configuraciones personales:

```bash
# Desarrollador 1
KEYCLOAK_ISSUER_URI=http://localhost:8080/realms/mi-realm

# Desarrollador 2 (Keycloak en Docker con puerto diferente)
KEYCLOAK_ISSUER_URI=http://localhost:9080/realms/mi-realm
```

---

## 🔍 Troubleshooting

### Problema: "Variable XXX no está configurada"

**Síntoma**: El script `start-all-with-env.sh` muestra un error sobre variables faltantes.

**Solución**:

1. Verificar que el archivo `.env` existe
2. Verificar que la variable está definida en `.env`
3. Verificar que no hay espacios extra: `VAR=valor` (no `VAR = valor`)

### Problema: "Issuer mismatch"

**Síntoma**: Logs muestran `The iss claim is not valid`

**Solución**:

Verificar que `KEYCLOAK_ISSUER_URI` coincide **EXACTAMENTE** con el claim `iss` del JWT:

```bash
# Decodificar JWT en https://jwt.io/
# Verificar claim "iss": "http://localhost:8080/realms/mi-realm"

# Tu variable debe ser:
KEYCLOAK_ISSUER_URI=http://localhost:8080/realms/mi-realm
#                    ^^^^^^^^^ Sin / al final
```

### Problema: "Audience validation failed"

**Síntoma**: Logs muestran `The aud claim is not valid`

**Solución**:

1. Verificar que el token incluye el audience correcto
2. En Keycloak, configurar el Client Scope con el audience mapper
3. Verificar que `JWT_AUDIENCE` coincide con el claim `aud` del token

### Problema: "Cannot download JWK keys"

**Síntoma**: `Error downloading JWK keys from ...`

**Solución**:

1. Verificar que `KEYCLOAK_JWK_SET_URI` es accesible desde el servicio
2. Probar con curl:

```bash
curl $KEYCLOAK_JWK_SET_URI
# Debe retornar JSON con las claves públicas
```

3. Verificar firewall/red si Keycloak está en servidor remoto

### Problema: Las variables no se cargan

**Síntoma**: Servicios usan valores por defecto en vez de variables

**Solución**:

1. Verificar que usaste `./start-all-with-env.sh` (no `start-all.sh`)
2. Exportar manualmente:

```bash
export $(cat .env | grep -v '^#' | xargs)
./start-all.sh
```

3. Verificar sintaxis del `.env` (sin comillas, sin espacios extra)

---

## 📚 Referencias

- [12 Factor App - Config](https://12factor.net/config)
- [Spring Boot Externalized Configuration](https://docs.spring.io/spring-boot/reference/features/external-config.html)
- [Keycloak Documentation](https://www.keycloak.org/documentation)
- [JWT.io](https://jwt.io/) - Para decodificar y debuggear tokens

---

## 📝 Checklist de Configuración

Antes de desplegar, verifica:

- [ ] Archivo `.env` creado (desde `.env.example`)
- [ ] Todas las variables configuradas
- [ ] URLs usan HTTPS en producción
- [ ] Archivo `.env` está en `.gitignore`
- [ ] Variables validadas con el script `start-all-with-env.sh`
- [ ] Tokens JWT decodificados para verificar claims `iss` y `aud`
- [ ] JWK Set URI accesible desde los servicios

---

**Última actualización**: 23 Noviembre 2025
**Versión**: 1.0
**Autor**: Implementación de mejora crítica #1 del archivo MEJORAS.md
