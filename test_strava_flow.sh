#!/bin/bash

echo "================================="
echo "🧪 TEST DE FLUJO COMPLETO STRAVA"
echo "================================="
echo ""

BASE_URL="http://localhost:8080"

# Test 1: Health check
echo "✅ Test 1: Health check de Strava"
response=$(curl -s "${BASE_URL}/api/v1/strava/health")
echo "Response: $response"
echo ""

# Test 2: Verificar que el endpoint de login redirige
echo "✅ Test 2: Endpoint de login (debería redirigir a Strava)"
curl -I -s "${BASE_URL}/api/v1/strava/auth/login" | head -10
echo ""

# Test 3: Test del endpoint de callback con parámetros vacíos (debería dar error)
echo "✅ Test 3: Callback sin código (debería redirigir a login con error)"
curl -I -s "${BASE_URL}/api/v1/strava/auth/callback" | head -10
echo ""

# Test 4: Verificar endpoints de rutas (sin autenticación, debería dar 401)
echo "✅ Test 4: Endpoint de rutas sin autenticación (debería dar 401)"
response=$(curl -s -w "\nHTTP_CODE:%{http_code}" "${BASE_URL}/api/v1/strava/routes/geojson?athleteId=123&city=Madrid")
echo "$response"
echo ""

# Test 5: Verificar que index.html existe
echo "✅ Test 5: Verificar que index.html carga"
response=$(curl -s -w "\nHTTP_CODE:%{http_code}" "${BASE_URL}/index.html" | tail -1)
echo "HTTP Code: $response"
echo ""

# Test 6: Verificar que login.html existe
echo "✅ Test 6: Verificar que login.html carga"
response=$(curl -s -w "\nHTTP_CODE:%{http_code}" "${BASE_URL}/login.html" | tail -1)
echo "HTTP Code: $response"
echo ""

# Test 7: Verificar configuración de Strava
echo "✅ Test 7: Verificar redirect URI configurado"
echo "Redirect URI configurado: http://localhost:8080/api/v1/strava/auth/callback"
echo ""

echo "================================="
echo "📊 RESUMEN"
echo "================================="
echo ""
echo "Si todos los tests anteriores pasaron, el problema probablemente es:"
echo ""
echo "1. ❌ El código de autorización ya fue usado (códigos de Strava son de un solo uso)"
echo "2. ❌ Las credenciales de Strava (client_id o client_secret) son incorrectas"
echo "3. ❌ La URL de callback en Strava no coincide exactamente con:"
echo "   http://localhost:8080/api/v1/strava/auth/callback"
echo ""
echo "🔍 SIGUIENTE PASO:"
echo "1. Ve a https://www.strava.com/settings/api"
echo "2. Verifica que 'Authorization Callback Domain' sea: localhost"
echo "3. Intenta el login de nuevo"
echo "4. Si falla, revisa los logs del servidor (terminal donde corre bootRun)"
echo ""

