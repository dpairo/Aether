# Cambios en el Frontend - Integración Sistema de Colores AQI

## 🎯 Resumen de Cambios Implementados

Se ha integrado completamente el sistema de colores AQI del backend en el frontend, con mejoras significativas en la visualización del mapa y la experiencia de usuario.

---

## 📐 1. Diseño y Layout (CSS)

### **Leyenda Ampliada**
- **Ubicación**: Lado izquierdo del mapa (como se muestra en la imagen de referencia)
- **Tamaño**: 350px de ancho fijo
- **Diseño mejorado**:
  - Título descriptivo agregado
  - Swatches de color más grandes (24x24px)
  - Mejor espaciado entre elementos
  - Sombra y backdrop blur para mejor legibilidad
  - Colores actualizados para coincidir con el sistema EPA

### **Mapa Expandido**
- **Tamaño**: Ocupa todo el espacio restante (`flex: 1`)
- **Altura**: 100% del contenedor disponible
- **Layout**: Flex container que usa todo el espacio vertical menos el header

### Cambios específicos en `index.css`:
```css
.information {
    display: flex;
    align-items: stretch;
    gap: 20px;
    padding: 20px;
    height: calc(100vh - 90px);  /* Altura completa menos header */
    width: 100%;
}

#map {
    flex: 1;                 /* Toma todo el espacio restante */
    height: 100%;
    width: 100%;
}

.aqi-legend {
    width: 350px;
    min-width: 350px;
    height: fit-content;
    /* Colores actualizados a sistema EPA */
    --aqi-good: #00E400;
    --aqi-moderate: #FFFF00;
    --aqi-sensitive: #FF7E00;
    --aqi-unhealthy: #FF0000;
    --aqi-very: #8F3F97;
    --aqi-hazard: #7E0023;
}
```

---

## 🗺️ 2. Funcionalidad del Mapa (JavaScript)

### **Vista Inicial - España Completa**

Al cargar la página:
1. ✅ Mapa centrado en España: `[40.4168, -3.7038]`
2. ✅ Zoom nivel 6: Muestra toda España
3. ✅ Carga automática de provincias desde `/api/v1/air/quality/provinces`
4. ✅ Marcadores de provincias con colores del backend (`aqiColor`)

**Características de los marcadores de provincias:**
- Radio: 12px
- Color de relleno: `province.aqiColor` (del backend)
- Borde blanco de 2px
- Opacidad: 85%
- Popup con información detallada del AQI

### **Vista con Localización del Usuario**

Cuando el usuario da su ubicación:
1. ✅ Encuentra la provincia más cercana automáticamente
2. ✅ Zoom a la ubicación del usuario (nivel 11) - muestra ciudad y alrededores
3. ✅ Carga ciudades de la provincia desde `/api/v1/air/quality/province/{code}/cities`
4. ✅ Muestra ciudades con colores del backend
5. ✅ Reduce la opacidad de las provincias para dar énfasis a las ciudades
6. ✅ Marcador especial azul para la ubicación del usuario
7. ✅ Círculo de precisión mostrando el margen de error del GPS

**Características de los marcadores de ciudades:**
- Radio: 8px (más pequeños que provincias)
- Color de relleno: `city.aqiColor` (del backend)
- Borde blanco de 2px
- Opacidad: 90%
- Se superponen sobre las provincias

### **Código JavaScript - Nuevas Funciones**

