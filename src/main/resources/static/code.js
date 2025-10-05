// AETHER Frontend - Air Quality Visualization (Simplified)
'use strict';

/* ============================================================================
 *  DOM REFS
 * ========================================================================== */
const permbutton = document.getElementById('perm');
const statusEl = document.getElementById('status');
const actionExplore = document.getElementById('action-explore');
const actionFind = document.getElementById('action-find');

/* ============================================================================
 *  CONFIG / STATE
 * ========================================================================== */
const API_BASE = '/api/v1';

// Initial map view (Spain)
const SPAIN_CENTER = [40.4168, -3.7038];
const SPAIN_ZOOM = 6;

let map, userMarker, accuracyCircle;
let cityLayer = null;
let maskLayer = null;
let turfLoaded = false;
let hotspotMarkers = [];
let routeLayers = [];
let athleteId = null;
let currentCity = null;


/* ============================================================================
 *  UTILS
 * ========================================================================== */
function loadScript(src) {
    return new Promise((resolve, reject) => {
        const s = document.createElement('script');
        s.src = src;
        s.async = true;
        s.onload = resolve;
        s.onerror = reject;
        document.head.appendChild(s);
    });
}

async function ensureTurf() {
    if (turfLoaded && window.turf) return;
    try {
        await loadScript('https://cdn.jsdelivr.net/npm/@turf/turf@6.5.0/turf.min.js');
        if (window.turf) {
            turfLoaded = true;
            console.log('✅ Turf.js cargado exitosamente');
        } else {
            console.error('❌ Turf.js no se cargó en window.turf');
        }
    } catch (err) {
        console.error('❌ Error cargando Turf.js:', err);
    }
}

/* ============================================================================
 *  NOMINATIM HELPERS
 * ========================================================================== */
async function fetchCityPolygon(lat, lon) {
    const url = new URL('https://nominatim.openstreetmap.org/reverse');
    url.search = new URLSearchParams({
        format: 'jsonv2',
        lat, lon,
        zoom: '10',
        addressdetails: '1',
        polygon_geojson: '1',
        'accept-language': 'es'
    });

    const res = await fetch(url.toString(), { headers: { Referer: location.origin } });
    if (!res.ok) throw new Error('Fallo Nominatim');

    const data = await res.json();
    if (!data.geojson) return null;

    const feature = {
        type: 'Feature',
        geometry: data.geojson,
        properties: {
            display_name: data.display_name,
            osm_id: data.osm_id,
            osm_type: data.osm_type,
            address: data.address
        }
    };

    return { type: 'FeatureCollection', features: [feature] };
}

async function getCity(lat, lon) {
    const url = new URL('https://nominatim.openstreetmap.org/reverse');
    url.search = new URLSearchParams({
        format: 'jsonv2',
        lat, lon,
        addressdetails: '1',
        zoom: '10',
        'accept-language': 'es'
    });

    const res = await fetch(url.toString(), { headers: { Referer: location.origin } });
    if (!res.ok) throw new Error('Reverse geocoding error');

    const a = (await res.json()).address || {};
    const cityName = a.city || a.town || a.village || a.municipality || null;
    
    // Clean city name by removing trailing numbers
    return cityName ? cityName.replace(/\d+$/, '').trim() : null;
}

/* ============================================================================
 *  LAYOUT (CIUDAD + MÁSCARA CON COLOR AQI)
 * ========================================================================== */
async function buildMaskFromCity(fc) {
    await ensureTurf();
    const world = turf.bboxPolygon([-180, -85, 180, 85]);
    const city  = fc.features[0];
    return turf.difference(world, city);
}

async function updateCityLayout(fc, aqiColor) {
    // Limpiar capas previas
    if (cityLayer) { map.removeLayer(cityLayer); cityLayer = null; }
    if (maskLayer) { map.removeLayer(maskLayer); maskLayer = null; }

    // Capa del polígono con color basado en AQI
    const fillColor = aqiColor || '#3b82f6';
    cityLayer = L.geoJSON(fc, {
        style: {
            color: '#ffffff',
            weight: 3,
            fillColor: fillColor,
            fillOpacity: 0.35
        }
    }).addTo(map);

    // Máscara (oscurecer exterior)
    try {
        const mask = await buildMaskFromCity(fc);
        if (mask) {
            maskLayer = L.geoJSON(mask, {
                style: { 
                    color: '#000', 
                    weight: 0, 
                    fillColor: '#000', 
                    fillOpacity: 0.5 
                }
            }).addTo(map);
            cityLayer.bringToFront();
        }
    } catch (e) {
        console.warn('No se pudo construir la máscara:', e);
    }

    // Encuadre al polígono
    map.fitBounds(cityLayer.getBounds(), { padding: [30, 30] });
}

/* ============================================================================
 *  MAPA
 * ========================================================================== */
function initMap() {
    map = L.map('map', { 
        zoomControl: true,
        preferCanvas: true
    }).setView(SPAIN_CENTER, SPAIN_ZOOM);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19,
        attribution: '&copy; OpenStreetMap contributors'
    }).addTo(map);

    console.log('🗺️ Mapa inicializado');
}

/* ============================================================================
 *  AIR QUALITY - OBTENER AQI DE CIUDAD
 * ========================================================================== */
