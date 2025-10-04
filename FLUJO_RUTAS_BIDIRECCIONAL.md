# Flujo Bidireccional de Rutas de Strava

## Resumen

La funcionalidad ahora funciona **en ambos órdenes**, sin importar si el usuario primero da su ubicación o primero se autentica con Strava.

## Arquitectura de Persistencia

```
sessionStorage (navegador)
├── athleteId        → ID del atleta de Strava
├── currentCity      → Nombre de la ciudad detectada
├── userLat          → Latitud del usuario
├── userLon          → Longitud del usuario
└── cityAqiColor     → Color AQI para restaurar visualización
```

## Flujo 1: Strava → Ubicación ✅

```
┌─────────────────────────────────────────────────────────────────┐
│ PASO 1: Usuario hace clic en "Find Your Route"                 │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ PASO 2: Redirección a Strava OAuth                             │
│ → Usuario autoriza la aplicación                               │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ PASO 3: Callback de Strava                                     │
│ → /index.html?auth=success&athlete=123456                      │
│ → athleteId guardado en sessionStorage                         │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ PASO 4: Usuario hace clic en botón de ubicación               │
│ → Geolocalización del navegador                                │
│ → Reverse geocoding con Nominatim                              │
│ → Ciudad, lat, lon guardados en sessionStorage                 │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ PASO 5: Detección automática                                   │
│ → ✅ Hay athleteId en sessionStorage                           │
│ → ✅ Hay currentCity en sessionStorage                         │
│ → ✅ TRIGGER: fetchAndDrawRoutesIfAvailable(city)             │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ RESULTADO: Rutas dibujadas en el mapa                         │
└─────────────────────────────────────────────────────────────────┘
```

## Flujo 2: Ubicación → Strava ✅ (NUEVO!)

```
┌─────────────────────────────────────────────────────────────────┐
│ PASO 1: Usuario hace clic en botón de ubicación               │
│ → Geolocalización del navegador                                │
│ → Reverse geocoding con Nominatim                              │
│ → Ciudad, lat, lon guardados en sessionStorage                 │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ PASO 2: Usuario hace clic en "Find Your Route"                │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ PASO 3: Redirección a Strava OAuth                             │
│ → Usuario autoriza la aplicación                               │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ PASO 4: Callback de Strava                                     │
│ → /index.html?auth=success&athlete=123456                      │
│ → athleteId guardado en sessionStorage                         │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ PASO 5: DOMContentLoaded - Detección al cargar página         │
│ → ✅ Detecta ?auth=success&athlete=123456 en URL              │
│ → ✅ Lee currentCity, userLat, userLon de sessionStorage      │
│ → ✅ TRIGGER automático:                                       │
│   1. Restaura mapa con fetchCityPolygon(lat, lon)             │
│   2. Añade marcador de usuario                                 │
│   3. Llama a fetchAndDrawRoutesIfAvailable(city)              │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ RESULTADO: Rutas dibujadas automáticamente                     │
│ (sin necesidad de volver a dar ubicación)                      │
└─────────────────────────────────────────────────────────────────┘
```

## Código Clave

### 1. Guardar ubicación (cuando usuario da permiso)

```javascript
// En el botón de ubicación:
sessionStorage.setItem('currentCity', cityName);
sessionStorage.setItem('userLat', Ulat.toString());
sessionStorage.setItem('userLon', Ulon.toString());
sessionStorage.setItem('cityAqiColor', cityAqiColor);
```

### 2. Detectar autenticación y restaurar (al cargar página)

```javascript
document.addEventListener('DOMContentLoaded', () => {
    const urlParams = new URLSearchParams(window.location.search);
    const authSuccess = urlParams.get('auth');
    const athleteParam = urlParams.get('athlete');
    
    if (authSuccess === 'success' && athleteParam) {
        sessionStorage.setItem('athleteId', athleteParam);
        
        // Verificar si ya hay ubicación guardada
        const savedCity = sessionStorage.getItem('currentCity');
        const savedLat = sessionStorage.getItem('userLat');
        const savedLon = sessionStorage.getItem('userLon');
        
        if (savedCity && savedLat && savedLon) {
            // ✅ ¡Restaurar automáticamente!
            restaurarMapaYCargarRutas(savedCity, savedLat, savedLon);
        }
    }
});
```

