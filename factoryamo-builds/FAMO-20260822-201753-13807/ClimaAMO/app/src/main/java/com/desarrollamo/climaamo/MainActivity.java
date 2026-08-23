package com.desarrollamo.climaamo;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.util.Linkify;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

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
    private static final int LOCATION_REQUEST = 421;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final WeatherRepository repository = new WeatherRepository();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private ScrollView scrollView;
    private EditText cityInput;
    private Button searchButton;
    private ImageButton gpsButton;
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

    private TextView sourceTitle;
    private LinearLayout sourceContainer;

    private TextView airTitle;
    private LinearLayout airContainer;

    private TextView forecastTitle;
    private TextView forecastHint;
    private LinearLayout forecastContainer;

    private LinearLayout tempTrendCard;
    private WeatherTrendView tempTrendView;
    private LinearLayout moistureTrendCard;
    private WeatherTrendView moistureTrendView;

    private WeatherRepository.Forecast activeForecast;
    private int selectedDayIndex = 0;

    private LocationManager locationManager;
    private LocationListener locationListener;
    private boolean waitingLocation = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        setContentView(buildUi());
    }

    private View buildUi() {
        scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(22), dp(20), dp(34));
        scrollView.addView(root, new ScrollView.LayoutParams(-1, -2));

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
        brandTextWrap.addView(text("DESARROLLAMO · candidate 0.2.1", 11, ACCENT, true));

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
        cityInput.setHint("Ej. Pergamino, Malmö");
        cityInput.setHintTextColor(Color.rgb(103, 128, 153));
        cityInput.setTextColor(TEXT);
        cityInput.setTextSize(15);
        cityInput.setSingleLine(true);
        cityInput.setPadding(dp(14), 0, dp(14), 0);
        cityInput.setBackground(rounded(PANEL_2, dp(14), Color.rgb(46, 70, 95)));
        cityInput.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
        searchRow.addView(cityInput, new LinearLayout.LayoutParams(0, dp(50), 1f));

        gpsButton = new ImageButton(this);
        gpsButton.setImageResource(R.drawable.ic_gps);
        gpsButton.setContentDescription("Usar mi ubicación");
        gpsButton.setPadding(dp(13), dp(13), dp(13), dp(13));
        gpsButton.setBackground(rounded(ACCENT, dp(14), ACCENT));
        LinearLayout.LayoutParams gpsLp = new LinearLayout.LayoutParams(dp(50), dp(50));
        gpsLp.leftMargin = dp(8);
        searchRow.addView(gpsButton, gpsLp);

        searchButton = new Button(this);
        searchButton.setText("Buscar");
        searchButton.setTextColor(BG);
        searchButton.setTextSize(13);
        searchButton.setTypeface(Typeface.DEFAULT_BOLD);
        searchButton.setAllCaps(false);
        searchButton.setBackground(rounded(ACCENT, dp(14), ACCENT));
        LinearLayout.LayoutParams buttonLp = new LinearLayout.LayoutParams(dp(82), dp(50));
        buttonLp.leftMargin = dp(8);
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

        statusText = text("Buscá una ciudad o tocá el GPS para usar Aquí.", 13, MUTED, false);
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

        sourceTitle = sectionTitle("FUENTES · COMPARACIÓN REAL");
        sourceTitle.setVisibility(View.GONE);
        root.addView(sourceTitle, sectionLp(18));
        sourceContainer = new LinearLayout(this);
        sourceContainer.setOrientation(LinearLayout.VERTICAL);
        sourceContainer.setVisibility(View.GONE);
        root.addView(sourceContainer, sectionLp(8));

        airTitle = sectionTitle("CALIDAD DEL AIRE · AHORA");
        airTitle.setVisibility(View.GONE);
        root.addView(airTitle, sectionLp(18));
        airContainer = new LinearLayout(this);
        airContainer.setOrientation(LinearLayout.VERTICAL);
        airContainer.setVisibility(View.GONE);
        root.addView(airContainer, sectionLp(8));

        forecastTitle = sectionTitle("PRÓXIMOS 5 DÍAS");
        forecastTitle.setVisibility(View.GONE);
        root.addView(forecastTitle, sectionLp(22));
        forecastHint = text("Tocá cualquier día para abrir sus horas y todos sus datos.", 12, CYAN, false);
        forecastHint.setVisibility(View.GONE);
        root.addView(forecastHint, sectionLp(6));
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
        TextView tempLegend = text("● Máxima     ● Mínima", 12, ACCENT, true);
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

        TextView sourceFooter = text(
                "Datos reales · Open-Meteo (CC BY 4.0): https://open-meteo.com/ · MET Norway (CC BY 4.0): https://api.met.no/ · sin promedios ocultos ni valores simulados.",
                10, Color.rgb(95, 120, 145), false
        );
        sourceFooter.setGravity(Gravity.CENTER_HORIZONTAL);
        Linkify.addLinks(sourceFooter, Linkify.WEB_URLS);
        sourceFooter.setLinkTextColor(CYAN);
        LinearLayout.LayoutParams sourceLp = new LinearLayout.LayoutParams(-1, -2);
        sourceLp.topMargin = dp(24);
        root.addView(sourceFooter, sourceLp);

        searchButton.setOnClickListener(v -> performSearch());
        gpsButton.setOnClickListener(v -> useHere());
        cityInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });

        return scrollView;
    }

    private void performSearch() {
        String raw = cityInput.getText().toString().trim();
        if (raw.length() < 2) {
            showStatus("Escribí al menos 2 caracteres.", true);
            return;
        }
        hideKeyboard();
        setLoading(true, "Buscando ciudad y consultando fuentes reales…");
        executor.execute(() -> {
            try {
                WeatherRepository.Place place = repository.geocode(raw);
                WeatherRepository.Forecast forecast = repository.load(place);
                runOnUiThread(() -> renderForecast(forecast));
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setLoading(false, null);
                    showStatus(readableError(e), true);
                });
            }
        });
    }

    private void useHere() {
        hideKeyboard();
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_REQUEST);
            return;
        }
        acquireLocation();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != LOCATION_REQUEST) return;
        boolean granted = false;
        for (int result : grantResults) if (result == PackageManager.PERMISSION_GRANTED) granted = true;
        if (granted) acquireLocation();
        else showStatus("Sin permiso de ubicación. Podés seguir buscando una ciudad manualmente.", true);
    }

    private void acquireLocation() {
        if (waitingLocation) return;
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            showStatus("Android no ofrece un proveedor de ubicación en este dispositivo.", true);
            return;
        }

        Location best = null;
        for (String provider : new String[]{LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER}) {
            try {
                Location candidate = locationManager.getLastKnownLocation(provider);
                if (candidate != null && (best == null || candidate.getTime() > best.getTime())) best = candidate;
            } catch (Exception ignored) { }
        }
        long tenMinutes = 10L * 60L * 1000L;
        if (best != null && System.currentTimeMillis() - best.getTime() <= tenMinutes) {
            loadCoordinates(best);
            return;
        }

        boolean gpsEnabled = false;
        boolean networkEnabled = false;
        try { gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER); } catch (Exception ignored) { }
        try { networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER); } catch (Exception ignored) { }
        if (!gpsEnabled && !networkEnabled) {
            showStatus("Activá la ubicación/GPS de Android y tocá Aquí otra vez.", true);
            return;
        }

        waitingLocation = true;
        setLoading(true, "Buscando tu ubicación real…");
        locationListener = new LocationListener() {
            @Override public void onLocationChanged(Location location) {
                if (!waitingLocation || location == null) return;
                stopLocationUpdates();
                loadCoordinates(location);
            }
            @Override public void onStatusChanged(String provider, int status, Bundle extras) { }
            @Override public void onProviderEnabled(String provider) { }
            @Override public void onProviderDisabled(String provider) { }
        };

        try {
            if (gpsEnabled) locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, locationListener, Looper.getMainLooper());
            if (networkEnabled) locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 0f, locationListener, Looper.getMainLooper());
        } catch (SecurityException e) {
            stopLocationUpdates();
            showStatus("Android bloqueó el acceso a la ubicación.", true);
            return;
        }

        handler.postDelayed(() -> {
            if (!waitingLocation) return;
            stopLocationUpdates();
            setLoading(false, null);
            showStatus("No recibí una ubicación a tiempo. Revisá GPS y señal e intentá otra vez.", true);
        }, 12000L);
    }

    private void stopLocationUpdates() {
        waitingLocation = false;
        handler.removeCallbacksAndMessages(null);
        if (locationManager != null && locationListener != null) {
            try { locationManager.removeUpdates(locationListener); } catch (SecurityException ignored) { }
        }
        locationListener = null;
    }

    private void loadCoordinates(Location location) {
        final double lat = location.getLatitude();
        final double lon = location.getLongitude();
        setLoading(true, "Ubicación obtenida · consultando el clima de Aquí…");
        executor.execute(() -> {
            try {
                WeatherRepository.Place place = placeFromCoordinates(lat, lon);
                WeatherRepository.Forecast forecast = repository.load(place);
                runOnUiThread(() -> {
                    cityInput.setText("Aquí");
                    renderForecast(forecast);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setLoading(false, null);
                    showStatus(readableError(e), true);
                });
            }
        });
    }

    private WeatherRepository.Place placeFromCoordinates(double lat, double lon) {
        String display = String.format(Locale.getDefault(), "Aquí · %.4f, %.4f", lat, lon);
        try {
            Geocoder geocoder = new Geocoder(this, new Locale("es"));
            List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address a = addresses.get(0);
                String locality = firstNonBlank(a.getLocality(), a.getSubAdminArea(), a.getAdminArea());
                String country = a.getCountryName();
                if (locality != null && !locality.isEmpty()) {
                    display = "Aquí · " + locality + (country == null || country.isEmpty() ? "" : " · " + country);
                }
            }
        } catch (Exception ignored) { }
        return new WeatherRepository.Place(lat, lon, display);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) if (value != null && !value.trim().isEmpty()) return value.trim();
        return null;
    }

    private void renderForecast(WeatherRepository.Forecast f) {
        activeForecast = f;
        selectedDayIndex = 0;
        setLoading(false, null);
        String updated = displayTime(f.currentTime);
        String sources = f.met == null ? "Open-Meteo" : "Open-Meteo + MET Norway";
        showStatus((updated.isEmpty() ? "Datos actualizados" : "Actualizado " + updated) + " · " + sources, false);

        currentCard.setVisibility(View.VISIBLE);
        hourlyTitle.setVisibility(View.VISIBLE);
        hourlyScroll.setVisibility(View.VISIBLE);
        detailsTitle.setVisibility(View.VISIBLE);
        detailsContainer.setVisibility(View.VISIBLE);
        sourceTitle.setVisibility(View.VISIBLE);
        sourceContainer.setVisibility(View.VISIBLE);
        forecastTitle.setVisibility(View.VISIBLE);
        forecastHint.setVisibility(View.VISIBLE);
        tempTrendCard.setVisibility(View.VISIBLE);
        moistureTrendCard.setVisibility(View.VISIBLE);

        if (f.airQuality != null) {
            airTitle.setVisibility(View.VISIBLE);
            airContainer.setVisibility(View.VISIBLE);
            renderAirQuality(f.airQuality);
        } else {
            airTitle.setVisibility(View.GONE);
            airContainer.setVisibility(View.GONE);
        }

        renderSelectedDay(0, false);
        renderTrends(f);
    }

    private void renderSelectedDay(int index, boolean scrollToTop) {
        if (activeForecast == null || activeForecast.days.isEmpty()) return;
        selectedDayIndex = Math.max(0, Math.min(index, activeForecast.days.size() - 1));
        WeatherRepository.Day day = activeForecast.days.get(selectedDayIndex);
        boolean today = selectedDayIndex == 0;

        if (today) renderToday(activeForecast, day);
        else renderFutureDay(activeForecast, day);

        renderHourly(activeForecast, day, today);
        renderDetails(activeForecast, day, today);
        renderSources(activeForecast, day, today);
        renderForecastCards(activeForecast);

        if (scrollToTop) {
            scrollView.post(() -> scrollView.smoothScrollTo(0, Math.max(0, currentCard.getTop() - dp(12))));
        }
    }

    private void renderToday(WeatherRepository.Forecast f, WeatherRepository.Day day) {
        locationText.setText(f.location);
        currentIcon.setText(WeatherMapper.emoji(f.weatherCode, f.isDay));
        currentTemp.setText(formatTemp(f.temperature));
        currentTemp.setTextSize(42);
        currentDescription.setText(WeatherMapper.label(f.weatherCode));
        currentToday.setText("Hoy " + formatTemp(day.max) + " / " + formatTemp(day.min)
                + "   ·   Lluvia " + formatPercent(day.precipProbabilityMax));
        detailText.setText("Sensación " + formatTemp(f.apparent)
                + "   ·   Humedad " + formatPercent(f.humidity)
                + "   ·   Viento " + formatSpeed(f.wind));
        hourlyTitle.setText("PRÓXIMAS HORAS");
        detailsTitle.setText("AHORA · DETALLES REALES");
    }

    private void renderFutureDay(WeatherRepository.Forecast f, WeatherRepository.Day day) {
        locationText.setText(f.location + " · " + fullDay(day.isoDate));
        currentIcon.setText(WeatherMapper.emoji(day.code));
        currentTemp.setText(formatTemp(day.max) + " / " + formatTemp(day.min));
        currentTemp.setTextSize(31);
        currentDescription.setText(WeatherMapper.label(day.code));
        currentToday.setText("Pronóstico del día · Lluvia " + formatMm(day.precipitationSum)
                + " · " + formatPercent(day.precipProbabilityMax));
        detailText.setText("Sensación " + formatTemp(day.apparentMax) + " / " + formatTemp(day.apparentMin)
                + "   ·   Humedad " + humidityRange(day)
                + "   ·   Viento máx. " + formatSpeed(day.windMax));
        hourlyTitle.setText("HORAS · " + fullDay(day.isoDate).toUpperCase(new Locale("es")));
        detailsTitle.setText("DETALLE · " + fullDay(day.isoDate).toUpperCase(new Locale("es")));
    }

    private void renderHourly(WeatherRepository.Forecast f, WeatherRepository.Day day, boolean today) {
        hourlyContainer.removeAllViews();
        List<WeatherRepository.Hour> hours = new ArrayList<>();
        if (today) hours.addAll(f.nextHours);
        else {
            for (WeatherRepository.Hour h : f.allHours) if (h.time.startsWith(day.isoDate)) hours.add(h);
        }
        for (int i = 0; i < hours.size(); i++) hourlyContainer.addView(hourCard(hours.get(i), today && i == 0));
        hourlyScroll.scrollTo(0, 0);
    }

    private void renderDetails(WeatherRepository.Forecast f, WeatherRepository.Day day, boolean today) {
        detailsContainer.removeAllViews();
        WeatherRepository.Hour representative = representativeHour(f, day.isoDate);
        if (today) {
            detailsContainer.addView(metricRow(
                    metricCard("Precipitación", formatMm(f.precipitation), "Ahora · prob. " + formatPercent(f.currentPrecipProbability)),
                    metricCard("Viento", formatSpeed(f.wind), directionLabel(f.windDirection) + " · ráfagas " + formatSpeed(f.gust))));
            detailsContainer.addView(metricRow(
                    metricCard("Humedad", formatPercent(f.humidity), "Hoy " + humidityRange(day)),
                    metricCard("Presión", formatPressure(f.pressureMsl), "Nivel del mar")));
            detailsContainer.addView(metricRow(
                    metricCard("Visibilidad", formatKm(f.visibilityKm), "Dato horario del modelo"),
                    metricCard("Nubosidad", formatPercent(f.cloudCover), "Cobertura total")));
        } else {
            detailsContainer.addView(metricRow(
                    metricCard("Lluvia del día", formatMm(day.precipitationSum), "Prob. máx. " + formatPercent(day.precipProbabilityMax)),
                    metricCard("Viento máximo", formatSpeed(day.windMax), directionLabel(day.windDirection) + " · ráfagas " + formatSpeed(day.gustMax))));
            detailsContainer.addView(metricRow(
                    metricCard("Humedad", humidityRange(day), "Rango horario previsto"),
                    metricCard("Presión", representative == null ? "N/D" : formatPressure(representative.pressureMsl), "Referencia de las 12:00")));
            detailsContainer.addView(metricRow(
                    metricCard("Visibilidad", representative == null ? "N/D" : formatKm(representative.visibilityKm), "Referencia de las 12:00"),
                    metricCard("Nubosidad", representative == null ? "N/D" : formatPercent(representative.cloudCover), "Referencia de las 12:00")));
        }
        detailsContainer.addView(metricRow(
                metricCard("Sol", displayTime(day.sunrise), "Atardecer " + displayTime(day.sunset)),
                metricCard("UV máximo", formatOne(day.uvMax), "Pronóstico del día")));
    }

    private WeatherRepository.Hour representativeHour(WeatherRepository.Forecast f, String isoDate) {
        WeatherRepository.Hour first = null;
        for (WeatherRepository.Hour h : f.allHours) {
            if (!h.time.startsWith(isoDate)) continue;
            if (first == null) first = h;
            if (h.time.contains("T12:")) return h;
        }
        return first;
    }

    private void renderSources(WeatherRepository.Forecast f, WeatherRepository.Day day, boolean today) {
        sourceContainer.removeAllViews();
        LinearLayout box = card();
        box.setPadding(dp(14), dp(14), dp(14), dp(14));
        sourceContainer.addView(box, new LinearLayout.LayoutParams(-1, -2));

        if (today) {
            box.addView(sourceLine("Open-Meteo",
                    formatTemp(f.temperature) + " · Hum " + formatPercent(f.humidity)
                            + " · Viento " + formatSpeed(f.wind) + " · " + formatPressure(f.pressureMsl), ACCENT));
            if (f.met != null && f.met.current != null) {
                WeatherRepository.MetSnapshot m = f.met.current;
                box.addView(sourceLine("MET Norway",
                        formatTemp(m.temperature) + " · Hum " + formatPercent(m.humidity)
                                + " · Viento " + formatSpeed(m.wind) + " · " + formatPressure(m.pressureMsl)
                                + " · Próx. 1h " + formatMm(m.precipitationNextHour), CYAN));
            } else {
                box.addView(sourceLine("MET Norway", "No disponible en esta consulta.", CYAN));
            }
        } else {
            box.addView(sourceLine("Open-Meteo",
                    formatTemp(day.max) + " / " + formatTemp(day.min)
                            + " · Lluvia " + formatMm(day.precipitationSum)
                            + " · Viento máx. " + formatSpeed(day.windMax), ACCENT));
            WeatherRepository.MetDay metDay = f.met == null ? null : f.met.findDay(day.isoDate);
            if (metDay != null && (Double.isFinite(metDay.max) || Double.isFinite(metDay.min))) {
                box.addView(sourceLine("MET Norway",
                        formatTemp(metDay.max) + " / " + formatTemp(metDay.min)
                                + " · Lluvia " + formatMm(metDay.precipitationSum)
                                + " · Viento máx. " + formatSpeed(metDay.windMax), CYAN));
            } else {
                box.addView(sourceLine("MET Norway", "No disponible para este día/ubicación.", CYAN));
            }
        }
        TextView note = text("Las fuentes se muestran por separado. ClimaAMO no promedia ni oculta diferencias entre modelos.", 10, MUTED, false);
        LinearLayout.LayoutParams noteLp = new LinearLayout.LayoutParams(-1, -2);
        noteLp.topMargin = dp(10);
        box.addView(note, noteLp);
    }

    private View sourceLine(String source, String values, int sourceColor) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, -2);
        rowLp.bottomMargin = dp(9);
        row.setLayoutParams(rowLp);
        row.addView(text(source, 12, sourceColor, true));
        TextView v = text(values, 12, TEXT, false);
        LinearLayout.LayoutParams vLp = new LinearLayout.LayoutParams(-1, -2);
        vLp.topMargin = dp(3);
        row.addView(v, vLp);
        return row;
    }

    private void renderAirQuality(WeatherRepository.AirQuality aq) {
        airContainer.removeAllViews();
        airContainer.addView(metricRow(
                metricCard("AQI europeo", formatOne(aq.europeanAqi), "Open-Meteo Air Quality"),
                metricCard("AQI EE. UU.", formatOne(aq.usAqi), "Índice equivalente")));
        airContainer.addView(metricRow(
                metricCard("PM2.5", formatMicrograms(aq.pm25), "Partículas finas"),
                metricCard("PM10", formatMicrograms(aq.pm10), "Partículas")));
        airContainer.addView(metricRow(
                metricCard("Ozono", formatMicrograms(aq.ozone), "O₃"),
                metricCard("Actualizado", displayTime(aq.time), "Dato actual de la API")));
    }

    private void renderForecastCards(WeatherRepository.Forecast f) {
        forecastContainer.removeAllViews();
        for (int i = 0; i < f.days.size(); i++) {
            final int index = i;
            forecastContainer.addView(dayCard(f.days.get(i), index, index == selectedDayIndex));
        }
    }

    private void renderTrends(WeatherRepository.Forecast f) {
        String[] labels = new String[f.days.size()];
        double[] max = new double[f.days.size()];
        double[] min = new double[f.days.size()];
        double[] humMax = new double[f.days.size()];
        double[] humMin = new double[f.days.size()];
        double[] rain = new double[f.days.size()];
        for (int i = 0; i < f.days.size(); i++) {
            WeatherRepository.Day d = f.days.get(i);
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

    private View hourCard(WeatherRepository.Hour h, boolean now) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.setPadding(dp(10), dp(11), dp(10), dp(11));
        box.setBackground(rounded(PANEL, dp(16), STROKE));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(112), -2);
        lp.rightMargin = dp(8);
        box.setLayoutParams(lp);

        TextView time = text(now ? "Ahora" : displayTime(h.time), 12, TEXT, true);
        time.setGravity(Gravity.CENTER);
        box.addView(time);
        TextView icon = text(WeatherMapper.emoji(h.code, h.isDay), 26, TEXT, false);
        icon.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(-1, -2);
        iconLp.topMargin = dp(5);
        box.addView(icon, iconLp);
        TextView temp = text(formatTemp(h.temperature), 20, TEXT, true);
        temp.setGravity(Gravity.CENTER);
        box.addView(temp);
        TextView rain = text("Lluvia " + formatPercent(h.precipProbability), 11, CYAN, false);
        rain.setGravity(Gravity.CENTER);
        box.addView(rain);
        TextView wind = text(directionLabel(h.windDirection) + " " + formatSpeed(h.wind), 10, MUTED, false);
        wind.setGravity(Gravity.CENTER);
        box.addView(wind);
        TextView humidity = text("Humedad " + formatPercent(h.humidity), 10, MUTED, false);
        humidity.setGravity(Gravity.CENTER);
        box.addView(humidity);
        return box;
    }

    private View dayCard(WeatherRepository.Day d, int index, boolean selected) {
        LinearLayout box = card();
        box.setBackground(rounded(PANEL, dp(18), selected ? ACCENT : STROKE, selected ? 2 : 1));
        LinearLayout.LayoutParams boxLp = new LinearLayout.LayoutParams(-1, -2);
        boxLp.bottomMargin = dp(10);
        box.setLayoutParams(boxLp);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        box.addView(header, new LinearLayout.LayoutParams(-1, -2));

        TextView date = text(index == 0 ? "Hoy · " + fullDay(d.isoDate) : fullDay(d.isoDate), 14, TEXT, true);
        header.addView(date, new LinearLayout.LayoutParams(dp(102), -2));
        TextView condition = text(WeatherMapper.emoji(d.code) + "  " + WeatherMapper.label(d.code), 12, MUTED, false);
        header.addView(condition, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView temps = text(formatTemp(d.max) + " / " + formatTemp(d.min), 14, TEXT, true);
        temps.setGravity(Gravity.END);
        header.addView(temps, new LinearLayout.LayoutParams(dp(86), -2));

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

        TextView action = text(selected ? "Viendo este día ↑" : "Tocar para ver detalle →", 11, selected ? ACCENT : CYAN, true);
        LinearLayout.LayoutParams actionLp = new LinearLayout.LayoutParams(-1, -2);
        actionLp.topMargin = dp(8);
        box.addView(action, actionLp);
        box.setOnClickListener(v -> renderSelectedDay(index, true));
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

    private void setLoading(boolean loading, String message) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        searchButton.setEnabled(!loading);
        gpsButton.setEnabled(!loading);
        cityInput.setEnabled(!loading);
        if (loading && message != null) showStatus(message, false);
    }

    private void showStatus(String message, boolean isError) {
        statusText.setText(message);
        statusText.setTextColor(isError ? ERROR : MUTED);
    }

    private String readableError(Exception e) {
        if (e instanceof IllegalArgumentException && e.getMessage() != null) return e.getMessage();
        return "No pude obtener todos los datos. Revisá tu conexión e intentá otra vez.";
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(cityInput.getWindowToken(), 0);
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
        return rounded(fill, radius, stroke, 1);
    }

    private GradientDrawable rounded(int fill, int radius, int stroke, int strokeWidth) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(radius);
        drawable.setStroke(dp(strokeWidth), stroke);
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
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

    private String formatMicrograms(double value) {
        return Double.isFinite(value) ? String.format(Locale.getDefault(), "%.1f µg/m³", value) : "N/D";
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

    private String humidityRange(WeatherRepository.Day d) {
        if (d == null || d.humidityMin < 0 || d.humidityMax < 0) return "N/D";
        return d.humidityMin + "–" + d.humidityMax + "%";
    }

    private String displayTime(String iso) {
        if (iso == null || iso.isEmpty()) return "N/D";
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
        stopLocationUpdates();
        executor.shutdownNow();
        super.onDestroy();
    }
}
