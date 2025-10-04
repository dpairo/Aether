# 🏃 Setup de Strava - Aether

## 📝 Descripción

Guía completa para configurar la integración OAuth con Strava.

## ⏱️ Setup Rápido (5 minutos)

### Paso 1: Crear App en Strava

1. Ve a https://www.strava.com/settings/api
2. Click en "Create App" o "My API Application"
3. Rellena el formulario:

```
Application Name: Aether Local
Category: Visualizer  
Club: (dejar vacío)
Website: http://localhost:8080
Authorization Callback Domain: localhost
```

**⚠️ IMPORTANTE:**
- "Authorization Callback Domain" debe ser SOLO `localhost`
- **NO** incluir `http://`, `https://`, ni puerto
- **NO** `http://localhost` ni `localhost:8080`
- Solo: `localhost`

4. Click "Create"
5. **Copia tu Client ID y Client Secret**

---

### Paso 2: Configurar Credenciales

Tienes 3 opciones:

#### Opción A: Variables de Entorno (Recomendado)

```bash
export STRAVA_CLIENT_ID="123456"
export STRAVA_CLIENT_SECRET="abc123def456..."
export STRAVA_REDIRECT_URI="http://localhost:8080/api/v1/strava/auth/callback"
```

#### Opción B: Archivo .env

Crea `.env` en la raíz del proyecto:

```bash
STRAVA_CLIENT_ID=123456
STRAVA_CLIENT_SECRET=abc123def456...
STRAVA_REDIRECT_URI=http://localhost:8080/api/v1/strava/auth/callback
```

Luego cárgalo:
```bash
source .env
./gradlew bootRun
```

#### Opción C: Actualizar application.yml

Edita `src/main/resources/application.yml`:

```yaml
strava:
  client-id: "123456"
  client-secret: "abc123def456..."
  redirect-uri: "http://localhost:8080/api/v1/strava/auth/callback"
```

**⚠️ NOTA:** Si usas esta opción, NO hagas commit de las credenciales.

---

### Paso 3: Ejecutar Aplicación

```bash
./gradlew bootRun
```

Espera a ver:
```
Started AetherApplication in X.XXX seconds
```

---

### Paso 4: Probar

1. Abre http://localhost:8080/login.html
2. Click "Connect with Strava"
3. Autoriza la aplicación
4. Deberías volver a `/index.html?auth=success&athlete=123456`

¡Listo! 🎉

---

## 🔐 Flujo OAuth Completo

```
1. Usuario → /login.html
   ↓
2. Click "Connect with Strava"
   ↓
3. Frontend → GET /api/v1/strava/auth/login
   ↓
4. Backend genera URL con:
   - client_id
   - redirect_uri
   - scope (read, activity:read_all, profile:read_all)
   - state (protección CSRF)
   ↓
5. Redirige → Strava Authorization Page
   ↓
6. Usuario autoriza/deniega
   ↓
7. Strava redirige → GET /api/v1/strava/auth/callback
   ?code=abc123
   &state=xyz789
   ↓
8. Backend:
   - Valida state
   - Intercambia code por tokens (POST a Strava)
   - Guarda tokens en BD (H2)
   - Redirige → /index.html?auth=success&athlete=123456
   ↓
9. Frontend:
   - Detecta auth=success
   - Guarda athleteId en sessionStorage
   - Usuario autenticado ✅
```

---

## 🔑 Scopes de Strava

La app solicita estos permisos:

| Scope | Descripción |
|-------|-------------|
| `read` | Leer perfil básico del atleta |
| `activity:read_all` | Leer todas las actividades (públicas y privadas) |
| `profile:read_all` | Leer perfil completo |

**No se solicita** `activity:write` ni otros permisos de escritura.

---

## 📊 Tokens

### Access Token
- **Duración:** 6 horas
- **Uso:** Hacer requests a la API de Strava
- **Renovación:** Automática usando refresh token

### Refresh Token
- **Duración:** Permanente (hasta que usuario revoque)
- **Uso:** Obtener nuevos access tokens

### Almacenamiento

Tabla `strava_tokens` en H2:

