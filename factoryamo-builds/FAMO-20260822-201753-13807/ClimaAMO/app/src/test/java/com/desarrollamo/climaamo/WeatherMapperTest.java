package com.desarrollamo.climaamo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class WeatherMapperTest {
    @Test
    public void mapsCoreWmoCodes() {
        assertEquals("Despejado", WeatherMapper.label(0));
        assertEquals("Parcialmente nublado", WeatherMapper.label(2));
        assertEquals("Lluvia", WeatherMapper.label(63));
        assertEquals("Nieve", WeatherMapper.label(73));
        assertEquals("Tormenta", WeatherMapper.label(95));
    }

    @Test
    public void providesStableFallback() {
        assertEquals("Condiciones variables", WeatherMapper.label(1000));
        assertEquals("🌡️", WeatherMapper.emoji(1000));
    }
}
