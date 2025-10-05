# 🔍 Búsqueda de Ciudades Españolas

## Descripción

Aether ahora permite buscar ciudades españolas por nombre usando la API de Nominatim de OpenStreetMap. Cuando el usuario escribe el nombre de una ciudad, la aplicación:

1. 🗺️ **Obtiene las coordenadas** (latitud y longitud) de la ciudad
2. 🎨 **Muestra el polígono** de la ciudad pintado según su índice de calidad del aire (AQI)
3. 📍 **Ajusta el perímetro** del mapa para encuadrar la ciudad perfectamente
4. 🔴 **Genera 3 puntos nocivos** aleatorios dentro del polígono de la ciudad

## 📚 Componentes Implementados

### Backend

#### 1. `NominatimService`
**Ubicación:** `src/main/java/com/aether/app/air/NominatimService.java`

Servicio que se comunica con la API de Nominatim para buscar ciudades españolas.

**Métodos principales:**
- `searchSpanishCity(String cityName)`: Busca una ciudad española por nombre y devuelve sus coordenadas

**Características:**
- Búsqueda específica para España (`countrycodes=es`)
- Respuesta en español (`accept-language=es`)
- Extracción inteligente del nombre de la ciudad desde diferentes campos (city, town, village, municipality)
- Manejo de errores robusto

#### 2. DTOs

**`CitySearchDTO.java`**
```java
public record CitySearchDTO(
    String city,           // Nombre de la ciudad
    String latitude,       // Latitud
    String longitude,      // Longitud
    String displayName,    // Nombre completo para mostrar
    String[] boundingBox   // Caja delimitadora [sur, norte, oeste, este]
)
```

**`NominatimResponseDTO.java`**
DTO para mapear la respuesta completa de la API de Nominatim, incluyendo detalles de dirección.

#### 3. Endpoint de API

**`AirController`** - Nuevo endpoint:

```java
GET /api/v1/air/search/city?cityName={nombre}
```

**Parámetros:**
- `cityName` (String, requerido): Nombre de la ciudad española a buscar

**Respuesta:**
```json
{
  "city": "Madrid",
  "latitude": "40.4168",
  "longitude": "-3.7038",
  "displayName": "Madrid, Comunidad de Madrid, España",
  "boundingBox": ["40.31", "40.64", "-3.89", "-3.52"]
}
```

### Frontend

#### 1. Input de Búsqueda

**Ubicación:** `src/main/resources/static/index.html`

```html
<div class="search-box">
    <p>Buscar ciudad española</p>
    <input type="text" id="search" placeholder="Ej: Madrid, Barcelona, Valencia...">
    <button type="button" id="search-btn" title="Buscar ciudad">
        <i class="fa-solid fa-magnifying-glass"></i>
    </button>
</div>
```

#### 2. Funcionalidad JavaScript

**Ubicación:** `src/main/resources/static/code.js`

**Función principal:** `searchAndDisplayCity(cityName)`

**Flujo de trabajo:**

1. **Búsqueda de ciudad:**
   ```javascript
   const searchUrl = `${API_BASE}/air/search/city?cityName=${cityName}`;
   const cityData = await fetch(searchUrl).then(r => r.json());
   ```

2. **Obtención de AQI:**
   ```javascript
   const aqiData = await getCityAirQuality(cityNameFound);
   const cityAqiColor = aqiData.aqiColor; // Color basado en AQI
   ```

3. **Obtención de polígono:**
   ```javascript
   const fc = await fetchCityPolygon(lat, lon);
   await updateCityLayout(fc, cityAqiColor);
   ```

4. **Generación de puntos nocivos:**
   ```javascript
   const hotspots = generateHotspotsInCity(fc, 3);
   drawHotspotMarkers(hotspots);
   ```

5. **Obtención de rutas de Strava** (si el usuario está autenticado):
   ```javascript
   await fetchAndDrawRoutesIfAvailable(cityNameFound);
   ```

#### 3. Eventos de UI

- **Tecla Enter:** Buscar al presionar Enter en el input
- **Botón de búsqueda:** Buscar al hacer click en el botón con el ícono de lupa

#### 4. Estilos CSS

**Ubicación:** `src/main/resources/static/css/index.css`

```css
.search-box #search {
    /* Input de búsqueda con estilo glassmorphism */
}

.search-box #search-btn {
    /* Botón de búsqueda cuadrado con hover effect */
}
```

