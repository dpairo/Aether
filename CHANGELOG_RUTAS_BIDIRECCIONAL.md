# Changelog - Funcionalidad Bidireccional de Rutas

## Fecha: 4 de Octubre de 2025

## 🎯 Objetivo
Permitir que la funcionalidad de rutas más repetidas funcione independientemente del orden en que el usuario:
1. Da su ubicación
2. Inicia sesión en Strava

## ✅ Cambios Implementados

### Frontend (`code.js`)

#### 1. Persistencia en sessionStorage
**Antes:** Solo se guardaba temporalmente en variables
**Ahora:** Se guarda en `sessionStorage` para persistencia durante la sesión

```javascript
// Nuevas líneas añadidas al detectar ubicación:
sessionStorage.setItem('currentCity', cityName);
sessionStorage.setItem('userLat', Ulat.toString());
sessionStorage.setItem('userLon', Ulon.toString());
sessionStorage.setItem('cityAqiColor', cityAqiColor);
```

#### 2. Detección al cargar página
**Nuevo código en `DOMContentLoaded`:**

```javascript
// Detecta si el usuario viene de autenticación de Strava
const authSuccess = urlParams.get('auth');
const athleteParam = urlParams.get('athlete');

if (authSuccess === 'success' && athleteParam) {
    // Guarda athleteId
    sessionStorage.setItem('athleteId', athleteParam);
    
    // Verifica si ya hay ubicación guardada
    const savedCity = sessionStorage.getItem('currentCity');
    const savedLat = sessionStorage.getItem('userLat');
    const savedLon = sessionStorage.getItem('userLon');
    
    if (savedCity && savedLat && savedLon) {
        // ✨ MAGIA: Restaura mapa y carga rutas automáticamente
        restaurarMapaYCargarRutas();
    }
}
```

#### 3. Limpieza de URL
**Nuevo:** Los parámetros de la URL se limpian después de procesarlos

```javascript
// Limpia ?auth=success&athlete=xxx de la URL
window.history.replaceState({}, document.title, window.location.pathname);
```

## 📊 Comparación: Antes vs Ahora

| Aspecto | Antes | Ahora |
|---------|-------|-------|
| **Orden requerido** | Solo Strava → Ubicación | Cualquier orden ✅ |
| **Persistencia** | Solo en variables | sessionStorage ✅ |
| **Restauración** | Manual | Automática ✅ |
| **UX** | Usuario debía repetir pasos | Sin repeticiones ✅ |

## 🔄 Flujos Soportados

### ✅ Flujo 1: Strava → Ubicación (ya funcionaba)
1. Login Strava
2. Dar ubicación
3. **Rutas cargadas**

### ✅ Flujo 2: Ubicación → Strava (NUEVO)
1. Dar ubicación
2. Login Strava
3. **Rutas cargadas automáticamente al volver**

### ✅ Flujo 3: Recarga de página (MEJORADO)
1. Completar flujo 1 o 2
2. Recargar página
3. **Datos persisten en sessionStorage**

## 🐛 Bugs Corregidos

1. **Bug:** Si dabas ubicación primero y luego iniciabas sesión en Strava, no se cargaban las rutas
   - **Fix:** Ahora se detecta automáticamente al volver del callback de Strava

2. **Bug:** Al volver del callback de Strava, perdías la visualización del mapa
   - **Fix:** El mapa se restaura automáticamente con los datos guardados

3. **Bug:** URL quedaba con parámetros ?auth=success&athlete=xxx
   - **Fix:** Se limpian automáticamente después de procesarlos

## 📝 Archivos Modificados

1. `src/main/resources/static/code.js`
   - Añadidas ~70 líneas de código
   - 3 funciones modificadas
   - 1 evento nuevo (DOMContentLoaded mejorado)

2. `RUTAS_REPETIDAS_FEATURE.md`
   - Actualizada documentación
   - Añadidos 2 ejemplos de uso

3. `FLUJO_RUTAS_BIDIRECCIONAL.md` (NUEVO)
   - Documentación visual de ambos flujos
   - Diagramas de flujo
   - Casos de uso

4. `CHANGELOG_RUTAS_BIDIRECCIONAL.md` (este archivo)
   - Registro detallado de cambios

## 🧪 Testing

### Casos de Prueba

#### Test 1: Orden Strava → Ubicación ✅
```
PASOS:
1. Ir a /login.html
2. Conectar con Strava
3. Dar ubicación
4. Verificar rutas en mapa

ESPERADO: ✅ Rutas visibles
RESULTADO: PASS
```

#### Test 2: Orden Ubicación → Strava ✅
```
PASOS:
1. Ir a /index.html
2. Dar ubicación
3. Conectar con Strava
4. Verificar rutas en mapa (automático)

ESPERADO: ✅ Rutas visibles automáticamente
RESULTADO: PASS
```

#### Test 3: Persistencia sessionStorage ✅
```
PASOS:
1. Completar Test 1 o Test 2
2. Verificar sessionStorage en DevTools
3. Verificar valores guardados

ESPERADO: ✅ athleteId, currentCity, userLat, userLon guardados
RESULTADO: PASS
```

## 🚀 Mejoras Futuras (opcional)

1. **localStorage vs sessionStorage**: Evaluar si usar localStorage para persistencia entre sesiones
2. **Cache de rutas**: Guardar las rutas en sessionStorage para evitar llamadas duplicadas
3. **Indicador visual**: Mostrar badge cuando el usuario está autenticado con Strava
4. **Botón "Actualizar rutas"**: Permitir refrescar manualmente las rutas

## 📈 Impacto

- **UX mejorada:** Usuario puede hacer las cosas en el orden que prefiera
- **Menos fricción:** No hay que repetir acciones
- **Más conversión:** Usuario no abandona por tener que repetir pasos
- **Más robusto:** Maneja diferentes flujos de navegación

## ✨ Resumen

La funcionalidad ahora es **verdaderamente bidireccional** y ofrece una experiencia de usuario fluida independientemente del orden en que el usuario interactúe con la aplicación.

**Antes:** 🔄 Solo funcionaba en un orden específico
**Ahora:** ✨ Funciona en cualquier orden + restauración automática

