// AETHER Frontend - Air Quality Visualization
const permbutton = document.getElementById('perm');
const statusEl = document.getElementById('status');
const actionExplore = document.getElementById('action-explore');
const actionFind = document.getElementById('action-find');
const localPollutionMode = document.getElementById('local-pollution-mode');
const betterRouteMode = document.getElementById('better-route-mode');

// API Base URL
const API_BASE = '/api/v1';

// Strava Authentication State
let stravaAthleteId = localStorage.getItem('stravaAthleteId');

// Map initialization
const SPAIN_CENTER = [40.4168, -3.7038];  // Centro de España
const SPAIN_ZOOM = 6;  // Zoom para ver toda España
let map, userMarker, accuracyCircle;
let airQualityData = [];
let provinceMarkers = [];
let cityMarkers = [];
let currentMode = 'local-pollution';
let userLocation = null;

// Initialize map
function initMap() {
    map = L.map('map', { 
        zoomControl: true,
        preferCanvas: true  // Mejor rendimiento con muchos marcadores
    }).setView(SPAIN_CENTER, SPAIN_ZOOM);
    
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19,
        attribution: '&copy; OpenStreetMap contributors'
    }).addTo(map);
    
    console.log('🗺️ Mapa inicializado. Vista: España completa');
}

// Load air quality data for provinces (vista inicial)
async function loadAirQualityData() {
    try {
        console.log('🌍 Cargando datos de AQI de provincias...');
        const response = await fetch(`${API_BASE}/air/quality/provinces`);
        airQualityData = await response.json();
        console.log(`✅ Datos cargados: ${airQualityData.length} provincias`);
        displayProvincesOnMap();
    } catch (error) {
        console.error('❌ Error loading air quality data:', error);
    }
}

// Display provinces on map (vista inicial)
function displayProvincesOnMap() {
    // Limpiar marcadores de provincias anteriores
    provinceMarkers.forEach(marker => map.removeLayer(marker));
    provinceMarkers = [];

    console.log('🗺️ Pintando provincias en el mapa...');
    
    airQualityData.forEach(province => {
        const aqi = province.aqi;
        const color = province.aqiColor || '#808080';  // Usar color del backend
        
        const marker = L.circleMarker([province.latitude, province.longitude], {
            radius: 12,
            fillColor: color,
            color: '#fff',
            weight: 2,
            opacity: 1,
            fillOpacity: 0.85
        }).addTo(map);

        const popupContent = `
            <div style="text-align: center; min-width: 180px;">
                <h3 style="margin: 0 0 10px 0; color: #333; font-size: 16px;">${province.province}</h3>
                <div style="background: ${color}; padding: 8px; border-radius: 4px; margin-bottom: 8px;">
                    <strong style="color: #fff; font-size: 20px;">${aqi !== null ? aqi : 'N/A'}</strong>
                </div>
                <p style="margin: 5px 0; font-size: 13px;"><strong>Estado:</strong> ${province.aqiStatus}</p>
                <p style="margin: 5px 0; font-size: 12px; color: #666;"><strong>Contaminante:</strong> ${province.dominantPollutant || 'N/A'}</p>
            </div>
        `;
        
        marker.bindPopup(popupContent);
        provinceMarkers.push(marker);
    });
    
    console.log(`✅ ${provinceMarkers.length} provincias pintadas en el mapa`);
}

// Cargar ciudades de una provincia específica
async function loadCitiesForProvince(provinceCode) {
    try {
        console.log(`🏙️ Cargando ciudades de provincia ${provinceCode}...`);
        const response = await fetch(`${API_BASE}/air/quality/province/${provinceCode}/cities`);
        const cities = await response.json();
        console.log(`✅ ${cities.length} ciudades cargadas`);
        return cities;
    } catch (error) {
        console.error('❌ Error loading cities:', error);
        return [];
    }
}

