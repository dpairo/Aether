# 🏃 Configuración de Strava - Guía Paso a Paso

## 📋 Requisitos Previos
- Una cuenta de Strava (gratis en https://www.strava.com)
- Tu proyecto Aether descargado
- Java 17 instalado

---

## 🎯 Paso 1: Crear Aplicación en Strava (5 minutos)

### 1.1 Acceder a Strava API Settings

1. Abre tu navegador y ve a: **https://www.strava.com/settings/api**
2. Inicia sesión con tu cuenta de Strava si no lo has hecho

### 1.2 Crear Nueva Aplicación

Verás un formulario con los siguientes campos. Rellénalos así:

```
┌─────────────────────────────────────────────────────────┐
│  Create a New Application                               │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  Application Name:  Aether                              │
│  Category:          Visualizer                          │
│  Club:              (dejar vacío)                       │
│  Website:           http://localhost:8080               │
│  Application Description:                               │
│    Air quality visualization for runners                │
│                                                          │
│  Authorization Callback Domain:  localhost              │
│                                                          │
│  ⚠️ IMPORTANTE: Debe ser exactamente "localhost"       │
│     sin http:// ni puerto                               │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

### 1.3 Aceptar Términos y Crear

1. ✅ Marca la casilla "I agree to the API Agreement"
2. Click en el botón **"Create"**

### 1.4 Guardar Credenciales

Después de crear la aplicación, verás una página con tus credenciales:

```
┌─────────────────────────────────────────────────────────┐
│  My API Application - Aether                            │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  Application ID:      123456                            │
│  Client ID:           123456                            │
│  Client Secret:       a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6  │
│                                                          │
│  🔴 IMPORTANTE: Guarda estos valores ahora              │
└─────────────────────────────────────────────────────────┘
```

**📝 Anota en un lugar seguro:**
- **Client ID**: (un número de 5-6 dígitos)
- **Client Secret**: (una cadena alfanumérica larga)

---

## 🔧 Paso 2: Configurar el Proyecto Aether

### Opción A: Usando el Script Automático (Recomendado) ⭐

1. **Abre la terminal** en la carpeta de tu proyecto:
   ```bash
   cd /Users/dpairo/Developer/Aether
   ```

2. **Ejecuta el script de configuración:**
   ```bash
   ./setup-strava.sh
   ```

3. **Sigue las instrucciones en pantalla:**
   ```
   ===================================
     AETHER - Configuración de Strava
   ===================================
   
   Obtén tus credenciales en: https://www.strava.com/settings/api
   
   Ingresa tu STRAVA_CLIENT_ID: 123456
   Ingresa tu STRAVA_CLIENT_SECRET: a1b2c3d4e5f6...
   Ingresa el REDIRECT_URI [http://localhost:8080/api/v1/strava/auth/callback]: 
   Ingresa tu WAQI_TOKEN [demo]: 
   
   ✅ Archivo .env creado exitosamente!
   ```

4. **¡Listo!** El archivo `.env` se ha creado automáticamente.

### Opción B: Configuración Manual

Si prefieres configurar manualmente:

1. **Crea un archivo `.env`** en la raíz del proyecto:
   ```bash
   cd /Users/dpairo/Developer/Aether
   touch .env
   ```

2. **Abre el archivo `.env`** con tu editor favorito:
   ```bash
   nano .env
   # o
   code .env
   # o
   open -a TextEdit .env
   ```

3. **Añade las siguientes líneas** (reemplaza con tus valores):
   ```bash
   STRAVA_CLIENT_ID=123456
   STRAVA_CLIENT_SECRET=a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6
   STRAVA_REDIRECT_URI=http://localhost:8080/api/v1/strava/auth/callback
   WAQI_TOKEN=demo
   ```

4. **Guarda el archivo** (Ctrl+O en nano, Cmd+S en otros editores)

---

## 🚀 Paso 3: Ejecutar la Aplicación

### 3.1 Cargar Variables de Entorno

En la terminal, carga las variables de entorno:

```bash
source .env
```

### 3.2 Verificar que las Variables se Cargaron

```bash
echo "Client ID: $STRAVA_CLIENT_ID"
echo "Redirect URI: $STRAVA_REDIRECT_URI"
```

Deberías ver tus valores impresos.

### 3.3 Ejecutar Aether

```bash
./gradlew bootRun
```

Espera a ver este mensaje:
```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.3.3)

Started AetherApplication in 3.456 seconds
```

**✅ ¡La aplicación está corriendo!**

---

## 🧪 Paso 4: Probar la Integración

### 4.1 Abrir la Aplicación

1. Abre tu navegador
2. Ve a: **http://localhost:8080**
3. Deberías ver el mapa de Aether

### 4.2 Hacer Login con Strava

1. **Busca el enlace** "🏃 Connect with Strava" (está cerca del buscador)
2. **Click** en el enlace
3. Se abrirá la página de login de Aether con un diseño morado bonito
4. **Click** en el botón naranja **"Connect with Strava"**

### 4.3 Autorizar en Strava

Serás redirigido a Strava. Verás una página como esta:

```
┌─────────────────────────────────────────────────────┐
│  Authorize Aether to use your Strava account?      │
├─────────────────────────────────────────────────────┤
│                                                      │
│  Aether would like to:                              │
│  • View your profile                                │
│  • View your activities                             │
│  • Read detailed activity data                      │
│                                                      │
│  [Authorize]  [Cancel]                              │
└─────────────────────────────────────────────────────┘
```

1. **Click en "Authorize"** (Autorizar)

### 4.4 Verificar Autenticación Exitosa

Después de autorizar:

1. Serás **redirigido automáticamente** a http://localhost:8080
2. Verás tu **nombre de Strava** en la parte superior izquierda:
   ```
   🏃 Welcome, Tu Nombre!
   ```
3. Abre la **consola del navegador** (F12) y deberías ver:
   ```
   Successfully authenticated with Strava! Athlete ID: 123456
   Athlete info: {athleteId: 123456, firstName: "Tu", ...}
   ```

**🎉 ¡Felicidades! La integración está funcionando correctamente.**

---

## 🔍 Verificación Adicional

### Probar los Endpoints Manualmente

Puedes probar que todo funciona correctamente con estos comandos:

```bash
# 1. Health check
curl http://localhost:8080/api/v1/strava/health

# Debería responder: "Strava integration is operational"

# 2. Obtener tu información (reemplaza 123456 con tu athleteId)
curl http://localhost:8080/api/v1/strava/auth/me?athleteId=123456

# Debería responder con tu información en JSON
```

---

## ⚠️ Solución de Problemas

### ❌ Error: "Invalid redirect_uri"

**Problema:** Strava rechaza el callback.

**Solución:**
1. Ve a https://www.strava.com/settings/api
2. Edita tu aplicación
3. Verifica que "Authorization Callback Domain" sea exactamente: `localhost`
4. NO pongas `http://`, ni puerto, ni `www.`

### ❌ Error: "Port 8080 already in use"

**Problema:** El puerto ya está siendo usado.

**Solución 1 - Cambiar puerto:**
1. Abre `src/main/resources/application.yml`
2. Cambia:
   ```yaml
   server:
     port: 8081  # Cambiado de 8080 a 8081
   ```
3. Actualiza tu `.env`:
   ```bash
   STRAVA_REDIRECT_URI=http://localhost:8081/api/v1/strava/auth/callback
   ```
4. Actualiza en Strava API settings el redirect URI

**Solución 2 - Matar el proceso:**
```bash
lsof -ti:8080 | xargs kill -9
```

### ❌ Error: "Invalid client"

**Problema:** Las credenciales son incorrectas.

**Solución:**
1. Verifica que copiaste correctamente Client ID y Secret
2. Asegúrate de haber ejecutado `source .env`
3. Verifica con: `echo $STRAVA_CLIENT_ID`

### ❌ No aparece "Welcome, Tu Nombre"

**Problema:** El frontend no detecta la autenticación.

**Solución:**
1. Abre la consola del navegador (F12)
2. Busca errores en rojo
3. Verifica que la URL tenga `?auth=success&athlete=123456` después del login
4. Limpia el localStorage: `localStorage.clear()` en la consola

### ❌ Variables de entorno no se cargan

**Problema:** `$STRAVA_CLIENT_ID` está vacío.

**Solución en macOS/Linux:**
```bash
# Cargar .env antes de cada ejecución
source .env
./gradlew bootRun
```

**Solución en Windows (PowerShell):**
```powershell
Get-Content .env | ForEach-Object {
    if ($_ -match '^([^=]+)=(.*)$') {
        [Environment]::SetEnvironmentVariable($matches[1], $matches[2])
    }
}
.\gradlew.bat bootRun
```

---

## 📚 Recursos Adicionales

- **Documentación completa:** [STRAVA_INTEGRATION.md](STRAVA_INTEGRATION.md)
- **Ejemplos de código:** [API_EXAMPLES.md](API_EXAMPLES.md)
- **Guía rápida:** [QUICK_START.md](QUICK_START.md)
- **Strava API Docs:** https://developers.strava.com/docs/

---

## 🎓 Siguiente Nivel

Una vez que todo funciona, puedes:

1. **Obtener actividades:**
   - Implementar endpoint para obtener actividades recientes
   - Mostrar rutas en el mapa

2. **Cruzar con calidad del aire:**
   - Ver qué días corriste con mejor/peor aire
   - Crear alertas personalizadas

3. **Análisis:**
   - Estadísticas de exposición a contaminantes
   - Recomendaciones de horarios óptimos

---

## ✅ Checklist Final

Marca cada paso cuando lo completes:

- [ ] Crear aplicación en Strava
- [ ] Copiar Client ID y Client Secret
- [ ] Ejecutar `./setup-strava.sh` (o crear `.env` manual)
- [ ] Ejecutar `source .env`
- [ ] Ejecutar `./gradlew bootRun`
- [ ] Abrir http://localhost:8080
- [ ] Click en "Connect with Strava"
- [ ] Autorizar en Strava
- [ ] Ver "Welcome, Tu Nombre" en la app
- [ ] Verificar en consola del navegador

**¡Todo listo! 🎉**

---

¿Tienes dudas? Abre un issue o revisa la documentación completa en los archivos `.md` del proyecto.

