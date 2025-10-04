# 🌈 Sistema de Colores AQI - Aether

## 📊 Escala AQI Estándar EPA

El Air Quality Index (AQI) es un índice estandarizado por la EPA (Environmental Protection Agency) que indica qué tan contaminado está el aire.

---

## 🎨 Tabla de Colores

| AQI | Color | Hex | Estado | Descripción |
|-----|-------|-----|--------|-------------|
| 0-50 | 🟢 Verde | #2ECC71 | Good | Aire limpio, sin riesgos |
| 51-100 | 🟡 Amarillo | #F1C40F | Moderate | Aceptable; puede afectar a personas sensibles |
| 101-150 | 🟠 Naranja | #E67E22 | Unhealthy for Sensitive Groups | Personas con asma, niños y ancianos pueden sufrir |
| 151-200 | 🔴 Rojo | #E74C3C | Unhealthy | Riesgo para toda la población |
| 201-300 | 🟣 Morado | #9B59B6 | Very Unhealthy | Riesgo serio para la salud |
| 301+ | 🟤 Marrón | #6E2C00 | Hazardous | Alerta sanitaria, muy nocivo |

---

## 💡 Implementación en Aether

### Backend

El color se calcula en `CityAirQualityDTO`:

```java
public String getAqiColor() {
    if (aqi <= 50) return "#2ECC71";    // Verde
    if (aqi <= 100) return "#F1C40F";   // Amarillo
    if (aqi <= 150) return "#E67E22";   // Naranja
    if (aqi <= 200) return "#E74C3C";   // Rojo
    if (aqi <= 300) return "#9B59B6";   // Morado
    return "#6E2C00";                    // Marrón
}
```

### Frontend

```javascript
function getAQIColorFromValue(aqi) {
    if (aqi <= 50) return '#2ECC71';
    if (aqi <= 100) return '#F1C40F';
    if (aqi <= 150) return '#E67E22';
    if (aqi <= 200) return '#E74C3C';
    if (aqi <= 300) return '#9B59B6';
    return '#6E2C00';
}
```

---

## 🗺️ Visualización en el Mapa

### Polígono de Ciudad

El polígono de la ciudad se colorea según el AQI:

```javascript
cityLayer = L.geoJSON(cityPolygon, {
    style: {
        color: '#ffffff',
        weight: 3,
        fillColor: aqiColor,  // Color según AQI
        fillOpacity: 0.35
    }
}).addTo(map);
```

### Máscara Exterior

El área fuera de la ciudad se oscurece:

```javascript
maskLayer = L.geoJSON(maskPolygon, {
    style: {
        color: '#000',
        weight: 0,
        fillColor: '#000',
        fillOpacity: 0.5
    }
}).addTo(map);
```

---

## 📍 Hotspots de Contaminación

Los puntos contaminados usan marcadores coloreados:

```javascript
function getMarkerColorFromAQI(aqi) {
    if (aqi <= 50) return 'green';
    if (aqi <= 100) return 'yellow';
    if (aqi <= 150) return 'orange';
    if (aqi <= 200) return 'red';
    if (aqi <= 300) return 'violet';
    return 'black';
}
```

Iconos de Leaflet Color Markers:
```
https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-{color}.png
```

---

## 🌡️ Contaminantes Principales

El AQI se calcula basándose en varios contaminantes:

| Contaminante | Nombre | Fuente Principal |
|--------------|--------|------------------|
| PM2.5 | Partículas finas | Tráfico, combustión |
| PM10 | Partículas gruesas | Polvo, construcción |
| O3 | Ozono | Reacciones fotoquímicas |
| NO2 | Dióxido de nitrógeno | Tráfico, industria |
| SO2 | Dióxido de azufre | Combustión de carbón |
| CO | Monóxido de carbono | Tráfico, calefacción |

**PM2.5** suele ser el contaminante dominante en ciudades españolas.

---

## 📊 Cálculo de AQI desde PM2.5

```javascript
function calculatePM25FromAQI(aqi) {
    if (aqi <= 50) return (aqi / 50) * 12.0;
    if (aqi <= 100) return 12.1 + ((aqi - 51) / 49) * (35.4 - 12.1);
    if (aqi <= 150) return 35.5 + ((aqi - 101) / 49) * (55.4 - 35.5);
    if (aqi <= 200) return 55.5 + ((aqi - 151) / 49) * (150.4 - 55.5);
    if (aqi <= 300) return 150.5 + ((aqi - 201) / 99) * (250.4 - 150.5);
    return 250.5;
}
```

---

## 🎯 Recomendaciones por Nivel

### 🟢 Good (0-50)
- ✅ Perfecto para actividad física al aire libre
- ✅ Sin restricciones para ningún grupo

### 🟡 Moderate (51-100)
- ⚠️ Personas sensibles: reducir actividad intensa
- ✅ Población general: sin restricciones

### 🟠 USG (101-150)
- ⚠️ Sensibles: evitar actividad intensa prolongada
- ⚠️ General: considerar reducir tiempo al aire libre

### 🔴 Unhealthy (151-200)
- ❌ Sensibles: evitar actividad al aire libre
- ⚠️ General: reducir actividad intensa

### 🟣 Very Unhealthy (201-300)
- ❌ Todos: evitar actividad al aire libre
- 🏠 Permanecer en interiores

### 🟤 Hazardous (301+)
- 🚨 Emergencia sanitaria
- 🏠 Permanecer en interiores con aire filtrado

---

## 🏃 Para Runners

### Mejores Condiciones (AQI < 50)
- 🌅 Madrugada: Menos tráfico
- 🌳 Parques: Mejor calidad del aire
- 💨 Días ventosos: Dispersión de contaminantes

### Evitar
- 🚗 Horas pico (7-9am, 6-8pm)
- 🏙️ Avenidas principales
- 🔥 Días de alta contaminación

### Planifica con Aether
1. Verifica AQI antes de salir
2. Elige rutas en zonas con mejor calidad
3. Ajusta horarios según contaminación
4. Usa las rutas más repetidas en zonas limpias

---

## 📱 Leyenda en la Aplicación

En `index.html` se muestra una leyenda fija:

```html
<div class="aqi-legend">
    <h3>📊 Escala típica del AQI (EPA)</h3>
    <ul class="aqi-list">
        <li>
            <span class="aqi-swatch aqi-good"></span>
            <span>Aire limpio, sin riesgos.</span>
        </li>
        <!-- ... más niveles ... -->
    </ul>
</div>
```

CSS:
```css
.aqi-good { background-color: #2ECC71; }
.aqi-moderate { background-color: #F1C40F; }
.aqi-sensitive { background-color: #E67E22; }
.aqi-unhealthy { background-color: #E74C3C; }
.aqi-very { background-color: #9B59B6; }
.aqi-hazard { background-color: #6E2C00; }
```

---

## 🔗 Referencias

- [EPA AQI Guide](https://www.airnow.gov/aqi/aqi-basics/)
- [WHO Air Quality Guidelines](https://www.who.int/news-room/fact-sheets/detail/ambient-(outdoor)-air-quality-and-health)
- [WAQI Project](https://waqi.info/)

---

**Última actualización:** 4 de Octubre de 2025

