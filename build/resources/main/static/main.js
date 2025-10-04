// JS
const permbutton = document.getElementById('perm');
const statusEl = document.getElementById('status');
const actionExplore = document.getElementById('action-explore');

actionExplore.addEventListener('click', (event) => {
    statusEl.classList.remove('hide');
})
const Mlat = 40.4168;
const Mlon = -3.7038;

map = L.map('map', { zoomControl: true }).setView([Mlat, Mlon], 5);

L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 19,
    attribution: '&copy; OpenStreetMap contributors'
}).addTo(map);


let userMarker, accuracyCircle;

permbutton.addEventListener('click', () => {
    statusEl.classList.toggle('hide', false);

    navigator.geolocation.getCurrentPosition(
        ({ coords:{ latitude: Ulat, longitude: Ulon, accuracy } }) => {
            console.log('lat:', Ulat, 'lon:', Ulon, '±', accuracy, 'm');

            map.setView([Ulat, Ulon], 16);

            // 2) Crea/actualiza el marcador de usuario
            if (!userMarker) {
                userMarker = L.marker([Ulat, Ulon], {
                    title: 'Mi ubicación'
                }).addTo(map);
            } else {
                userMarker.setLatLng([Ulat, Ulon]);
            }

            statusEl.classList.add('hide');
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

