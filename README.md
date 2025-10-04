# 🌬️ AETHER - Air Quality Visualization Platform

**Breathe Better, Run Smarter**

Aether es una plataforma web que visualiza datos de calidad del aire en tiempo real, diseñada especialmente para runners y atletas que desean optimizar sus entrenamientos según las condiciones ambientales.

## ✨ Características

- 🗺️ **Visualización de calidad del aire** en tiempo real sobre mapa interactivo
- 🏃 **Integración con Strava** para datos personalizados de actividades
- 📍 **Geolocalización** para obtener datos del aire en tu ubicación
- 🌈 **Código de colores AQI** (Air Quality Index) estándar
- 🚦 **Detección de contaminantes** dominantes por zona
- 📊 **Información detallada** por provincias y ciudades de España

## 🚀 Inicio Rápido

### Prerequisitos

- Java 17 o superior
- Gradle 7.x o superior
- Cuenta de Strava (para autenticación)

### Instalación

1. **Clonar el repositorio**
   ```bash
   git clone https://github.com/tuusuario/Aether.git
   cd Aether
   ```

2. **Configurar Strava OAuth**
   
   Ejecuta el script de configuración:
   ```bash
   ./setup-strava.sh
   ```
   
   O configura manualmente las variables de entorno:
   ```bash
   export STRAVA_CLIENT_ID="tu_client_id"
   export STRAVA_CLIENT_SECRET="tu_client_secret"
   export STRAVA_REDIRECT_URI="http://localhost:8080/api/v1/strava/auth/callback"
   ```
   
   Para obtener tus credenciales de Strava:
   - Ve a [https://www.strava.com/settings/api](https://www.strava.com/settings/api)
   - Crea una nueva aplicación
   - Copia el Client ID y Client Secret

3. **Ejecutar la aplicación**
   ```bash
   source .env  # Solo si usaste el script de configuración
   ./gradlew bootRun
   ```

4. **Abrir en el navegador**
   ```
   http://localhost:8080
   ```

## 📖 Documentación

- [Integración con Strava](STRAVA_INTEGRATION.md) - Guía completa de autenticación OAuth
- [API Documentation](API_DOCUMENTATION.md) - Endpoints y contratos de la API

## 🏗️ Arquitectura

### Backend (Spring Boot)

```
src/main/java/com/aether/app/
├── air/                    # Servicios de calidad del aire
│   └── AirQualityService.java
├── location/               # Servicios de localización
│   ├── LocationService.java
│   ├── ReverseGeocodingService.java
│   └── LocationConsent.java
├── strava/                 # Integración con Strava
│   ├── StravaAuthService.java
│   ├── StravaToken.java
│   └── StravaTokenRepository.java
└── infrastructure/
    └── web/
        ├── controller/     # Controladores REST
        │   ├── AirController.java
        │   ├── LocationController.java
        │   └── StravaController.java
        └── dto/            # Data Transfer Objects
            ├── StravaAthleteDTO.java
            ├── StravaTokenResponseDTO.java
            └── ...
```

### Frontend (Vanilla JS)

```
src/main/resources/static/
├── index.html              # Página principal con mapa
├── login.html              # Página de autenticación Strava
├── main.js                 # Lógica de la aplicación
└── css/
    └── index.css           # Estilos
```

## 🔌 API Endpoints

### Calidad del Aire

- `GET /api/v1/air/quality/provinces` - Obtener AQI de todas las provincias
- `GET /api/v1/air/quality/city/{city}` - Obtener AQI de una ciudad específica
- `GET /api/v1/air/stations` - Obtener todas las estaciones de medición

### Localización

- `POST /api/v1/location/consent` - Guardar consentimiento de ubicación
- `GET /api/v1/location/latest` - Obtener última ubicación con consentimiento
- `POST /api/v1/location/revoke/{id}` - Revocar consentimiento

### Strava OAuth

- `GET /api/v1/strava/auth/login` - Iniciar flujo de autenticación
- `GET /api/v1/strava/auth/callback` - Callback de OAuth
- `GET /api/v1/strava/auth/me` - Obtener usuario autenticado
- `POST /api/v1/strava/auth/logout` - Cerrar sesión

## 🎨 Tecnologías

### Backend
- **Spring Boot 3.3.3** - Framework principal
- **Spring Data JPA** - Persistencia de datos
- **H2 Database** - Base de datos en memoria (desarrollo)
- **Spring Validation** - Validación de datos
- **RestTemplate** - Cliente HTTP para APIs externas

### Frontend
- **Leaflet.js** - Mapas interactivos
- **OpenStreetMap** - Tiles de mapa
- **Font Awesome** - Iconos
- **Vanilla JavaScript** - Sin frameworks

### APIs Externas
- **WAQI (World Air Quality Index)** - Datos de calidad del aire
- **Strava API** - Autenticación y datos de actividades
- **Nominatim (OpenStreetMap)** - Geocodificación inversa

## 🔐 Seguridad

- ✅ OAuth 2.0 con Strava
- ✅ Protección CSRF con parámetro state
- ✅ Tokens almacenados de forma segura
- ✅ Renovación automática de tokens expirados
- ✅ Validación de entrada en todos los endpoints

## 🧪 Testing

Ejecutar tests:
```bash
./gradlew test
```

Ver reporte de tests:
```bash
./gradlew test
open build/reports/tests/test/index.html
```

## 📊 Base de Datos

En desarrollo se usa H2 (en memoria). La base de datos incluye:

- `location_consent` - Consentimientos de ubicación
- `strava_tokens` - Tokens de autenticación de Strava

Para ver la consola H2 en desarrollo:
```
http://localhost:8080/h2-console
```

Credenciales:
- JDBC URL: `jdbc:h2:mem:devdb`
- Usuario: `sa`
- Password: *(vacío)*

## 🚀 Producción

Para desplegar en producción:

1. Cambiar a PostgreSQL en `application.yml`
2. Configurar variables de entorno seguras
3. Habilitar HTTPS
4. Configurar dominio en Strava API settings
5. Actualizar `STRAVA_REDIRECT_URI`

## 🤝 Contribuir

1. Fork el proyecto
2. Crea una rama para tu feature (`git checkout -b feature/AmazingFeature`)
3. Commit tus cambios (`git commit -m 'Add some AmazingFeature'`)
4. Push a la rama (`git push origin feature/AmazingFeature`)
5. Abre un Pull Request

## 📝 Licencia

Este proyecto está bajo la Licencia MIT.

## 👥 Equipo

**Fighting Nerds** - Proyecto Aether

## 🙏 Agradecimientos

- World Air Quality Index Project por los datos de calidad del aire
- Strava por su API de actividades deportivas
- OpenStreetMap por los datos de mapas
- Leaflet.js por la biblioteca de mapas

---

**¿Preguntas?** Abre un issue en GitHub o contacta al equipo.

