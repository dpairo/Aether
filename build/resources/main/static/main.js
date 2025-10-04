// AETHER Frontend - Air Quality Visualization
const permbutton = document.getElementById('perm');
const statusEl = document.getElementById('status');
const actionExplore = document.getElementById('action-explore');
const actionFind = document.getElementById('action-find');
const localPollutionMode = document.getElementById('local-pollution-mode');
const betterRouteMode = document.getElementById('better-route-mode');

// API Base URL
const API_BASE = '/api/v1';

// Map initialization
const Mlat = 40.4168;
const Mlon = -3.7038;
let map, userMarker, accuracyCircle;
let airQualityData = [];
let currentMode = 'local-pollution';

// Initialize map
function initMap() {
    map = L.map('map', { zoomControl: true }).setView([Mlat, Mlon], 5);
    
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19,
        attribution: '&copy; OpenStreetMap contributors'
    }).addTo(map);
}

// Load air quality data for provinces
async function loadAirQualityData() {
    try {
        const response = await fetch(`${API_BASE}/air/quality/provinces`);
        airQualityData = await response.json();
        console.log('Air quality data loaded:', airQualityData);
        displayAirQualityOnMap();
    } catch (error) {
        console.error('Error loading air quality data:', error);
    }
}

// Display air quality data on map
function displayAirQualityOnMap() {
    // Clear existing markers
    map.eachLayer(layer => {
        if (layer instanceof L.CircleMarker) {
            map.removeLayer(layer);
        }
    });

    airQualityData.forEach(province => {
        const aqi = province.aqi;
        const color = getAQIColor(aqi);
        
        const marker = L.circleMarker([province.latitude, province.longitude], {
            radius: 8,
            fillColor: color,
            color: '#000',
            weight: 1,
            opacity: 1,
            fillOpacity: 0.8
        }).addTo(map);

        const popupContent = `
            <div style="text-align: center;">
                <h3>${province.province}</h3>
                <p><strong>AQI:</strong> ${aqi || 'N/A'}</p>
                <p><strong>Estado:</strong> ${province.aqiStatus}</p>
                <p><strong>Contaminante:</strong> ${province.dominantPollutant}</p>
            </div>
        `;
        
        marker.bindPopup(popupContent);
    });
}

// Get AQI color based on value
function getAQIColor(aqi) {
    if (!aqi) return '#666666';
    if (aqi <= 50) return '#2ECC71';      // Good
    if (aqi <= 100) return '#F1C40F';     // Moderate
    if (aqi <= 150) return '#E67E22';     // Unhealthy for Sensitive
    if (aqi <= 200) return '#E74C3C';     // Unhealthy
    if (aqi <= 300) return '#9B59B6';     // Very Unhealthy
    return '#6E2C00';                     // Hazardous
}

// Event listeners
actionExplore.addEventListener('click', (event) => {
    statusEl.classList.remove('hide');
});

actionFind.addEventListener('click', (event) => {
    loadAirQualityData();
});

localPollutionMode.addEventListener('click', (event) => {
    event.preventDefault();
    currentMode = 'local-pollution';
    updateModeDisplay();
    loadAirQualityData();
});

betterRouteMode.addEventListener('click', (event) => {
    event.preventDefault();
    currentMode = 'better-route';
    updateModeDisplay();
    // TODO: Implement route finding logic
});

function updateModeDisplay() {
    // Update visual indicators for current mode
    document.querySelectorAll('.implementations a').forEach(link => {
        link.style.backgroundColor = '';
    });
    document.getElementById(`${currentMode}-mode`).style.backgroundColor = '#1b1b1b';
}

permbutton.addEventListener('click', () => {
    statusEl.classList.toggle('hide', false);

    navigator.geolocation.getCurrentPosition(
        ({ coords:{ latitude: Ulat, longitude: Ulon, accuracy } }) => {
            console.log('lat:', Ulat, 'lon:', Ulon, '±', accuracy, 'm');

            map.setView([Ulat, Ulon], 16);

            // Create/update user marker
            if (!userMarker) {
                userMarker = L.marker([Ulat, Ulon], {
                    title: 'Mi ubicación'
                }).addTo(map);
            } else {
                userMarker.setLatLng([Ulat, Ulon]);
            }

            // Create accuracy circle
            if (accuracyCircle) {
                map.removeLayer(accuracyCircle);
            }
            accuracyCircle = L.circle([Ulat, Ulon], {
                radius: accuracy,
                color: '#0078ff',
                fillColor: '#0078ff',
                fillOpacity: 0.1
            }).addTo(map);

            statusEl.classList.add('hide');
            
            // Load air quality data for user location
            loadAirQualityData();
        },
        (err) => {
            console.warn(err);
            alert('AETHER cannot obtain your location, check the permissions');
            statusEl.classList.add('hide');
        },
        {
            enableHighAccuracy: true,
            maximumAge: 0
        }
    );
});

// Initialize application
document.addEventListener('DOMContentLoaded', () => {
    initMap();
    loadAirQualityData();
    updateModeDisplay();
});