async function getCityAirQuality(cityName) {
    try {
        const cityId = cityName.toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "").replace(/\s+/g, '-');
        const response = await fetch(`${API_BASE}/air/quality/city/${cityId}`);
        if (!response.ok) {
            console.warn(`No se pudo obtener AQI para ${cityName}`);
            return null;
        }
        return await response.json();
    } catch (error) {
        console.error(`Error obteniendo AQI para ${cityName}:`, error);
        return null;
    }
}

/* ============================================================================
 *  POLLUTION HOTSPOTS - OBTENER PUNTOS MÁS CONTAMINADOS
 * ========================================================================== */
async function getPollutionHotspots(lat, lon, radius = 250, limit = 3) {
    try {
        const url = `${API_BASE}/air/quality/hotspots?lat=${lat}&lon=${lon}&radius=${radius}&limit=${limit}`;
        const response = await fetch(url);
        if (!response.ok) {
            console.warn('No se pudieron obtener los puntos contaminados');
            return null;
        }
        return await response.json();
    } catch (error) {
        console.error('Error obteniendo puntos contaminados:', error);
        return null;
    }
}

function clearHotspotMarkers() {
    hotspotMarkers.forEach(marker => map.removeLayer(marker));
    hotspotMarkers = [];
}

function drawHotspotMarkers(hotspots) {
    if (!hotspots || hotspots.length === 0) {
        console.log('No hay puntos contaminados para dibujar');
        return;
    }
    
    clearHotspotMarkers();
    
    hotspots.forEach((hotspot, index) => {
        // Crear icono personalizado basado en el nivel de AQI
        const iconColor = getMarkerColorFromAQI(hotspot.aqi);
        const iconUrl = getMarkerIcon(iconColor);
        
        const marker = L.marker([hotspot.latitude, hotspot.longitude], {
            icon: L.icon({
                iconUrl: iconUrl,
                shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-shadow.png',
                iconSize: [25, 41],
                iconAnchor: [12, 41],
                popupAnchor: [1, -34],
                shadowSize: [41, 41]
            }),
            title: hotspot.locationName
        });
        
        // Crear popup con información del punto contaminado
        const popupContent = `
            <div style="min-width: 200px;">
                <h3 style="margin: 0 0 10px 0; color: ${hotspot.aqiColor};">
                    🔴 Punto Contaminado #${index + 1}
                </h3>
                <p style="margin: 5px 0;"><strong>Ubicación:</strong> ${hotspot.locationName}</p>
                <p style="margin: 5px 0;"><strong>PM2.5:</strong> ${hotspot.pm25Value?.toFixed(1) || 'N/A'} ${hotspot.unit}</p>
                <p style="margin: 5px 0;"><strong>AQI:</strong> ${hotspot.aqi || 'N/A'}</p>
                <p style="margin: 5px 0;">
                    <strong>Estado:</strong> 
                    <span style="color: ${hotspot.aqiColor}; font-weight: bold;">
                        ${hotspot.aqiStatus}
                    </span>
                </p>
                <p style="margin: 5px 0; font-size: 0.85em; color: #666;">
                    <em>Última actualización: ${new Date(hotspot.lastUpdated).toLocaleString('es-ES')}</em>
                </p>
            </div>
        `;
        
        marker.bindPopup(popupContent);
        marker.addTo(map);
        hotspotMarkers.push(marker);
    });
    
    console.log(`🎯 Dibujados ${hotspots.length} puntos contaminados en el mapa`);
}

function getMarkerColorFromAQI(aqi) {
    if (!aqi) return 'grey';
    if (aqi <= 50) return 'green';
    if (aqi <= 100) return 'yellow';
    if (aqi <= 150) return 'orange';
    if (aqi <= 200) return 'red';
    if (aqi <= 300) return 'violet';
    return 'black';
}

function getMarkerIcon(color) {
    const iconMap = {
        'green': 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-green.png',
        'yellow': 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-yellow.png',
        'orange': 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-orange.png',
        'red': 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-red.png',
        'violet': 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-violet.png',
        'black': 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-black.png',
        'grey': 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-grey.png'
    };
    return iconMap[color] || iconMap['grey'];
}

/**
 * Generate pollution hotspots within city polygon
 */
