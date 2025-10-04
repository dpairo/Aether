# Test Manual - Funcionalidad de Rutas Repetidas

## Prerequisitos

1. Servidor Spring Boot ejecutándose en `http://localhost:8080`
2. Credenciales de Strava configuradas en `application.yml`
3. Navegador con soporte de geolocalización
4. Cuenta de Strava con actividades registradas

## Tests Automatizados

### Ejecutar Tests Unitarios

```bash
./gradlew test
```

Esto ejecutará:
- ✅ `PolylineUtilTest` - Tests del decodificador de polylines
- ✅ `StravaActivityServiceTest` - Tests del servicio de actividades
- ✅ `StravaControllerIntegrationTest` - Tests del endpoint REST

### Ver Resultados de Tests

```bash
./gradlew test --info
```

O abre el reporte HTML:
```bash
open build/reports/tests/test/index.html
```

## Tests Manuales

### Test 1: Flujo Strava → Ubicación ✅

**Objetivo:** Verificar que las rutas se cargan cuando primero te autenticas y luego das ubicación.

**Pasos:**

1. **Limpiar estado anterior**
   ```javascript
   // Abre la consola del navegador (F12) y ejecuta:
   sessionStorage.clear();
   ```

2. **Ir a la página de login**
   ```
   http://localhost:8080/login.html
   ```

3. **Hacer clic en "Connect with Strava"**
   - Deberías ser redirigido a Strava
   - Autoriza la aplicación
   
4. **Verificar redirección**
   - URL debería ser: `http://localhost:8080/index.html?auth=success&athlete=XXXXX`
   - Abre consola (F12) y verifica:
     ```javascript
     console.log(sessionStorage.getItem('athleteId')); // Debería mostrar tu ID
     ```

5. **Dar ubicación**
   - Haz clic en el botón de ubicación (icono de ubicación)
   - Acepta el permiso de geolocalización
   - Espera a que se detecte la ciudad

6. **Verificar resultados en consola**
   ```
   ✅ Ciudad detectada: [Nombre de tu ciudad]
   ✅ Obteniendo rutas para atleta [ID] en [Ciudad]...
   ✅ Rutas obtenidas: [GeoJSON object]
   ✅ [N] rutas dibujadas correctamente
   ```

7. **Verificar en el mapa**
   - Deberías ver líneas de colores (rojo, azul, verde) representando tus rutas
   - Haz clic en una ruta para ver el popup con información
   - Verifica que muestre: nombre, distancia, tiempo, **repeticiones**

**Resultado esperado:** ✅ Rutas visibles en el mapa con popups funcionando

---

### Test 2: Flujo Ubicación → Strava ✅ (Bidireccional)

**Objetivo:** Verificar que las rutas se cargan cuando primero das ubicación y luego te autenticas.

**Pasos:**

1. **Limpiar estado anterior**
   ```javascript
   sessionStorage.clear();
   ```

2. **Ir directamente a index**
   ```
   http://localhost:8080/index.html
   ```

3. **Dar ubicación primero**
   - Haz clic en el botón de ubicación
   - Acepta el permiso
   - Espera a que se detecte la ciudad

4. **Verificar que se guardó la ubicación**
   ```javascript
   console.log(sessionStorage.getItem('currentCity'));  // Tu ciudad
   console.log(sessionStorage.getItem('userLat'));      // Latitud
   console.log(sessionStorage.getItem('userLon'));      // Longitud
   ```

5. **Hacer clic en "Find Your Route"**
   - Serás redirigido a Strava
   - Autoriza la aplicación

6. **Verificar restauración automática**
   - Al volver, la consola debería mostrar:
     ```
     ✅ Usuario autenticado con Strava, athlete ID: [ID]
     ✅ Ciudad previamente detectada: [Ciudad]
     ✅ Cargando rutas automáticamente...
     ✅ Rutas cargadas después de autenticación
     ```

7. **Verificar en el mapa**
   - El mapa debería estar centrado en tu ciudad
   - Las rutas deberían estar dibujadas **automáticamente**
   - Sin necesidad de volver a dar ubicación

**Resultado esperado:** ✅ Mapa restaurado y rutas visibles automáticamente

---

### Test 3: Verificar Datos en sessionStorage

**Objetivo:** Comprobar que los datos persisten correctamente.

**Pasos:**

1. Completa Test 1 o Test 2

2. Abre la consola y ejecuta:
   ```javascript
   console.log('=== SESSION STORAGE ===');
   console.log('athleteId:', sessionStorage.getItem('athleteId'));
   console.log('currentCity:', sessionStorage.getItem('currentCity'));
   console.log('userLat:', sessionStorage.getItem('userLat'));
   console.log('userLon:', sessionStorage.getItem('userLon'));
   console.log('cityAqiColor:', sessionStorage.getItem('cityAqiColor'));
   ```

**Resultado esperado:**
```
=== SESSION STORAGE ===
athleteId: 123456
currentCity: Madrid
userLat: 40.4168
userLon: -3.7038
cityAqiColor: #2ECC71
```

---

### Test 4: Verificar Endpoint REST Directamente

