package com.desarrollamo.climaamo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

public final class WeatherTrendView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private String[] labels = new String[0];
    private double[] primary = new double[0];
    private double[] secondary = new double[0];
    private double[] bars = null;
    private String suffix = "";
    private int primaryColor = Color.rgb(246, 183, 60);
    private int secondaryColor = Color.rgb(90, 178, 255);

    public WeatherTrendView(Context context) {
        super(context);
        setMinimumHeight(dp(220));
    }

    public void setData(String[] labels, double[] primary, double[] secondary, double[] bars,
                        String suffix, int primaryColor, int secondaryColor) {
        this.labels = labels == null ? new String[0] : labels;
        this.primary = primary == null ? new double[0] : primary;
        this.secondary = secondary == null ? new double[0] : secondary;
        this.bars = bars;
        this.suffix = suffix == null ? "" : suffix;
        this.primaryColor = primaryColor;
        this.secondaryColor = secondaryColor;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int count = Math.min(labels.length, Math.min(primary.length, secondary.length));
        if (count == 0) return;

        float left = dp(26);
        float right = getWidth() - dp(18);
        float top = dp(38);
        float bottom = getHeight() - dp(44);
        if (right <= left || bottom <= top) return;

        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < count; i++) {
            if (Double.isFinite(primary[i])) {
                min = Math.min(min, primary[i]);
                max = Math.max(max, primary[i]);
            }
            if (Double.isFinite(secondary[i])) {
                min = Math.min(min, secondary[i]);
                max = Math.max(max, secondary[i]);
            }
        }
        if (!Double.isFinite(min) || !Double.isFinite(max)) return;
        if (Math.abs(max - min) < 0.001) {
            max += 1.0;
            min -= 1.0;
        }
        double padding = Math.max(1.0, (max - min) * 0.18);
        min -= padding;
        max += padding;

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dpF(1));
        paint.setColor(Color.rgb(42, 62, 84));
        for (int g = 0; g < 4; g++) {
            float y = top + (bottom - top) * g / 3f;
            canvas.drawLine(left, y, right, y, paint);
        }

        if (bars != null && bars.length >= count) {
            double maxBar = 0.0;
            for (int i = 0; i < count; i++) if (Double.isFinite(bars[i])) maxBar = Math.max(maxBar, bars[i]);
            if (maxBar > 0.0) {
                float slot = count == 1 ? (right - left) : (right - left) / (count - 1f);
                float width = Math.min(dp(22), slot * 0.42f);
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(Color.argb(105, 90, 178, 255));
                for (int i = 0; i < count; i++) {
                    if (!Double.isFinite(bars[i]) || bars[i] <= 0.0) continue;
                    float x = xFor(i, count, left, right);
                    float h = (float) ((bars[i] / maxBar) * (bottom - top) * 0.34);
                    canvas.drawRoundRect(new RectF(x - width / 2f, bottom - h, x + width / 2f, bottom),
                            dpF(5), dpF(5), paint);
                }
            }
        }

        drawSeries(canvas, primary, count, left, right, top, bottom, min, max, primaryColor, true);
        drawSeries(canvas, secondary, count, left, right, top, bottom, min, max, secondaryColor, false);

        paint.setStyle(Paint.Style.FILL);
        paint.setTypeface(android.graphics.Typeface.DEFAULT);
        paint.setTextSize(sp(11));
        paint.setColor(Color.rgb(154, 177, 201));
        paint.setTextAlign(Paint.Align.CENTER);
        for (int i = 0; i < count; i++) {
            canvas.drawText(labels[i], xFor(i, count, left, right), getHeight() - dp(14), paint);
        }
    }

    private void drawSeries(Canvas canvas, double[] values, int count, float left, float right,
                            float top, float bottom, double min, double max, int color, boolean labelsAbove) {
        Path path = new Path();
        boolean started = false;
        for (int i = 0; i < count; i++) {
            if (!Double.isFinite(values[i])) continue;
            float x = xFor(i, count, left, right);
            float y = yFor(values[i], top, bottom, min, max);
            if (!started) {
                path.moveTo(x, y);
                started = true;
            } else {
                path.lineTo(x, y);
            }
        }
        if (!started) return;

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dpF(3));
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setColor(color);
        canvas.drawPath(path, paint);

        for (int i = 0; i < count; i++) {
            if (!Double.isFinite(values[i])) continue;
            float x = xFor(i, count, left, right);
            float y = yFor(values[i], top, bottom, min, max);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(color);
            canvas.drawCircle(x, y, dpF(4.5f), paint);

            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(sp(10));
            paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            paint.setColor(color);
            String label = format(values[i]) + suffix;
            canvas.drawText(label, x, y + (labelsAbove ? -dp(12) : dp(20)), paint);
        }
    }

    private float xFor(int index, int count, float left, float right) {
        if (count <= 1) return (left + right) / 2f;
        return left + (right - left) * index / (count - 1f);
    }

    private float yFor(double value, float top, float bottom, double min, double max) {
        double ratio = (value - min) / (max - min);
        return (float) (bottom - ratio * (bottom - top));
    }

    private String format(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.05) return String.format(java.util.Locale.getDefault(), "%.0f", value);
        return String.format(java.util.Locale.getDefault(), "%.1f", value);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private float dpF(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private float sp(float value) {
        return value * getResources().getDisplayMetrics().scaledDensity;
    }
}
