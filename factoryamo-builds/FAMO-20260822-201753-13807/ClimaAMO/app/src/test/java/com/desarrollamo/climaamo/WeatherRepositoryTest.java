package com.desarrollamo.climaamo;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class WeatherRepositoryTest {
    @Test
    public void convertsMetNorwayWindFromMetersPerSecond() {
        assertEquals(18.0, WeatherRepository.mpsToKmh(5.0), 0.001);
    }

    @Test
    public void findsCurrentOpenMeteoHour() {
        List<WeatherRepository.Hour> hours = new ArrayList<>();
        WeatherRepository.Hour first = new WeatherRepository.Hour();
        first.time = "2026-08-23T10:00";
        hours.add(first);
        WeatherRepository.Hour second = new WeatherRepository.Hour();
        second.time = "2026-08-23T11:00";
        hours.add(second);
        assertEquals(1, WeatherRepository.findCurrentHour(hours, "2026-08-23T11:12"));
    }
}
