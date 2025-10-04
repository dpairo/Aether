# Solución de Problemas con IntelliJ IDEA

## Problema: Error de ejecución en IntelliJ
Si estás experimentando errores al ejecutar la aplicación desde IntelliJ IDEA, sigue estos pasos:

### 1. **Verificar Configuración del Proyecto**
- Asegúrate de que IntelliJ reconoce el proyecto como un proyecto Gradle
- Ve a `File > Project Structure > Project` y verifica que el SDK de Java esté configurado correctamente
- El SDK debe ser Java 17 o superior

### 2. **Refrescar el Proyecto Gradle**
```bash
# Desde la terminal en el directorio del proyecto
./gradlew clean build
```
Luego en IntelliJ:
- Haz clic derecho en el archivo `build.gradle`
- Selecciona "Reload Gradle Project"

### 3. **Configurar Run Configuration**
1. Ve a `Run > Edit Configurations...`
2. Crea una nueva configuración de tipo "Application"
3. Configura:
   - **Name**: AetherApplication
   - **Main class**: `com.aether.app.AetherApplication`
   - **Module**: Aether.main
   - **VM options**: `-Dspring.profiles.active=default`
   - **Working directory**: `$PROJECT_DIR$`

### 4. **Verificar Variables de Entorno**
Si planeas usar datos reales de WAQI:
```bash
export WAQI_TOKEN=tu_token_aqui
```

### 5. **Limpiar Cache de IntelliJ**
- `File > Invalidate Caches and Restart...`
- Selecciona "Invalidate and Restart"

### 6. **Verificar Puerto 8080**
Si el puerto está ocupado:
```bash
# Encontrar proceso usando el puerto
lsof -ti:8080

# Matar el proceso (reemplaza PID con el número real)
kill -9 PID
```

### 7. **Ejecutar desde Terminal (Alternativa)**
Si IntelliJ sigue dando problemas, puedes ejecutar desde terminal:
```bash
./gradlew bootRun
```

### 8. **Verificar Dependencias**
Asegúrate de que todas las dependencias estén descargadas:
```bash
./gradlew dependencies
```

## Estado Actual del Proyecto
✅ **Compilación**: 100% exitosa  
✅ **APIs**: Funcionando correctamente  
✅ **Estructura**: Packages corregidos  
✅ **Documentación**: Completa  

## APIs Disponibles
- `GET /api/v1/air/quality/cities` - Todas las ciudades españolas
- `GET /api/v1/air/quality/city/{cityId}` - Ciudad específica

## Próximos Pasos
1. Configurar token de WAQI para datos reales
2. Integrar con el frontend para el mapa
3. Implementar cache para mejorar rendimiento
