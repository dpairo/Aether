# 🏃 Feature: Rutas Más Repetidas de Strava

## 📝 Descripción

Esta funcionalidad muestra las rutas más frecuentes que un usuario hace en una ciudad específica, visualizadas en formato GeoJSON sobre el mapa.

## ✨ Características

- ✅ Detección automática de rutas similares (umbral: 100m)
- ✅ Muestra hasta 3 rutas más repetidas
- ✅ Cada ruta con color distinto (rojo, azul, verde)
- ✅ Contador de repeticiones por ruta
- ✅ Funciona en ambos órdenes: `Ubicación→Strava` o `Strava→Ubicación`

## 🔄 Flujo de Funcionamiento

### Opción 1: Strava → Ubicación

```
1. Usuario → /login.html
2. Connect with Strava
3. Autoriza en Strava
4. Vuelve a /index.html?auth=success&athlete=123
5. Da ubicación
6. ✨ Rutas cargadas automáticamente
```

### Opción 2: Ubicación → Strava (Bidireccional)

```
1. Usuario → /index.html
2. Da ubicación (se guarda en sessionStorage)
3. Connect with Strava
4. Autoriza en Strava
5. Vuelve a /index.html?auth=success&athlete=123
6. ✨ Rutas cargadas automáticamente (restaura mapa y datos)
```

## 🎨 Visualización

- **Ruta #1 (más repetida)**: Rojo (#E74C3C)
- **Ruta #2**: Azul (#3498DB)
- **Ruta #3**: Verde (#2ECC71)

## 🔧 Algoritmo de Agrupación

1. Obtiene últimas 50 actividades del usuario en la ciudad
2. Filtra actividades con polyline válido
3. Agrupa rutas similares:
   - Punto de inicio a menos de 100m
   - Punto de fin a menos de 100m
4. Ordena por número de repeticiones (descendente)
5. Devuelve top 3

## 📊 API

### Endpoint Principal

```
GET /api/v1/strava/routes/geojson
  ?athleteId=123456
  &city=Madrid
  &limit=3
```

**Respuesta:**
```json
{
  "type": "FeatureCollection",
  "features": [
    {
      "type": "Feature",
      "geometry": {
        "type": "LineString",
        "coordinates": [[lon, lat], ...]
      },
      "properties": {
        "name": "Morning Run",
        "repetitions": 5,
        "color": "#E74C3C",
        "distance": 5000.0
      }
    }
  ],
  "metadata": {
    "totalRoutes": 3,
    "totalRepetitions": 15
  }
}
```

## 💾 Persistencia

Usa `sessionStorage` para mantener datos durante la sesión:

```javascript
sessionStorage.setItem('athleteId', '123456');
sessionStorage.setItem('currentCity', 'Madrid');
sessionStorage.setItem('userLat', '40.4168');
sessionStorage.setItem('userLon', '-3.7038');
```

## 🔍 Decodificación de Polylines

Strava devuelve rutas en formato Google Polyline (comprimido):

```
Entrada:  "_p~iF~ps|U_ulLnnqC"
Salida:   [[lon, lat], [lon, lat], ...]
```

La clase `PolylineUtil.java` maneja esta conversión.

## 🧪 Testing

### Tests Automatizados

```bash
./gradlew test --tests "PolylineUtilTest"
./gradlew test --tests "StravaActivityServiceTest"
./gradlew test --tests "StravaControllerIntegrationTest"
```

### Tests Manuales

1. Login en Strava
2. Da ubicación
3. Verifica que aparecen rutas en el mapa
4. Haz clic en una ruta → debería mostrar popup con info

## 📈 Casos de Uso

### Usuario nuevo en la ciudad
```
Estado: No tiene rutas en esa ciudad
Resultado: features = [], mensaje informativo
```

### Usuario regular
```
Estado: Tiene 10 actividades en Madrid
- 5 veces misma ruta → Ruta #1 (5 repeticiones)
- 3 veces otra ruta → Ruta #2 (3 repeticiones)
- 2 veces otra ruta → Ruta #3 (2 repeticiones)
Resultado: 3 rutas dibujadas en el mapa
```

## 🔧 Configuración

No requiere configuración adicional. Solo necesitas:
- Estar autenticado con Strava
- Tener ubicación configurada

## 🎯 Limitaciones

- Solo busca en las últimas 50 actividades (configurable)
- Umbral de similitud fijo: 100m
- Máximo 3 rutas por defecto (configurable via `limit`)
- Solo muestra actividades con polyline (algunas actividades indoor no tienen)

## 💡 Tips

- Si no ves rutas, verifica que tienes actividades en esa ciudad
- Las rutas deben empezar y terminar en puntos similares
- Los códigos de autorización de Strava son de un solo uso
- Limpia sessionStorage si tienes problemas: `sessionStorage.clear()`

---

**Última actualización:** 4 de Octubre de 2025

