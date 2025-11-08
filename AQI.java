// Firebase Config (replace with your project's config)
const firebaseConfig = {
    apiKey: "YOUR_FIREBASE_API_KEY",
    authDomain: "YOUR_PROJECT.firebaseapp.com",
    databaseURL: "https://YOUR_PROJECT-default-rtdb.firebaseio.com",
    projectId: "YOUR_PROJECT",
    storageBucket: "YOUR_PROJECT.appspot.com",
    messagingSenderId: "YOUR_SENDER_ID",
    appId: "YOUR_APP_ID"
};
firebase.initializeApp(firebaseConfig);
const database = firebase.database();

// Mapbox Setup
mapboxgl.accessToken = 'YOUR_MAPBOX_ACCESS_TOKEN';
const map = new mapboxgl.Map({
    container: 'map',
    style: 'mapbox://styles/mapbox/streets-v11',
    center: [-74.5, 40], // Default to NYC; will update with user location
    zoom: 9
});

// OpenWeather API Key
const openWeatherKey = 'YOUR_OPENWEATHER_API_KEY';

// Global variables
let userLocation = null;
let alertsEnabled = false;

// Get user's location
document.getElementById('get-location').addEventListener('click', () => {
    if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition(position => {
            userLocation = [position.coords.longitude, position.coords.latitude];
            map.setCenter(userLocation);
            fetchAirQuality(userLocation);
        });
    } else {
        alert('Geolocation not supported.');
    }
});

// Fetch air quality from OpenWeather
function fetchAirQuality(coords) {
    const url = `https://api.openweathermap.org/data/2.5/air_pollution?lat=${coords[1]}&lon=${coords[0]}&appid=${openWeatherKey}`;
    fetch(url)
        .then(response => response.json())
        .then(data => {
            const aqi = data.list[0].main.aqi; // Air Quality Index (1-5)
            const pm25 = data.list[0].components.pm2_5;
            displayAirQuality(coords, aqi, pm25);
            if (alertsEnabled && aqi > 3) { // Alert if poor quality
                showAlert(`Air quality is poor (AQI: ${aqi}). Stay indoors!`);
            }
        })
        .catch(err => console.error('Error fetching air quality:', err));
}

// Display air quality on map
function displayAirQuality(coords, aqi, pm25) {
    const color = aqi === 1 ? 'green' : aqi === 2 ? 'yellow' : aqi === 3 ? 'orange' : aqi === 4 ? 'red' : 'purple';
    new mapboxgl.Marker({ color }).setLngLat(coords).addTo(map);
    // Add popup
    const popup = new mapboxgl.Popup().setHTML(`<p>AQI: ${aqi} (PM2.5: ${pm25} µg/m³)</p>`);
    new mapboxgl.Marker({ color }).setLngLat(coords).setPopup(popup).addTo(map);
}

// Submit user report
document.getElementById('submit-report').addEventListener('click', () => {
    const reportText = document.getElementById('report-text').value;
    if (reportText && userLocation) {
        const report = {
            text: reportText,
            lat: userLocation[1],
            lon: userLocation[0],
            timestamp: Date.now()
        };
        database.ref('reports').push(report);
        document.getElementById('report-text').value = '';
        loadReports();
    } else {
        alert('Enter a report and set your location.');
    }
});

// Load and display reports
function loadReports() {
    database.ref('reports').on('value', snapshot => {
        const reports = snapshot.val();
        const list = document.getElementById('reports');
        list.innerHTML = '';
        for (let id in reports) {
            const report = reports[id];
            const li = document.createElement('li');
            li.textContent = `${report.text} (Lat: ${report.lat}, Lon: ${report.lon})`;
            list.appendChild(li);
            // Add marker on map
            new mapboxgl.Marker({ color: 'blue' }).setLngLat([report.lon, report.lat]).addTo(map);
        }
    });
}

// Toggle alerts
document.getElementById('alert-toggle').addEventListener('click', () => {
    alertsEnabled = !alertsEnabled;
    document.getElementById('alert-toggle').textContent = alertsEnabled ? 'Disable Alerts' : 'Enable Alerts';
    if (alertsEnabled && 'Notification' in window) {
        Notification.requestPermission();
    }
});

// Show browser alert
function showAlert(message) {
    if (Notification.permission === 'granted') {
        new Notification('Air Quality Alert', { body: message });
    } else {
        alert(message);
    }
}

// Load reports on page load
loadReports();
