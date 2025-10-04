// AETHER Frontend - Air Quality Visualization
'use strict';

/* ============================================================================
 *  DOM REFS
 * ========================================================================== */
const permbutton        = document.getElementById('perm');
const statusEl          = document.getElementById('status');
const actionExplore     = document.getElementById('action-explore');
const actionFind        = document.getElementById('action-find');
const localPollutionMode= document.getElementById('local-pollution-mode');
const betterRouteMode   = document.getElementById('better-route-mode');
const infoLegend        = document.getElementById('information');
const loginBox          = document.getElementById('login-container');

/* ============================================================================
 *  CONFIG / STATE
 * ========================================================================== */
const API_BASE = '/api/v1';

const Mlat = 40.4168;       // Madrid
const Mlon = -3.7038;

let isLoggedIn = false;      // nombre claro y modificable
let cityname    = '';

let map, userMarker, accuracyCircle;
let airQualityData = [];

// Capas de layout (polígono de ciudad + máscara World-City)
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
// Polígono (GeoJSON) de la ciudad para un punto dado
async function fetchCityPolygon(lat, lon) {
    const url = new URL('https://nominatim.openstreetmap.org/reverse');
    url.search = new URLSearchParams({
        format: 'jsonv2',
        lat, lon,
        zoom: '10',                 // ~ciudad/municipio
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
        geometry: data.geojson,     // Polygon/MultiPolygon
        properties: {
            display_name: data.display_name,
            osm_id: data.osm_id,
            osm_type: data.osm_type,
            address: data.address
        }
    };

    return { type: 'FeatureCollection', features: [feature] };
}

// Nombre de la ciudad para un punto dado
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
 *  LAYOUT (CIUDAD + MÁSCARA)
 * ========================================================================== */
async function buildMaskFromCity(fc) {
    await ensureTurf();
    const world = turf.bboxPolygon([-180, -85, 180, 85]); // evita los polos
    const city  = fc.features[0]; // asumimos 1 feature
    return turf.difference(world, city);
}

async function CityAir() {
    fetch(`/api/v1/air/quality/city/${cityname}`).then(r=>r.json()).then(d=>console.log(d.aqiStatus));
    console.log()
}

async function CityColor()
{
    switch (cityname) {
        case 'Air':
    }
}

async function updateCityLayout(fc) {
    // 1) limpiar capas previas
    if (cityLayer) { map.removeLayer(cityLayer); cityLayer = null; }
    if (maskLayer) { map.removeLayer(maskLayer); maskLayer = null; }

    // 2) capa del polígono (relleno + borde)
    cityLayer = L.geoJSON(fc, {
        style: {
            color: '#1d4ed8',
            weight: 2,
            fillColor: '#3b82f6',
            fillOpacity: 0.2
        }
    }).addTo(map);

    // 3) máscara (oscurecer exterior)
    try {
        const mask = await buildMaskFromCity(fc);
        if (mask) {
            maskLayer = L.geoJSON(mask, {
                style: { color: '#000', weight: 0, fillColor: '#000', fillOpacity: 0.5 }
            }).addTo(map);
            cityLayer.bringToFront();
        }
    } catch (e) {
        console.warn('No se pudo construir la máscara:', e);
    }

    // 4) encuadre al polígono
    map.fitBounds(cityLayer.getBounds(), { padding: [30, 30] });
}

/* ============================================================================
 *  MAPA
 * ========================================================================== */
function initMap() {
    map = L.map('map', { zoomControl: true }).setView([Mlat, Mlon], 5);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19,
        attribution: '&copy; OpenStreetMap contributors'
    }).addTo(map);
}

/* ============================================================================
 *  EVENTOS UI
 * ========================================================================== */
actionExplore?.addEventListener('click', () => {
    statusEl?.classList.remove('hide');
});

actionFind?.addEventListener('click', () => {
    // Aquí podrías disparar una búsqueda por dirección y luego:
    // const fc = await fetchCityPolygon(lat, lon); await updateCityLayout(fc);
});

permbutton?.addEventListener('click', () => {
    statusEl?.classList.toggle('hide', false);

    navigator.geolocation.getCurrentPosition(
        async ({ coords: { latitude: Ulat, longitude: Ulon, accuracy } }) => {
            console.log('lat:', Ulat, 'lon:', Ulon, '±', accuracy, 'm');

            // <- CORREGIDO: ahora sí esperamos el nombre de la ciudad
            try {
                cityname = await getCity(Ulat, Ulon);
            } catch (e) {
                console.warn('No se pudo obtener el nombre de la ciudad:', e);
                cityname = '';
            }

            map?.setView([Ulat, Ulon], 16);

            // Marcador de usuario
            if (!userMarker) {
                userMarker = L.marker([Ulat, Ulon], { title: 'Mi ubicación' }).addTo(map);
            } else {
                userMarker.setLatLng([Ulat, Ulon]);
            }

            // Círculo de precisión
            if (accuracyCircle) map.removeLayer(accuracyCircle);
            accuracyCircle = L.circle([Ulat, Ulon], {
                radius: accuracy,
                color: '#0078ff',
                fillColor: '#0078ff',
                fillOpacity: 0.1
            }).addTo(map);

            // Polígono y máscara de la ciudad actual
            try {
                const fc = await fetchCityPolygon(Ulat, Ulon);
                if (fc) {
                    await updateCityLayout(fc);
                } else {
                    console.warn('No se encontró polígono para estas coordenadas.');
                }
            } catch (e) {
                console.warn('No se pudo obtener el polígono:', e);
            }

            statusEl?.classList.add('hide');

            // Datos de calidad del aire (si luego filtras por bbox/nearest)
            // TODO: implementa estas funciones si no existen en otro módulo
            try { loadAirQualityData?.(); } catch {}
        },
        (err) => {
            console.warn(err);
            alert('AETHER no puede obtener tu ubicación. Revisa los permisos.');
            statusEl?.classList.add('hide');
        },
        {
            enableHighAccuracy: true,
            maximumAge: 30000, // acepta hasta 30s de caché
            timeout: 10000
        }
    );
});

/* ============================================================================
 *  APP INIT
 * ========================================================================== */
document.addEventListener('DOMContentLoaded', async () => {
    initMap();

    // TODO: implementa estas funciones si no existen en otro módulo
    try { loadAirQualityData?.(); } catch {}
    try { updateModeDisplay?.(); } catch {}

    // Layout inicial centrado en Madrid
    try {
        const fc = await fetchCityPolygon(Mlat, Mlon);
        if (fc) await updateCityLayout(fc);
    } catch (e) {
        console.warn('No se pudo cargar el polígono inicial:', e);
    }
});
