# 🔧 Troubleshooting - Aether

## 🚨 Problemas Comunes

### Error 400 al Login de Strava

**Síntoma:** Después de autorizar en Strava, ves "Esta página no funciona - HTTP ERROR 400"

**Causas más comunes:**

1. **Código ya usado** (90% de los casos)
   - Los códigos de Strava son de un solo uso
   - Si recargas la página, el código ya no es válido
   
   **Solución:**
   ```bash
   # Cierra TODAS las pestañas
   # Abre de nuevo: http://localhost:8080/login.html
   # Connect with Strava (genera nuevo código)
   # NO recargues después del callback
   ```

2. **URL de callback incorrecta**
   - La URL en Strava debe coincidir EXACTAMENTE
   
   **Verificar:**
   - Ve a https://www.strava.com/settings/api
   - "Authorization Callback Domain" debe ser: `localhost`
   - **NO** `http://localhost` ni `localhost:8080`, solo `localhost`

3. **Credenciales incorrectas**
   ```bash
   # Verifica en application.yml:
   cat src/main/resources/application.yml | grep -A 3 "strava:"
   
   # Debe coincidir con tu app en Strava:
   # https://www.strava.com/settings/api
   ```

---

### Puerto 8080 Ocupado

**Síntoma:** `Port 8080 is already in use`

**Solución:**
```bash
# Encontrar proceso
lsof -ti:8080

# Matar proceso (reemplaza PID)
kill -9 PID

# O cambiar puerto en application.yml:
server:
  port: 8081
```

---

### Servidor No Inicia

**Síntoma:** `./gradlew bootRun` falla

**Solución:**
```bash
# 1. Limpiar y reconstruir
./gradlew clean build

# 2. Verificar Java version
java -version  # Debe ser 17+

# 3. Ver logs detallados
./gradlew bootRun --info --stacktrace
```

---

### No Se Ven Rutas en el Mapa

**Síntoma:** Autenticado correctamente pero no aparecen rutas

**Posibles causas:**

1. **No tienes actividades en esa ciudad**
   ```javascript
   // Verifica en consola del navegador (F12):
   console.log(sessionStorage.getItem('currentCity'));
   // Asegúrate de que es la ciudad correcta
   ```

2. **Las actividades no tienen polyline**
   - Algunas actividades indoor no tienen GPS
   - Solo se muestran actividades con datos de ruta

3. **Error de autenticación**
   ```javascript
   // Verifica en consola:
   console.log(sessionStorage.getItem('athleteId'));
   // Si es null, reautentica
   ```

---

### Variable de Entorno No Cargada

**Síntoma:** `client-id: ${STRAVA_CLIENT_ID}` aparece literal

**Solución:**
```bash
# Opción A: Cargar .env
source .env
./gradlew bootRun

# Opción B: Export manual
export STRAVA_CLIENT_ID="123456"
export STRAVA_CLIENT_SECRET="abc..."
./gradlew bootRun

# Opción C: Actualizar application.yml
strava:
  client-id: "123456"  # Sin ${...}
```

---

### IntelliJ No Compila

**Síntoma:** "Cannot find symbol" o errores de compilación en IntelliJ

**Solución:**
```bash
# 1. Reload Gradle Project
# Clic derecho en build.gradle → Reload Gradle Project

# 2. Invalidar cache
# File → Invalidate Caches and Restart

# 3. Verificar SDK
# File → Project Structure → Project → SDK = Java 17

# 4. Limpiar desde terminal
./gradlew clean build
```

---

### Error H2 Console

**Síntoma:** No puedes acceder a `/h2-console`

**Nota:** H2 console está deshabilitado por seguridad.

**Para habilitarlo:**
```yaml
# application.yml
spring:
  h2:
    console:
      enabled: true
```

Luego accede: http://localhost:8080/h2-console
```
JDBC URL: jdbc:h2:mem:devdb
User: sa
Password: (vacío)
```

---

### Tests Fallan

**Síntoma:** `./gradlew test` falla

**Solución:**
```bash
# Ver detalles
./gradlew test --info

# Limpiar y reconstruir
./gradlew clean test

# Ver reporte
open build/reports/tests/test/index.html
```

---

### Frontend No Se Actualiza

**Síntoma:** Los cambios en JS/HTML no se reflejan

**Solución:**
```bash
# 1. Hard refresh en navegador
# Cmd+Shift+R (Mac) o Ctrl+Shift+R (Win/Linux)

# 2. Limpiar cache del navegador

# 3. Rebuild proyecto
./gradlew clean build
./gradlew bootRun
```

---

### No Aparecen Logs

**Síntoma:** No ves logs en la terminal

**Solución:**
```bash
# Ejecutar con más verbosidad
./gradlew bootRun --info

# O configurar en application.yml:
logging:
  level:
    com.aether.app: DEBUG
```

---

### CORS Errors

**Síntoma:** `Access-Control-Allow-Origin` error en consola

**Nota:** Solo si usas frontend separado (no aplica para la config actual)

**Solución:** Configurar CORS en Spring:
```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:3000");
    }
}
```

---

### sessionStorage Se Borra

**Síntoma:** Pierdes la autenticación al recargar

**Comportamiento esperado:** `sessionStorage` persiste durante la sesión del navegador

**Se borra cuando:**
- Cierras todas las pestañas del sitio
- Usas modo incógnito y cierras la ventana
- Limpias manualmente: `sessionStorage.clear()`

**Para persistencia permanente:** Usa `localStorage` en lugar de `sessionStorage`

---

## 🧪 Comandos de Diagnóstico

```bash
# Test de conectividad
curl http://localhost:8080/api/v1/strava/health

# Ver configuración
cat src/main/resources/application.yml | grep -A 5 "strava"

# Ver variables de entorno
echo $STRAVA_CLIENT_ID
echo $STRAVA_CLIENT_SECRET

# Test endpoints
./test_strava_flow.sh

# Ver logs en tiempo real
./gradlew bootRun | grep "ERROR"
```

---

## 📞 Obtener Más Ayuda

1. **Revisa los logs del servidor**
   - Busca líneas con "ERROR" o "WARN"
   - Especialmente después de callbacks

2. **Revisa consola del navegador** (F12)
   - Pestaña "Console" para errores de JS
   - Pestaña "Network" para ver requests fallidos

3. **Verifica configuración de Strava**
   - https://www.strava.com/settings/api
   - Client ID, Secret, Callback Domain

4. **Limpia y reinicia**
   ```bash
   ./gradlew clean
   # Cierra navegador
   ./gradlew bootRun
   # Abre navegador en modo incógnito
   ```

---

## ✅ Checklist de Verificación

- [ ] Java 17+ instalado
- [ ] Puerto 8080 libre
- [ ] Servidor corriendo (`./gradlew bootRun`)
- [ ] Health check OK (`curl localhost:8080/api/v1/strava/health`)
- [ ] Credenciales de Strava configuradas
- [ ] Callback domain en Strava = `localhost`
- [ ] Browser permite cookies/sessionStorage
- [ ] No hay errores en logs del servidor
- [ ] No hay errores en consola del navegador

---

**Última actualización:** 4 de Octubre de 2025