// Mostrar ciudades en el mapa (cuando hay localización del usuario)
function displayCitiesOnMap(cities) {
    // Limpiar marcadores de ciudades anteriores
    cityMarkers.forEach(marker => map.removeLayer(marker));
    cityMarkers = [];

    console.log(`🗺️ Pintando ${cities.length} ciudades en el mapa...`);
    
    cities.forEach(city => {
        const aqi = city.aqi;
        const color = city.aqiColor || '#808080';  // Usar color del backend
        
        const marker = L.circleMarker([city.latitude, city.longitude], {
            radius: 8,
            fillColor: color,
            color: '#fff',
            weight: 2,
            opacity: 1,
            fillOpacity: 0.9
        }).addTo(map);

        const popupContent = `
            <div style="text-align: center; min-width: 160px;">
                <h4 style="margin: 0 0 8px 0; color: #333; font-size: 14px;">${city.city}</h4>
                <div style="background: ${color}; padding: 6px; border-radius: 4px; margin-bottom: 6px;">
                    <strong style="color: #fff; font-size: 18px;">${aqi !== null ? aqi : 'N/A'}</strong>
                </div>
                <p style="margin: 4px 0; font-size: 12px;"><strong>Estado:</strong> ${city.aqiStatus}</p>
                <p style="margin: 4px 0; font-size: 11px; color: #666;">${city.dominantPollutant || 'N/A'}</p>
            </div>
        `;
        
        marker.bindPopup(popupContent);
        cityMarkers.push(marker);
    });
    
    console.log(`✅ ${cityMarkers.length} ciudades pintadas`);
}

// Encontrar la provincia más cercana a las coordenadas del usuario
function findNearestProvince(lat, lon) {
    let nearest = null;
    let minDistance = Infinity;
    
    airQualityData.forEach(province => {
        const distance = Math.sqrt(
            Math.pow(province.latitude - lat, 2) + 
            Math.pow(province.longitude - lon, 2)
        );
        
        if (distance < minDistance) {
            minDistance = distance;
            nearest = province;
        }
    });
    
    return nearest;
}

// Event listeners
actionExplore.addEventListener('click', (event) => {
    statusEl.classList.remove('hide');
});

actionFind.addEventListener('click', (event) => {
    // Redirect to Strava authentication
    window.location.href = '/api/v1/strava/auth/login';
});

localPollutionMode.addEventListener('click', (event) => {
    event.preventDefault();
    currentMode = 'local-pollution';
    updateModeDisplay();
    
    // Volver a la vista de provincias
    cityMarkers.forEach(marker => map.removeLayer(marker));
    cityMarkers = [];
    
    // Restaurar opacidad de provincias
    provinceMarkers.forEach(marker => {
        marker.setStyle({ 
            fillOpacity: 0.85,
            radius: 12
        });
    });
    
    // Volver al zoom de España
    map.setView(SPAIN_CENTER, SPAIN_ZOOM);
    
    console.log('🔄 Modo: Local Pollution - Vista de provincias restaurada');
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
        async ({ coords:{ latitude: Ulat, longitude: Ulon, accuracy } }) => {
            console.log('📍 Ubicación obtenida:', Ulat, 'lat,', Ulon, 'lon ±', accuracy, 'm');
            
            userLocation = { lat: Ulat, lon: Ulon };

            // Encontrar la provincia más cercana
            const nearestProvince = findNearestProvince(Ulat, Ulon);
            
            if (nearestProvince) {
                console.log(`📍 Provincia más cercana: ${nearestProvince.province} (${nearestProvince.provinceCode})`);
                
                // Hacer zoom a la provincia con énfasis en la ciudad del usuario
                map.setView([Ulat, Ulon], 11);  // Zoom 11 muestra la ciudad y alrededores
                
                // Cargar y mostrar ciudades de la provincia
                const cities = await loadCitiesForProvince(nearestProvince.provinceCode);
                displayCitiesOnMap(cities);
                
                // Mantener las provincias visibles pero con menos prominencia
                provinceMarkers.forEach(marker => {
                    marker.setStyle({ 
                        fillOpacity: 0.3,
                        radius: 8
                    });
                });
            } else {
                // Si no se encuentra provincia cercana, solo hacer zoom
                map.setView([Ulat, Ulon], 13);
            }

            // Create/update user marker (marcador especial para el usuario)
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

            // Create accuracy circle
            if (accuracyCircle) {
                map.removeLayer(accuracyCircle);
            }
            accuracyCircle = L.circle([Ulat, Ulon], {
                radius: accuracy,
                color: '#0078ff',
                fillColor: '#0078ff',
                fillOpacity: 0.1,
                weight: 2
            }).addTo(map);

            statusEl.classList.add('hide');
            
            console.log('✅ Ubicación del usuario mostrada en el mapa');
            
            // Check if user is authenticated with Strava and search for activities
            await checkForStravaActivities(Ulat, Ulon);
        },
        (err) => {
            console.warn('❌ Error obteniendo ubicación:', err);
            alert('AETHER cannot obtain your location, check the permissions');
            statusEl.classList.add('hide');
        },
        {
            enableHighAccuracy: true,
            maximumAge: 0
        }
    );
});

