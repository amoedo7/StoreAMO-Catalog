package com.desarrollamo.climaamo;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class WeatherRepository {
    static final String OPEN_METEO = "Open-Meteo";
    static final String MET_NORWAY = "MET Norway";
    private static final String USER_AGENT = "ClimaAMO/0.2.1 https://desarrollamo.com.ar";

    Place geocode(String query) throws Exception {
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8.name());
        JSONObject json = getJson(
                "https://geocoding-api.open-meteo.com/v1/search?name=" + encoded + "&count=1&language=es&format=json",
                USER_AGENT
        );
        JSONArray results = json.optJSONArray("results");
        if (results == null || results.length() == 0) {
            throw new IllegalArgumentException("No encontré esa ciudad.");
        }
        JSONObject first = results.getJSONObject(0);
        String name = first.optString("name", query);
        String admin = first.optString("admin1", "");
        String country = first.optString("country", "");
        String display = name;
        if (!admin.isEmpty() && !admin.equalsIgnoreCase(name)) display += ", " + admin;
        if (!country.isEmpty()) display += " · " + country;
        return new Place(first.getDouble("latitude"), first.getDouble("longitude"), display);
    }

    Forecast load(Place place) throws Exception {
        Forecast forecast = fetchOpenMeteo(place);
        try {
            forecast.met = fetchMetNorway(place, forecast.utcOffsetSeconds, forecast.days);
        } catch (Exception ignored) {
            forecast.met = null;
        }
        try {
            forecast.airQuality = fetchAirQuality(place);
        } catch (Exception ignored) {
            forecast.airQuality = null;
        }
        return forecast;
    }

    private Forecast fetchOpenMeteo(Place place) throws Exception {
        String url = "https://api.open-meteo.com/v1/forecast"
                + "?latitude=" + place.latitude
                + "&longitude=" + place.longitude
                + "&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,precipitation,weather_code,cloud_cover,pressure_msl,surface_pressure,wind_speed_10m,wind_direction_10m,wind_gusts_10m"
                + "&hourly=temperature_2m,relative_humidity_2m,apparent_temperature,precipitation_probability,precipitation,weather_code,cloud_cover,visibility,pressure_msl,wind_speed_10m,wind_direction_10m,wind_gusts_10m,is_day"
                + "&daily=weather_code,temperature_2m_max,temperature_2m_min,apparent_temperature_max,apparent_temperature_min,sunrise,sunset,precipitation_sum,precipitation_probability_max,wind_speed_10m_max,wind_gusts_10m_max,wind_direction_10m_dominant,uv_index_max"
                + "&timezone=auto&forecast_days=5";

        JSONObject json = getJson(url, USER_AGENT);
        JSONObject current = json.getJSONObject("current");
        JSONObject hourly = json.getJSONObject("hourly");
        JSONObject daily = json.getJSONObject("daily");

        Forecast f = new Forecast();
        f.place = place;
        f.location = place.displayName;
        f.utcOffsetSeconds = json.optInt("utc_offset_seconds", 0);
        f.currentTime = current.optString("time", "");
        f.temperature = current.optDouble("temperature_2m", Double.NaN);
        f.apparent = current.optDouble("apparent_temperature", Double.NaN);
        f.humidity = current.optInt("relative_humidity_2m", -1);
        f.wind = current.optDouble("wind_speed_10m", Double.NaN);
        f.windDirection = current.optDouble("wind_direction_10m", Double.NaN);
        f.gust = current.optDouble("wind_gusts_10m", Double.NaN);
        f.precipitation = current.optDouble("precipitation", Double.NaN);
        f.cloudCover = current.optInt("cloud_cover", -1);
        f.pressureMsl = current.optDouble("pressure_msl", Double.NaN);
        f.surfacePressure = current.optDouble("surface_pressure", Double.NaN);
        f.weatherCode = current.optInt("weather_code", -1);
        f.isDay = current.optInt("is_day", 1) == 1;

        JSONArray hTimes = hourly.getJSONArray("time");
        JSONArray hTemp = hourly.getJSONArray("temperature_2m");
        JSONArray hHumidity = hourly.getJSONArray("relative_humidity_2m");
        JSONArray hApparent = hourly.getJSONArray("apparent_temperature");
        JSONArray hProb = hourly.getJSONArray("precipitation_probability");
        JSONArray hPrecip = hourly.getJSONArray("precipitation");
        JSONArray hCode = hourly.getJSONArray("weather_code");
        JSONArray hCloud = hourly.getJSONArray("cloud_cover");
        JSONArray hVisibility = hourly.getJSONArray("visibility");
        JSONArray hPressure = hourly.getJSONArray("pressure_msl");
        JSONArray hWind = hourly.getJSONArray("wind_speed_10m");
        JSONArray hWindDir = hourly.getJSONArray("wind_direction_10m");
        JSONArray hGust = hourly.getJSONArray("wind_gusts_10m");
        JSONArray hIsDay = hourly.getJSONArray("is_day");

        for (int i = 0; i < hTimes.length(); i++) {
            Hour h = new Hour();
            h.time = hTimes.optString(i, "");
            h.temperature = optDouble(hTemp, i);
            h.humidity = optInt(hHumidity, i);
            h.apparent = optDouble(hApparent, i);
            h.precipProbability = optInt(hProb, i);
            h.precipitation = optDouble(hPrecip, i);
            h.code = optInt(hCode, i);
            h.cloudCover = optInt(hCloud, i);
            h.visibilityKm = safeDivide(optDouble(hVisibility, i), 1000.0);
            h.pressureMsl = optDouble(hPressure, i);
            h.wind = optDouble(hWind, i);
            h.windDirection = optDouble(hWindDir, i);
            h.gust = optDouble(hGust, i);
            h.isDay = optInt(hIsDay, i) != 0;
            f.allHours.add(h);
        }

        int currentIndex = findCurrentHour(f.allHours, f.currentTime);
        if (!f.allHours.isEmpty()) {
            Hour nowHour = f.allHours.get(Math.min(currentIndex, f.allHours.size() - 1));
            f.currentPrecipProbability = nowHour.precipProbability;
            f.visibilityKm = nowHour.visibilityKm;
        }
        for (int i = currentIndex; i < Math.min(currentIndex + 12, f.allHours.size()); i++) {
            f.nextHours.add(f.allHours.get(i));
        }

        JSONArray dTimes = daily.getJSONArray("time");
        JSONArray dCode = daily.getJSONArray("weather_code");
        JSONArray dMax = daily.getJSONArray("temperature_2m_max");
        JSONArray dMin = daily.getJSONArray("temperature_2m_min");
        JSONArray dAppMax = daily.getJSONArray("apparent_temperature_max");
        JSONArray dAppMin = daily.getJSONArray("apparent_temperature_min");
        JSONArray dSunrise = daily.getJSONArray("sunrise");
        JSONArray dSunset = daily.getJSONArray("sunset");
        JSONArray dRain = daily.getJSONArray("precipitation_sum");
        JSONArray dRainProb = daily.getJSONArray("precipitation_probability_max");
        JSONArray dWind = daily.getJSONArray("wind_speed_10m_max");
        JSONArray dGust = daily.getJSONArray("wind_gusts_10m_max");
        JSONArray dWindDir = daily.getJSONArray("wind_direction_10m_dominant");
        JSONArray dUv = daily.getJSONArray("uv_index_max");

        int dayCount = Math.min(5, dTimes.length());
        for (int i = 0; i < dayCount; i++) {
            Day d = new Day();
            d.isoDate = dTimes.optString(i, "");
            d.code = optInt(dCode, i);
            d.max = optDouble(dMax, i);
            d.min = optDouble(dMin, i);
            d.apparentMax = optDouble(dAppMax, i);
            d.apparentMin = optDouble(dAppMin, i);
            d.sunrise = dSunrise.optString(i, "");
            d.sunset = dSunset.optString(i, "");
            d.precipitationSum = optDouble(dRain, i);
            d.precipProbabilityMax = optInt(dRainProb, i);
            d.windMax = optDouble(dWind, i);
            d.gustMax = optDouble(dGust, i);
            d.windDirection = optDouble(dWindDir, i);
            d.uvMax = optDouble(dUv, i);

            for (Hour h : f.allHours) {
                if (h.time.startsWith(d.isoDate) && h.humidity >= 0) {
                    if (d.humidityMin < 0 || h.humidity < d.humidityMin) d.humidityMin = h.humidity;
                    if (d.humidityMax < 0 || h.humidity > d.humidityMax) d.humidityMax = h.humidity;
                }
            }
            f.days.add(d);
        }
        return f;
    }

    private MetData fetchMetNorway(Place place, int utcOffsetSeconds, List<Day> targetDays) throws Exception {
        String lat = String.format(Locale.US, "%.4f", place.latitude);
        String lon = String.format(Locale.US, "%.4f", place.longitude);
        JSONObject json = getJson(
                "https://api.met.no/weatherapi/locationforecast/2.0/compact?lat=" + lat + "&lon=" + lon,
                USER_AGENT
        );
        JSONArray timeseries = json.getJSONObject("properties").getJSONArray("timeseries");
        MetData out = new MetData();
        Map<String, MetDay> byDate = new LinkedHashMap<>();
        for (Day d : targetDays) byDate.put(d.isoDate, new MetDay(d.isoDate));
        ZoneOffset offset = ZoneOffset.ofTotalSeconds(utcOffsetSeconds);

        for (int i = 0; i < timeseries.length(); i++) {
            JSONObject point = timeseries.getJSONObject(i);
            String utc = point.optString("time", "");
            String localDate = "";
            String localTime = utc;
            try {
                OffsetDateTime local = OffsetDateTime.ofInstant(Instant.parse(utc), offset);
                localDate = local.toLocalDate().toString();
                localTime = local.toLocalDateTime().toString();
            } catch (Exception ignored) { }

            JSONObject data = point.optJSONObject("data");
            if (data == null) continue;
            JSONObject instant = data.optJSONObject("instant");
            JSONObject details = instant == null ? null : instant.optJSONObject("details");
            if (details == null) continue;

            double temp = details.optDouble("air_temperature", Double.NaN);
            int humidity = details.has("relative_humidity") ? (int) Math.round(details.optDouble("relative_humidity", -1)) : -1;
            double windKmh = mpsToKmh(details.optDouble("wind_speed", Double.NaN));
            double windDirection = details.optDouble("wind_from_direction", Double.NaN);
            double pressure = details.optDouble("air_pressure_at_sea_level", Double.NaN);
            double precip1h = Double.NaN;
            JSONObject next1 = data.optJSONObject("next_1_hours");
            if (next1 != null) {
                JSONObject nextDetails = next1.optJSONObject("details");
                if (nextDetails != null) precip1h = nextDetails.optDouble("precipitation_amount", Double.NaN);
            }

            if (out.current == null) {
                MetSnapshot current = new MetSnapshot();
                current.time = localTime;
                current.temperature = temp;
                current.humidity = humidity;
                current.wind = windKmh;
                current.windDirection = windDirection;
                current.pressureMsl = pressure;
                current.precipitationNextHour = precip1h;
                out.current = current;
            }

            MetDay day = byDate.get(localDate);
            if (day == null) continue;
            if (Double.isFinite(temp)) {
                if (!Double.isFinite(day.min) || temp < day.min) day.min = temp;
                if (!Double.isFinite(day.max) || temp > day.max) day.max = temp;
            }
            if (humidity >= 0) {
                if (day.humidityMin < 0 || humidity < day.humidityMin) day.humidityMin = humidity;
                if (day.humidityMax < 0 || humidity > day.humidityMax) day.humidityMax = humidity;
            }
            if (Double.isFinite(windKmh) && (!Double.isFinite(day.windMax) || windKmh > day.windMax)) {
                day.windMax = windKmh;
            }
            if (Double.isFinite(precip1h)) {
                if (!Double.isFinite(day.precipitationSum)) day.precipitationSum = 0.0;
                day.precipitationSum += precip1h;
            }
        }
        out.days.addAll(byDate.values());
        return out;
    }

    private AirQuality fetchAirQuality(Place place) throws Exception {
        String url = "https://air-quality-api.open-meteo.com/v1/air-quality"
                + "?latitude=" + place.latitude
                + "&longitude=" + place.longitude
                + "&current=european_aqi,us_aqi,pm10,pm2_5,ozone"
                + "&timezone=auto";
        JSONObject json = getJson(url, USER_AGENT);
        JSONObject current = json.optJSONObject("current");
        if (current == null) return null;
        AirQuality aq = new AirQuality();
        aq.time = current.optString("time", "");
        aq.europeanAqi = current.optDouble("european_aqi", Double.NaN);
        aq.usAqi = current.optDouble("us_aqi", Double.NaN);
        aq.pm10 = current.optDouble("pm10", Double.NaN);
        aq.pm25 = current.optDouble("pm2_5", Double.NaN);
        aq.ozone = current.optDouble("ozone", Double.NaN);
        return aq;
    }

    private JSONObject getJson(String urlString, String userAgent) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(urlString).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(12000);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("User-Agent", userAgent);
        int code = connection.getResponseCode();
        if (code < 200 || code >= 300) {
            connection.disconnect();
            throw new IllegalStateException("El servicio respondió " + code + ".");
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder out = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) out.append(line);
            return new JSONObject(out.toString());
        } finally {
            connection.disconnect();
        }
    }

    static int findCurrentHour(List<Hour> hours, String currentTime) {
        if (hours.isEmpty() || currentTime == null || currentTime.length() < 13) return 0;
        String key = currentTime.substring(0, 13);
        for (int i = 0; i < hours.size(); i++) {
            if (hours.get(i).time.startsWith(key)) return i;
        }
        try {
            LocalDateTime current = LocalDateTime.parse(currentTime);
            for (int i = 0; i < hours.size(); i++) {
                LocalDateTime candidate = LocalDateTime.parse(hours.get(i).time);
                if (!candidate.isBefore(current)) return i;
            }
        } catch (Exception ignored) { }
        return 0;
    }

    static double mpsToKmh(double value) {
        return Double.isFinite(value) ? value * 3.6 : Double.NaN;
    }

    private static double optDouble(JSONArray array, int index) {
        if (array == null || index < 0 || index >= array.length() || array.isNull(index)) return Double.NaN;
        return array.optDouble(index, Double.NaN);
    }

    private static int optInt(JSONArray array, int index) {
        if (array == null || index < 0 || index >= array.length() || array.isNull(index)) return -1;
        return array.optInt(index, -1);
    }

    private static double safeDivide(double value, double divisor) {
        return Double.isFinite(value) ? value / divisor : Double.NaN;
    }

    static final class Place {
        final double latitude;
        final double longitude;
        final String displayName;

        Place(double latitude, double longitude, String displayName) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.displayName = displayName;
        }
    }

    static final class Forecast {
        Place place;
        String location;
        int utcOffsetSeconds;
        String currentTime;
        double temperature = Double.NaN;
        double apparent = Double.NaN;
        int humidity = -1;
        double wind = Double.NaN;
        double windDirection = Double.NaN;
        double gust = Double.NaN;
        double precipitation = Double.NaN;
        int currentPrecipProbability = -1;
        int cloudCover = -1;
        double visibilityKm = Double.NaN;
        double pressureMsl = Double.NaN;
        double surfacePressure = Double.NaN;
        int weatherCode = -1;
        boolean isDay = true;
        final List<Hour> nextHours = new ArrayList<>();
        final List<Hour> allHours = new ArrayList<>();
        final List<Day> days = new ArrayList<>();
        MetData met;
        AirQuality airQuality;
    }

    static final class Hour {
        String time = "";
        double temperature = Double.NaN;
        double apparent = Double.NaN;
        int humidity = -1;
        int precipProbability = -1;
        double precipitation = Double.NaN;
        int code = -1;
        int cloudCover = -1;
        double visibilityKm = Double.NaN;
        double pressureMsl = Double.NaN;
        double wind = Double.NaN;
        double windDirection = Double.NaN;
        double gust = Double.NaN;
        boolean isDay = true;
    }

    static final class Day {
        String isoDate = "";
        int code = -1;
        double max = Double.NaN;
        double min = Double.NaN;
        double apparentMax = Double.NaN;
        double apparentMin = Double.NaN;
        String sunrise = "";
        String sunset = "";
        double precipitationSum = Double.NaN;
        int precipProbabilityMax = -1;
        double windMax = Double.NaN;
        double gustMax = Double.NaN;
        double windDirection = Double.NaN;
        double uvMax = Double.NaN;
        int humidityMin = -1;
        int humidityMax = -1;
    }

    static final class MetData {
        MetSnapshot current;
        final List<MetDay> days = new ArrayList<>();

        MetDay findDay(String isoDate) {
            for (MetDay day : days) if (day.isoDate.equals(isoDate)) return day;
            return null;
        }
    }

    static final class MetSnapshot {
        String time = "";
        double temperature = Double.NaN;
        int humidity = -1;
        double wind = Double.NaN;
        double windDirection = Double.NaN;
        double pressureMsl = Double.NaN;
        double precipitationNextHour = Double.NaN;
    }

    static final class MetDay {
        final String isoDate;
        double max = Double.NaN;
        double min = Double.NaN;
        double precipitationSum = Double.NaN;
        double windMax = Double.NaN;
        int humidityMin = -1;
        int humidityMax = -1;

        MetDay(String isoDate) {
            this.isoDate = isoDate;
        }
    }

    static final class AirQuality {
        String time = "";
        double europeanAqi = Double.NaN;
        double usAqi = Double.NaN;
        double pm10 = Double.NaN;
        double pm25 = Double.NaN;
        double ozone = Double.NaN;
    }
}