```javascript
// Constantes para España
const SPAIN_CENTER = [40.4168, -3.7038];
const SPAIN_ZOOM = 6;

// Variables para gestionar marcadores
let provinceMarkers = [];
let cityMarkers = [];
let userLocation = null;

// Cargar provincias (llamada automática al inicio)
async function loadAirQualityData() {
    const response = await fetch('/api/v1/air/quality/provinces');
    airQualityData = await response.json();
    displayProvincesOnMap();
}

// Mostrar provincias con colores del backend
function displayProvincesOnMap() {
    airQualityData.forEach(province => {
        const color = province.aqiColor || '#808080';  // ← USO DEL COLOR DEL BACKEND
        
        const marker = L.circleMarker([province.latitude, province.longitude], {
            radius: 12,
            fillColor: color,  // ← AQUÍ SE USA
            color: '#fff',
            weight: 2,
            fillOpacity: 0.85
        }).addTo(map);
        
        provinceMarkers.push(marker);
    });
}

// Cargar ciudades de una provincia
async function loadCitiesForProvince(provinceCode) {
    const response = await fetch(`/api/v1/air/quality/province/${provinceCode}/cities`);
    return await response.json();
}

// Mostrar ciudades con colores del backend
function displayCitiesOnMap(cities) {
    cities.forEach(city => {
        const color = city.aqiColor || '#808080';  // ← USO DEL COLOR DEL BACKEND
        
        const marker = L.circleMarker([city.latitude, city.longitude], {
            radius: 8,
            fillColor: color,  // ← AQUÍ SE USA
            color: '#fff',
            weight: 2,
            fillOpacity: 0.9
        }).addTo(map);
        
        cityMarkers.push(marker);
    });
}

// Encontrar provincia más cercana al usuario
function findNearestProvince(lat, lon) {
    let nearest = null;
    let minDistance = Infinity;
    
    airQualityData.forEach(province => {
        const distance = Math.sqrt(
            Math.pow(province.latitude - lat, 2) + 
            Math.pow(province.longitude - lon, 2)
        );
        
        if (distance < minDistance) {
            minDistance = distance;
            nearest = province;
        }
    });
    
    return nearest;
}
```

---

## 🎨 3. Uso de Colores del Backend

### **Eliminado: Función Local de Colores**
❌ Se eliminó la función `getAQIColor()` que calculaba colores en el frontend.

### **Implementado: Uso de `aqiColor` del Backend**
✅ Todos los marcadores ahora usan directamente el campo `aqiColor` de las respuestas API:

```javascript
// ANTES (calculado localmente)
const color = getAQIColor(aqi);

// AHORA (del backend)
const color = province.aqiColor || '#808080';
```

### **Ventajas**
1. **Consistencia total**: Los mismos colores en toda la aplicación
2. **Degradado suave**: El backend ya calcula la interpolación
3. **Mantenibilidad**: Un solo lugar para cambiar colores (backend)
4. **Menos código**: No duplicar lógica de colores

---

## 🔄 4. Flujo de Usuario

### **Escenario 1: Primera Carga**
```
1. Usuario abre http://localhost:8080
2. Mapa se centra en España (zoom 6)
3. Se cargan automáticamente las provincias
4. Se pintan marcadores con colores del backend
5. Usuario ve toda España con provincias coloreadas según AQI
```

### **Escenario 2: Usuario Da Ubicación**
```
1. Usuario hace clic en "Explore my location"
2. Usuario permite acceso a ubicación
3. Sistema encuentra provincia más cercana
4. Mapa hace zoom a la ubicación del usuario (zoom 11)
5. Se cargan ciudades de esa provincia
6. Se pintan ciudades con colores del backend
7. Provincias quedan en segundo plano (opacidad reducida)
8. Usuario ve su ciudad y ciudades cercanas con AQI
```

### **Escenario 3: Volver a Vista de Provincias**
```
1. Usuario hace clic en "Show Local Pollution"
2. Se eliminan marcadores de ciudades
3. Se restaura opacidad de provincias
4. Mapa vuelve a zoom de España
5. Usuario ve de nuevo todas las provincias
```

---

## 📊 5. Popups Mejorados

### **Popup de Provincia**
```html
<div style="text-align: center; min-width: 180px;">
    <h3 style="margin: 0 0 10px 0;">Madrid</h3>
    <div style="background: #FF7E00; padding: 8px; border-radius: 4px;">
        <strong style="color: #fff; font-size: 20px;">125</strong>
    </div>
    <p><strong>Estado:</strong> Unhealthy for Sensitive Groups</p>
    <p><strong>Contaminante:</strong> pm25</p>
</div>
```

### **Popup de Ciudad**
```html
<div style="text-align: center; min-width: 160px;">
    <h4>Valencia</h4>
    <div style="background: #FFFF00; padding: 6px; border-radius: 4px;">
        <strong style="color: #fff; font-size: 18px;">75</strong>
    </div>
    <p><strong>Estado:</strong> Moderate</p>
    <p>pm25</p>
</div>
```

---

## 🎯 6. Endpoints Utilizados

### **Provincias** (Vista Inicial)
```
GET /api/v1/air/quality/provinces

Respuesta incluye:
- aqi: número
- aqiColor: hexadecimal (ej: "#FF7E00")
- aqiStatus: string
- province: nombre
- provinceCode: código
- latitude, longitude
- dominantPollutant
- airQuality: objeto con contaminantes
- timestamp
```