// Check Strava authentication on page load
function checkStravaAuth() {
    const urlParams = new URLSearchParams(window.location.search);
    const authSuccess = urlParams.get('auth');
    const athleteId = urlParams.get('athlete');
    
    if (authSuccess === 'success' && athleteId) {
        // Store athlete ID
        localStorage.setItem('stravaAthleteId', athleteId);
        stravaAthleteId = athleteId;
        
        // Remove query parameters from URL
        window.history.replaceState({}, document.title, window.location.pathname);
        
        // Show success message
        console.log('Successfully authenticated with Strava! Athlete ID:', athleteId);
        
        // Optionally, fetch athlete info
        fetchAthleteInfo(athleteId);
    }
}

// Fetch athlete information from backend
async function fetchAthleteInfo(athleteId) {
    try {
        const response = await fetch(`${API_BASE}/strava/auth/me?athleteId=${athleteId}`);
        
        if (response.ok) {
            const athlete = await response.json();
            console.log('Athlete info:', athlete);
            // You can display athlete info in the UI here
            displayAthleteInfo(athlete);
        } else {
            console.error('Failed to fetch athlete info');
            localStorage.removeItem('stravaAthleteId');
            stravaAthleteId = null;
        }
    } catch (error) {
        console.error('Error fetching athlete info:', error);
    }
}

// Display athlete information in the UI
function displayAthleteInfo(athlete) {
    // You can customize this to display athlete info in your UI
    const infoProjecto = document.querySelector('.info-proyecto');
    
    if (infoProjecto) {
        const athleteDiv = document.createElement('div');
        athleteDiv.style.cssText = 'padding: 15px; background: rgba(255,255,255,0.1); margin: 10px 0; border-radius: 8px;';
        athleteDiv.innerHTML = `
            <p style="margin: 0; color: white; font-size: 14px;">
                <i class="fa-brands fa-strava"></i> 
                Welcome, ${athlete.firstName} ${athlete.lastName || ''}!
            </p>
        `;
        
        // Insert at the beginning of info-proyecto
        infoProjecto.insertBefore(athleteDiv, infoProjecto.firstChild);
    }
}

// Logout from Strava
async function logoutStrava() {
    if (!stravaAthleteId) {
        console.log('Not logged in');
        return;
    }
    
    try {
        const response = await fetch(`${API_BASE}/strava/auth/logout?athleteId=${stravaAthleteId}`, {
            method: 'POST'
        });
        
        if (response.ok) {
            console.log('Logged out successfully');
            localStorage.removeItem('stravaAthleteId');
            stravaAthleteId = null;
            location.reload();
        }
    } catch (error) {
        console.error('Error during logout:', error);
    }
}

