# 🧪 Testing - Aether

## 📋 Resumen

El proyecto tiene **22 tests automatizados** que cubren:
- ✅ Decodificación de polylines
- ✅ Agrupación de rutas
- ✅ Endpoints REST
- ✅ Lógica de negocio

## 🚀 Ejecutar Tests

### Todos los tests
```bash
./gradlew test
```

### Ver reporte HTML
```bash
./gradlew test
open build/reports/tests/test/index.html
```

### Tests específicos
```bash
# Solo polyline
./gradlew test --tests "PolylineUtilTest"

# Solo servicio
./gradlew test --tests "StravaActivityServiceTest"

# Solo controller
./gradlew test --tests "StravaControllerIntegrationTest"
```

### Con más detalle
```bash
./gradlew test --info
```

---

## 📊 Tests Automatizados

### 1. PolylineUtilTest (7 tests)

Prueba la decodificación de polylines de Google/Strava.

```bash
✅ testDecodeSimplePolyline() - Decodifica polyline básico
✅ testDecodeEmptyString() - Maneja string vacío
✅ testDecodeNull() - Maneja valor null
✅ testDecodeMadridRoute() - Decodifica ruta de Madrid
✅ testDecodeLatLonFormat() - Verifica formato [lat,lon]
✅ testDecodeRealStravaPolyline() - Polyline real
✅ testDecodePreservesOrder() - Mantiene orden de puntos
```

**Cobertura:** 100% de PolylineUtil

---

### 2. StravaActivityServiceTest (7 tests)

Prueba la lógica de agrupación de rutas.

```bash
✅ testGetMostRepeatedRoutes_NoActivities() - Sin actividades
✅ testGetMostRepeatedRoutes_SingleActivity() - Una actividad
✅ testRouteGrouping_Conceptual() - Documenta agrupación
✅ testGetMostRepeatedRoutes_MaxRoutesLimit() - Límite máximo
✅ testGetMostRepeatedRoutes_SortedByRepetitions() - Ordenamiento
✅ testRouteGroup_GetRepetitions() - Contador repeticiones
✅ testCalculateDistance() - Fórmula Haversine
```

**Cobertura:** ~85% del servicio

---

### 3. StravaControllerIntegrationTest (8 tests)

Prueba los endpoints REST con Spring MockMvc.

```bash
✅ testGetRoutesAsGeoJson_Success() - Respuesta exitosa
✅ testGetRoutesAsGeoJson_NotAuthenticated() - HTTP 401
✅ testGetRoutesAsGeoJson_NoRoutesFound() - Sin rutas
✅ testGetRoutesAsGeoJson_MissingParameters() - HTTP 400
✅ testGetRoutesAsGeoJson_DefaultLimit() - Límite por defecto
✅ testGetRoutesAsGeoJson_CustomLimit() - Límite custom
✅ testGetRoutesAsGeoJson_GeoJsonStructure() - Estructura válida
✅ [más...]
```

**Cobertura:** ~90% del controller

---

## 🧪 Tests Manuales

### Test 1: Flujo Strava → Ubicación

```bash
1. Abre http://localhost:8080/login.html
2. Click "Connect with Strava"
3. Autoriza en Strava
4. En index.html, da ubicación
5. ✅ Verifica que aparecen rutas en el mapa
```

**Verificar:**
- URL contiene `?auth=success&athlete=XXXXX`
- sessionStorage tiene `athleteId`
- Rutas aparecen en el mapa
- Popups funcionan al hacer clic

---

### Test 2: Flujo Ubicación → Strava

```bash
1. Abre http://localhost:8080/index.html
2. Da ubicación primero
3. Click "Connect with Strava"
4. Autoriza en Strava
5. ✅ Verifica que rutas aparecen automáticamente
```

**Verificar:**
- Mapa se restaura automáticamente
- Rutas se cargan sin volver a dar ubicación
- sessionStorage persiste datos

---

### Test 3: Verificar sessionStorage

```javascript
// Abre consola del navegador (F12)
console.log('=== SESSION STORAGE ===');
console.log('athleteId:', sessionStorage.getItem('athleteId'));
console.log('currentCity:', sessionStorage.getItem('currentCity'));
console.log('userLat:', sessionStorage.getItem('userLat'));
console.log('userLon:', sessionStorage.getItem('userLon'));
```

**Resultado esperado:**
```
athleteId: 123456
currentCity: Madrid
userLat: 40.4168
userLon: -3.7038
```

---

### Test 4: Endpoint REST Directo

```bash
# Obtén tu athleteId después de autenticarte

curl "http://localhost:8080/api/v1/strava/routes/geojson?athleteId=123456&city=Madrid&limit=3"
```

**Verificar:**
- Status 200
- Respuesta JSON válida
- `type: "FeatureCollection"`
- Array de `features`
- `metadata` presente

---

### Test 5: Agrupación de Rutas

**Prerequisito:** Tener 2+ actividades similares en Strava

**Verificar:**
- Rutas similares se agrupan
- `repetitions` > 1 en popup
- Se muestran top 3 rutas

---

### Test 6: Error - No Autenticado

```javascript
// Limpia sessionStorage
sessionStorage.removeItem('athleteId');

// Intenta cargar rutas
// ℹ️ Debería mostrar mensaje: "Usuario no autenticado"
```

---

### Test 7: Error - Sin Actividades

```bash
# Busca en ciudad donde no tienes actividades
curl "http://localhost:8080/api/v1/strava/routes/geojson?athleteId=123456&city=Tokyo"
```

**Resultado esperado:**
```json
{
  "type": "FeatureCollection",
  "features": [],
  "metadata": {
    "totalRoutes": 0,
    "message": "No routes found in this city"
  }
}
```

---

## 📈 Cobertura de Código

```
PolylineUtil:              100% ✅
RouteGeoJsonDTO:           100% ✅
StravaActivityService:     ~85% ✅
StravaController:          ~90% ✅
```

---

## 🔍 Debugging Tests

### Ver logs detallados
```bash
./gradlew test --info --stacktrace
```

### Test solo una clase
```bash
./gradlew test --tests "PolylineUtilTest" --info
```

### Test solo un método
```bash
./gradlew test --tests "PolylineUtilTest.testDecodeSimplePolyline"
```

### Limpiar y volver a ejecutar
```bash
./gradlew clean test
```

---

## 🧪 Tests de Performance

### Test de Carga (Manual)

```bash
# Generar 100 requests
for i in {1..100}; do
  curl -s http://localhost:8080/api/v1/strava/health > /dev/null
done

# Medir tiempo
time curl http://localhost:8080/api/v1/air/quality/city/madrid
```

---

## ✅ Checklist de Testing

Antes de hacer commit/deploy:

- [ ] `./gradlew test` pasa sin errores
- [ ] Cobertura > 80%
- [ ] Tests manuales completados
- [ ] No hay warnings en logs
- [ ] Reporte HTML revisado
- [ ] Tests de regresión OK

---

## 🐛 Si Tests Fallan

1. **Limpiar build:**
   ```bash
   ./gradlew clean
   ```

2. **Ver stacktrace completo:**
   ```bash
   ./gradlew test --stacktrace
   ```

3. **Verificar dependencias:**
   ```bash
   ./gradlew dependencies
   ```

4. **Abrir reporte:**
   ```bash
   open build/reports/tests/test/index.html
   ```

---

## 📊 Métricas

```
Total tests: 22
Tiempo ejecución: ~2 segundos
Test más lento: ~200ms
Test más rápido: ~10ms
```

---

**Última actualización:** 4 de Octubre de 2025