### **Ciudades de Provincia** (Vista con Ubicación)
```
GET /api/v1/air/quality/province/{provinceCode}/cities

Respuesta incluye (array):
- aqi: número
- aqiColor: hexadecimal (ej: "#FFFF00")
- aqiStatus: string
- city: nombre
- cityId: identificador
- latitude, longitude
- dominantPollutant
- airQuality: objeto
- timestamp
```

---

## 🚀 7. Cómo Probar

1. **Abrir la aplicación**: `http://localhost:8080`
   - ✅ Deberías ver España completa con provincias coloreadas
   - ✅ La leyenda debería estar a la izquierda
   - ✅ El mapa debería ocupar el resto del espacio

2. **Hacer clic en marcador de provincia**
   - ✅ Debería aparecer popup con AQI y color de fondo

3. **Hacer clic en "Explore my location"**
   - ✅ Permitir acceso a ubicación
   - ✅ Mapa hace zoom a tu ciudad
   - ✅ Se muestran ciudades cercanas con colores AQI
   - ✅ Aparece marcador azul en tu ubicación

4. **Hacer clic en "Show Local Pollution"**
   - ✅ Vuelve a mostrar vista de provincias
   - ✅ Zoom vuelve a España completa

---

## 📦 Archivos Modificados

### Frontend
- ✅ `src/main/resources/static/index.html` - Título de leyenda
- ✅ `src/main/resources/static/css/index.css` - Layout y estilos
- ✅ `src/main/resources/static/main.js` - Lógica completa del mapa

### Backend (ya implementado anteriormente)
- ✅ `AQIColorUtil.java` - Cálculo de colores
- ✅ `CityAirQualityDTO.java` - Campo aqiColor
- ✅ `ProvinceAirQualityDTO.java` - Campo aqiColor
- ✅ `AirSampleDTO.java` - Campo aqiColor

---

## 🎨 Sistema de Colores Integrado

### Del Backend al Frontend
```
Backend (Java)
    ↓
AQIColorUtil.getAQIColor(aqi)
    ↓
Interpolación RGB (degradado suave)
    ↓
"#FF7E00" (hexadecimal)
    ↓
JSON Response { aqiColor: "#FF7E00" }
    ↓
Frontend (JavaScript)
    ↓
const color = province.aqiColor;
    ↓
Leaflet CircleMarker { fillColor: color }
    ↓
🎨 Marcador en el mapa
```

---

## ✨ Características Especiales

### 1. **Degradado Suave Visible**
Los usuarios pueden ver el degradado suave al hacer zoom entre provincias cercanas con AQIs similares.

### 2. **Transición Suave entre Vistas**
Al cambiar de provincias a ciudades y viceversa, hay una transición visual suave.

### 3. **Gestión Inteligente de Marcadores**
- Provincias permanecen en el mapa pero con menos prominencia cuando se muestran ciudades
- Ciudades tienen prioridad visual cuando el usuario da su ubicación

### 4. **Responsive y Performante**
- Canvas rendering para mejor rendimiento con muchos marcadores
- Layout flex que se adapta a diferentes tamaños de pantalla

---

## 🎯 Resultado Final

✅ **Al entrar a la web**: Mapa de España con provincias coloreadas según AQI del backend
✅ **Leyenda**: Lado izquierdo, ampliada, con título descriptivo
✅ **Mapa**: Ocupa todo el espacio restante
✅ **Con ubicación**: Zoom a ciudad, muestra ciudades cercanas con colores del backend
✅ **Sistema de colores**: 100% integrado con el backend (degradado suave incluido)
✅ **Experiencia fluida**: Transiciones suaves y navegación intuitiva

---

## 🔍 Logs para Debugging

El sistema incluye logs detallados en la consola:

```javascript
🗺️ Mapa inicializado. Vista: España completa
🌍 Cargando datos de AQI de provincias...
✅ Datos cargados: 15 provincias
🗺️ Pintando provincias en el mapa...
✅ 15 provincias pintadas en el mapa
📍 Ubicación obtenida: 39.4699 lat, -0.3763 lon ± 20 m
📍 Provincia más cercana: Valencia (V)
🏙️ Cargando ciudades de provincia V...
✅ 3 ciudades cargadas
🗺️ Pintando 3 ciudades en el mapa...
✅ 3 ciudades pintadas
✅ Ubicación del usuario mostrada en el mapa
```

Estos logs facilitan el debugging y permiten entender el flujo de la aplicación.

