# 🔌 Ejemplos de Uso de la API

Esta guía contiene ejemplos prácticos de cómo usar la API de Aether, especialmente la integración con Strava.

## 🏃 Autenticación con Strava

### 1. Iniciar el Flujo OAuth

**Desde el navegador:**
```
http://localhost:8080/api/v1/strava/auth/login
```

**Desde JavaScript:**
```javascript
// Redirigir al usuario a la página de autorización
window.location.href = '/api/v1/strava/auth/login';
```

El usuario será redirigido a Strava, autorizará la app, y será devuelto a:
```
http://localhost:8080/index.html?auth=success&athlete=123456
```

### 2. Obtener Información del Usuario

**cURL:**
```bash
curl -X GET "http://localhost:8080/api/v1/strava/auth/me?athleteId=123456"
```

**JavaScript (Fetch):**
```javascript
async function getAthleteInfo(athleteId) {
    const response = await fetch(`/api/v1/strava/auth/me?athleteId=${athleteId}`);
    
    if (response.ok) {
        const data = await response.json();
        console.log('Athlete:', data);
        return data;
    } else {
        console.error('Not authenticated');
    }
}

// Uso
const athleteId = localStorage.getItem('stravaAthleteId');
getAthleteInfo(athleteId);
```

**Respuesta exitosa:**
```json
{
  "athleteId": 123456,
  "firstName": "Juan",
  "lastName": "Pérez",
  "username": "juanp_runner",
  "accessToken": "a1b2c3d4e5f6...",
  "expiresAt": 1728000000,
  "message": "Authenticated successfully"
}
```

**Respuesta de error:**
```json
{
  "error": "not_authenticated",
  "message": "User not authenticated"
}
```

### 3. Cerrar Sesión

**cURL:**
```bash
curl -X POST "http://localhost:8080/api/v1/strava/auth/logout?athleteId=123456"
```

**JavaScript:**
```javascript
async function logout() {
    const athleteId = localStorage.getItem('stravaAthleteId');
    
    const response = await fetch(`/api/v1/strava/auth/logout?athleteId=${athleteId}`, {
        method: 'POST'
    });
    
    if (response.ok) {
        localStorage.removeItem('stravaAthleteId');
        console.log('Logged out successfully');
        window.location.href = '/login.html';
    }
}
```

## 🌬️ API de Calidad del Aire

### 1. Obtener Calidad del Aire por Provincias

**cURL:**
```bash
curl -X GET "http://localhost:8080/api/v1/air/quality/provinces"
```

**JavaScript:**
```javascript
async function getProvinceAirQuality() {
    const response = await fetch('/api/v1/air/quality/provinces');
    const provinces = await response.json();
    
    provinces.forEach(province => {
        console.log(`${province.province}: AQI ${province.aqi} - ${province.aqiStatus}`);
    });
    
    return provinces;
}
```

**Respuesta:**
```json
[
  {
    "province": "Madrid",
    "aqi": 45,
    "aqiStatus": "Good",
    "dominantPollutant": "PM2.5",
    "latitude": 40.4168,
    "longitude": -3.7038,
    "lastUpdate": "2024-10-04T10:30:00Z"
  },
  {
    "province": "Barcelona",
    "aqi": 72,
    "aqiStatus": "Moderate",
    "dominantPollutant": "NO2",
    "latitude": 41.3851,
    "longitude": 2.1734,
    "lastUpdate": "2024-10-04T10:30:00Z"
  }
]
```

### 2. Obtener Calidad del Aire de una Ciudad

**cURL:**
```bash
curl -X GET "http://localhost:8080/api/v1/air/quality/city/madrid"
```

**JavaScript:**
```javascript
async function getCityAirQuality(cityName) {
    const response = await fetch(`/api/v1/air/quality/city/${cityName.toLowerCase()}`);
    const data = await response.json();
    
    console.log(`AQI en ${data.city}: ${data.aqi}`);
    console.log(`Estado: ${data.aqiStatus}`);
    console.log(`Contaminante principal: ${data.dominantPollutant}`);
    
    return data;
}

// Uso
getCityAirQuality('Madrid');
```

## 📍 API de Localización

### 1. Guardar Consentimiento de Ubicación

**cURL:**
```bash
curl -X POST "http://localhost:8080/api/v1/location/consent" \
  -H "Content-Type: application/json" \
  -d '{
    "consent": true,
    "lat": 40.4168,
    "lon": -3.7038,
    "accuracyMeters": 10.5,
    "source": "gps",
    "consentVersion": "1.0",
    "assertedAtIso": "2024-10-04T10:30:00Z"
  }'
```

**JavaScript:**
```javascript
async function saveLocationConsent(coords) {
    const request = {
        consent: true,
        lat: coords.latitude,
        lon: coords.longitude,
        accuracyMeters: coords.accuracy,
        source: 'gps',
        consentVersion: '1.0',
        assertedAtIso: new Date().toISOString()
    };
    
    const response = await fetch('/api/v1/location/consent', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(request)
    });
    
    const data = await response.json();
    console.log('Location saved:', data);
    return data;
}

// Uso con Geolocation API
navigator.geolocation.getCurrentPosition(
    (position) => {
        saveLocationConsent(position.coords);
    },
    (error) => {
        console.error('Error getting location:', error);
    }
);
```

### 2. Obtener Última Ubicación

