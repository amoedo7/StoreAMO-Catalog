package com.desarrollamo.climaamo;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.Button;
import android.widget.EditText;
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
    private static final int ERROR = Color.rgb(255, 134, 134);

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
    private TextView detailText;
    private TextView forecastTitle;
    private LinearLayout forecastContainer;

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
        root.setPadding(dp(20), dp(22), dp(20), dp(32));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));

        LinearLayout brandRow = new LinearLayout(this);
        brandRow.setOrientation(LinearLayout.HORIZONTAL);
        brandRow.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(brandRow, new LinearLayout.LayoutParams(-1, -2));

        ImageView logo = new ImageView(this);
        logo.setImageResource(com.desarrollamo.climaamo.R.drawable.ic_climaamo);
        brandRow.addView(logo, new LinearLayout.LayoutParams(dp(46), dp(46)));

        LinearLayout brandTextWrap = new LinearLayout(this);
        brandTextWrap.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams brandTextLp = new LinearLayout.LayoutParams(0, -2, 1f);
        brandTextLp.leftMargin = dp(12);
        brandRow.addView(brandTextWrap, brandTextLp);

        TextView brand = text("ClimaAMO", 24, TEXT, true);
        brandTextWrap.addView(brand);
        TextView byline = text("DESARROLLAMO · candidate 0.1.0", 11, ACCENT, true);
        brandTextWrap.addView(byline);

        TextView intro = text("El clima que importa, sin cuentas ni claves.", 15, MUTED, false);
        LinearLayout.LayoutParams introLp = new LinearLayout.LayoutParams(-1, -2);
        introLp.topMargin = dp(18);
        root.addView(intro, introLp);

        LinearLayout searchCard = card();
        LinearLayout.LayoutParams searchCardLp = new LinearLayout.LayoutParams(-1, -2);
        searchCardLp.topMargin = dp(18);
        root.addView(searchCard, searchCardLp);

        TextView searchLabel = text("BUSCAR CIUDAD", 11, MUTED, true);
        searchCard.addView(searchLabel);

        LinearLayout searchRow = new LinearLayout(this);
        searchRow.setOrientation(LinearLayout.HORIZONTAL);
        searchRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams searchRowLp = new LinearLayout.LayoutParams(-1, -2);
        searchRowLp.topMargin = dp(10);
        searchCard.addView(searchRow, searchRowLp);

        cityInput = new EditText(this);
        cityInput.setHint("Ej. Barcelona, Malmö, Buenos Aires");
        cityInput.setHintTextColor(Color.rgb(103, 128, 153));
        cityInput.setTextColor(TEXT);
        cityInput.setTextSize(15);
        cityInput.setSingleLine(true);
        cityInput.setPadding(dp(14), 0, dp(14), 0);
        cityInput.setBackground(rounded(PANEL_2, dp(14), Color.rgb(46, 70, 95)));
        cityInput.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
        LinearLayout.LayoutParams inputLp = new LinearLayout.LayoutParams(0, dp(50), 1f);
        searchRow.addView(cityInput, inputLp);

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
        tempRow.addView(currentIcon, new LinearLayout.LayoutParams(dp(64), -2));

        LinearLayout tempWrap = new LinearLayout(this);
        tempWrap.setOrientation(LinearLayout.VERTICAL);
        tempRow.addView(tempWrap, new LinearLayout.LayoutParams(0, -2, 1f));

        currentTemp = text("", 42, TEXT, true);
        tempWrap.addView(currentTemp);
        currentDescription = text("", 16, ACCENT, true);
        tempWrap.addView(currentDescription);

        detailText = text("", 14, MUTED, false);
        LinearLayout.LayoutParams detailLp = new LinearLayout.LayoutParams(-1, -2);
        detailLp.topMargin = dp(12);
        currentCard.addView(detailText, detailLp);

        forecastTitle = text("PRÓXIMOS 5 DÍAS", 12, MUTED, true);
        forecastTitle.setVisibility(View.GONE);
        LinearLayout.LayoutParams forecastTitleLp = new LinearLayout.LayoutParams(-1, -2);
        forecastTitleLp.topMargin = dp(22);
        root.addView(forecastTitle, forecastTitleLp);

        forecastContainer = new LinearLayout(this);
        forecastContainer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams forecastLp = new LinearLayout.LayoutParams(-1, -2);
        forecastLp.topMargin = dp(8);
        root.addView(forecastContainer, forecastLp);

        TextView source = text("Datos meteorológicos: Open-Meteo · sin API key", 11, Color.rgb(95, 120, 145), false);
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

    private Forecast fetchForecast(Place place) throws Exception {
        String url = "https://api.open-meteo.com/v1/forecast"
                + "?latitude=" + place.latitude
                + "&longitude=" + place.longitude
                + "&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m"
                + "&daily=weather_code,temperature_2m_max,temperature_2m_min"
                + "&timezone=auto&forecast_days=5";
        JSONObject json = getJson(url);
        JSONObject current = json.getJSONObject("current");
        JSONObject daily = json.getJSONObject("daily");

        Forecast forecast = new Forecast();
        forecast.location = place.displayName;
        forecast.temperature = current.getDouble("temperature_2m");
        forecast.apparent = current.getDouble("apparent_temperature");
        forecast.humidity = current.getInt("relative_humidity_2m");
        forecast.wind = current.getDouble("wind_speed_10m");
        forecast.weatherCode = current.getInt("weather_code");

        JSONArray dates = daily.getJSONArray("time");
        JSONArray codes = daily.getJSONArray("weather_code");
        JSONArray max = daily.getJSONArray("temperature_2m_max");
        JSONArray min = daily.getJSONArray("temperature_2m_min");
        int count = Math.min(5, dates.length());
        for (int i = 0; i < count; i++) {
            Day day = new Day();
            day.isoDate = dates.getString(i);
            day.code = codes.getInt(i);
            day.max = max.getDouble(i);
            day.min = min.getDouble(i);
            forecast.days.add(day);
        }
        return forecast;
    }

    private JSONObject getJson(String urlString) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(urlString).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(9000);
        connection.setReadTimeout(9000);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("User-Agent", "ClimaAMO/0.1.0");
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

    private void renderForecast(Forecast forecast) {
        setLoading(false);
        showStatus("Actualizado ahora.", false);
        currentCard.setVisibility(View.VISIBLE);
        forecastTitle.setVisibility(View.VISIBLE);

        locationText.setText(forecast.location);
        currentIcon.setText(WeatherMapper.emoji(forecast.weatherCode));
        currentTemp.setText(String.format(Locale.getDefault(), "%.0f°", forecast.temperature));
        currentDescription.setText(WeatherMapper.label(forecast.weatherCode));
        detailText.setText(String.format(Locale.getDefault(),
                "Sensación %.0f°   ·   Humedad %d%%   ·   Viento %.0f km/h",
                forecast.apparent, forecast.humidity, forecast.wind));

        forecastContainer.removeAllViews();
        for (Day day : forecast.days) {
            forecastContainer.addView(dayRow(day));
        }
    }

    private View dayRow(Day day) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(14), dp(16), dp(14));
        row.setBackground(rounded(PANEL, dp(16), Color.rgb(32, 54, 78)));
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, -2);
        rowLp.bottomMargin = dp(8);
        row.setLayoutParams(rowLp);

        LocalDate date = LocalDate.parse(day.isoDate);
        String dayLabel = date.format(DateTimeFormatter.ofPattern("EEE d", new Locale("es")));
        TextView dateText = text(capitalize(dayLabel), 14, TEXT, true);
        row.addView(dateText, new LinearLayout.LayoutParams(dp(78), -2));

        TextView condition = text(WeatherMapper.emoji(day.code) + "  " + WeatherMapper.label(day.code), 13, MUTED, false);
        row.addView(condition, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView temps = text(String.format(Locale.getDefault(), "%.0f° / %.0f°", day.max, day.min), 14, TEXT, true);
        temps.setGravity(Gravity.END);
        row.addView(temps, new LinearLayout.LayoutParams(dp(82), -2));

        return row;
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        searchButton.setEnabled(!loading);
        cityInput.setEnabled(!loading);
        if (loading) showStatus("Consultando Open-Meteo…", false);
    }

    private void showStatus(String message, boolean isError) {
        statusText.setText(message);
        statusText.setTextColor(isError ? ERROR : MUTED);
    }

    private String readableError(Exception e) {
        if (e instanceof IllegalArgumentException) return e.getMessage();
        return "No pude obtener el clima. Revisá tu conexión e intentá otra vez.";
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackground(rounded(PANEL, dp(18), Color.rgb(32, 54, 78)));
        return card;
    }

    private TextView text(String value, float sizeSp, int color, boolean bold) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(sizeSp);
        text.setTextColor(color);
        text.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL);
        return text;
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
        double temperature;
        double apparent;
        int humidity;
        double wind;
        int weatherCode;
        final List<Day> days = new ArrayList<>();
    }

    private static final class Day {
        String isoDate;
        int code;
        double max;
        double min;
    }
}
