# 🌬️ AETHER - Air Quality Visualization Platform

**Breathe Better, Run Smarter**

Plataforma web que visualiza datos de calidad del aire en tiempo real, diseñada especialmente para runners y atletas que desean optimizar sus entrenamientos según las condiciones ambientales.

---

## ✨ Características

- 🗺️ Visualización de calidad del aire en tiempo real sobre mapa interactivo
- 🔍 **Búsqueda de ciudades españolas** con coordenadas automáticas
- 🏃 Integración con Strava para rutas personalizadas
- 📍 Geolocalización para datos del aire en tu ubicación
- 🌈 Código de colores AQI (Air Quality Index) estándar EPA
- 🔄 Rutas más repetidas con contador de frecuencia
- 📊 Información detallada de ciudades españolas
- 🔴 Puntos nocivos generados automáticamente dentro de cada ciudad

---

## 🚀 Inicio Rápido

### Prerequisitos

- Java 17 o superior
- Gradle 7.x o superior
- Cuenta de Strava (opcional)

### Instalación Básica

```bash
# 1. Clonar
git clone https://github.com/tuusuario/Aether.git
cd Aether

# 2. Compilar
./gradlew build

# 3. Ejecutar
./gradlew bootRun

# 4. Abrir
http://localhost:8080
```

### Setup con Strava (Opcional)

Para ver tus rutas más repetidas:

```bash
# 1. Crear app en Strava: https://www.strava.com/settings/api
# 2. Configurar credenciales
export STRAVA_CLIENT_ID="tu_client_id"
export STRAVA_CLIENT_SECRET="tu_client_secret"

# 3. Reiniciar
./gradlew bootRun
```

Ver [docs/STRAVA_SETUP.md](docs/STRAVA_SETUP.md) para guía completa.

---

## 📖 Documentación

| Documento | Descripción |
|-----------|-------------|
| [**Guía de Inicio Rápido**](docs/QUICK_START.md) | Setup en 5 minutos |
| [**API Reference**](docs/API.md) | Endpoints y ejemplos |
| [**Búsqueda de Ciudades**](docs/CITY_SEARCH.md) | Feature de búsqueda con Nominatim |
| [**Rutas Repetidas**](docs/RUTAS_REPETIDAS.md) | Feature de Strava |
| [**Setup de Strava**](docs/STRAVA_SETUP.md) | Configuración OAuth |
| [**Tests**](docs/TESTS.md) | Guía de testing |
| [**Troubleshooting**](docs/TROUBLESHOOTING.md) | Solución de problemas |

---

## 🎯 Funcionalidades

### 1. Visualización de Calidad del Aire

- Mapa interactivo con Leaflet.js
- Colores según AQI estándar EPA
- Datos en tiempo real de ciudades españolas
- Hotspots de contaminación

### 2. 🔍 Búsqueda de Ciudades (NUEVO)

- Búsqueda de ciudades españolas por nombre
- Integración con API de Nominatim (OpenStreetMap)
- Visualización automática del polígono de la ciudad
- Color del polígono basado en el AQI de la ciudad
- Generación de 3 puntos nocivos aleatorios dentro de la ciudad
- Ajuste automático del mapa al perímetro de la ciudad
- Búsqueda por Enter o botón de búsqueda

### 3. Integración con Strava

- OAuth 2.0 seguro
- Detección de rutas más repetidas
- Visualización en formato GeoJSON
- Contador de frecuencia por ruta

### 4. Geolocalización

- Permiso del usuario
- Reverse geocoding con Nominatim
- Persistencia en sessionStorage
- Flujo bidireccional (ubicación ↔ Strava)

---

## 🏗️ Arquitectura

### Backend (Spring Boot 3.3.3)

```
src/main/java/com/aether/app/
├── air/                    # Servicios de calidad del aire
│   ├── AirQualityService.java
│   └── OpenAQService.java
├── strava/                 # Integración con Strava
│   ├── StravaAuthService.java
│   ├── StravaActivityService.java
│   ├── StravaToken.java
│   └── StravaTokenRepository.java
└── infrastructure/web/
    ├── controller/         # Controladores REST
    │   ├── AirController.java
    │   └── StravaController.java
    └── dto/                # Data Transfer Objects
        ├── PolylineUtil.java
        ├── RouteGeoJsonDTO.java
        └── ...
```