## 🎯 Uso

### Para el usuario

1. **Escribir el nombre de una ciudad española** en el campo de búsqueda
2. **Presionar Enter** o hacer **click en el botón de búsqueda** (🔍)
3. La aplicación automáticamente:
   - Navegará a la ciudad en el mapa
   - Mostrará el polígono de la ciudad con el color según su AQI
   - Generará 3 puntos nocivos aleatorios dentro de la ciudad
   - Si está autenticado con Strava, mostrará sus rutas más repetidas

### Ejemplos de búsqueda

- `Madrid`
- `Barcelona`
- `Valencia`
- `Sevilla`
- `Málaga`
- `Bilbao`
- etc.

## 🔧 Detalles Técnicos

### API de Nominatim

**Base URL:** `https://nominatim.openstreetmap.org/search`

**Parámetros utilizados:**
- `format=json`: Respuesta en formato JSON
- `q={cityName}, España`: Búsqueda con país específico
- `countrycodes=es`: Limitar resultados a España
- `limit=1`: Solo el primer resultado (más relevante)
- `addressdetails=1`: Incluir detalles de dirección
- `accept-language=es`: Respuesta en español

### Generación de Puntos Nocivos

Los puntos nocivos se generan usando **Turf.js**:

```javascript
// Generar punto aleatorio dentro del bounding box
const randomLat = minLat + Math.random() * (maxLat - minLat);
const randomLng = minLng + Math.random() * (maxLng - minLng);
const point = turf.point([randomLng, randomLat]);

// Verificar si está dentro del polígono
if (turf.booleanPointInPolygon(point, cityPolygon)) {
    // Generar datos del hotspot
    const aqi = 50 + Math.floor(Math.random() * 120);
    // ...
}
```

Los hotspots incluyen:
- **Ubicación aleatoria** dentro del polígono de la ciudad
- **AQI simulado** entre 50 y 170
- **Valor PM2.5** calculado desde el AQI
- **Estado y color** basados en el AQI
- **Nombre descriptivo** (Estación de Tráfico, Zona Industrial, etc.)

## 🎨 Colores AQI

Los polígonos y marcadores se pintan según la escala EPA:

| Rango AQI | Color | Estado |
|-----------|-------|--------|
| 0-50 | 🟢 Verde | Bueno |
| 51-100 | 🟡 Amarillo | Moderado |
| 101-150 | 🟠 Naranja | Dañino para grupos sensibles |
| 151-200 | 🔴 Rojo | Dañino |
| 201-300 | 🟣 Morado | Muy dañino |
| 301+ | 🟤 Marrón | Peligroso |

## 📝 Notas

- La API de Nominatim es **gratuita** pero tiene límites de tasa
- Se recomienda no hacer más de **1 petición por segundo**
- Los polígonos de ciudades varían en calidad según los datos de OpenStreetMap
- Los puntos nocivos son **simulados** para demostración

## 🚀 Mejoras Futuras

- [ ] Agregar autocomplete para sugerencias de ciudades
- [ ] Caché de resultados de búsqueda
- [ ] Soporte para búsqueda de ciudades de otros países
- [ ] Integrar datos reales de sensores de contaminación
- [ ] Búsqueda por código postal
- [ ] Historial de búsquedas recientes

## 🐛 Troubleshooting

**Problema:** "No se encontró la ciudad"
- Verificar que el nombre esté escrito correctamente
- Intentar con el nombre oficial de la ciudad
- Probar sin tildes (aunque debería funcionar con ellas)

**Problema:** No se muestra el polígono de la ciudad
- Algunas ciudades pequeñas pueden no tener polígonos en OpenStreetMap
- En este caso, el mapa solo hará zoom a las coordenadas

**Problema:** Los puntos nocivos no se generan
- Verificar que Turf.js esté cargado correctamente
- Revisar la consola del navegador para errores
- Asegurarse de que el polígono de la ciudad se haya cargado correctamente

## 📚 Referencias

- [Nominatim API Documentation](https://nominatim.org/release-docs/latest/api/Overview/)
- [OpenStreetMap](https://www.openstreetmap.org/)
- [Turf.js Documentation](https://turfjs.org/)
- [EPA AQI Information](https://www.airnow.gov/aqi/aqi-basics/)