// Check for Strava activities in user location
async function checkForStravaActivities(lat, lon) {
    // Only check if user is authenticated with Strava
    if (!stravaAthleteId) {
        console.log('User not authenticated with Strava, skipping activity search');
        return;
    }
    
    console.log('🏃 Checking for Strava activities near location...');
    
    try {
        // First, get the city name from reverse geocoding (using location consent endpoint)
        const consentResponse = await fetch(`${API_BASE}/location/consent`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                consent: true,
                lat: lat,
                lon: lon,
                accuracyMeters: 100,
                source: 'gps',
                consentVersion: '1.0',
                assertedAtIso: new Date().toISOString()
            })
        });
        
        if (!consentResponse.ok) {
            console.error('Failed to get location details');
            return;
        }
        
        const locationData = await consentResponse.json();
        const city = locationData.city;
        
        console.log(`📍 User location: ${city}, ${locationData.state}, ${locationData.country}`);
        
        // Search for activities in the same city
        const activitiesResponse = await fetch(
            `${API_BASE}/strava/activities/city?athleteId=${stravaAthleteId}&city=${encodeURIComponent(city)}`
        );
        
        if (!activitiesResponse.ok) {
            console.error('Failed to fetch activities');
            return;
        }
        
        const activitiesData = await activitiesResponse.json();
        
        if (activitiesData.totalActivities > 0) {
            console.log(`✅ Found ${activitiesData.totalActivities} activities in ${city}!`);
            console.log('Activities:', activitiesData.activities);
            
            // Show notification to user
            showActivitiesNotification(activitiesData);
            
            // You can add logic here to display routes on the map
            displayActivitiesOnMap(activitiesData.activities);
        } else {
            console.log(`ℹ️ No activities found in ${city}`);
        }
        
    } catch (error) {
        console.error('Error checking for Strava activities:', error);
    }
}

// Show notification when activities are found
function showActivitiesNotification(data) {
    const notification = document.createElement('div');
    notification.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        background: linear-gradient(135deg, #fc4c02, #e34402);
        color: white;
        padding: 20px;
        border-radius: 10px;
        box-shadow: 0 4px 15px rgba(252, 76, 2, 0.4);
        z-index: 10000;
        max-width: 300px;
        animation: slideIn 0.3s ease-out;
    `;
    
    notification.innerHTML = `
        <strong style="display: block; margin-bottom: 8px;">
            🏃 ${data.totalActivities} actividades encontradas
        </strong>
        <p style="margin: 0; font-size: 14px; opacity: 0.95;">
            Tienes ${data.totalActivities} rutas en ${data.city}. 
            Verás tus rutas en el mapa.
        </p>
    `;
    
    document.body.appendChild(notification);
    
    // Remove notification after 5 seconds
    setTimeout(() => {
        notification.style.animation = 'slideOut 0.3s ease-in';
        setTimeout(() => notification.remove(), 300);
    }, 5000);
}

// Display activity routes on the map
function displayActivitiesOnMap(activities) {
    activities.forEach(activity => {
        // Only display activities with start coordinates
        if (activity.startLatLng && activity.startLatLng.length >= 2) {
            const [lat, lon] = activity.startLatLng;
            
            // Create a marker for the activity start point
            const marker = L.marker([lat, lon], {
                icon: L.icon({
                    iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-orange.png',
                    shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-shadow.png',
                    iconSize: [25, 41],
                    iconAnchor: [12, 41],
                    popupAnchor: [1, -34],
                    shadowSize: [41, 41]
                })
            }).addTo(map);
            
            // Create popup with activity info
            const popupContent = `
                <div style="text-align: center;">
                    <strong>${activity.name}</strong><br>
                    <span style="font-size: 12px; color: #666;">${activity.type}</span><br>
                    <span style="font-size: 12px;">
                        ${(activity.distance / 1000).toFixed(2)} km
                    </span>
                </div>
            `;
            
            marker.bindPopup(popupContent);
        }
    });
    
    console.log(`Displayed ${activities.length} activities on map`);
}

// Initialize application
document.addEventListener('DOMContentLoaded', () => {
    initMap();
    loadAirQualityData();
    updateModeDisplay();
    checkStravaAuth();
});

