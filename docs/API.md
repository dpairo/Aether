# 🔌 API Reference - Aether

Documentación completa de los endpoints REST de Aether.

## Base URL

```
http://localhost:8080/api/v1
```

---

## 🌬️ Air Quality Endpoints

### GET /air/quality/city/{cityId}

Obtiene la calidad del aire de una ciudad específica.

**Parámetros:**
- `cityId` (path): ID de la ciudad (madrid, barcelona, valencia, etc.)

**Respuesta exitosa (200):**
```json
{
  "city": "Madrid",
  "cityId": "madrid",
  "latitude": 40.4168,
  "longitude": -3.7038,
  "aqi": 45,
  "aqiStatus": "Good",
  "aqiColor": "#2ECC71",
  "dominantPollutant": "pm25",
  "lastUpdated": "2025-10-04T20:00:00Z"
}
```

**Ejemplo:**
```bash
curl http://localhost:8080/api/v1/air/quality/city/madrid
```

### GET /air/quality/hotspots

Obtiene puntos más contaminados cerca de una ubicación.

**Parámetros:**
- `lat` (query, requerido): Latitud
- `lon` (query, requerido): Longitud  
- `radius` (query, opcional): Radio en metros (default: 500)
- `limit` (query, opcional): Número máximo de puntos (default: 3)

**Respuesta exitosa (200):**
```json
{
  "centerLat": 40.4168,
  "centerLon": -3.7038,
  "radiusMeters": 500,
  "hotspots": [
    {
      "locationName": "Estación de Tráfico #1",
      "latitude": 40.4180,
      "longitude": -3.7050,
      "pm25Value": 35.5,
      "unit": "µg/m³",
      "aqi": 120,
      "aqiStatus": "Moderate",
      "aqiColor": "#F1C40F",
      "lastUpdated": "2025-10-04T20:00:00Z"
    }
  ]
}
```

**Ejemplo:**
```bash
curl "http://localhost:8080/api/v1/air/quality/hotspots?lat=40.4168&lon=-3.7038&radius=500&limit=3"
```

---

## 🏃 Strava Endpoints

### GET /strava/health

Health check del servicio de Strava.

**Respuesta (200):**
```
Strava integration is operational
```

### GET /strava/auth/login

Inicia el flujo de autenticación OAuth con Strava.

**Comportamiento:**  
Redirige automáticamente a la página de autorización de Strava.

**Ejemplo:**
```
http://localhost:8080/api/v1/strava/auth/login
```

### GET /strava/auth/callback

Callback de OAuth de Strava (no llamar manualmente).

**Parámetros:**
- `code` (query): Código de autorización de Strava
- `state` (query): Estado CSRF
- `error` (query, opcional): Error si el usuario negó acceso

**Comportamiento:**
- Éxito: Redirige a `/index.html?auth=success&athlete={id}`
- Error: Redirige a `/login.html?error={tipo}`

### GET /strava/auth/me

Obtiene información del usuario autenticado.

**Parámetros:**
- `athleteId` (query, requerido): ID del atleta

**Respuesta exitosa (200):**
```json
{
  "athleteId": 123456,
  "firstName": "John",
  "lastName": "Doe",
  "username": "johndoe",
  "accessToken": "abc123...",
  "expiresAt": 1728072000,
  "message": "Authenticated successfully"
}
```

**Respuesta error (401):**
```json
{
  "error": "not_authenticated",
  "message": "User not authenticated"
}
```

### POST /strava/auth/logout

Cierra sesión y revoca tokens.

**Parámetros:**
- `athleteId` (query, requerido): ID del atleta

**Respuesta (200):**
```json
{
  "athleteId": 123456,
  "message": "Logged out successfully"
}
```

### GET /strava/activities/city

Obtiene actividades del usuario en una ciudad.

**Parámetros:**
- `athleteId` (query, requerido): ID del atleta
- `city` (query, requerido): Nombre de la ciudad

