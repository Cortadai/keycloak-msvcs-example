# 🔐 Configurar Audience (aud) en Keycloak

Guía paso a paso para agregar el claim "aud" al JWT de Keycloak.

---

## 📋 MÉTODO 1: Usar el Client ID como Audience (MÁS SIMPLE)

Este método usa el Client ID automáticamente como audience. Es la forma más rápida.

### **Paso 1: Ir a Client Scopes**

En tu captura de pantalla actual, ya estás en:
```
Clients > spring-boot-client > Client scopes
```

### **Paso 2: Ir a "Client scopes" en el menú lateral**

1. En el menú de la izquierda, haz clic en **"Client scopes"** (debajo de "Clients")
2. Verás una lista de scopes disponibles

### **Paso 3: Buscar o crear el scope "roles"**

1. En la lista de Client Scopes, busca **"roles"**
2. Si existe, haz clic en él
3. Si NO existe, salta al MÉTODO 2

### **Paso 4: Agregar Mapper de Audience**

1. Dentro del scope "roles", ve a la pestaña **"Mappers"**
2. Haz clic en **"Add mapper"** → **"By configuration"**
3. Selecciona **"Audience"** (no "Audience Resolve")

### **Paso 5: Configurar el Mapper**

Completa el formulario:
```
Name: audience-mapper
Mapper Type: Audience
Included Client Audience: spring-boot-client
Add to ID token: ON
Add to access token: ON
```

### **Paso 6: Guardar**

Haz clic en **"Save"**

---

## 📋 MÉTODO 2: Crear Client Scope Dedicado (RECOMENDADO)

Este método crea un scope específico para audience.

### **Paso 1: Crear Nuevo Client Scope**

1. En el menú izquierdo, haz clic en **"Client scopes"**
2. Haz clic en el botón **"Create client scope"** (arriba a la derecha)

### **Paso 2: Configurar el Client Scope**

Completa el formulario:
```
Name: audience
Description: Adds audience claim to JWT
Type: Default
Protocol: OpenID Connect
Display on consent screen: OFF
Include in token scope: ON
```

Haz clic en **"Save"**

### **Paso 3: Agregar Mapper de Audience**

1. Dentro del nuevo scope "audience", ve a la pestaña **"Mappers"**
2. Haz clic en **"Configure a new mapper"**
3. Selecciona **"Audience"** en la lista

### **Paso 4: Configurar el Mapper**

Completa el formulario:
```
Name: audience-mapper
Mapper Type: Audience
Included Client Audience: spring-boot-client
Add to ID token: ON
Add to access token: ON
```

**IMPORTANTE:** En "Included Client Audience" debes escribir exactamente: **spring-boot-client**

Haz clic en **"Save"**

### **Paso 5: Asignar el Scope al Client**

1. Ve a **Clients** en el menú izquierdo
2. Haz clic en **"spring-boot-client"**
3. Ve a la pestaña **"Client scopes"**
4. En la sección **"Setup"**, busca el scope **"audience"** que acabas de crear
5. Haz clic en **"Add client scope"**
6. Selecciona **"audience"** de la lista
7. Asegúrate de que esté marcado como **"Default"** (no "Optional")

---

## 📋 MÉTODO 3: Usar el Scope Dedicado del Client

Veo en tu captura que tienes **"spring-boot-client-dedicated"**. Puedes usar ese.

### **Paso 1: Ir al Scope Dedicado**

1. En **Clients > spring-boot-client > Client scopes**
2. Haz clic en **"spring-boot-client-dedicated"** (el primero de la lista)

### **Paso 2: Ir a Mappers**

1. Dentro de "spring-boot-client-dedicated", ve a la pestaña **"Mappers"**

### **Paso 3: Agregar Mapper de Audience**

1. Haz clic en **"Configure a new mapper"** o **"Add mapper" → "By configuration"**
2. Selecciona **"Audience"**

### **Paso 4: Configurar el Mapper**

```
Name: audience-mapper
Mapper Type: Audience
Included Client Audience: spring-boot-client
Add to ID token: ON
Add to access token: ON
```

Haz clic en **"Save"**

---

## ✅ VERIFICAR LA CONFIGURACIÓN

### **Paso 1: Obtener un Token**

```bash
curl -X POST 'http://localhost:8080/realms/mi-realm/protocol/openid-connect/token' \
  -d 'client_id=spring-boot-client' \
  -d 'client_secret=TU_CLIENT_SECRET' \
  -d 'grant_type=password' \
  -d 'username=user' \
  -d 'password=password'
```

**IMPORTANTE:** Reemplaza `TU_CLIENT_SECRET` con el secret de tu client.

Para obtener el secret:
1. Ve a **Clients > spring-boot-client > Credentials**
2. Copia el **Client secret**

### **Paso 2: Decodificar el Token**

