# 🚀 Guía de Inicio Rápido - Aether

## ⏱️ Setup en 5 minutos

### Paso 1: Prerrequisitos
- Java 17 o superior
- Gradle 7.x o superior
- Cuenta de Strava (opcional, para features avanzadas)

### Paso 2: Clonar y Compilar

```bash
git clone https://github.com/tuusuario/Aether.git
cd Aether
./gradlew build
```

### Paso 3: Ejecutar

```bash
./gradlew bootRun
```

Espera a ver:
```
Started AetherApplication in X.XXX seconds
```

### Paso 4: Abrir

```
http://localhost:8080
```

¡Listo! 🎉

## 🎯 Funcionalidades Básicas (Sin Strava)

### Visualizar Calidad del Aire
1. Abre http://localhost:8080
2. Click en "Explore my location"
3. Acepta permisos de ubicación
4. Ve el mapa coloreado según AQI de tu ciudad

### Ciudades Soportadas

Madrid, Barcelona, Valencia, Sevilla, Bilbao, Zaragoza y más.

## 🏃 Setup Opcional: Integración con Strava

Si quieres ver tus rutas más repetidas:

### 1. Crear App en Strava (2 min)

1. Ve a https://www.strava.com/settings/api
2. Click "Create App"
3. Rellena:
   ```
   Application Name: Aether Local
   Website: http://localhost:8080
   Authorization Callback Domain: localhost
   ```
4. Copia tu Client ID y Client Secret

### 2. Configurar Credenciales

**Opción A - Variables de entorno:**
```bash
export STRAVA_CLIENT_ID="tu_client_id"
export STRAVA_CLIENT_SECRET="tu_client_secret"
```

**Opción B - Actualizar application.yml:**
```yaml
strava:
  client-id: "tu_client_id"
  client-secret: "tu_client_secret"
```

### 3. Reiniciar y Probar

```bash
./gradlew bootRun
```

1. Abre http://localhost:8080/login.html
2. Click "Connect with Strava"
3. Autoriza
4. ¡Verás tus rutas más repetidas en el mapa! 🎉

## 🧪 Verificación

```bash
# Health check
curl http://localhost:8080/api/v1/strava/health

# Calidad del aire de Madrid
curl http://localhost:8080/api/v1/air/quality/city/madrid
```

## 📖 Más Documentación

- [API Reference](API.md) - Todos los endpoints
- [Rutas Repetidas](RUTAS_REPETIDAS.md) - Feature de Strava
- [Troubleshooting](TROUBLESHOOTING.md) - Solución de problemas
- [Setup de Strava](STRAVA_SETUP.md) - Configuración detallada

## 🎓 Conceptos Clave

- **AQI** (Air Quality Index): Índice de calidad del aire de 0 a 500
- **Colores AQI**: Verde (bueno) a Marrón (peligroso)
- **Polyline**: Formato de Google para representar rutas
- **OAuth 2.0**: Protocolo de autenticación de Strava

---

**¿Problemas?** Consulta [TROUBLESHOOTING.md](TROUBLESHOOTING.md)