### 3. Verificar siempre al obtener ubicación

```javascript
// Después de obtener la ubicación:
await fetchAndDrawRoutesIfAvailable(cityName);

// Función que verifica si hay athleteId
async function fetchAndDrawRoutesIfAvailable(city) {
    const athleteId = getAthleteIdFromURL();
    
    if (!athleteId || !city) {
        console.log('Falta athleteId o ciudad');
        return;
    }
    
    // ✅ Ambos disponibles, cargar rutas
    const geoJson = await fetchRepeatedRoutes(athleteId, city);
    drawRoutesOnMap(geoJson);
}
```

## Ventajas del Enfoque Bidireccional

| Característica | Descripción |
|----------------|-------------|
| **Flexibilidad** | El usuario puede hacer las cosas en el orden que prefiera |
| **UX mejorada** | No hay que repetir acciones |
| **Persistencia** | Los datos se mantienen durante la sesión |
| **Restauración automática** | El mapa y las rutas se cargan automáticamente |
| **Sin fricción** | Todo sucede en segundo plano |

## Casos de Uso

### Caso 1: Usuario deportista habitual
1. Primera visita: Strava → Ubicación
2. **Rutas cargadas** ✅
3. Cierra y vuelve a abrir la app (misma sesión)
4. Todo se restaura automáticamente desde `sessionStorage`

### Caso 2: Usuario nuevo explorando
1. Primera visita: Da ubicación para ver calidad del aire
2. Ve los hotspots contaminados
3. Luego decide conectar Strava
4. **Rutas cargadas automáticamente** ✅
5. No necesita volver a dar ubicación

### Caso 3: Usuario móvil
1. Llega a la app desde el móvil
2. Autoriza Strava primero (más fácil en móvil)
3. Luego da permiso de ubicación
4. **Rutas cargadas** ✅

## Limpieza de Estado

- Los datos en `sessionStorage` persisten durante la sesión del navegador
- Se limpian automáticamente al cerrar todas las pestañas del sitio
- Para limpiar manualmente: `sessionStorage.clear()`
- Los parámetros de la URL (?auth=success&athlete=xxx) se limpian después de procesarlos

## Testing Manual

### Test 1: Orden Strava → Ubicación
```bash
1. Abrir http://localhost:8080/login.html
2. Hacer clic en "Connect with Strava"
3. Autorizar
4. En index.html, hacer clic en botón de ubicación
5. ✅ Verificar que aparecen las rutas
```

### Test 2: Orden Ubicación → Strava
```bash
1. Abrir http://localhost:8080/index.html
2. Hacer clic en botón de ubicación
3. Aceptar permiso de ubicación
4. Hacer clic en "Find Your Route"
5. Autorizar en Strava
6. ✅ Verificar que aparecen las rutas automáticamente al volver
```

### Test 3: Persistencia en sesión
```bash
1. Completar Test 1 o Test 2
2. Recargar la página (F5)
3. ✅ Verificar que los datos persisten en sessionStorage
4. ✅ Verificar que el mapa se mantiene (pero rutas se limpian en recarga)
```

## Notas Técnicas

- **sessionStorage vs localStorage**: Se usa `sessionStorage` para que los datos se limpien automáticamente al cerrar el navegador
- **Limpieza de URL**: Los parámetros `?auth=success&athlete=xxx` se eliminan de la URL después de procesarlos para mantener la URL limpia
- **Idempotencia**: Las funciones están diseñadas para poder llamarse múltiples veces sin efectos secundarios
- **Manejo de errores**: Si falta algún dato, se registra en consola pero no se bloquea la app

