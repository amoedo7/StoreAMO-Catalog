package com.desarrollamo.climaamo;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int BG = Color.rgb(7, 17, 31);
    private static final int PANEL = Color.rgb(16, 31, 49);
    private static final int PANEL_2 = Color.rgb(20, 39, 61);
    private static final int TEXT = Color.rgb(238, 246, 255);
    private static final int MUTED = Color.rgb(154, 177, 201);
    private static final int ACCENT = Color.rgb(246, 183, 60);
    private static final int CYAN = Color.rgb(90, 178, 255);
    private static final int ERROR = Color.rgb(255, 134, 134);
    private static final int STROKE = Color.rgb(32, 54, 78);

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private EditText cityInput;
    private Button searchButton;
    private ProgressBar progressBar;
    private TextView statusText;

    private LinearLayout currentCard;
    private TextView locationText;
    private TextView currentIcon;
    private TextView currentTemp;
    private TextView currentDescription;
    private TextView currentToday;
    private TextView detailText;

    private TextView hourlyTitle;
    private HorizontalScrollView hourlyScroll;
    private LinearLayout hourlyContainer;

    private TextView detailsTitle;
    private LinearLayout detailsContainer;

    private TextView forecastTitle;
    private LinearLayout forecastContainer;

    private LinearLayout tempTrendCard;
    private WeatherTrendView tempTrendView;
    private LinearLayout moistureTrendCard;
    private WeatherTrendView moistureTrendView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        setContentView(buildUi());
    }

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(22), dp(20), dp(34));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));

        LinearLayout brandRow = new LinearLayout(this);
        brandRow.setOrientation(LinearLayout.HORIZONTAL);
        brandRow.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(brandRow, new LinearLayout.LayoutParams(-1, -2));

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.ic_climaamo);
        brandRow.addView(logo, new LinearLayout.LayoutParams(dp(48), dp(48)));

        LinearLayout brandTextWrap = new LinearLayout(this);
        brandTextWrap.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams brandTextLp = new LinearLayout.LayoutParams(0, -2, 1f);
        brandTextLp.leftMargin = dp(12);
        brandRow.addView(brandTextWrap, brandTextLp);

        brandTextWrap.addView(text("ClimaAMO", 24, TEXT, true));
        brandTextWrap.addView(text("DESARROLLAMO · candidate 0.2.0", 11, ACCENT, true));

        TextView intro = text("El clima que importa, sin cuentas ni claves.", 15, MUTED, false);
        LinearLayout.LayoutParams introLp = new LinearLayout.LayoutParams(-1, -2);
        introLp.topMargin = dp(18);
        root.addView(intro, introLp);

        LinearLayout searchCard = card();
        LinearLayout.LayoutParams searchCardLp = new LinearLayout.LayoutParams(-1, -2);
        searchCardLp.topMargin = dp(18);
        root.addView(searchCard, searchCardLp);
        searchCard.addView(text("BUSCAR CIUDAD", 11, MUTED, true));

        LinearLayout searchRow = new LinearLayout(this);
        searchRow.setOrientation(LinearLayout.HORIZONTAL);
        searchRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams searchRowLp = new LinearLayout.LayoutParams(-1, -2);
        searchRowLp.topMargin = dp(10);
        searchCard.addView(searchRow, searchRowLp);

        cityInput = new EditText(this);
        cityInput.setHint("Ej. Pergamino, Malmö, Barcelona");
        cityInput.setHintTextColor(Color.rgb(103, 128, 153));
        cityInput.setTextColor(TEXT);
        cityInput.setTextSize(15);
        cityInput.setSingleLine(true);
        cityInput.setPadding(dp(14), 0, dp(14), 0);
        cityInput.setBackground(rounded(PANEL_2, dp(14), Color.rgb(46, 70, 95)));
        cityInput.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
        searchRow.addView(cityInput, new LinearLayout.LayoutParams(0, dp(50), 1f));

        searchButton = new Button(this);
        searchButton.setText("Buscar");
        searchButton.setTextColor(BG);
        searchButton.setTextSize(14);
        searchButton.setTypeface(Typeface.DEFAULT_BOLD);
        searchButton.setAllCaps(false);
        searchButton.setBackground(rounded(ACCENT, dp(14), ACCENT));
        LinearLayout.LayoutParams buttonLp = new LinearLayout.LayoutParams(dp(92), dp(50));
        buttonLp.leftMargin = dp(10);
        searchRow.addView(searchButton, buttonLp);

        LinearLayout statusRow = new LinearLayout(this);
        statusRow.setOrientation(LinearLayout.HORIZONTAL);
        statusRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams statusRowLp = new LinearLayout.LayoutParams(-1, -2);
        statusRowLp.topMargin = dp(10);
        searchCard.addView(statusRow, statusRowLp);

        progressBar = new ProgressBar(this);
        progressBar.setIndeterminate(true);
        progressBar.setVisibility(View.GONE);
        statusRow.addView(progressBar, new LinearLayout.LayoutParams(dp(22), dp(22)));

        statusText = text("Escribí una ciudad para empezar.", 13, MUTED, false);
        LinearLayout.LayoutParams statusLp = new LinearLayout.LayoutParams(0, -2, 1f);
        statusLp.leftMargin = dp(8);
        statusRow.addView(statusText, statusLp);

        currentCard = card();
        currentCard.setVisibility(View.GONE);
        LinearLayout.LayoutParams currentLp = new LinearLayout.LayoutParams(-1, -2);
        currentLp.topMargin = dp(16);
        root.addView(currentCard, currentLp);

        locationText = text("", 15, MUTED, true);
        currentCard.addView(locationText);

        LinearLayout tempRow = new LinearLayout(this);
        tempRow.setOrientation(LinearLayout.HORIZONTAL);
        tempRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams tempRowLp = new LinearLayout.LayoutParams(-1, -2);
        tempRowLp.topMargin = dp(8);
        currentCard.addView(tempRow, tempRowLp);

        currentIcon = text("", 42, TEXT, false);
        tempRow.addView(currentIcon, new LinearLayout.LayoutParams(dp(66), -2));

        LinearLayout tempWrap = new LinearLayout(this);
        tempWrap.setOrientation(LinearLayout.VERTICAL);
        tempRow.addView(tempWrap, new LinearLayout.LayoutParams(0, -2, 1f));
        currentTemp = text("", 42, TEXT, true);
        tempWrap.addView(currentTemp);
        currentDescription = text("", 16, ACCENT, true);
        tempWrap.addView(currentDescription);

        currentToday = text("", 13, TEXT, true);
        LinearLayout.LayoutParams todayLp = new LinearLayout.LayoutParams(-1, -2);
        todayLp.topMargin = dp(10);
        currentCard.addView(currentToday, todayLp);

        detailText = text("", 14, MUTED, false);
        LinearLayout.LayoutParams detailLp = new LinearLayout.LayoutParams(-1, -2);
        detailLp.topMargin = dp(8);
        currentCard.addView(detailText, detailLp);

        hourlyTitle = sectionTitle("PRÓXIMAS HORAS");
        hourlyTitle.setVisibility(View.GONE);
        root.addView(hourlyTitle, sectionLp(22));

        hourlyScroll = new HorizontalScrollView(this);
        hourlyScroll.setHorizontalScrollBarEnabled(false);
        hourlyScroll.setVisibility(View.GONE);
        hourlyContainer = new LinearLayout(this);
        hourlyContainer.setOrientation(LinearLayout.HORIZONTAL);
        hourlyScroll.addView(hourlyContainer, new HorizontalScrollView.LayoutParams(-2, -2));
        root.addView(hourlyScroll, sectionLp(8));

        detailsTitle = sectionTitle("AHORA · DETALLES REALES");
        detailsTitle.setVisibility(View.GONE);
        root.addView(detailsTitle, sectionLp(22));
        detailsContainer = new LinearLayout(this);
        detailsContainer.setOrientation(LinearLayout.VERTICAL);
        detailsContainer.setVisibility(View.GONE);
        root.addView(detailsContainer, sectionLp(8));

        forecastTitle = sectionTitle("PRÓXIMOS 5 DÍAS");
        forecastTitle.setVisibility(View.GONE);
        root.addView(forecastTitle, sectionLp(22));
        forecastContainer = new LinearLayout(this);
        forecastContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(forecastContainer, sectionLp(8));

        tempTrendCard = card();
        tempTrendCard.setVisibility(View.GONE);
        tempTrendCard.addView(text("Tendencia térmica", 19, TEXT, true));
        TextView tempSub = text("Máxima y mínima previstas por día, tomadas directamente del pronóstico.", 13, MUTED, false);
        LinearLayout.LayoutParams tempSubLp = new LinearLayout.LayoutParams(-1, -2);
        tempSubLp.topMargin = dp(6);
        tempTrendCard.addView(tempSub, tempSubLp);
        TextView tempLegend = text("● Máxima     ● Mínima", 12, MUTED, true);
        tempLegend.setTextColor(ACCENT);
        LinearLayout.LayoutParams tempLegendLp = new LinearLayout.LayoutParams(-1, -2);
        tempLegendLp.topMargin = dp(10);
        tempTrendCard.addView(tempLegend, tempLegendLp);
        tempTrendView = new WeatherTrendView(this);
        LinearLayout.LayoutParams chartLp = new LinearLayout.LayoutParams(-1, dp(240));
        chartLp.topMargin = dp(8);
        tempTrendCard.addView(tempTrendView, chartLp);
        root.addView(tempTrendCard, sectionLp(18));

        moistureTrendCard = card();
        moistureTrendCard.setVisibility(View.GONE);
        moistureTrendCard.addView(text("Tendencia de humedad y lluvia", 19, TEXT, true));
        TextView moistureSub = text("Humedad horaria mínima y máxima por día; lluvia acumulada en barras.", 13, MUTED, false);
        LinearLayout.LayoutParams moistureSubLp = new LinearLayout.LayoutParams(-1, -2);
        moistureSubLp.topMargin = dp(6);
        moistureTrendCard.addView(moistureSub, moistureSubLp);
        TextView moistureLegend = text("● Humedad máx.     ● Humedad mín.     ▮ Lluvia", 12, CYAN, true);
        LinearLayout.LayoutParams moistureLegendLp = new LinearLayout.LayoutParams(-1, -2);
        moistureLegendLp.topMargin = dp(10);
        moistureTrendCard.addView(moistureLegend, moistureLegendLp);
        moistureTrendView = new WeatherTrendView(this);
        LinearLayout.LayoutParams moistureChartLp = new LinearLayout.LayoutParams(-1, dp(240));
        moistureChartLp.topMargin = dp(8);
        moistureTrendCard.addView(moistureTrendView, moistureChartLp);
        root.addView(moistureTrendCard, sectionLp(12));

        TextView source = text("Datos meteorológicos reales: Open-Meteo Forecast API · sin API key. Los valores pueden diferir de Google o Meteored porque usan modelos y actualizaciones distintas.", 11, Color.rgb(95, 120, 145), false);
        source.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams sourceLp = new LinearLayout.LayoutParams(-1, -2);
        sourceLp.topMargin = dp(24);
        root.addView(source, sourceLp);

        searchButton.setOnClickListener(v -> performSearch());
        cityInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });

        return scroll;
    }

    private void performSearch() {
        String raw = cityInput.getText().toString().trim();
        if (raw.length() < 2) {
            showStatus("Escribí al menos 2 caracteres.", true);
            return;
        }

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(cityInput.getWindowToken(), 0);

        setLoading(true);
        executor.execute(() -> {
            try {
                Place place = geocode(raw);
                Forecast forecast = fetchForecast(place);
                runOnUiThread(() -> renderForecast(forecast));
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    showStatus(readableError(e), true);
                });
            }
        });
    }

    private Place geocode(String query) throws Exception {
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8.name());
        String url = "https://geocoding-api.open-meteo.com/v1/search?name=" + encoded + "&count=1&language=es&format=json";
        JSONObject json = getJson(url);
        JSONArray results = json.optJSONArray("results");
        if (results == null || results.length() == 0) throw new IllegalArgumentException("No encontré esa ciudad.");
        JSONObject first = results.getJSONObject(0);
        String name = first.optString("name", query);
        String admin = first.optString("admin1", "");
        String country = first.optString("country", "");
        String display = name;
        if (!admin.isEmpty() && !admin.equalsIgnoreCase(name)) display += ", " + admin;
        if (!country.isEmpty()) display += " · " + country;
        return new Place(first.getDouble("latitude"), first.getDouble("longitude"), display);
    }

    private Forecast fetchForecast(Place place) throws Exception {
        String url = "https://api.open-meteo.com/v1/forecast"
                + "?latitude=" + place.latitude
                + "&longitude=" + place.longitude
                + "&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,precipitation,weather_code,cloud_cover,pressure_msl,surface_pressure,wind_speed_10m,wind_direction_10m,wind_gusts_10m"
                + "&hourly=temperature_2m,relative_humidity_2m,apparent_temperature,precipitation_probability,precipitation,weather_code,cloud_cover,visibility,pressure_msl,wind_speed_10m,wind_direction_10m,wind_gusts_10m,is_day"
                + "&daily=weather_code,temperature_2m_max,temperature_2m_min,apparent_temperature_max,apparent_temperature_min,sunrise,sunset,precipitation_sum,precipitation_probability_max,wind_speed_10m_max,wind_gusts_10m_max,wind_direction_10m_dominant,uv_index_max"
                + "&timezone=auto&forecast_days=5";

        JSONObject json = getJson(url);
        JSONObject current = json.getJSONObject("current");
        JSONObject hourly = json.getJSONObject("hourly");
        JSONObject daily = json.getJSONObject("daily");

        Forecast f = new Forecast();
        f.location = place.displayName;
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

        List<Hour> allHours = new ArrayList<>();
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
            allHours.add(h);
        }

        int currentIndex = findCurrentHour(allHours, f.currentTime);
        if (!allHours.isEmpty()) {
            Hour nowHour = allHours.get(Math.min(currentIndex, allHours.size() - 1));
            f.currentPrecipProbability = nowHour.precipProbability;
            f.visibilityKm = nowHour.visibilityKm;
        }
        for (int i = currentIndex; i < Math.min(currentIndex + 12, allHours.size()); i++) f.hours.add(allHours.get(i));

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

            int minHumidity = Integer.MAX_VALUE;
            int maxHumidity = Integer.MIN_VALUE;
            for (Hour h : allHours) {
                if (h.time.startsWith(d.isoDate) && h.humidity >= 0) {
                    minHumidity = Math.min(minHumidity, h.humidity);
                    maxHumidity = Math.max(maxHumidity, h.humidity);
                }
            }
            if (minHumidity != Integer.MAX_VALUE) {
                d.humidityMin = minHumidity;
                d.humidityMax = maxHumidity;
            }
            f.days.add(d);
        }
        return f;
    }

    private JSONObject getJson(String urlString) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(urlString).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(9000);
        connection.setReadTimeout(9000);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("User-Agent", "ClimaAMO/0.2.0");
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

    private void renderForecast(Forecast f) {
        setLoading(false);
        String updated = displayTime(f.currentTime);
        showStatus(updated.isEmpty() ? "Datos recibidos de Open-Meteo." : "Actualizado " + updated + " · Open-Meteo", false);

        currentCard.setVisibility(View.VISIBLE);
        hourlyTitle.setVisibility(View.VISIBLE);
        hourlyScroll.setVisibility(View.VISIBLE);
        detailsTitle.setVisibility(View.VISIBLE);
        detailsContainer.setVisibility(View.VISIBLE);
        forecastTitle.setVisibility(View.VISIBLE);
        tempTrendCard.setVisibility(View.VISIBLE);
        moistureTrendCard.setVisibility(View.VISIBLE);

        locationText.setText(f.location);
        currentIcon.setText(WeatherMapper.emoji(f.weatherCode, f.isDay));
        currentTemp.setText(formatTemp(f.temperature));
        currentDescription.setText(WeatherMapper.label(f.weatherCode));

        if (!f.days.isEmpty()) {
            Day today = f.days.get(0);
            currentToday.setText("Hoy " + formatTemp(today.max) + " / " + formatTemp(today.min)
                    + "   ·   Lluvia " + formatPercent(today.precipProbabilityMax));
        } else currentToday.setText("");

        detailText.setText("Sensación " + formatTemp(f.apparent)
                + "   ·   Humedad " + formatPercent(f.humidity)
                + "   ·   Viento " + formatSpeed(f.wind));

        hourlyContainer.removeAllViews();
        for (int i = 0; i < f.hours.size(); i++) hourlyContainer.addView(hourCard(f.hours.get(i), i == 0));

        detailsContainer.removeAllViews();
        Day today = f.days.isEmpty() ? null : f.days.get(0);
        detailsContainer.addView(metricRow(
                metricCard("Precipitación", formatMm(f.precipitation), "Ahora · prob. " + formatPercent(f.currentPrecipProbability)),
                metricCard("Viento", formatSpeed(f.wind), directionLabel(f.windDirection) + " · ráfagas " + formatSpeed(f.gust))));
        detailsContainer.addView(metricRow(
                metricCard("Humedad", formatPercent(f.humidity), today == null ? "" : "Hoy " + humidityRange(today)),
                metricCard("Presión", formatPressure(f.pressureMsl), "Nivel del mar")));
        detailsContainer.addView(metricRow(
                metricCard("Visibilidad", formatKm(f.visibilityKm), "Dato horario del modelo"),
                metricCard("Nubosidad", formatPercent(f.cloudCover), "Cobertura total")));
        detailsContainer.addView(metricRow(
                metricCard("Sol", today == null ? "N/D" : displayTime(today.sunrise), today == null ? "" : "Atardecer " + displayTime(today.sunset)),
                metricCard("UV máximo", today == null ? "N/D" : formatOne(today.uvMax), "Pronóstico de hoy")));

        forecastContainer.removeAllViews();
        for (Day d : f.days) forecastContainer.addView(dayCard(d));

        String[] labels = new String[f.days.size()];
        double[] max = new double[f.days.size()];
        double[] min = new double[f.days.size()];
        double[] humMax = new double[f.days.size()];
        double[] humMin = new double[f.days.size()];
        double[] rain = new double[f.days.size()];
        for (int i = 0; i < f.days.size(); i++) {
            Day d = f.days.get(i);
            labels[i] = shortDay(d.isoDate);
            max[i] = d.max;
            min[i] = d.min;
            humMax[i] = d.humidityMax >= 0 ? d.humidityMax : Double.NaN;
            humMin[i] = d.humidityMin >= 0 ? d.humidityMin : Double.NaN;
            rain[i] = d.precipitationSum;
        }
        tempTrendView.setData(labels, max, min, null, "°", ACCENT, CYAN);
        moistureTrendView.setData(labels, humMax, humMin, rain, "%", Color.rgb(76, 190, 220), CYAN);
    }

    private View hourCard(Hour h, boolean now) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(dp(10), dp(11), dp(10), dp(11));
        card.setBackground(rounded(PANEL, dp(16), STROKE));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(112), -2);
        lp.rightMargin = dp(8);
        card.setLayoutParams(lp);

        TextView time = text(now ? "Ahora" : displayTime(h.time), 12, TEXT, true);
        time.setGravity(Gravity.CENTER);
        card.addView(time);
        TextView icon = text(WeatherMapper.emoji(h.code, h.isDay), 26, TEXT, false);
        icon.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(-1, -2);
        iconLp.topMargin = dp(5);
        card.addView(icon, iconLp);
        TextView temp = text(formatTemp(h.temperature), 20, TEXT, true);
        temp.setGravity(Gravity.CENTER);
        card.addView(temp);
        TextView rain = text("Lluvia " + formatPercent(h.precipProbability), 11, CYAN, false);
        rain.setGravity(Gravity.CENTER);
        card.addView(rain);
        TextView wind = text(directionLabel(h.windDirection) + " " + formatSpeed(h.wind), 10, MUTED, false);
        wind.setGravity(Gravity.CENTER);
        card.addView(wind);
        TextView humidity = text("Humedad " + formatPercent(h.humidity), 10, MUTED, false);
        humidity.setGravity(Gravity.CENTER);
        card.addView(humidity);
        return card;
    }

    private View dayCard(Day d) {
        LinearLayout box = card();
        LinearLayout.LayoutParams boxLp = new LinearLayout.LayoutParams(-1, -2);
        boxLp.bottomMargin = dp(10);
        box.setLayoutParams(boxLp);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        box.addView(header, new LinearLayout.LayoutParams(-1, -2));

        TextView date = text(fullDay(d.isoDate), 15, TEXT, true);
        header.addView(date, new LinearLayout.LayoutParams(dp(88), -2));
        TextView condition = text(WeatherMapper.emoji(d.code) + "  " + WeatherMapper.label(d.code), 13, MUTED, false);
        header.addView(condition, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView temps = text(formatTemp(d.max) + " / " + formatTemp(d.min), 15, TEXT, true);
        temps.setGravity(Gravity.END);
        header.addView(temps, new LinearLayout.LayoutParams(dp(92), -2));

        TextView line1 = text("Sensación " + formatTemp(d.apparentMax) + " / " + formatTemp(d.apparentMin)
                + "   ·   Lluvia " + formatMm(d.precipitationSum) + " · " + formatPercent(d.precipProbabilityMax), 12, MUTED, false);
        LinearLayout.LayoutParams lineLp = new LinearLayout.LayoutParams(-1, -2);
        lineLp.topMargin = dp(9);
        box.addView(line1, lineLp);

        TextView line2 = text("Viento máx. " + formatSpeed(d.windMax) + "   ·   Ráfagas " + formatSpeed(d.gustMax)
                + "   ·   " + directionLabel(d.windDirection), 12, MUTED, false);
        LinearLayout.LayoutParams line2Lp = new LinearLayout.LayoutParams(-1, -2);
        line2Lp.topMargin = dp(5);
        box.addView(line2, line2Lp);

        TextView line3 = text("Humedad " + humidityRange(d) + "   ·   UV " + formatOne(d.uvMax)
                + "   ·   ☀ " + displayTime(d.sunrise) + "–" + displayTime(d.sunset), 12, MUTED, false);
        LinearLayout.LayoutParams line3Lp = new LinearLayout.LayoutParams(-1, -2);
        line3Lp.topMargin = dp(5);
        box.addView(line3, line3Lp);
        return box;
    }

    private View metricRow(View left, View right) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setBaselineAligned(false);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, -2);
        rowLp.bottomMargin = dp(8);
        row.setLayoutParams(rowLp);

        LinearLayout.LayoutParams leftLp = new LinearLayout.LayoutParams(0, -1, 1f);
        leftLp.rightMargin = dp(4);
        row.addView(left, leftLp);
        LinearLayout.LayoutParams rightLp = new LinearLayout.LayoutParams(0, -1, 1f);
        rightLp.leftMargin = dp(4);
        row.addView(right, rightLp);
        return row;
    }

    private View metricCard(String title, String value, String subtitle) {
        LinearLayout mini = new LinearLayout(this);
        mini.setOrientation(LinearLayout.VERTICAL);
        mini.setPadding(dp(13), dp(12), dp(13), dp(12));
        mini.setBackground(rounded(PANEL_2, dp(14), STROKE));
        mini.addView(text(title, 11, MUTED, true));
        TextView val = text(value, 19, TEXT, true);
        LinearLayout.LayoutParams valLp = new LinearLayout.LayoutParams(-1, -2);
        valLp.topMargin = dp(4);
        mini.addView(val, valLp);
        TextView sub = text(subtitle, 10, MUTED, false);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(-1, -2);
        subLp.topMargin = dp(4);
        mini.addView(sub, subLp);
        return mini;
    }

    private int findCurrentHour(List<Hour> hours, String currentTime) {
        if (hours.isEmpty() || currentTime == null || currentTime.length() < 13) return 0;
        String key = currentTime.substring(0, 13);
        for (int i = 0; i < hours.size(); i++) if (hours.get(i).time.startsWith(key)) return i;
        try {
            LocalDateTime current = LocalDateTime.parse(currentTime);
            for (int i = 0; i < hours.size(); i++) {
                LocalDateTime candidate = LocalDateTime.parse(hours.get(i).time);
                if (!candidate.isBefore(current)) return i;
            }
        } catch (Exception ignored) { }
        return 0;
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        searchButton.setEnabled(!loading);
        cityInput.setEnabled(!loading);
        if (loading) showStatus("Consultando datos reales en Open-Meteo…", false);
    }

    private void showStatus(String message, boolean isError) {
        statusText.setText(message);
        statusText.setTextColor(isError ? ERROR : MUTED);
    }

    private String readableError(Exception e) {
        if (e instanceof IllegalArgumentException && e.getMessage() != null) return e.getMessage();
        return "No pude obtener el clima. Revisá tu conexión e intentá otra vez.";
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackground(rounded(PANEL, dp(18), STROKE));
        return card;
    }

    private TextView sectionTitle(String title) {
        return text(title, 12, MUTED, true);
    }

    private LinearLayout.LayoutParams sectionLp(int topMargin) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(topMargin);
        return lp;
    }

    private TextView text(String value, float sizeSp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(sizeSp);
        t.setTextColor(color);
        t.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL);
        return t;
    }

    private GradientDrawable rounded(int fill, int radius, int stroke) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(radius);
        drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
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

    private String formatTemp(double value) {
        return Double.isFinite(value) ? String.format(Locale.getDefault(), "%.0f°", value) : "N/D";
    }

    private String formatSpeed(double value) {
        return Double.isFinite(value) ? String.format(Locale.getDefault(), "%.0f km/h", value) : "N/D";
    }

    private String formatMm(double value) {
        return Double.isFinite(value) ? String.format(Locale.getDefault(), "%.1f mm", value) : "N/D";
    }

    private String formatPressure(double value) {
        return Double.isFinite(value) ? String.format(Locale.getDefault(), "%.0f hPa", value) : "N/D";
    }

    private String formatKm(double value) {
        return Double.isFinite(value) ? String.format(Locale.getDefault(), "%.1f km", value) : "N/D";
    }

    private String formatOne(double value) {
        return Double.isFinite(value) ? String.format(Locale.getDefault(), "%.1f", value) : "N/D";
    }

    private String formatPercent(int value) {
        return value >= 0 ? value + "%" : "N/D";
    }

    private String directionLabel(double degrees) {
        if (!Double.isFinite(degrees)) return "N/D";
        String[] labels = {"N", "NE", "E", "SE", "S", "SO", "O", "NO"};
        int index = (int) Math.round((((degrees % 360.0) + 360.0) % 360.0) / 45.0) % 8;
        return labels[index];
    }

    private String humidityRange(Day d) {
        if (d == null || d.humidityMin < 0 || d.humidityMax < 0) return "N/D";
        return d.humidityMin + "–" + d.humidityMax + "%";
    }

    private String displayTime(String iso) {
        if (iso == null || iso.isEmpty()) return "";
        try {
            return LocalDateTime.parse(iso).format(DateTimeFormatter.ofPattern("HH:mm"));
        } catch (Exception ignored) {
            int t = iso.indexOf('T');
            return t >= 0 && iso.length() >= t + 6 ? iso.substring(t + 1, t + 6) : iso;
        }
    }

    private String shortDay(String isoDate) {
        try {
            return capitalize(LocalDate.parse(isoDate).format(DateTimeFormatter.ofPattern("EEE", new Locale("es"))));
        } catch (Exception ignored) {
            return isoDate;
        }
    }

    private String fullDay(String isoDate) {
        try {
            return capitalize(LocalDate.parse(isoDate).format(DateTimeFormatter.ofPattern("EEE d", new Locale("es"))));
        } catch (Exception ignored) {
            return isoDate;
        }
    }

    private String capitalize(String value) {
        if (value == null || value.isEmpty()) return value;
        return value.substring(0, 1).toUpperCase(new Locale("es")) + value.substring(1);
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }

    private static final class Place {
        final double latitude;
        final double longitude;
        final String displayName;
        Place(double latitude, double longitude, String displayName) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.displayName = displayName;
        }
    }

    private static final class Forecast {
        String location;
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
        final List<Hour> hours = new ArrayList<>();
        final List<Day> days = new ArrayList<>();
    }

    private static final class Hour {
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

    private static final class Day {
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
}
