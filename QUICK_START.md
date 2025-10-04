# 🚀 Guía de Inicio Rápido - Aether + Strava

## ⏱️ Setup en 5 minutos

### Paso 1: Crear App en Strava (2 min)

1. Ve a: https://www.strava.com/settings/api
2. Click en "Create App"
3. Rellena:
   ```
   Application Name: Aether
   Category: Visualizer
   Website: http://localhost:8080
   Authorization Callback Domain: localhost
   ```
4. **Copia tu Client ID y Client Secret**

### Paso 2: Configurar Credenciales (1 min)

**Opción A - Script automático:**
```bash
./setup-strava.sh
```

**Opción B - Manual:**
```bash
export STRAVA_CLIENT_ID="tu_client_id_aqui"
export STRAVA_CLIENT_SECRET="tu_client_secret_aqui"
export STRAVA_REDIRECT_URI="http://localhost:8080/api/v1/strava/auth/callback"
```

### Paso 3: Ejecutar Aplicación (1 min)

```bash
./gradlew bootRun
```

Espera a ver:
```
Started AetherApplication in X.XXX seconds
```

### Paso 4: Probar (1 min)

1. Abre: http://localhost:8080
2. Click en "Connect with Strava"
3. Autoriza en Strava
4. ¡Listo! 🎉

## 🧪 Verificación Rápida

```bash
# Health check
curl http://localhost:8080/api/v1/strava/health

# Calidad del aire
curl http://localhost:8080/api/v1/air/quality/provinces
```

## 📂 Archivos Importantes

```
Aether/
├── src/main/java/com/aether/app/
│   ├── strava/                         # 🆕 Autenticación Strava
│   │   ├── StravaAuthService.java      # Lógica OAuth
│   │   ├── StravaToken.java            # Entidad de tokens
│   │   └── StravaTokenRepository.java  # Persistencia
│   └── infrastructure/web/
│       ├── controller/
│       │   └── StravaController.java   # 🆕 Endpoints REST
│       └── dto/
│           ├── StravaAthleteDTO.java   # 🆕 DTOs
│           ├── StravaTokenResponseDTO.java
│           ├── StravaAuthResponseDTO.java
│           └── StravaErrorDTO.java
│
├── src/main/resources/
│   ├── application.yml                 # ✏️ Configuración Strava
│   └── static/
│       ├── login.html                  # 🆕 Página de login
│       ├── main.js                     # ✏️ Funciones auth
│       └── index.html                  # ✏️ Link a login
│
├── STRAVA_INTEGRATION.md              # 🆕 Documentación completa
├── API_EXAMPLES.md                    # 🆕 Ejemplos de uso
├── IMPLEMENTATION_SUMMARY.md          # 🆕 Resumen técnico
├── setup-strava.sh                    # 🆕 Script de setup
└── .gitignore                         # ✏️ Protección .env

🆕 = Archivo nuevo
✏️ = Archivo modificado
```

## 🎯 Endpoints Principales

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/v1/strava/auth/login` | Inicia OAuth |
| GET | `/api/v1/strava/auth/callback` | Callback OAuth |
| GET | `/api/v1/strava/auth/me` | Info usuario |
| POST | `/api/v1/strava/auth/logout` | Cerrar sesión |
| GET | `/api/v1/air/quality/provinces` | Calidad aire |
| POST | `/api/v1/location/consent` | Guardar ubicación |

## ⚠️ Troubleshooting Express

### "Invalid redirect_uri"
```bash
# Verifica que el redirect URI en Strava sea exactamente:
http://localhost:8080/api/v1/strava/auth/callback

# Y que "Authorization Callback Domain" sea:
localhost
```

### "Port 8080 already in use"
```bash
# Cambiar puerto en application.yml:
server:
  port: 8081

# Y actualizar redirect URI en Strava
```

### "Variables de entorno no cargadas"
```bash
# Asegúrate de cargar .env ANTES de ejecutar:
source .env
./gradlew bootRun
```

### "Cannot find bean StravaAuthService"
```bash
# Recargar proyecto Gradle en tu IDE
# O ejecutar:
./gradlew clean build
```

## 📚 Más Información

- **Documentación completa**: [STRAVA_INTEGRATION.md](STRAVA_INTEGRATION.md)
- **Ejemplos de código**: [API_EXAMPLES.md](API_EXAMPLES.md)
- **Detalles técnicos**: [IMPLEMENTATION_SUMMARY.md](IMPLEMENTATION_SUMMARY.md)
- **README general**: [README.md](README.md)

## 🎓 Conceptos Clave

- **OAuth 2.0**: Protocolo de autorización usado por Strava
- **Access Token**: Token temporal para hacer requests (válido 6h)
- **Refresh Token**: Token para renovar el access token
- **Athlete ID**: Identificador único del usuario en Strava
- **AQI**: Air Quality Index (índice de calidad del aire)

## 🔑 Variables de Entorno Requeridas

```bash
STRAVA_CLIENT_ID=123456          # De tu app en Strava
STRAVA_CLIENT_SECRET=abc123...   # De tu app en Strava
STRAVA_REDIRECT_URI=http://...   # Callback URL
```

## 🎨 Tecnologías

- **Backend**: Spring Boot 3.3.3 + Java 17
- **Frontend**: Vanilla JS + Leaflet.js
- **Database**: H2 (dev) → PostgreSQL (prod)
- **APIs**: Strava + WAQI + Nominatim

## ✅ Checklist de Inicio

- [ ] Crear app en Strava
- [ ] Copiar Client ID y Secret
- [ ] Ejecutar `./setup-strava.sh` (o configurar manualmente)
- [ ] Ejecutar `./gradlew bootRun`
- [ ] Abrir http://localhost:8080
- [ ] Hacer login con Strava
- [ ] Ver mapa con calidad del aire

## 💡 Tips

1. **Logs**: Para ver logs detallados, usa `--info` o `--debug`
2. **Base de datos**: Accede a H2 console en `/h2-console`
3. **Hot reload**: Usa Spring DevTools para desarrollo
4. **Postman**: Importa los endpoints para testing
5. **CORS**: Si usas frontend separado, configura CORS

## 🚀 Siguiente Nivel

Una vez funcionando:

1. ✅ Obtener actividades de Strava
2. ✅ Visualizar rutas en el mapa
3. ✅ Cruzar datos con calidad del aire
4. ✅ Crear recomendaciones inteligentes
5. ✅ Añadir notificaciones

Ver [IMPLEMENTATION_SUMMARY.md](IMPLEMENTATION_SUMMARY.md) para ideas.

---

**¿Problemas?** Revisa [STRAVA_INTEGRATION.md](STRAVA_INTEGRATION.md) → Sección Troubleshooting


