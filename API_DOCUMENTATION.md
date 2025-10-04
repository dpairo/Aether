# Aether - Air Quality API Documentation

## Overview
This API provides air quality data for Spanish cities to support the NASA Space Apps Challenge project focused on predicting cleaner, safer skies.

## Base URL
```
http://localhost:8080/api/v1
```

## Endpoints

### 1. Get Air Quality for All Spanish Provinces
**GET** `/air/quality/provinces`

Get air quality data for all Spanish provinces. This endpoint provides province-level AQI data for map visualization.

**Response:**
```json
[
  {
    "province": "Madrid",
    "provinceCode": "M",
    "latitude": 40.4168,
    "longitude": -3.7038,
    "aqi": 52,
    "aqiStatus": "Moderate",
    "dominantPollutant": "pm25",
    "airQuality": {
      "pm25": 12.3,
      "pm10": 15.1,
      "no2": 25.4,
      "o3": 45.2,
      "co": 1.2,
      "so2": 3.1
    },
    "timestamp": "2025-10-04T10:00:00Z"
  }
]
```

**Example:**
```bash
curl http://localhost:8080/api/v1/air/quality/provinces
```

### 2. Get Air Quality for Cities in a Specific Province
**GET** `/air/quality/province/{provinceCode}/cities`

Get air quality data for all cities within a specific Spanish province.

**Parameters:**
- `provinceCode` (path): Province code (e.g., "M" for Madrid, "B" for Barcelona, "V" for Valencia)

**Response:**
```json
[
  {
    "city": "Madrid",
    "cityId": "madrid",
    "latitude": 40.4168,
    "longitude": -3.7038,
    "aqi": 52,
    "aqiStatus": "Moderate",
    "dominantPollutant": "pm25",
    "airQuality": {
      "pm25": 12.3,
      "pm10": 15.1,
      "no2": 25.4,
      "o3": 45.2,
      "co": 1.2,
      "so2": 3.1
    },
    "timestamp": "2025-10-04T10:00:00Z"
  }
]
```

**Example:**
```bash
curl http://localhost:8080/api/v1/air/quality/province/M/cities
```

### 3. Get Air Quality for a Specific City
**GET** `/air/quality/city/{cityId}`

Get air quality data for a specific Spanish city.

**Parameters:**
- `cityId` (path): City identifier (e.g., "madrid", "barcelona", "valencia", "sevilla", "bilbao", "zaragoza")

**Response:**
```json
{
  "city": "Madrid",
  "cityId": "madrid",
  "latitude": 40.4168,
  "longitude": -3.7038,
  "aqi": 52,
  "aqiStatus": "Moderate",
  "dominantPollutant": "pm25",
  "airQuality": {
    "pm25": 12.3,
    "pm10": 15.1,
    "no2": 25.4,
    "o3": 45.2,
    "co": 1.2,
    "so2": 3.1
  },
  "timestamp": "2025-10-04T10:00:00Z"
}
```

**Example:**
```bash
curl http://localhost:8080/api/v1/air/quality/city/madrid
```

### 2. Get Air Quality for All Spanish Cities
**GET** `/air/quality/cities`

Get air quality data for all configured Spanish cities. This endpoint is designed for frontend map visualization.

**Response:**
```json
[
  {
    "city": "Madrid",
    "cityId": "madrid",
    "latitude": 40.4168,
    "longitude": -3.7038,
    "aqi": 52,
    "aqiStatus": "Moderate",
    "dominantPollutant": "pm25",
    "airQuality": {
      "pm25": 12.3,
      "pm10": 15.1,
      "no2": 25.4,
      "o3": 45.2,
      "co": 1.2,
      "so2": 3.1
    },
    "timestamp": "2025-10-04T10:00:00Z"
  },
  {
    "city": "Barcelona",
    "cityId": "barcelona",
    "latitude": 41.3851,
    "longitude": 2.1734,
    "aqi": 45,
    "aqiStatus": "Good",
    "dominantPollutant": "pm10",
    "airQuality": {
      "pm25": 8.2,
      "pm10": 12.5,
      "no2": 18.3,
      "o3": 38.7,
      "co": 0.9,
      "so2": 2.4
    },
    "timestamp": "2025-10-04T10:00:00Z"
  }
]
```

**Example:**
```bash
curl http://localhost:8080/api/v1/air/quality/cities
```

## AQI Status Levels
- **Good** (0-50): Air quality is satisfactory
- **Moderate** (51-100): Air quality is acceptable for most people
- **Unhealthy for Sensitive Groups** (101-150): Sensitive individuals may experience health effects
- **Unhealthy** (151-200): Everyone may begin to experience health effects
- **Very Unhealthy** (201-300): Health alert - everyone may experience more serious health effects
- **Hazardous** (301+): Health warnings of emergency conditions

## Available Provinces and Cities

### Provinces (with codes):
- Madrid (M)
- Barcelona (B)
- Valencia (V)
- Sevilla (SE)
- Bilbao (BI)
- Zaragoza (Z)
- Málaga (MA)
- Murcia (MU)
- Palma (PM)
- Las Palmas (GC)
- Alicante (A)
- Córdoba (CO)
- Valladolid (VA)
- Vigo (PO)
- Gijón (O)

### Cities by Province:
- **Madrid (M)**: Madrid, Alcalá de Henares, Getafe
- **Barcelona (B)**: Barcelona, L'Hospitalet, Badalona
- **Valencia (V)**: Valencia, Gandía, Sagunto
- **Sevilla (SE)**: Sevilla, Dos Hermanas, Alcalá de Guadaíra
- **Bilbao (BI)**: Bilbao, Barakaldo, Getxo
- **Zaragoza (Z)**: Zaragoza, Calatayud, Ejea de los Caballeros

## Configuration
The API uses the WAQI (World Air Quality Index) service. To use with real data:

1. Get a free token from [WAQI](https://waqi.info/api/)
2. Set the environment variable: `export WAQI_TOKEN=your_token_here`
3. Restart the application

## Error Handling
- If a city is not found, returns HTTP 404
- If the external API is unavailable, returns default values with "Unknown" status
- All endpoints include proper error handling and timeouts

## Frontend Integration

### For Province-Level Maps:
Use the `/air/quality/provinces` endpoint to get all provinces' data at once:
```javascript
// Fetch all provinces' air quality data
fetch('/api/v1/air/quality/provinces')
  .then(response => response.json())
  .then(provinces => {
    provinces.forEach(province => {
      // Use province.latitude, province.longitude for map positioning
      // Use province.aqi for color coding
      // Use province.aqiStatus for display text
      // Use province.provinceCode for province identification
    });
  });
```

### For City-Level Maps within a Province:
Use the `/air/quality/province/{provinceCode}/cities` endpoint:
```javascript
// Fetch cities within a specific province
fetch('/api/v1/air/quality/province/M/cities')
  .then(response => response.json())
  .then(cities => {
    cities.forEach(city => {
      // Use city.latitude, city.longitude for map positioning
      // Use city.aqi for color coding
      // Use city.aqiStatus for display text
    });
  });
```

### For All Cities (Original):
```javascript
// Fetch all cities' air quality data
fetch('/api/v1/air/quality/cities')
  .then(response => response.json())
  .then(cities => {
    cities.forEach(city => {
      // Use city.latitude, city.longitude for map positioning
      // Use city.aqi for color coding
      // Use city.aqiStatus for display text
    });
  });
```
