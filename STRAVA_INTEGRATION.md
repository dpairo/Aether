# Integración con Strava OAuth

## Descripción
Esta integración permite a los usuarios de Aether autenticarse con Strava para acceder a funcionalidades personalizadas basadas en sus actividades de running.

## Configuración

### 1. Crear una aplicación en Strava

1. Ve a [https://www.strava.com/settings/api](https://www.strava.com/settings/api)
2. Crea una nueva aplicación con los siguientes datos:
   - **Application Name**: Aether
   - **Category**: Visualizer
   - **Website**: http://localhost:8080
   - **Authorization Callback Domain**: localhost

3. Una vez creada, obtendrás:
   - **Client ID**: un número de 5-6 dígitos
   - **Client Secret**: una cadena alfanumérica larga

### 2. Configurar variables de entorno

Crea un archivo `.env` en la raíz del proyecto (o configura las variables de entorno en tu sistema):

```bash
export STRAVA_CLIENT_ID="tu_client_id"
export STRAVA_CLIENT_SECRET="tu_client_secret"
export STRAVA_REDIRECT_URI="http://localhost:8080/api/v1/strava/auth/callback"
```

Para desarrollo local, el redirect URI debe ser exactamente:
```
http://localhost:8080/api/v1/strava/auth/callback
```

### 3. Ejecutar la aplicación

```bash
# En macOS/Linux
source .env
./gradlew bootRun

# En Windows (PowerShell)
$env:STRAVA_CLIENT_ID="tu_client_id"
$env:STRAVA_CLIENT_SECRET="tu_client_secret"
$env:STRAVA_REDIRECT_URI="http://localhost:8080/api/v1/strava/auth/callback"
.\gradlew.bat bootRun
```

## Flujo de autenticación

1. El usuario visita `/login.html`
2. Hace clic en "Connect with Strava"
3. Es redirigido a Strava para autorizar la aplicación
4. Strava redirige de vuelta a `/api/v1/strava/auth/callback` con un código
5. El backend intercambia el código por un access token
6. Los tokens se guardan en la base de datos H2
7. El usuario es redirigido a `/index.html` con su información

## Endpoints API

### Autenticación

#### `GET /api/v1/strava/auth/login`
Inicia el flujo OAuth. Redirige al usuario a la página de autorización de Strava.

#### `GET /api/v1/strava/auth/callback`
Callback de OAuth. Strava redirige aquí después de la autorización.

**Query Parameters:**
- `code`: Código de autorización de Strava
- `state`: Estado para protección CSRF
- `error`: Error si el usuario denegó el acceso

#### `GET /api/v1/strava/auth/me`
Obtiene información del usuario autenticado.

**Query Parameters:**
- `athleteId`: ID del atleta en Strava

**Response:**
```json
{
  "athleteId": 12345678,
  "firstName": "Juan",
  "lastName": "Pérez",
  "username": "juanp",
  "accessToken": "xxx",
  "expiresAt": 1234567890,
  "message": "Authenticated successfully"
}
```

#### `POST /api/v1/strava/auth/logout`
Cierra sesión y revoca el token.

**Query Parameters:**
- `athleteId`: ID del atleta en Strava

### Health Check

#### `GET /api/v1/strava/health`
Verifica que la integración esté operativa.

## Estructura de datos

### DTOs

- **StravaAthleteDTO**: Información del atleta
- **StravaTokenResponseDTO**: Respuesta de Strava con tokens
- **StravaAuthResponseDTO**: Respuesta de autenticación para el frontend
- **StravaErrorDTO**: Manejo de errores

### Entidad

- **StravaToken**: Almacena tokens de acceso y refresh en la base de datos
  - `athleteId`: ID único del atleta
  - `accessToken`: Token de acceso actual
  - `refreshToken`: Token para renovar acceso
  - `expiresAt`: Timestamp de expiración
  - `firstName`, `lastName`, `username`, etc.: Información del atleta

### Servicio

- **StravaAuthService**: Maneja el flujo OAuth y gestión de tokens
  - `getAuthorizationUrl()`: Genera URL de autorización
  - `exchangeCodeForToken()`: Intercambia código por tokens
  - `refreshToken()`: Renueva token expirado
  - `saveToken()`: Guarda tokens en BD
  - `getValidToken()`: Obtiene token válido (renueva si es necesario)
  - `revokeToken()`: Elimina tokens de BD

## Permisos de Strava

La aplicación solicita los siguientes scopes:
- `read`: Lectura básica de perfil
- `activity:read_all`: Lectura de todas las actividades
- `profile:read_all`: Lectura completa del perfil

## Seguridad

- Los tokens se almacenan en la base de datos H2 (en producción, usa PostgreSQL con cifrado)
- El `client_secret` debe mantenerse en secreto y nunca exponerse al frontend
- El parámetro `state` en OAuth protege contra ataques CSRF
- Los tokens se renuevan automáticamente cuando expiran

## Frontend

### `login.html`
Página de inicio de sesión con diseño moderno que muestra:
- Botón "Connect with Strava" con colores oficiales
- Manejo de errores de autenticación
- Lista de funcionalidades que obtendrá el usuario

### `main.js`
Funciones JavaScript añadidas:
- `checkStravaAuth()`: Verifica autenticación al cargar
- `fetchAthleteInfo()`: Obtiene info del atleta
- `displayAthleteInfo()`: Muestra bienvenida al usuario
- `logoutStrava()`: Cierra sesión

## Base de datos

La tabla `strava_tokens` se crea automáticamente con el esquema:

```sql
CREATE TABLE strava_tokens (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    athlete_id BIGINT NOT NULL UNIQUE,
    access_token VARCHAR(255) NOT NULL,
    refresh_token VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    token_type VARCHAR(50) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    username VARCHAR(100),
    city VARCHAR(100),
    state VARCHAR(100),
    country VARCHAR(100),
    profile_url VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

## Producción

Para producción:

1. **Cambia la base de datos** de H2 a PostgreSQL
2. **Usa HTTPS** para todas las conexiones
3. **Actualiza el redirect URI** en Strava y en `application.yml`
4. **Configura secrets** usando un servicio como AWS Secrets Manager o HashiCorp Vault
5. **Implementa rate limiting** para prevenir abuso
6. **Añade logging y monitoring** para detectar problemas

## Troubleshooting

### Error: "Invalid redirect_uri"
- Verifica que el redirect URI en tu app de Strava coincida exactamente con el configurado
- El dominio debe estar en la lista de "Authorization Callback Domain"

### Error: "Invalid client"
- Verifica que el `STRAVA_CLIENT_ID` y `STRAVA_CLIENT_SECRET` sean correctos
- Asegúrate de que las variables de entorno estén cargadas

### Los tokens expiran
- El servicio renueva automáticamente los tokens expirados
- Si falla la renovación, el usuario debe autenticarse nuevamente

### La página no redirige después del login
- Verifica que el callback endpoint esté accesible
- Revisa los logs del backend para ver errores
- Asegúrate de que el puerto 8080 esté disponible

## Referencias

- [Strava API Documentation](https://developers.strava.com/)
- [OAuth 2.0 Specification](https://oauth.net/2/)
- [Spring Boot OAuth2](https://spring.io/guides/tutorials/spring-boot-oauth2/)


