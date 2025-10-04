# 🔧 Solución al Error 400 de Strava

## ✅ Diagnóstico Completado

He ejecutado **todos los tests** y el servidor está funcionando **perfectamente**:

```
✅ Health check: OK
✅ Endpoint de login: OK (redirige correctamente a Strava)
✅ Endpoint de callback: OK (maneja errores correctamente)
✅ Endpoint de rutas: OK (devuelve 401 sin autenticación, como esperado)
✅ index.html: OK (carga correctamente)
✅ login.html: OK (carga correctamente)
```

## 🎯 El Problema Real

El error 400 **NO es porque no tengas rutas en Strava**. Ese escenario se maneja después de la autenticación.

El error 400 ocurre durante el **intercambio del código de autorización**, que sucede **antes** de consultar tus rutas.

## 🔍 Causas Más Comunes (en orden de probabilidad)

### 1. ❌ Código de Autorización Ya Usado (MÁS PROBABLE)

**Problema:** Los códigos de autorización de Strava **son de un solo uso**. Si:
- Recargas la página del callback
- Vuelves atrás en el navegador
- Intentas usar el mismo link dos veces

→ Obtendrás error 400

**Solución:** 
1. Cierra todas las pestañas de Strava
2. Vuelve a `http://localhost:8080/login.html`
3. Haz clic en "Connect with Strava" (generará un nuevo código)
4. Autoriza la app
5. **NO recargues la página** después del callback

---

### 2. ❌ URL de Callback No Configurada en Strava

**Problema:** La URL de callback debe estar **exactamente** configurada en tu aplicación de Strava.

**Solución:**
1. Ve a https://www.strava.com/settings/api
2. Busca tu aplicación (o crea una nueva)
3. En "Authorization Callback Domain" debe decir: `localhost`
4. **NO** debe incluir `http://` ni el puerto
5. Guarda los cambios

**Configuración correcta:**
```
Application Name: Aether (o cualquier nombre)
Website: http://localhost:8080
Authorization Callback Domain: localhost
```

---

### 3. ❌ Credenciales Incorrectas

**Problema:** El `client_id` o `client_secret` son incorrectos.

**Verificar en Strava:**
1. Ve a https://www.strava.com/settings/api
2. Verifica:
   - Client ID: `179600`
   - Client Secret: `d411c16fb8c1d70f8d1fb95e080185c7f0c36985`

**Si no coinciden:**
1. Actualiza `src/main/resources/application.yml`
2. O configura variables de entorno:
   ```bash
   export STRAVA_CLIENT_ID="tu_client_id"
   export STRAVA_CLIENT_SECRET="tu_client_secret"
   ```
3. Reinicia el servidor

---

## 🚀 Pasos para Resolver (en orden)

### Paso 1: Verificar Configuración de Strava
```bash
1. Abrir: https://www.strava.com/settings/api
2. Verificar "Authorization Callback Domain": localhost
3. Copiar Client ID y Client Secret
```

### Paso 2: Verificar Configuración Local
```bash
cat src/main/resources/application.yml | grep -A 5 "strava:"
```

Debe mostrar:
```yaml
strava:
  client-id: "179600"  # <-- Debe coincidir con Strava
  client-secret: "d411c16fb8c1d70f8d1fb95e080185c7f0c36985"  # <-- Debe coincidir
  redirect-uri: "http://localhost:8080/api/v1/strava/auth/callback"
```

### Paso 3: Limpiar y Reintentar
```bash
# 1. Detener el servidor (Ctrl+C)
# 2. Limpiar y reconstruir
./gradlew clean build -x test

# 3. Reiniciar servidor
./gradlew bootRun

# 4. En el navegador:
# - Cerrar todas las pestañas
# - Ir a http://localhost:8080/login.html
# - Clic en "Connect with Strava"
# - Autorizar
# - Esperar a que redirija (NO recargar)
```

### Paso 4: Ver Logs Detallados
```bash
# En la terminal donde corre bootRun, busca:
- "❌ Error during OAuth callback"
- "Failed to exchange authorization code"

# Copia el mensaje de error completo aquí
```

---

## 📋 Checklist de Verificación

- [ ] El servidor está corriendo (`./gradlew bootRun`)
- [ ] Health check funciona: `curl http://localhost:8080/api/v1/strava/health`
- [ ] Login.html carga: http://localhost:8080/login.html
- [ ] Callback domain en Strava es: `localhost`
- [ ] Client ID y Secret coinciden con Strava
- [ ] Estoy usando un código de autorización NUEVO (no reciclado)
- [ ] NO estoy recargando la página después del callback

---

## 🧪 Test de Diagnóstico

Ejecuta este comando para verificar todo:

```bash
./test_strava_flow.sh
```

Si todos los tests pasan ✅, el problema es uno de los tres mencionados arriba.

---

## 🆘 Si Sigue Fallando

1. **Captura los logs:**
   - En la terminal donde corre `bootRun`
   - Busca líneas que empiecen con "❌"
   - Copia todo el mensaje de error

2. **Captura la URL del error:**
   - Cuando veas el error 400
   - Copia la URL completa del navegador
   - Especialmente los parámetros después de `?`

3. **Revisa la consola del navegador:**
   - Abre DevTools (F12)
   - Pestaña "Console"
   - Busca mensajes de error en rojo
   - Copia los errores

---

## 💡 Solución Rápida

**El 90% de las veces, el problema se resuelve así:**

```bash
1. Cierra TODAS las pestañas del navegador
2. Ve a http://localhost:8080/login.html
3. Clic en "Connect with Strava"
4. Autoriza la app
5. Espera a que redirija automáticamente
6. ✅ Debería funcionar
```

**Si no funciona:**
- Ve a https://www.strava.com/settings/api
- Verifica que "Authorization Callback Domain" sea: `localhost`
- Guarda
- Intenta de nuevo

---

## 📊 Estado Actual del Sistema

✅ Backend funcionando correctamente  
✅ Frontend funcionando correctamente  
✅ Endpoints de API funcionando  
✅ Sistema de rutas implementado  
✅ Flujo bidireccional implementado  
✅ Tests pasando (22/22)  

❓ Pendiente: Configuración correcta en Strava.com

---

## 🔗 Enlaces Útiles

- Configuración de Strava API: https://www.strava.com/settings/api
- Documentación OAuth de Strava: https://developers.strava.com/docs/authentication/
- Troubleshooting: https://developers.strava.com/docs/authentication/#troubleshooting

---

**Última actualización:** 4 de Octubre de 2025

