#!/bin/bash

# Script para configurar las credenciales de Strava
# Uso: ./setup-strava.sh

echo "==================================="
echo "  AETHER - Configuración de Strava"
echo "==================================="
echo ""

# Verificar si .env ya existe
if [ -f .env ]; then
    echo "⚠️  El archivo .env ya existe."
    read -p "¿Deseas sobrescribirlo? (y/n): " overwrite
    if [ "$overwrite" != "y" ]; then
        echo "Cancelado."
        exit 0
    fi
fi

echo "Obtén tus credenciales en: https://www.strava.com/settings/api"
echo ""

# Solicitar Client ID
read -p "Ingresa tu STRAVA_CLIENT_ID: " client_id
if [ -z "$client_id" ]; then
    echo "❌ Error: Client ID no puede estar vacío"
    exit 1
fi

# Solicitar Client Secret
read -sp "Ingresa tu STRAVA_CLIENT_SECRET: " client_secret
echo ""
if [ -z "$client_secret" ]; then
    echo "❌ Error: Client Secret no puede estar vacío"
    exit 1
fi

# Solicitar Redirect URI (con valor por defecto)
read -p "Ingresa el REDIRECT_URI [http://localhost:8080/api/v1/strava/auth/callback]: " redirect_uri
redirect_uri=${redirect_uri:-http://localhost:8080/api/v1/strava/auth/callback}

# Solicitar WAQI Token (opcional)
read -p "Ingresa tu WAQI_TOKEN [demo]: " waqi_token
waqi_token=${waqi_token:-demo}

# Crear archivo .env
cat > .env << EOF
# Strava OAuth Configuration
# Generated on $(date)

STRAVA_CLIENT_ID=$client_id
STRAVA_CLIENT_SECRET=$client_secret
STRAVA_REDIRECT_URI=$redirect_uri

# WAQI API Token
WAQI_TOKEN=$waqi_token
EOF

echo ""
echo "✅ Archivo .env creado exitosamente!"
echo ""
echo "Ahora puedes ejecutar la aplicación con:"
echo "  source .env"
echo "  ./gradlew bootRun"
echo ""
echo "⚠️  IMPORTANTE: Asegúrate de que en tu aplicación de Strava:"
echo "   - Authorization Callback Domain incluya: localhost"
echo "   - La aplicación esté en modo desarrollo"
echo ""