Opción 1: Usar jwt.io
1. Ve a https://jwt.io
2. Pega el token en el campo de la izquierda
3. Busca el claim "aud" en el payload

Opción 2: Usar jq (si tienes instalado)
```bash
echo "TU_TOKEN_AQUI" | cut -d. -f2 | base64 -d | jq .
```

### **Paso 3: Verificar el Claim "aud"**

Deberías ver algo como:
```json
{
  "exp": 1234567890,
  "iat": 1234567800,
  "iss": "http://localhost:8080/realms/mi-realm",
  "aud": "spring-boot-client",  ← ESTE ES EL CLAIM QUE BUSCAMOS
  "sub": "12345678-1234-1234-1234-123456789abc",
  "preferred_username": "user",
  ...
}
```

---

## 🎯 SOLUCIÓN DE PROBLEMAS

### **Problema 1: No veo el mapper "Audience"**

**Causa:** Versión antigua de Keycloak o mapper no disponible.

**Solución:** Usa "Hardcoded claim" en su lugar:
```
Mapper Type: Hardcoded claim
Token Claim Name: aud
Claim value: spring-boot-client
Claim JSON Type: String
Add to ID token: ON
Add to access token: ON
```

### **Problema 2: El claim "aud" es un array**

Keycloak puede devolver `"aud": ["spring-boot-client", "account"]`

**Solución:** Esto está bien. Nuestro código valida:
```java
audiences.contains("spring-boot-client")
```

Funciona tanto con String como con Array.

### **Problema 3: El token sigue sin tener "aud"**

**Causas posibles:**
1. El scope no está asignado como "Default"
2. El client usa un tipo de flujo que no incluye scopes
3. Necesitas cerrar sesión y obtener un nuevo token

**Solución:**
1. Verifica que el scope esté en "Default" (no "Optional")
2. Cierra todas las sesiones en Keycloak:
   - Ve a **Sessions** en el menú lateral
   - Haz clic en **"Sign out all active sessions"**
3. Obtén un nuevo token

### **Problema 4: Error 401 después de configurar audience**

**Causa:** Tu código espera "spring-boot-client" pero el token tiene otro audience.

**Solución:** Verifica que la configuración en `application.yml` sea:
```yaml
jwt:
  audience: spring-boot-client
```

Y que el mapper en Keycloak tenga:
```
Included Client Audience: spring-boot-client
```

Deben coincidir EXACTAMENTE.

---

## 📸 CAPTURAS DE REFERENCIA

### **Configuración del Mapper**

Cuando configures el mapper "Audience", debería verse así:

```
┌─────────────────────────────────────────┐
│ Add mapper                              │
├─────────────────────────────────────────┤
│ Name: audience-mapper                   │
│ Mapper Type: Audience                   │
│                                         │
│ Included Client Audience:               │
│ spring-boot-client                      │
│                                         │
│ [x] Add to ID token                     │
│ [x] Add to access token                 │
│                                         │
│ [Cancel]  [Save]                        │
└─────────────────────────────────────────┘
```

### **Client Scope Asignado**

En la pestaña "Client scopes" del client, deberías ver:

```
Assigned client scope      | Assigned type
─────────────────────────────────────────
spring-boot-client-dedicated | None
acr                          | Default
address                      | Optional
basic                        | Default
email                        | Default
audience                     | Default    ← NUEVO
```

---

## 🧪 TESTING DESPUÉS DE CONFIGURAR

### **Test 1: Obtener y Verificar Token**

```bash
# 1. Obtener token
TOKEN=$(curl -s -X POST 'http://localhost:8080/realms/mi-realm/protocol/openid-connect/token' \
  -d 'client_id=spring-boot-client' \
  -d 'client_secret=TU_SECRET' \
  -d 'grant_type=password' \
  -d 'username=user' \
  -d 'password=password' | jq -r '.access_token')

# 2. Ver el token decodificado
echo $TOKEN | cut -d. -f2 | base64 -d | jq .

# 3. Verificar que tiene "aud": "spring-boot-client"
```

### **Test 2: Probar con tu API**

```bash
# Llamar endpoint protegido
curl -H "Authorization: Bearer $TOKEN" http://localhost:8081/api/users/me

# Si funciona → ✅ Audience configurado correctamente
# Si falla con 401 → ❌ Revisar configuración
```

### **Test 3: Verificar Logs**

Revisa los logs del servicio:
- Si ves "Invalid JWT: aud claim validation failed" → El audience no coincide
- Si ves "Usuario autenticado: user" → ✅ Todo funciona

---

## 📝 RESUMEN

**Opción más simple:** MÉTODO 3 (usar spring-boot-client-dedicated)
1. Ve a **spring-boot-client-dedicated**
2. Agrega mapper "Audience"
3. Configura "Included Client Audience: spring-boot-client"
4. Guarda
5. Obtén nuevo token
6. Verifica que tenga `"aud": "spring-boot-client"`