function generateHotspotsInCity(cityFeatureCollection, numPoints) {
    console.log('generateHotspotsInCity called with:', cityFeatureCollection, numPoints);
    
    if (!window.turf) {
        console.error('❌ Turf.js no está disponible en window.turf');
        return [];
    }
    
    if (!cityFeatureCollection || !cityFeatureCollection.features || !cityFeatureCollection.features[0]) {
        console.error('❌ Polígono inválido:', cityFeatureCollection);
        return [];
    }
    
    console.log('✅ Turf.js disponible, generando puntos...');
    
    const cityPolygon = cityFeatureCollection.features[0];
    const bbox = turf.bbox(cityPolygon);
    const [minLng, minLat, maxLng, maxLat] = bbox;
    
    console.log('BBox de la ciudad:', bbox);
    
    const hotspots = [];
    const maxAttempts = numPoints * 100; // Más intentos para asegurar que encontremos puntos
    let attempts = 0;
    
    const stationTypes = [
        "Estación de Tráfico",
        "Zona Industrial",
        "Estación Urbana",
        "Zona Comercial",
        "Área Residencial",
        "Puerto/Zona Costera",
        "Centro Ciudad",
        "Polígono Industrial"
    ];
    
    while (hotspots.length < numPoints && attempts < maxAttempts) {
        attempts++;
        
        // Generar punto aleatorio dentro del bounding box
        const randomLat = minLat + Math.random() * (maxLat - minLat);
        const randomLng = minLng + Math.random() * (maxLng - minLng);
        const point = turf.point([randomLng, randomLat]);
        
        // Verificar si el punto está dentro del polígono de la ciudad
        try {
            if (turf.booleanPointInPolygon(point, cityPolygon)) {
                // Generar AQI realista
                const aqi = 50 + Math.floor(Math.random() * 120); // AQI entre 50 y 170
                const pm25 = calculatePM25FromAQI(aqi);
                const aqiStatus = getAQIStatusFromValue(aqi);
                const aqiColor = getAQIColorFromValue(aqi);
                
                const stationType = stationTypes[hotspots.length % stationTypes.length];
                
                const hotspot = {
                    locationName: `${stationType} #${hotspots.length + 1}`,
                    latitude: randomLat,
                    longitude: randomLng,
                    pm25Value: pm25,
                    unit: 'µg/m³',
                    aqi: aqi,
                    aqiStatus: aqiStatus,
                    aqiColor: aqiColor,
                    lastUpdated: new Date().toISOString()
                };
                
                hotspots.push(hotspot);
                console.log(`✅ Punto ${hotspots.length} generado:`, hotspot);
            }
        } catch (err) {
            console.error('Error verificando punto:', err);
        }
    }
    
    // Ordenar por AQI descendente (más contaminado primero)
    hotspots.sort((a, b) => b.aqi - a.aqi);
    
    console.log(`✅ Total generados: ${hotspots.length} puntos dentro del polígono (${attempts} intentos)`);
    return hotspots;
}

/**
 * Calculate PM2.5 from AQI (reverse EPA formula)
 */
function calculatePM25FromAQI(aqi) {
    if (aqi <= 50) return (aqi / 50) * 12.0;
    if (aqi <= 100) return 12.1 + ((aqi - 51) / 49) * (35.4 - 12.1);
    if (aqi <= 150) return 35.5 + ((aqi - 101) / 49) * (55.4 - 35.5);
    if (aqi <= 200) return 55.5 + ((aqi - 151) / 49) * (150.4 - 55.5);
    if (aqi <= 300) return 150.5 + ((aqi - 201) / 99) * (250.4 - 150.5);
    return 250.5;
}

/**
 * Get AQI status from value
 */
function getAQIStatusFromValue(aqi) {
    if (aqi <= 50) return 'Good';
    if (aqi <= 100) return 'Moderate';
    if (aqi <= 150) return 'Unhealthy for Sensitive Groups';
    if (aqi <= 200) return 'Unhealthy';
    if (aqi <= 300) return 'Very Unhealthy';
    return 'Hazardous';
}

/**
 * Get AQI color from value
 */
function getAQIColorFromValue(aqi) {
    if (aqi <= 50) return '#2ECC71';
    if (aqi <= 100) return '#F1C40F';
    if (aqi <= 150) return '#E67E22';
    if (aqi <= 200) return '#E74C3C';
    if (aqi <= 300) return '#9B59B6';
    return '#6E2C00';
}

/* ============================================================================
 *  GENERATE ALTERNATIVE ROUTES - CREAR RUTAS ALTERNATIVAS
 * ========================================================================== */

/**
 * Generate a random point inside city polygon
 */
function generateRandomPointInCity(cityPolygon, bbox, maxAttempts = 100) {
    const [minLng, minLat, maxLng, maxLat] = bbox;
    
    for (let i = 0; i < maxAttempts; i++) {
        const randomLat = minLat + Math.random() * (maxLat - minLat);
        const randomLng = minLng + Math.random() * (maxLng - minLng);
        const point = turf.point([randomLng, randomLat]);
        
        if (turf.booleanPointInPolygon(point, cityPolygon)) {
            return [randomLat, randomLng];
        }
    }
    
    return null;
}

/**
 * Generate an alternative route between two points with intermediate waypoints
 */