```sql
CREATE TABLE strava_tokens (
  id BIGINT PRIMARY KEY,
  athlete_id BIGINT UNIQUE,
  access_token VARCHAR(255),
  refresh_token VARCHAR(255),
  token_type VARCHAR(255),
  expires_at TIMESTAMP,
  first_name VARCHAR(255),
  last_name VARCHAR(255),
  username VARCHAR(255),
  city VARCHAR(255),
  state VARCHAR(255),
  country VARCHAR(255),
  profile_url VARCHAR(255),
  created_at TIMESTAMP,
  updated_at TIMESTAMP
);
```

---

## 🔒 Seguridad

### Protección CSRF

Se usa el parámetro `state`:
1. Backend genera UUID aleatorio
2. Se incluye en URL de autorización
3. Strava lo devuelve en callback
4. Backend verifica que coincida

### Client Secret

**NUNCA** exponer en frontend:
- ✅ Se usa solo en backend
- ✅ No se envía al navegador
- ✅ Protegido con .gitignore
- ❌ No hardcodear en código

### Tokens

- ✅ Almacenados en BD (no en cookies/localStorage)
- ✅ Renovación automática
- ✅ Método `isExpired()` para verificar
- ✅ Se pueden revocar

---

## 🧪 Testing

### Verificar Configuración

```bash
# Health check
curl http://localhost:8080/api/v1/strava/health

# Verificar redirect
curl -I http://localhost:8080/api/v1/strava/auth/login
# Debería redirigir a Strava
```

### Script de Test Completo

```bash
./test_strava_flow.sh
```

---

## ⚠️ Troubleshooting

### "Invalid redirect_uri"

**Problema:** La URL de callback no coincide

**Solución:**
1. Ve a https://www.strava.com/settings/api
2. Verifica "Authorization Callback Domain": `localhost`
3. Reinicia servidor
4. Intenta de nuevo

### "Código ya usado" (Error 400)

**Problema:** Los códigos de Strava son de un solo uso

**Solución:**
- Cierra TODAS las pestañas
- Vuelve a /login.html
- Click "Connect with Strava" (nuevo código)
- NO recargues después del callback

### "Unauthorized" (401)

**Problema:** Token expirado o inválido

**Solución:**
```bash
# Reautenticar
open http://localhost:8080/login.html
```

### Variables de Entorno No Cargadas

**Problema:** `${STRAVA_CLIENT_ID}` aparece literal

**Solución:**
```bash
# Asegúrate de cargar ANTES de ejecutar
source .env
./gradlew bootRun
```

---

## 🌐 Producción

Para desplegar en producción:

### 1. Crear App de Producción en Strava

```
Application Name: Aether
Website: https://tudominio.com
Authorization Callback Domain: tudominio.com
```

### 2. Actualizar Configuración

```yaml
strava:
  redirect-uri: "https://tudominio.com/api/v1/strava/auth/callback"
```

### 3. Variables de Entorno Seguras

```bash
# NO usar .env en producción
# Usar variables de entorno del sistema o secretos

export STRAVA_CLIENT_ID="..."
export STRAVA_CLIENT_SECRET="..."
```

### 4. HTTPS Obligatorio

Strava **requiere HTTPS** en producción:
- Configurar SSL/TLS
- Certificado válido
- Redirect HTTP → HTTPS

### 5. Rate Limits

Strava tiene límites:
- **600** requests per 15 minutos
- **30,000** requests per día

Implementar cache y rate limiting.

---

## 📚 Recursos

- [Strava API Docs](https://developers.strava.com/docs/)
- [OAuth 2.0 Spec](https://oauth.net/2/)
- [Strava API Settings](https://www.strava.com/settings/api)

---

## ✅ Checklist

- [ ] App creada en Strava
- [ ] Client ID y Secret copiados
- [ ] Callback domain = `localhost`
- [ ] Variables de entorno configuradas
- [ ] Servidor corriendo
- [ ] Health check OK
- [ ] Login funciona
- [ ] Callback redirige correctamente
- [ ] sessionStorage tiene athleteId

---

**Última actualización:** 4 de Octubre de 2025