**cURL:**
```bash
curl -X GET "http://localhost:8080/api/v1/location/latest"
```

**JavaScript:**
```javascript
async function getLatestLocation() {
    const response = await fetch('/api/v1/location/latest');
    
    if (response.ok) {
        const location = await response.json();
        console.log('Last location:', location.city, location.state, location.country);
        console.log('Coordinates:', location.latitude, location.longitude);
        return location;
    } else {
        console.log('No location consent found');
    }
}
```

## 🔄 Flujos Completos

### Flujo: Autenticación y Visualización de Calidad del Aire

```javascript
// 1. Verificar si el usuario está autenticado
async function checkAuth() {
    const athleteId = localStorage.getItem('stravaAthleteId');
    
    if (!athleteId) {
        console.log('User not authenticated');
        return null;
    }
    
    const response = await fetch(`/api/v1/strava/auth/me?athleteId=${athleteId}`);
    
    if (!response.ok) {
        localStorage.removeItem('stravaAthleteId');
        return null;
    }
    
    return await response.json();
}

// 2. Obtener ubicación del usuario
async function getUserLocation() {
    return new Promise((resolve, reject) => {
        navigator.geolocation.getCurrentPosition(
            (position) => resolve(position.coords),
            (error) => reject(error)
        );
    });
}

// 3. Guardar consentimiento de ubicación
async function saveConsent(coords) {
    const response = await fetch('/api/v1/location/consent', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            consent: true,
            lat: coords.latitude,
            lon: coords.longitude,
            accuracyMeters: coords.accuracy,
            source: 'gps',
            consentVersion: '1.0',
            assertedAtIso: new Date().toISOString()
        })
    });
    return await response.json();
}

// 4. Obtener calidad del aire
async function getAirQuality() {
    const response = await fetch('/api/v1/air/quality/provinces');
    return await response.json();
}

// 5. Flujo completo
async function initializeApp() {
    try {
        // Verificar autenticación
        const athlete = await checkAuth();
        
        if (athlete) {
            console.log(`Welcome, ${athlete.firstName}!`);
        } else {
            console.log('Please login with Strava');
        }
        
        // Obtener ubicación
        const coords = await getUserLocation();
        console.log('Location obtained:', coords);
        
        // Guardar consentimiento
        const consent = await saveConsent(coords);
        console.log('Consent saved:', consent);
        
        // Obtener y mostrar calidad del aire
        const airQuality = await getAirQuality();
        console.log('Air quality data:', airQuality);
        
        // Actualizar mapa y UI
        displayAirQualityOnMap(airQuality);
        
    } catch (error) {
        console.error('Error initializing app:', error);
    }
}

// Ejecutar al cargar la página
document.addEventListener('DOMContentLoaded', initializeApp);
```

### Flujo: Renovación Automática de Token

El backend maneja automáticamente la renovación de tokens. Desde el frontend:

```javascript
async function makeAuthenticatedRequest(athleteId) {
    // El backend verifica si el token está expirado
    // y lo renueva automáticamente si es necesario
    const response = await fetch(`/api/v1/strava/auth/me?athleteId=${athleteId}`);
    
    if (response.ok) {
        const data = await response.json();
        // El accessToken en la respuesta siempre será válido
        return data;
    } else if (response.status === 401) {
        // Token inválido o renovación falló
        console.log('Re-authentication required');
        localStorage.removeItem('stravaAthleteId');
        window.location.href = '/login.html';
    }
}
```

## 🛠️ Testing con cURL

### Script de Testing Completo

```bash
#!/bin/bash

# Variables
BASE_URL="http://localhost:8080/api/v1"
ATHLETE_ID="123456"  # Reemplazar con tu ID

echo "=== Testing Aether API ==="

# 1. Health Check
echo -e "\n1. Strava Health Check:"
curl -X GET "${BASE_URL}/strava/health"

# 2. Air Quality - Provinces
echo -e "\n\n2. Air Quality by Provinces:"
curl -X GET "${BASE_URL}/air/quality/provinces" | jq .

# 3. Air Quality - Specific City
echo -e "\n\n3. Air Quality for Madrid:"
curl -X GET "${BASE_URL}/air/quality/city/madrid" | jq .

# 4. Latest Location (si existe)
echo -e "\n\n4. Latest Location:"
curl -X GET "${BASE_URL}/location/latest" | jq .

# 5. Athlete Info (requiere autenticación previa)
echo -e "\n\n5. Athlete Info:"
curl -X GET "${BASE_URL}/strava/auth/me?athleteId=${ATHLETE_ID}" | jq .

echo -e "\n\n=== Testing Complete ==="
```

## 📝 Notas Importantes

1. **Rate Limits de Strava:**
   - 600 requests cada 15 minutos
   - 30,000 requests por día
   
2. **Tokens:**
   - Los access tokens expiran en 6 horas
   - La renovación es automática en el backend
   
3. **CORS:**
   - Si usas un frontend separado, configura CORS en Spring Boot
   
4. **Seguridad:**
   - Nunca expongas el `client_secret` al frontend
   - Usa HTTPS en producción
   - Implementa rate limiting

## 🔗 Referencias

- [Strava API Docs](https://developers.strava.com/docs/reference/)
- [WAQI API Docs](https://aqicn.org/api/)
- [Spring Boot REST](https://spring.io/guides/gs/rest-service/)