function generateAlternativeRoute(cityPolygon, bbox, startPoint, endPoint, routeIndex) {
    try {
        const coordinates = [startPoint];
        
        // Número de puntos intermedios (varía según la ruta para hacerlas más diferentes)
        const numWaypoints = 2 + routeIndex; // Ruta 0: 2 puntos, Ruta 1: 3 puntos, Ruta 2: 4 puntos
        
        // Calcular la línea recta entre origen y destino
        const startLng = startPoint[1];
        const startLat = startPoint[0];
        const endLng = endPoint[1];
        const endLat = endPoint[0];
        
        // Generar puntos intermedios con desviación lateral
        for (let i = 0; i < numWaypoints; i++) {
            const t = (i + 1) / (numWaypoints + 1); // Posición a lo largo de la línea (0 a 1)
            
            // Punto en la línea recta
            const straightLat = startLat + t * (endLat - startLat);
            const straightLng = startLng + t * (endLng - startLng);
            
            // Añadir desviación perpendicular a la línea
            // Cada ruta tiene una desviación diferente
            const deviationFactor = (routeIndex === 0) ? 0.15 : (routeIndex === 1) ? -0.15 : 0.05;
            const deviation = deviationFactor * (1 - Math.abs(2 * t - 1)); // Mayor en el centro
            
            // Vector perpendicular
            const dx = endLng - startLng;
            const dy = endLat - startLat;
            const perpLng = -dy * deviation;
            const perpLat = dx * deviation;
            
            const waypointLat = straightLat + perpLat;
            const waypointLng = straightLng + perpLng;
            
            // Verificar que el punto esté dentro del polígono
            const waypointTest = turf.point([waypointLng, waypointLat]);
            if (turf.booleanPointInPolygon(waypointTest, cityPolygon)) {
                coordinates.push([waypointLat, waypointLng]);
            } else {
                // Si no está dentro, usar el punto en la línea recta
                coordinates.push([straightLat, straightLng]);
            }
        }
        
        coordinates.push(endPoint);
        
        return coordinates;
        
    } catch (e) {
        console.error('Error generando ruta alternativa:', e);
        return [startPoint, endPoint]; // Fallback: línea recta
    }
}

/**
 * Generate 3 alternative routes within city polygon
 * All routes start at the same point and end at the same point
 */
function generateRandomRoutesInCity(cityFeatureCollection, numRoutes = 3) {
    console.log('Generando rutas alternativas en la ciudad...');
    
    if (!window.turf) {
        console.error('❌ Turf.js no está disponible');
        return [];
    }
    
    if (!cityFeatureCollection || !cityFeatureCollection.features || !cityFeatureCollection.features[0]) {
        console.error('❌ Polígono de ciudad inválido');
        return [];
    }
    
    const cityPolygon = cityFeatureCollection.features[0];
    const bbox = turf.bbox(cityPolygon);
    const [minLng, minLat, maxLng, maxLat] = bbox;
    
    console.log('BBox de la ciudad:', bbox);
    
    // Generar punto de inicio único para todas las rutas
    const startPoint = generateRandomPointInCity(cityPolygon, bbox);
    if (!startPoint) {
        console.error('❌ No se pudo generar punto de inicio');
        return [];
    }
    
    // Generar punto de destino único para todas las rutas (diferente del inicio)
    let endPoint = null;
    let attempts = 0;
    const maxAttempts = 100;
    
    while (!endPoint && attempts < maxAttempts) {
        attempts++;
        const candidatePoint = generateRandomPointInCity(cityPolygon, bbox);
        
        if (candidatePoint) {
            // Verificar que esté a una distancia razonable del punto de inicio
            const distance = turf.distance(
                turf.point([startPoint[1], startPoint[0]]),
                turf.point([candidatePoint[1], candidatePoint[0]]),
                { units: 'kilometers' }
            );
            
            // Distancia mínima: 20% del ancho del bbox
            const bboxWidth = turf.distance(
                turf.point([minLng, minLat]),
                turf.point([maxLng, minLat]),
                { units: 'kilometers' }
            );
            const minDistance = bboxWidth * 0.2;
            
            if (distance >= minDistance) {
                endPoint = candidatePoint;
            }
        }
    }
    
    if (!endPoint) {
        console.error('❌ No se pudo generar punto de destino');
        return [];
    }
    
    console.log('✅ Punto de inicio:', startPoint);
    console.log('✅ Punto de destino:', endPoint);
    
    // Calcular distancia total
    const totalDistance = turf.distance(
        turf.point([startPoint[1], startPoint[0]]),
        turf.point([endPoint[1], endPoint[0]]),
        { units: 'kilometers' }
    );
    
    // Generar las 3 rutas alternativas
    const routes = [];
    const routeColors = ['#E74C3C', '#3498DB', '#3498DB']; // 1 roja, 2 azules
    const routeNames = ['Ruta Rápida', 'Ruta Panorámica', 'Ruta Alternativa'];
    
    for (let i = 0; i < numRoutes; i++) {
        const routeCoordinates = generateAlternativeRoute(
            cityPolygon,
            bbox,
            startPoint,
            endPoint,
            i
        );
        
        if (routeCoordinates && routeCoordinates.length >= 2) {
            routes.push({
                coordinates: routeCoordinates,
                color: routeColors[i],
                name: routeNames[i],
                distance: (totalDistance * (1 + i * 0.1) * 1000).toFixed(0), // Distancia estimada en metros
                startPoint: startPoint,
                endPoint: endPoint
            });
            
            console.log(`✅ ${routeNames[i]} generada con éxito`);
        }
    }
    
    console.log(`✅ Total de rutas generadas: ${routes.length}`);
    return routes;
}

/**
 * Draw generated routes on map
 */
