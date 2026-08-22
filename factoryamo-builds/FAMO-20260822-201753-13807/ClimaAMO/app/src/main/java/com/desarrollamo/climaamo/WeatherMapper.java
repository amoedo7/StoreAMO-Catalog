package com.desarrollamo.climaamo;

public final class WeatherMapper {
    private WeatherMapper() {}

    public static String label(int code) {
        if (code == 0) return "Despejado";
        if (code == 1) return "Mayormente despejado";
        if (code == 2) return "Parcialmente nublado";
        if (code == 3) return "Nublado";
        if (code == 45 || code == 48) return "Niebla";
        if (code >= 51 && code <= 57) return "Llovizna";
        if (code >= 61 && code <= 67) return "Lluvia";
        if (code >= 71 && code <= 77) return "Nieve";
        if (code >= 80 && code <= 82) return "Chubascos";
        if (code == 85 || code == 86) return "Chubascos de nieve";
        if (code >= 95 && code <= 99) return "Tormenta";
        return "Condiciones variables";
    }

    public static String emoji(int code) {
        if (code == 0) return "☀️";
        if (code <= 2) return "🌤️";
        if (code == 3) return "☁️";
        if (code == 45 || code == 48) return "🌫️";
        if ((code >= 51 && code <= 67) || (code >= 80 && code <= 82)) return "🌧️";
        if ((code >= 71 && code <= 77) || code == 85 || code == 86) return "🌨️";
        if (code >= 95 && code <= 99) return "⛈️";
        return "🌡️";
    }
}
