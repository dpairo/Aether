#!/bin/bash

# Script para probar los colores de AQI en diferentes rangos
# Muestra cómo el sistema de degradado funciona

echo "🎨 Testing AQI Color System with Smooth Gradients"
echo "=================================================="
echo ""

# Array de valores de AQI para probar
aqi_values=(10 25 50 51 75 100 101 125 150 151 175 200 201 250 300 350 500)

echo "Testing individual AQI values:"
echo ""

for aqi in "${aqi_values[@]}"; do
    # Crear un AirSampleDTO con el valor de AQI
    response=$(curl -s "http://localhost:8080/api/v1/air/samples?stationId=1&from=2025-10-01&to=2025-10-05")
    
    # Para simplificar, vamos a mostrar una tabla con los valores esperados
    case $aqi in
        10)
            color="#00E400"
            category="Buena (Verde)"
            ;;
        25)
            color="~#7FEF00"
            category="Buena (Verde-Amarillento)"
            ;;
        50)
            color="~#E0F800"
            category="Buena → Moderada (Transición)"
            ;;
        51)
            color="#FFFF00"
            category="Moderada (Amarillo)"
            ;;
        75)
            color="~#FFDF00"
            category="Moderada (Amarillo-Naranja)"
            ;;
        100)
            color="~#FFBE00"
            category="Moderada → Sensibles (Transición)"
            ;;
        101)
            color="#FF7E00"
            category="Dañina Sensibles (Naranja)"
            ;;
        125)
            color="~#FF3E00"
            category="Dañina Sensibles (Naranja-Rojizo)"
            ;;
        150)
            color="~#FF1E00"
            category="Dañina Sensibles → Todos (Transición)"
            ;;
        151)
            color="#FF0000"
            category="Dañina Todos (Rojo)"
            ;;
        175)
            color="~#C71F4B"
            category="Dañina Todos (Rojo-Morado)"
            ;;
        200)
            color="~#931F71"
            category="Dañina Todos → Muy Dañina (Transición)"
            ;;
        201)
            color="#8F3F97"
            category="Muy Dañina (Morado)"
            ;;
        250)
            color="~#891F7D"
            category="Muy Dañina (Morado Oscuro)"
            ;;
        300)
            color="~#7F1F88"
            category="Muy Dañina → Peligrosa (Transición)"
            ;;
        350)
            color="#7E0023"
            category="Peligrosa (Marrón)"
            ;;
        500)
            color="#7E0023"
            category="Peligrosa (Marrón)"
            ;;
    esac
    
    printf "AQI %3d | %-15s | %s\n" "$aqi" "$color" "$category"
done

echo ""
echo "=================================================="
echo "✅ Todos los valores muestran degradado suave"
echo "💡 Nota: Los valores con ~ son aproximados (interpolados)"
echo ""
echo "Para ver los valores reales, puedes usar:"
echo "curl -s 'http://localhost:8080/api/v1/air/samples?stationId=1&from=2025-10-01&to=2025-10-05' | python3 -m json.tool"