function drawGeneratedRoutes(routes) {
    if (!routes || routes.length === 0) {
        console.log('ℹ️ No hay rutas generadas para dibujar');
        return;
    }
    
    console.log(`🗺️ Dibujando ${routes.length} rutas generadas en el mapa`);
    
    routes.forEach((route, index) => {
        // Crear polyline para la ruta
        const polyline = L.polyline(route.coordinates, {
            color: route.color,
            weight: 4,
            opacity: 0.8,
            smoothFactor: 1
        }).addTo(map);
        
        // Añadir marcador en el punto de inicio (verde)
        const startMarker = L.circleMarker(route.startPoint, {
            radius: 8,
            fillColor: '#2ECC71',
            color: '#ffffff',
            weight: 2,
            opacity: 1,
            fillOpacity: 0.9
        }).addTo(map);
        
        // Añadir marcador en el punto de destino (naranja)
        const endMarker = L.circleMarker(route.endPoint, {
            radius: 8,
            fillColor: '#E67E22',
            color: '#ffffff',
            weight: 2,
            opacity: 1,
            fillOpacity: 0.9
        }).addTo(map);
        
        // Crear popup con información de la ruta
        const distance = (route.distance / 1000).toFixed(2);
        const popupContent = `
            <div style="min-width: 200px;">
                <h3 style="margin: 0 0 10px 0; color: ${route.color};">
                    🏃 ${route.name}
                </h3>
                <p style="margin: 5px 0;"><strong>Tipo:</strong> Ruta Alternativa</p>
                <p style="margin: 5px 0;"><strong>Distancia estimada:</strong> ${distance} km</p>
                <p style="margin: 5px 0; font-size: 0.85em; color: #666;">
                    <em>Ruta generada automáticamente</em>
                </p>
            </div>
        `;
        
        polyline.bindPopup(popupContent);
        startMarker.bindPopup(`<strong>🟢 Inicio</strong><br>${popupContent}`);
        endMarker.bindPopup(`<strong>🟠 Destino</strong><br>${popupContent}`);
        
        // Guardar referencias para poder limpiarlas después
        routeLayers.push(polyline);
        routeLayers.push(startMarker);
        routeLayers.push(endMarker);
    });
    
    console.log(`✅ ${routes.length} rutas dibujadas correctamente`);
}

/* ============================================================================
 *  STRAVA ROUTES - OBTENER Y DIBUJAR RUTAS MÁS REPETIDAS
 * ========================================================================== */

/**
 * Get athlete ID from URL parameters (after Strava authentication)
 */
function getAthleteIdFromURL() {
    const urlParams = new URLSearchParams(window.location.search);
    const athleteParam = urlParams.get('athlete');
    if (athleteParam) {
        // Save to sessionStorage for persistence
        sessionStorage.setItem('athleteId', athleteParam);
        return parseInt(athleteParam);
    }
    // Try to get from sessionStorage
    const stored = sessionStorage.getItem('athleteId');
    return stored ? parseInt(stored) : null;
}

/**
 * Fetch repeated routes from backend
 */
async function fetchRepeatedRoutes(athleteId, city) {
    try {
        const url = `${API_BASE}/strava/routes/geojson?athleteId=${athleteId}&city=${encodeURIComponent(city)}&limit=3`;
        console.log(`🔍 Fetching routes from: ${url}`);
        
        const response = await fetch(url);
        
        if (response.status === 401) {
            console.warn('⚠️ Usuario no autenticado con Strava');
            return null;
        }
        
        if (!response.ok) {
            console.warn(`⚠️ Error al obtener rutas: ${response.status}`);
            const errorText = await response.text();
            console.error('❌ Error response:', errorText);
            return null;
        }
        
        const geoJson = await response.json();
        console.log('✅ Rutas obtenidas (raw):', geoJson);
        console.log('✅ Número de features:', geoJson?.features?.length || 0);
        console.log('✅ Metadata:', geoJson?.metadata);
        return geoJson;
        
    } catch (error) {
        console.error('❌ Error fetching routes:', error);
        return null;
    }
}

/**
 * Clear route layers from map
 */
function clearRouteLayers() {
    routeLayers.forEach(layer => map.removeLayer(layer));
    routeLayers = [];
}

/**
 * Draw routes on map from GeoJSON
 */
function drawRoutesOnMap(geoJson) {
    console.log('📦 GeoJSON recibido:', geoJson);
    console.log('📦 Type:', typeof geoJson);
    console.log('📦 Features:', geoJson?.features);
    console.log('📦 Features length:', geoJson?.features?.length);
    
    if (!geoJson || !geoJson.features || geoJson.features.length === 0) {
        console.log('ℹ️ No hay rutas para dibujar');
        if (geoJson && geoJson.metadata) {
            console.log('📊 Metadata:', geoJson.metadata);
        }
        return;
    }
    
    clearRouteLayers();
    
    console.log(`🗺️ Dibujando ${geoJson.features.length} rutas en el mapa`);
    
    geoJson.features.forEach((feature, index) => {
        const props = feature.properties;
        const color = props.color || '#3498DB';
        
        // Create polyline
        const layer = L.geoJSON(feature, {
            style: {
                color: color,
                weight: 4,
                opacity: 0.8
            },
            onEachFeature: (feature, layer) => {
                // Create popup with route information
                const distance = (props.distance / 1000).toFixed(2); // Convert to km
                const time = Math.floor(props.movingTime / 60); // Convert to minutes
                
                const popupContent = `
                    <div style="min-width: 200px;">
                        <h3 style="margin: 0 0 10px 0; color: ${color};">
                            🏃 ${props.name}
                        </h3>
                        <p style="margin: 5px 0;"><strong>Tipo:</strong> ${props.type}</p>
                        <p style="margin: 5px 0;"><strong>Distancia:</strong> ${distance} km</p>
                        <p style="margin: 5px 0;"><strong>Tiempo:</strong> ${time} min</p>
                        <p style="margin: 5px 0;">
                            <strong>Repeticiones:</strong> 
                            <span style="color: ${color}; font-weight: bold; font-size: 1.2em;">
                                ${props.repetitions}x
                            </span>
                        </p>
                        <p style="margin: 5px 0; font-size: 0.85em; color: #666;">
                            <em>${props.startDate ? new Date(props.startDate).toLocaleDateString('es-ES') : ''}</em>
                        </p>
                    </div>
                `;
                
                layer.bindPopup(popupContent);
            }
        }).addTo(map);
        
        routeLayers.push(layer);
    });
    
    console.log(`✅ ${geoJson.features.length} rutas dibujadas correctamente`);
    
    // Show success message
    if (geoJson.metadata) {
        console.log(`📊 ${geoJson.metadata.message}`);
    }
}

