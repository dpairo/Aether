# Sistema de Colores AQI

## Descripción

Se ha implementado un sistema de colores hexadecimales para los valores de AQI (Air Quality Index) que se devuelven al frontend. Este sistema incluye **degradado suave** entre rangos para evitar cambios bruscos de color.

## Rangos de Colores Base

Basado en la escala EPA utilizada en España:

| Rango AQI | Categoría | Color Base | Significado |
|-----------|-----------|------------|-------------|
| 0 - 50 | Buena | Verde (#00E400) | Aire limpio, sin riesgos |
| 51 - 100 | Moderada | Amarillo (#FFFF00) | Aceptable, puede afectar a personas sensibles |
| 101 - 150 | Dañina para sensibles | Naranja (#FF7E00) | Personas con asma, niños y ancianos pueden sufrir |
| 151 - 200 | Dañina para todos | Rojo (#FF0000) | Riesgo para toda la población |
| 201 - 300 | Muy dañina | Morado (#8F3F97) | Riesgo serio para la salud |
| 301 - 500 | Peligrosa | Marrón (#7E0023) | Alerta sanitaria, muy nocivo |

## Degradado Suave

El sistema implementa **interpolación lineal** de colores RGB entre los rangos. Esto significa que:

- Un AQI de 50 tendrá un tono más amarillento que uno de 10
- Un AQI de 51 no será amarillo puro, sino un amarillo-verdoso
- La transición entre categorías es gradual y visualmente agradable

### Ejemplos de Colores Interpolados

| AQI | Color Hexadecimal | Descripción |
|-----|-------------------|-------------|
| 10 | ~#00E400 | Verde brillante |
| 25 | ~#7FEF00 | Verde-amarillento |
| 50 | ~#E0F800 | Amarillo-verdoso |
| 75 | ~#FFDF00 | Amarillo-naranja claro |
| 100 | ~#FFBE00 | Amarillo-naranja |
| 125 | ~#FF8E00 | Naranja |
| 150 | ~#FF4700 | Naranja-rojizo |
| 175 | ~#C71F4B | Rojo-morado |
| 200 | ~#931F71 | Morado-rojizo |
| 250 | ~#891F7D | Morado |
| 300 | ~#7F1F88 | Morado-marrón |
| 400+ | #7E0023 | Marrón oscuro |

## Uso en el Frontend

Todos los DTOs que devuelven información de AQI ahora incluyen el campo `aqiColor`:

### Ejemplo de Respuesta - CityAirQualityDTO

```json
{
  "city": "Madrid",
  "cityId": "madrid",
  "latitude": 40.4168,
  "longitude": -3.7038,
  "aqi": 75,
  "aqiStatus": "Moderate",
  "aqiColor": "#FFDF00",
  "dominantPollutant": "pm25",
  "airQuality": {
    "pm25": 22.5,
    "pm10": 35.0,
    "no2": 18.0,
    "o3": 45.0,
    "co": 0.5,
    "so2": 5.0
  },
  "timestamp": "2025-10-04T12:00:00Z"
}
```

### Ejemplo de Respuesta - ProvinceAirQualityDTO

```json
{
  "province": "Madrid",
  "provinceCode": "M",
  "latitude": 40.4168,
  "longitude": -3.7038,
  "aqi": 125,
  "aqiStatus": "Unhealthy for Sensitive Groups",
  "aqiColor": "#FF8E00",
  "dominantPollutant": "pm25",
  "airQuality": { ... },
  "timestamp": "2025-10-04T12:00:00Z"
}
```

### Ejemplo de Respuesta - AirSampleDTO

```json
{
  "stationId": 1,
  "timestamp": "2025-10-04T10:00:00Z",
  "pm25": 12.3,
  "no2": 40.1,
  "o3": 35.0,
  "aqi": 52,
  "aqiColor": "#F5F300"
}
```

## Implementación Técnica

### Clase Utilitaria: `AQIColorUtil`

```java
// Obtener el color para cualquier valor de AQI
String color = AQIColorUtil.getAQIColor(75);  // Retorna "#FFDF00"
String unknownColor = AQIColorUtil.getAQIColor(null);  // Retorna "#808080" (gris)
```

### Integración Automática

El campo `aqiColor` se calcula y asigna automáticamente en:

1. **CityAirQualityDTO**: Al convertir desde WAQI response
2. **ProvinceAirQualityDTO**: Al convertir desde city data
3. **AirSampleDTO**: Al crear instancias con el constructor compacto

## Uso en el Frontend con Leaflet

```javascript
// Ejemplo de cómo usar el color en marcadores de mapa
function createAQIMarker(data) {
  const marker = L.circleMarker([data.latitude, data.longitude], {
    color: data.aqiColor,  // Usar el color del backend
    fillColor: data.aqiColor,
    fillOpacity: 0.6,
    radius: 10,
    weight: 2
  });
  
  marker.bindPopup(`
    <strong>${data.city}</strong><br>
    AQI: ${data.aqi}<br>
    Status: ${data.aqiStatus}
  `);
  
  return marker;
}
```

## Ventajas del Sistema

1. **Consistencia**: Los colores se calculan en el backend, garantizando que todos los clientes muestren los mismos colores
2. **Degradado Suave**: No hay cambios bruscos entre categorías
3. **Semántica Clara**: Cada rango de color tiene un significado bien definido
4. **Fácil Mantenimiento**: Toda la lógica de colores está centralizada en `AQIColorUtil`
5. **Escalabilidad**: Fácil de ajustar los rangos o colores en un solo lugar

## Notas

- Valores `null` o negativos se representan con color gris (#808080)
- Valores superiores a 500 se representan con marrón muy oscuro (#4C0013)
- Los colores están optimizados para visualización en mapas con Leaflet