### Frontend (Vanilla JS)

```
src/main/resources/static/
├── index.html              # Página principal con mapa
├── login.html              # Página de autenticación Strava
├── code.js                 # Lógica de la aplicación
└── css/
    └── index.css           # Estilos
```

---

## 🔌 API Endpoints Principales

### Calidad del Aire

```
GET /api/v1/air/quality/city/{cityId}
GET /api/v1/air/quality/hotspots?lat={lat}&lon={lon}
```

### Strava

```
GET /api/v1/strava/auth/login
GET /api/v1/strava/auth/callback
GET /api/v1/strava/routes/geojson?athleteId={id}&city={ciudad}
GET /api/v1/strava/activities/city?athleteId={id}&city={ciudad}
```

Ver [docs/API.md](docs/API.md) para documentación completa.

---

## 🎨 Tecnologías

### Backend
- **Spring Boot 3.3.3** - Framework principal
- **Spring Data JPA** - Persistencia
- **H2 Database** - BD en memoria (desarrollo)
- **RestTemplate** - Cliente HTTP

### Frontend
- **Leaflet.js** - Mapas interactivos
- **OpenStreetMap** - Tiles de mapa
- **Font Awesome** - Iconos
- **Vanilla JavaScript** - Sin frameworks

### APIs Externas
- **WAQI** - Datos de calidad del aire
- **OpenAQ** - Hotspots de contaminación
- **Strava API** - Actividades deportivas
- **Nominatim** - Geocodificación

---

## 🧪 Testing

```bash
# Ejecutar todos los tests
./gradlew test

# Ver reporte
./gradlew test
open build/reports/tests/test/index.html
```

**22 tests automatizados** cubriendo:
- Decodificación de polylines
- Agrupación de rutas
- Endpoints REST
- Lógica de negocio

Ver [docs/TESTS.md](docs/TESTS.md) para guía completa.

---

## 🔐 Seguridad

- ✅ OAuth 2.0 con Strava
- ✅ Protección CSRF con state parameter
- ✅ Tokens almacenados de forma segura en BD
- ✅ Renovación automática de tokens
- ✅ Client secret nunca expuesto al frontend
- ✅ Validación de entrada en todos los endpoints

---

## 📊 Base de Datos

En desarrollo se usa **H2** (en memoria):

```sql
-- Tabla de tokens de Strava
strava_tokens (
  id, athlete_id, access_token, refresh_token,
  expires_at, first_name, last_name, username,
  city, state, country, created_at, updated_at
)
```

Para producción, migrar a PostgreSQL.

---

## 🚀 Despliegue

### Desarrollo
```bash
./gradlew bootRun
```

### Producción

1. Configurar PostgreSQL
2. Actualizar `application.yml`
3. Configurar variables de entorno
4. Habilitar HTTPS
5. Actualizar callback URL en Strava

---

## 🎨 Escala de Colores AQI

| AQI | Color | Estado |
|-----|-------|--------|
| 0-50 | 🟢 Verde | Good |
| 51-100 | 🟡 Amarillo | Moderate |
| 101-150 | 🟠 Naranja | Unhealthy for Sensitive Groups |
| 151-200 | 🔴 Rojo | Unhealthy |
| 201-300 | 🟣 Morado | Very Unhealthy |
| 301+ | 🟤 Marrón | Hazardous |

---

## 🤝 Contribuir

1. Fork el proyecto
2. Crea una rama (`git checkout -b feature/AmazingFeature`)
3. Commit tus cambios (`git commit -m 'Add AmazingFeature'`)
4. Push a la rama (`git push origin feature/AmazingFeature`)
5. Abre un Pull Request

---

## 📝 Licencia

Este proyecto está bajo la Licencia MIT.

---

## 👥 Equipo

**Fighting Nerds** - Proyecto Aether

---

## 🙏 Agradecimientos

- World Air Quality Index Project
- Strava API
- OpenStreetMap / Nominatim
- Leaflet.js
- OpenAQ

---

## 📞 Soporte

¿Problemas? Consulta:
1. [Troubleshooting](docs/TROUBLESHOOTING.md)
2. [API Docs](docs/API.md)
3. Abre un issue en GitHub

---

**Última actualización:** 4 de Octubre de 2025