/**
 * Fetch and draw routes if both location and Strava auth are available
 */
async function fetchAndDrawRoutesIfAvailable(city) {
    const athleteId = getAthleteIdFromURL();
    
    if (!athleteId) {
        console.log('ℹ️ Usuario no autenticado con Strava, no se pueden obtener rutas');
        return;
    }
    
    if (!city) {
        console.log('ℹ️ Ciudad no disponible, no se pueden obtener rutas');
        return;
    }
    
    console.log(`🔄 Obteniendo rutas para atleta ${athleteId} en ${city}...`);
    
    const geoJson = await fetchRepeatedRoutes(athleteId, city);
    
    if (geoJson) {
        drawRoutesOnMap(geoJson);
    }
}

/* ============================================================================
 *  SEARCH CITY BY NAME
 * ========================================================================== */
const searchInput = document.getElementById('search');
const searchBtn = document.getElementById('search-btn');

async function searchAndDisplayCity(cityName) {
    if (!cityName || cityName.trim() === '') {
        console.log('❌ Nombre de ciudad vacío');
        return;
    }

    try {
        console.log(`🔍 Buscando ciudad: ${cityName}`);
        
        // 1. Buscar la ciudad en Nominatim
        const searchUrl = `${API_BASE}/air/search/city?cityName=${encodeURIComponent(cityName)}`;
        const searchResponse = await fetch(searchUrl);
        
        if (!searchResponse.ok) {
            alert(`No se encontró la ciudad: ${cityName}`);
            return;
        }
        
        const cityData = await searchResponse.json();
        console.log('✅ Ciudad encontrada:', cityData);
        
        const lat = parseFloat(cityData.latitude);
        const lon = parseFloat(cityData.longitude);
        // Clean city name by removing trailing numbers
        const cityNameFound = cityData.city ? cityData.city.replace(/\d+$/, '').trim() : cityData.city;
        
        // Save city data for later use
        currentCity = cityNameFound;
        sessionStorage.setItem('currentCity', cityNameFound);
        sessionStorage.setItem('userLat', lat.toString());
        sessionStorage.setItem('userLon', lon.toString());
        
        // 2. Obtener AQI y color de la ciudad
        let cityAqiColor = '#3b82f6'; // Default color
        try {
            const aqiData = await getCityAirQuality(cityNameFound);
            if (aqiData) {
                cityAqiColor = aqiData.aqiColor;
                sessionStorage.setItem('cityAqiColor', cityAqiColor);
                console.log(`🎨 AQI: ${aqiData.aqi}, Color: ${cityAqiColor}, Estado: ${aqiData.aqiStatus}`);
            }
        } catch (e) {
            console.warn('No se pudo obtener AQI de la ciudad, usando color por defecto');
        }
        
        // 3. Obtener polígono de la ciudad desde Nominatim
        let fc = null;
        try {
            fc = await fetchCityPolygon(lat, lon);
            if (fc) {
                await updateCityLayout(fc, cityAqiColor);
            } else {
                // Si no hay polígono, solo hacer zoom
                map.setView([lat, lon], 12);
            }
        } catch (e) {
            console.warn('No se pudo obtener el polígono de la ciudad');
            map.setView([lat, lon], 12);
        }
        
        // 4. Generar y dibujar puntos contaminados dentro del polígono
        try {
            if (fc) {
                await ensureTurf();
                const hotspots = generateHotspotsInCity(fc, 3);
                if (hotspots && hotspots.length > 0) {
                    drawHotspotMarkers(hotspots);
                    console.log(`✅ Generados ${hotspots.length} puntos contaminados en ${cityNameFound}`);
                }
            }
        } catch (e) {
            console.error('⚠️ Error al generar puntos contaminados:', e);
        }
        
        // 5. Obtener y dibujar rutas más repetidas de Strava (si el usuario está autenticado)
        try {
            await fetchAndDrawRoutesIfAvailable(cityNameFound);
        } catch (e) {
            console.error('⚠️ Error al obtener rutas de Strava:', e);
        }
        
        // 6. Generar y dibujar 3 rutas alternativas (1 roja, 2 azules)
        // Se dibuja después de Strava para no ser borradas
        try {
            if (fc) {
                await ensureTurf();
                console.log('🔄 Generando 3 rutas alternativas para la ciudad...');
                const generatedRoutes = generateRandomRoutesInCity(fc, 3);
                if (generatedRoutes && generatedRoutes.length > 0) {
                    drawGeneratedRoutes(generatedRoutes);
                    console.log(`✅ Generadas ${generatedRoutes.length} rutas alternativas en ${cityNameFound}`);
                }
            }
        } catch (e) {
            console.error('⚠️ Error al generar rutas alternativas:', e);
        }
        
        console.log(`✅ Ciudad ${cityNameFound} mostrada exitosamente`);
        
    } catch (error) {
        console.error('❌ Error buscando ciudad:', error);
        alert(`Error al buscar la ciudad: ${error.message}`);
    }
}

