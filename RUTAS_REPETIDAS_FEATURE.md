# Feature: Rutas Más Repetidas de Strava

## Descripción

Esta funcionalidad permite visualizar automáticamente las rutas más repetidas de un usuario de Strava cuando:
1. El usuario ha autorizado su cuenta de Strava
2. El usuario ha proporcionado su ubicación

## Funcionamiento

### Backend

#### 1. Decodificador de Polylines (`PolylineUtil.java`)
- Decodifica el formato de Google Polyline usado por Strava
- Convierte las coordenadas codificadas en pares [longitud, latitud] para GeoJSON

#### 2. DTOs para GeoJSON (`RouteGeoJsonDTO.java`)
- `RouteGeoJsonDTO`: Contenedor principal del FeatureCollection
- `RouteFeature`: Cada ruta individual
- `RouteGeometry`: Geometría LineString con coordenadas
- `RouteProperties`: Metadata de la ruta (nombre, distancia, repeticiones, etc.)
- `RouteMetadata`: Información agregada sobre la respuesta

#### 3. Servicio de Actividades (`StravaActivityService.java`)

**Método `getMostRepeatedRoutes(athleteId, cityName, maxRoutes)`:**
- Obtiene todas las actividades del usuario en la ciudad especificada
- Filtra actividades con coordenadas y polyline válidos
- Agrupa rutas similares (umbral: 100m entre puntos inicio/fin)
- Ordena por número de repeticiones
- Devuelve las N rutas más repetidas

**Algoritmo de agrupación:**
- Compara puntos de inicio y fin de cada actividad
- Si ambos puntos están dentro de 100m, considera las rutas como "la misma ruta"
- Cuenta las repeticiones de cada grupo

#### 4. Endpoint REST (`StravaController.java`)

```
GET /api/v1/strava/routes/geojson?athleteId={id}&city={ciudad}&limit=3
```

**Parámetros:**
- `athleteId` (requerido): ID del atleta en Strava
- `city` (requerido): Nombre de la ciudad
- `limit` (opcional, default=3): Número máximo de rutas a devolver

**Respuesta:**
```json
{
  "type": "FeatureCollection",
  "features": [
    {
      "type": "Feature",
      "geometry": {
        "type": "LineString",
        "coordinates": [[lon, lat], [lon, lat], ...]
      },
      "properties": {
        "activityId": 123456,
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
    "athleteId": 789,
    "city": "Madrid",
    "totalRoutes": 3,
    "totalRepetitions": 15,
    "message": "Found 3 unique routes with 15 total activities"
  }
}
```

### Frontend

#### Flujo Automático (`code.js`)

La funcionalidad funciona **en ambos órdenes**:

##### Opción 1: Ubicación → Strava (orden tradicional)
1. Usuario hace clic en el botón de ubicación
2. Se detecta la ciudad y se guarda en `sessionStorage`
3. Usuario hace clic en "Connect with Strava"
4. Después del login, se redirige a: `/index.html?auth=success&athlete={id}`
5. El sistema detecta que ya hay una ciudad guardada
6. **Automáticamente** restaura el mapa y carga las rutas

##### Opción 2: Strava → Ubicación (orden inverso)
1. Usuario hace clic en "Connect with Strava"
2. Después del login, el `athleteId` se guarda en `sessionStorage`
3. Usuario hace clic en el botón de ubicación
4. Se detecta la ciudad y se guarda en `sessionStorage`
5. **Automáticamente** se cargan las rutas

#### Detalles de Implementación

**Persistencia en sessionStorage:**
- `athleteId`: ID del atleta de Strava
- `currentCity`: Nombre de la ciudad detectada
- `userLat` / `userLon`: Coordenadas del usuario
- `cityAqiColor`: Color del AQI para restaurar la visualización

**Detección al cargar la página:**
- Si el usuario viene de autenticación de Strava (`?auth=success&athlete=xxx`)
- Y ya tiene ubicación guardada en `sessionStorage`
- Entonces se restaura automáticamente el mapa y se cargan las rutas

**Verificación continua:**
- Cada vez que se obtiene la ubicación, se verifica si hay `athleteId`
- Cada vez que se autentica con Strava, se verifica si hay ubicación guardada

#### Visualización en el Mapa

- Las rutas se dibujan como polylines de colores
- Cada ruta tiene un color diferente:
  - Ruta #1 (más repetida): Rojo (#E74C3C)
  - Ruta #2: Azul (#3498DB)
  - Ruta #3: Verde (#2ECC71)
- Al hacer clic en una ruta, se muestra un popup con:
  - Nombre de la actividad
  - Tipo (Run, Ride, etc.)
  - Distancia en km
  - Tiempo en minutos
  - **Número de repeticiones** (destacado)
  - Fecha de la última actividad

## Ejemplos de Uso

### Ejemplo 1: Strava primero, luego ubicación

1. Usuario visita `/login.html`
2. Hace clic en "Connect with Strava"
3. Autoriza la aplicación en Strava
4. Es redirigido a `/index.html?auth=success&athlete=123456`
5. Hace clic en el botón de ubicación
6. La app detecta: "Madrid"
7. **Automáticamente** se llama a: `/api/v1/strava/routes/geojson?athleteId=123456&city=Madrid&limit=3`
8. Se dibujan las 3 rutas más repetidas en el mapa
9. Usuario puede hacer clic en cada ruta para ver detalles

### Ejemplo 2: Ubicación primero, luego Strava (nuevo!)

1. Usuario visita `/index.html`
2. Hace clic en el botón de ubicación
3. La app detecta: "Barcelona" (guardada en `sessionStorage`)
4. Usuario hace clic en "Find Your Route" (botón de Strava)
5. Autoriza la aplicación en Strava
6. Es redirigido a `/index.html?auth=success&athlete=789`
7. **Automáticamente** el sistema:
   - Detecta que ya hay ubicación guardada
   - Restaura el mapa con la ciudad de Barcelona
   - Llama a: `/api/v1/strava/routes/geojson?athleteId=789&city=Barcelona&limit=3`
   - Dibuja las rutas más repetidas
8. Usuario ve sus rutas sin tener que volver a dar la ubicación

## Ventajas

- ✅ **Totalmente automático**: No requiere intervención del usuario
- ✅ **Flexible**: Funciona independientemente del orden (ubicación→Strava o Strava→ubicación)
- ✅ **Persistente**: Guarda el estado en `sessionStorage` para restauración automática
- ✅ **Inteligente**: Agrupa rutas similares basándose en ubicación
- ✅ **Visual**: Muestra las rutas con colores distintivos
- ✅ **Informativo**: Indica cuántas veces ha hecho cada ruta
- ✅ **Eficiente**: Solo busca actividades en la ciudad actual
- ✅ **Formato estándar**: Usa GeoJSON, compatible con cualquier librería de mapas

## Configuración

No requiere configuración adicional. Todo funciona automáticamente si:
- Las credenciales de Strava están configuradas en `application.yml`
- El usuario ha autorizado la aplicación
- El usuario permite el acceso a su ubicación

## Limitaciones

- Solo busca en las últimas 50 actividades del usuario (configurable en `StravaActivityService`)
- Las rutas deben estar en la misma ciudad (match exacto del nombre)
- El umbral de similitud es de 100m (configurable en `areRouteSimilar()`)
- Máximo 3 rutas por defecto (configurable via parámetro `limit`)

