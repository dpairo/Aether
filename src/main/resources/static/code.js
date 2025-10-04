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
    if (turfLoaded) return;
    await loadScript('https://unpkg.com/@turf/turf@6');
    turfLoaded = true;
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
    return a.city || a.town || a.village || a.municipality || null;
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
 *  EVENTOS UI
 * ========================================================================== */
actionExplore?.addEventListener('click', () => {
    statusEl?.classList.remove('hide');
});

actionFind?.addEventListener('click', () => {
    window.location.href = '/api/v1/strava/auth/login';
});

permbutton?.addEventListener('click', () => {
    statusEl?.classList.toggle('hide', false);

    navigator.geolocation.getCurrentPosition(
        async ({ coords: { latitude: Ulat, longitude: Ulon, accuracy } }) => {
            console.log('📍 Ubicación del usuario:', Ulat, ',', Ulon);
            
            // 1. Obtener nombre de ciudad desde Nominatim
            let cityName = '';
            let cityAqiColor = '#3b82f6'; // Default color
            
            try {
                cityName = await getCity(Ulat, Ulon);
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
                    console.log(`🎨 AQI: ${cityData.aqi}, Color: ${cityAqiColor}, Estado: ${cityData.aqiStatus}`);
                }
            } catch (e) {
                console.warn('No se pudo obtener AQI de la ciudad, usando color por defecto');
            }
            
            // 3. Obtener polígono de la ciudad desde Nominatim
            try {
                const fc = await fetchCityPolygon(Ulat, Ulon);
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
});

/* ============================================================================
 *  APP INIT
 * ========================================================================== */
document.addEventListener('DOMContentLoaded', () => {
    console.log('🚀 Inicializando AETHER...');
    initMap();
    console.log('✅ AETHER listo - Esperando ubicación del usuario');
});
