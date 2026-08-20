package com.desarrollamo.brujulamo;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Locale;

public class MainActivity extends Activity implements SensorEventListener {
    private SensorManager manager;
    private Sensor rotation;
    private TextView heading, cardinal, status;
    private static final String[] DIRS = {"N","NNE","NE","ENE","E","ESE","SE","SSE","S","SSO","SO","OSO","O","ONO","NO","NNO"};

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(32,48,32,32);
        TextView title = new TextView(this); title.setText("BrujulAMO"); title.setTextSize(30f); root.addView(title);
        heading = new TextView(this); heading.setText("—°"); heading.setTextSize(56f); heading.setPadding(0,48,0,8); root.addView(heading);
        cardinal = new TextView(this); cardinal.setText("Esperando sensor…"); cardinal.setTextSize(28f); root.addView(cardinal);
        status = new TextView(this); status.setText("Brújula offline · sin GPS · sin permisos"); status.setTextSize(16f); status.setPadding(0,32,0,0); root.addView(status);
        setContentView(root);
        manager = (SensorManager)getSystemService(SENSOR_SERVICE);
        rotation = manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        if (rotation == null) { heading.setText("Sin sensor"); cardinal.setText("Este teléfono no expone sensor de orientación"); }
    }

    @Override protected void onResume() {
        super.onResume();
        if (rotation != null) manager.registerListener(this, rotation, SensorManager.SENSOR_DELAY_UI);
    }

    @Override protected void onPause() {
        if (manager != null) manager.unregisterListener(this);
        super.onPause();
    }

    @Override public void onSensorChanged(SensorEvent event) {
        float[] r = new float[9]; float[] o = new float[3];
        SensorManager.getRotationMatrixFromVector(r, event.values);
        SensorManager.getOrientation(r, o);
        float deg = (float)Math.toDegrees(o[0]);
        if (deg < 0) deg += 360f;
        int idx = Math.round(deg / 22.5f) % 16;
        heading.setText(String.format(Locale.US, "%.0f°", deg));
        cardinal.setText(DIRS[idx]);
        status.setText("Precisión del sensor: " + accuracyLabel(event.accuracy) + " · offline · sin permisos");
    }

    private String accuracyLabel(int a) {
        if (a == SensorManager.SENSOR_STATUS_ACCURACY_HIGH) return "alta";
        if (a == SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM) return "media";
        if (a == SensorManager.SENSOR_STATUS_ACCURACY_LOW) return "baja";
        return "calibrar moviendo el teléfono en forma de 8";
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) { }
}
