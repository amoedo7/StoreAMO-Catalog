package com.desarrollamo.cronamo;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Locale;

public class MainActivity extends Activity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private long accumulated = 0L, startedAt = 0L;
    private boolean running = false;
    private TextView clock, laps;

    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            clock.setText(format(elapsed()));
            if (running) handler.postDelayed(this, 50);
        }
    };

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(32,48,32,32);
        TextView title = new TextView(this); title.setText("CronAMO"); title.setTextSize(30f); root.addView(title);
        clock = new TextView(this); clock.setText("00:00.000"); clock.setTextSize(42f); clock.setPadding(0,40,0,24); root.addView(clock);
        Button startPause = new Button(this); startPause.setText("Iniciar / Pausar"); startPause.setOnClickListener(v -> toggle()); root.addView(startPause);
        Button lap = new Button(this); lap.setText("Vuelta"); lap.setOnClickListener(v -> addLap()); root.addView(lap);
        Button reset = new Button(this); reset.setText("Reiniciar"); reset.setOnClickListener(v -> reset()); root.addView(reset);
        laps = new TextView(this); laps.setText("Vueltas"); laps.setTextSize(17f); laps.setPadding(0,24,0,0); root.addView(laps);
        setContentView(root);
    }

    private void toggle() {
        if (running) {
            accumulated = elapsed(); running = false; handler.removeCallbacks(ticker);
        } else {
            startedAt = SystemClock.elapsedRealtime(); running = true; handler.post(ticker);
        }
    }

    private long elapsed() { return running ? accumulated + (SystemClock.elapsedRealtime() - startedAt) : accumulated; }

    private void addLap() {
        String line = format(elapsed());
        laps.setText(line + "\n" + laps.getText());
    }

    private void reset() {
        accumulated = 0L; startedAt = SystemClock.elapsedRealtime(); clock.setText("00:00.000"); laps.setText("Vueltas");
        if (running) { handler.removeCallbacks(ticker); handler.post(ticker); }
    }

    private String format(long ms) {
        long minutes = ms / 60000; long seconds = (ms / 1000) % 60; long millis = ms % 1000;
        return String.format(Locale.US, "%02d:%02d.%03d", minutes, seconds, millis);
    }

    @Override protected void onDestroy() { handler.removeCallbacks(ticker); super.onDestroy(); }
}