**Objetivo:** Probar el endpoint sin el frontend.

**Pasos:**

1. Obtén tu `athleteId` de la consola después de autenticarte

2. Usa curl o Postman:
   ```bash
   curl "http://localhost:8080/api/v1/strava/routes/geojson?athleteId=123456&city=Madrid&limit=3"
   ```

3. Verifica la respuesta JSON:
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
           "activityId": 123,
           "name": "Morning Run",
           "type": "Run",
           "distance": 5000.0,
           "movingTime": 1800,
           "repetitions": 5,
           "color": "#E74C3C"
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

**Resultado esperado:** ✅ Respuesta JSON con estructura GeoJSON válida

---

### Test 5: Verificar Agrupación de Rutas

**Objetivo:** Comprobar que rutas similares se agrupan correctamente.

**Prerequisitos:** Tener al menos 2 actividades en Strava que:
- Empiecen en el mismo punto (±100m)
- Terminen en el mismo punto (±100m)
- Estén en la misma ciudad

**Pasos:**

1. Completa Test 1 o Test 2

2. En la consola, verifica:
   ```javascript
   // Las rutas deberían mostrar "repetitions > 1" si hay rutas similares
   ```

3. Haz clic en una ruta en el mapa

4. Verifica en el popup:
   - **Repeticiones:** debería ser > 1 si hay rutas agrupadas

**Resultado esperado:** ✅ Rutas similares agrupadas con contador de repeticiones

---

### Test 6: Caso de Error - Usuario No Autenticado

**Objetivo:** Verificar manejo de errores cuando no hay autenticación.

**Pasos:**

1. Limpiar sessionStorage:
   ```javascript
   sessionStorage.removeItem('athleteId');
   ```

2. Intentar cargar rutas:
   ```javascript
   fetchAndDrawRoutesIfAvailable('Madrid');
   ```

3. Verificar en consola:
   ```
   ℹ️ Usuario no autenticado con Strava, no se pueden obtener rutas
   ```

**Resultado esperado:** ✅ Mensaje informativo, sin errores

---

### Test 7: Caso de Error - Ciudad Sin Actividades

**Objetivo:** Verificar comportamiento cuando no hay actividades en la ciudad.

**Pasos:**

1. Completa autenticación

2. En la consola, fuerza una búsqueda en ciudad sin actividades:
   ```javascript
   fetchRepeatedRoutes(sessionStorage.getItem('athleteId'), 'Tokyo').then(console.log);
   ```

3. Verificar respuesta:
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

**Resultado esperado:** ✅ Respuesta válida con features vacío

---

## Checklist de Verificación

### Backend
- [ ] Tests unitarios pasan (`./gradlew test`)
- [ ] Endpoint responde correctamente
- [ ] GeoJSON tiene estructura válida
- [ ] Polylines se decodifican correctamente
- [ ] Rutas se agrupan por similitud

### Frontend
- [ ] Flujo Strava → Ubicación funciona
- [ ] Flujo Ubicación → Strava funciona
- [ ] Datos persisten en sessionStorage
- [ ] Mapa se restaura correctamente
- [ ] Rutas se dibujan en el mapa
- [ ] Popups muestran información correcta
- [ ] Colores de rutas son distintos (rojo, azul, verde)
- [ ] Contador de repeticiones visible

### UX
- [ ] No hay errores en consola
- [ ] Transiciones son fluidas
- [ ] URL se limpia después del callback
- [ ] Mensajes informativos en consola
- [ ] No se requiere repetir acciones

## Problemas Comunes

### "No routes found"
- **Causa:** No tienes actividades en esa ciudad
- **Solución:** Usa una ciudad donde tengas actividades registradas

### "Usuario no autenticado"
- **Causa:** Token expirado o no configurado
- **Solución:** Vuelve a autenticar con Strava

### Rutas no se dibujan
- **Causa:** Polyline vacío o inválido
- **Solución:** Verifica en consola los logs de decodificación

### Mapa no se restaura
- **Causa:** sessionStorage limpiado
- **Solución:** Vuelve a dar ubicación

## Logs Útiles

Para debug, puedes activar más logs en consola:

```javascript
// Ver todos los datos de sessionStorage
Object.keys(sessionStorage).forEach(key => {
    console.log(`${key}: ${sessionStorage.getItem(key)}`);
});

// Ver si hay rutas cargadas
console.log('Route layers:', routeLayers.length);

// Ver GeoJSON de una ruta
fetchRepeatedRoutes(athleteId, city).then(data => {
    console.log('GeoJSON:', JSON.stringify(data, null, 2));
});
```

## Reporte de Test

Después de completar los tests, documenta:

```
FECHA: ___________
NAVEGADOR: ___________
RESULTADO:

✅/❌ Test 1: Flujo Strava → Ubicación
✅/❌ Test 2: Flujo Ubicación → Strava
✅/❌ Test 3: Persistencia sessionStorage
✅/❌ Test 4: Endpoint REST
✅/❌ Test 5: Agrupación de rutas
✅/❌ Test 6: Error - No autenticado
✅/❌ Test 7: Error - Sin actividades

NOTAS:
_________________________________
_________________________________
```