// Event listener for search input (Enter key)
searchInput?.addEventListener('keypress', (e) => {
    if (e.key === 'Enter') {
        const cityName = searchInput.value.trim();
        searchAndDisplayCity(cityName);

        const el = document.getElementById('box');
        el.scrollTo({
            top: el.scrollHeight,
            behavior: 'smooth'
        });
    }
});

// Event listener for search button (click)
searchBtn?.addEventListener('click', () => {
    const cityName = searchInput.value.trim();
    searchAndDisplayCity(cityName);
    window.scrollTo({
        top: document.documentElement.scrollHeight,
        behavior: 'smooth'   // ← animación suave
    });
});

/* ============================================================================
 *  EVENTOS UI
 * ========================================================================== */
actionExplore?.addEventListener('click', () => {
    statusEl?.classList.remove('hide');
});

actionFind?.addEventListener('click', () => {
    window.scrollTo({
        top: document.documentElement.scrollHeight,
        behavior: 'smooth'   // ← animación suave
    });
});


permbutton?.addEventListener('click', () => {
    statusEl?.classList.toggle('hide', false);

    navigator.geolocation.getCurrentPosition(
        async ({ coords: { latitude: Ulat, longitude: Ulon, accuracy } }) => {
            console.log('📍 Ubicación del usuario:', Ulat, ',', Ulon);
            
            // 1. Obtener nombre de ciudad desde Nominatim
            let cityName = '';
            let cityAqiColor = '#3b82f6'; // Default color
            let fc = null; // Variable para guardar el polígono de la ciudad
            
            try {
                cityName = await getCity(Ulat, Ulon);
                currentCity = cityName; // Save for later use
                
                // Save location data in sessionStorage for later use
                sessionStorage.setItem('currentCity', cityName);
                sessionStorage.setItem('userLat', Ulat.toString());
                sessionStorage.setItem('userLon', Ulon.toString());
                
                console.log(`📍 Ciudad detectada: ${cityName}`);
            } catch (e) {
                console.warn('No se pudo obtener el nombre de la ciudad');
                statusEl?.classList.add('hide');
                alert('No se pudo detectar la ciudad');
                return;
            }
            
            // 2. Obtener AQI y color de la ciudad desde el backend
            try {
                const cityData = await getCityAirQuality(cityName);
                if (cityData) {
                    cityAqiColor = cityData.aqiColor;
                    // Save color for later restoration
                    sessionStorage.setItem('cityAqiColor', cityAqiColor);
                    console.log(`🎨 AQI: ${cityData.aqi}, Color: ${cityAqiColor}, Estado: ${cityData.aqiStatus}`);
                }
            } catch (e) {
                console.warn('No se pudo obtener AQI de la ciudad, usando color por defecto');
            }
            
            // 3. Obtener polígono de la ciudad desde Nominatim
            try {
                fc = await fetchCityPolygon(Ulat, Ulon);
                if (fc) {
                    await updateCityLayout(fc, cityAqiColor);
                } else {
                    // Si no hay polígono, solo hacer zoom
                    map.setView([Ulat, Ulon], 12);
                }
            } catch (e) {
                console.warn('No se pudo obtener el polígono de la ciudad');
                map.setView([Ulat, Ulon], 12);
            }

            // 4. Agregar marcador del usuario
            if (!userMarker) {
                userMarker = L.marker([Ulat, Ulon], {
                    title: 'Mi ubicación',
                    icon: L.icon({
                        iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-blue.png',
                        shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-shadow.png',
                        iconSize: [25, 41],
                        iconAnchor: [12, 41],
                        popupAnchor: [1, -34],
                        shadowSize: [41, 41]
                    })
                }).addTo(map);
            } else {
                userMarker.setLatLng([Ulat, Ulon]);
            }

            // 5. Círculo de precisión
            if (accuracyCircle) map.removeLayer(accuracyCircle);
            accuracyCircle = L.circle([Ulat, Ulon], {
                radius: accuracy,
                color: '#0078ff',
                fillColor: '#0078ff',
                fillOpacity: 0.1,
                weight: 2
            }).addTo(map);
            
            // 6. Obtener y dibujar puntos contaminados dentro del polígono de la ciudad
            try {
                console.log('🔍 Generando puntos contaminados dentro de la ciudad...');
                console.log('Turf loaded?', turfLoaded, 'fc exists?', !!fc);
                
                if (fc) {
                    // Asegurar que Turf.js esté cargado
                    await ensureTurf();
                    console.log('Turf.js cargado correctamente');
                    
                    const hotspots = generateHotspotsInCity(fc, 3);
                    console.log('Hotspots generados:', hotspots);
                    
                    if (hotspots && hotspots.length > 0) {
                        drawHotspotMarkers(hotspots);
                        console.log(`✅ Generados ${hotspots.length} puntos contaminados en la ciudad`);
                    } else {
                        console.warn('⚠️ No se generaron hotspots');
                    }
                } else {
                    console.log('ℹ️ No se pudo obtener el polígono de la ciudad');
                }
            } catch (e) {
                console.error('⚠️ Error al generar puntos contaminados:', e);
            }
            
            // 7. Obtener y dibujar rutas más repetidas de Strava (si el usuario está autenticado)
            try {
                await fetchAndDrawRoutesIfAvailable(cityName);
            } catch (e) {
                console.error('⚠️ Error al obtener rutas de Strava:', e);
            }
            
            // 8. Generar y dibujar 3 rutas alternativas (1 roja, 2 azules)
            // Se dibuja después de Strava para no ser borradas
            try {
                if (fc) {
                    await ensureTurf();
                    console.log('🔄 Generando 3 rutas alternativas para la ciudad...');
                    const generatedRoutes = generateRandomRoutesInCity(fc, 3);
                    if (generatedRoutes && generatedRoutes.length > 0) {
                        drawGeneratedRoutes(generatedRoutes);
                        console.log(`✅ Generadas ${generatedRoutes.length} rutas alternativas en ${cityName}`);
                    }
                }
            } catch (e) {
                console.error('⚠️ Error al generar rutas alternativas:', e);
            }

            statusEl?.classList.add('hide');
        },
        (err) => {
            console.warn('❌ Error obteniendo ubicación:', err);
            alert('AETHER no puede obtener tu ubicación. Por favor, permite el acceso a tu ubicación.');
            statusEl?.classList.add('hide');
        },
        {
            enableHighAccuracy: true,
            maximumAge: 30000,
            timeout: 10000
        }
    );
    window.scrollTo({
        top: document.documentElement.scrollHeight,
        behavior: 'smooth'   // ← animación suave
    });
});