**Respuesta exitosa (200):**
```json
{
  "athleteId": 123456,
  "city": "Madrid",
  "state": "Madrid",
  "country": "Spain",
  "totalActivities": 15,
  "activities": [
    {
      "id": 789012,
      "name": "Morning Run",
      "type": "Run",
      "startDate": "2025-10-04T08:00:00Z",
      "distance": 5000.0,
      "movingTime": 1800,
      "startLatLng": [40.4168, -3.7038],
      "endLatLng": [40.4180, -3.7050],
      "locationCity": "Madrid"
    }
  ],
  "message": "Found 15 activities in Madrid"
}
```

### GET /strava/activities/near

Obtiene actividades cerca de una ubicación.

**Parámetros:**
- `athleteId` (query, requerido): ID del atleta
- `lat` (query, requerido): Latitud
- `lon` (query, requerido): Longitud
- `radius` (query, opcional): Radio en km (default: 10)

**Ejemplo:**
```bash
curl "http://localhost:8080/api/v1/strava/activities/near?athleteId=123456&lat=40.4168&lon=-3.7038&radius=5"
```

### GET /strava/routes/geojson

Obtiene las rutas más repetidas en formato GeoJSON.

**Parámetros:**
- `athleteId` (query, requerido): ID del atleta
- `city` (query, requerido): Nombre de la ciudad
- `limit` (query, opcional): Número de rutas (default: 3)

**Respuesta exitosa (200):**
```json
{
  "type": "FeatureCollection",
  "features": [
    {
      "type": "Feature",
      "geometry": {
        "type": "LineString",
        "coordinates": [
          [-3.7038, 40.4168],
          [-3.7050, 40.4180]
        ]
      },
      "properties": {
        "activityId": 789012,
        "name": "Morning Run",
        "type": "Run",
        "distance": 5000.0,
        "movingTime": 1800,
        "startDate": "2025-10-04T08:00:00Z",
        "repetitions": 5,
        "color": "#E74C3C",
        "location_city": "Madrid"
      }
    }
  ],
  "metadata": {
    "athleteId": 123456,
    "city": "Madrid",
    "totalRoutes": 3,
    "totalRepetitions": 15,
    "message": "Found 3 unique routes with 15 total activities"
  }
}
```

**Errores comunes:**

401 - No autenticado:
```json
{
  "error": "not_authenticated",
  "message": "User not authenticated with Strava"
}
```

400 - Parámetros faltantes:
```json
{
  "error": "bad_request",
  "message": "Required parameter 'athleteId' is missing"
}
```

---

## 🔒 Autenticación

La mayoría de endpoints de Strava requieren que el usuario esté autenticado:

1. Usuario hace login: `GET /api/v1/strava/auth/login`
2. Se guarda el `athleteId` en el frontend
3. Se usa el `athleteId` en requests subsecuentes

---

## 📊 Códigos HTTP

| Código | Significado |
|--------|-------------|
| 200 | Éxito |
| 400 | Bad Request (parámetros faltantes/inválidos) |
| 401 | Unauthorized (no autenticado) |
| 404 | Not Found (recurso no existe) |
| 500 | Internal Server Error |

---

## 🎨 Escala AQI

| AQI | Color | Hex | Estado |
|-----|-------|-----|--------|
| 0-50 | Verde | #2ECC71 | Good |
| 51-100 | Amarillo | #F1C40F | Moderate |
| 101-150 | Naranja | #E67E22 | Unhealthy for Sensitive Groups |
| 151-200 | Rojo | #E74C3C | Unhealthy |
| 201-300 | Morado | #9B59B6 | Very Unhealthy |
| 301+ | Marrón | #6E2C00 | Hazardous |

---

## 🧪 Testing con cURL

```bash
# Health check
curl http://localhost:8080/api/v1/strava/health

# Calidad del aire
curl http://localhost:8080/api/v1/air/quality/city/madrid

# Hotspots
curl "http://localhost:8080/api/v1/air/quality/hotspots?lat=40.4168&lon=-3.7038"

# Login (abre en navegador)
open http://localhost:8080/api/v1/strava/auth/login
```

---

**Última actualización:** 4 de Octubre de 2025