/* ============================================================================
 *  APP INIT
 * ========================================================================== */
document.addEventListener('DOMContentLoaded', () => {
    console.log('🚀 Inicializando AETHER...');
    initMap();
    console.log('✅ AETHER listo - Esperando ubicación del usuario');
    
    // Check if user just authenticated with Strava
    const urlParams = new URLSearchParams(window.location.search);
    const authSuccess = urlParams.get('auth');
    const athleteParam = urlParams.get('athlete');
    
    if (authSuccess === 'success' && athleteParam) {
        console.log('✅ Usuario autenticado con Strava, athlete ID:', athleteParam);
        sessionStorage.setItem('athleteId', athleteParam);
        
        // Check if we already have a city saved
        const savedCity = sessionStorage.getItem('currentCity');
        const savedLat = sessionStorage.getItem('userLat');
        const savedLon = sessionStorage.getItem('userLon');
        
        if (savedCity && savedLat && savedLon) {
            console.log('🔄 Ciudad previamente detectada:', savedCity);
            console.log('🔄 Cargando rutas automáticamente...');
            
            // Restore the map view and fetch routes
            (async () => {
                try {
                    const lat = parseFloat(savedLat);
                    const lon = parseFloat(savedLon);
                    
                    // Restore city visualization
                    const cityAqiColor = sessionStorage.getItem('cityAqiColor') || '#3b82f6';
                    
                    // Get city polygon
                    const fc = await fetchCityPolygon(lat, lon);
                    if (fc) {
                        await updateCityLayout(fc, cityAqiColor);
                    } else {
                        map.setView([lat, lon], 12);
                    }
                    
                    // Add user marker
                    if (!userMarker) {
                        userMarker = L.marker([lat, lon], {
                            title: 'Mi ubicación',
                            icon: L.icon({
                                iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-blue.png',
                                shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-shadow.png',
                                iconSize: [25, 41],
                                iconAnchor: [12, 41],
                                popupAnchor: [1, -34],
                                shadowSize: [41, 41]
                            })
                        }).addTo(map);
                    }
                    
                    // Fetch and draw routes NOW
                    await fetchAndDrawRoutesIfAvailable(savedCity);
                    
                    // Generate and draw alternative routes (after Strava routes to avoid being cleared)
                    if (fc) {
                        await ensureTurf();
                        console.log('🔄 Generando 3 rutas alternativas para la ciudad...');
                        const generatedRoutes = generateRandomRoutesInCity(fc, 3);
                        if (generatedRoutes && generatedRoutes.length > 0) {
                            drawGeneratedRoutes(generatedRoutes);
                            console.log(`✅ Generadas ${generatedRoutes.length} rutas alternativas en ${savedCity}`);
                        }
                    }
                    
                    console.log('✅ Rutas cargadas después de autenticación');
                } catch (e) {
                    console.error('Error al cargar rutas después de autenticación:', e);
                }
            })();
        } else {
            console.log('ℹ️ Aún no hay ubicación guardada. Esperando a que el usuario proporcione su ubicación.');
        }
        
        // Clean URL (remove query parameters)
        if (window.history && window.history.replaceState) {
            window.history.replaceState({}, document.title, window.location.pathname);
        }
    }
});
