package com.desarrollamo.depositamo;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int PICK_FILE = 41;
    private static final String PREFS = "depositamo_local";
    private static final String KEY_ITEMS = "items";
    private static final int MAX_ITEMS = 100;

    private final int background = Color.rgb(6, 16, 28);
    private final int panel = Color.rgb(15, 32, 50);
    private final int cyan = Color.rgb(103, 210, 255);
    private final int muted = Color.rgb(172, 188, 205);

    private EditText textInput;
    private TextView summary;
    private LinearLayout history;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(background);
        getWindow().setNavigationBarColor(background);
        setContentView(buildUi());
        renderHistory();
    }

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(background);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(24), dp(20), dp(32));
        scroll.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));

        TextView eyebrow = text("DEPOSITAMO · LOCAL", 12, cyan, true);
        root.addView(eyebrow);

        TextView title = text("Depositar material", 30, Color.WHITE, true);
        title.setPadding(0, dp(8), 0, dp(6));
        root.addView(title);

        TextView intro = text(
                "Guardá textos y archivos dentro del teléfono. Cada depósito conserva tamaño y SHA-256 para detectar duplicados.",
                15, muted, false);
        intro.setPadding(0, 0, 0, dp(18));
        root.addView(intro);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackgroundColor(panel);
        root.addView(card, lpMatchWrap(dp(0)));

        TextView label = text("Texto rápido", 16, Color.WHITE, true);
        card.addView(label);

        textInput = new EditText(this);
        textInput.setHint("Pegá una idea, nota, prompt o fragmento…");
        textInput.setHintTextColor(Color.rgb(120, 140, 160));
        textInput.setTextColor(Color.WHITE);
        textInput.setTextSize(16f);
        textInput.setGravity(Gravity.TOP | Gravity.START);
        textInput.setMinLines(5);
        textInput.setMaxLines(10);
        textInput.setPadding(dp(12), dp(12), dp(12), dp(12));
        textInput.setBackgroundColor(Color.rgb(9, 24, 39));
        LinearLayout.LayoutParams inputLp = lpMatchWrap(dp(12));
        card.addView(textInput, inputLp);

        Button saveText = button("GUARDAR TEXTO");
        saveText.setOnClickListener(v -> saveTextDeposit());
        card.addView(saveText, lpMatchWrap(dp(12)));

        Button pickFile = button("ADJUNTAR ARCHIVO");
        pickFile.setOnClickListener(v -> openFilePicker());
        card.addView(pickFile, lpMatchWrap(dp(8)));

        summary = text("", 14, muted, false);
        summary.setPadding(0, dp(22), 0, dp(8));
        root.addView(summary);

        TextView historyTitle = text("Depósitos recientes", 20, Color.WHITE, true);
        historyTitle.setPadding(0, dp(4), 0, dp(10));
        root.addView(historyTitle);

        history = new LinearLayout(this);
        history.setOrientation(LinearLayout.VERTICAL);
        root.addView(history, lpMatchWrap(0));

        return scroll;
    }

    private void saveTextDeposit() {
        String value = textInput.getText().toString().trim();
        if (value.isEmpty()) {
            Toast.makeText(this, "Escribí algo antes de guardar.", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
            JSONObject item = new JSONObject();
            item.put("type", "text");
            item.put("name", "Texto " + timestampLabel());
            item.put("created_at", System.currentTimeMillis());
            item.put("size_bytes", bytes.length);
            item.put("sha256", sha256(bytes));
            item.put("preview", value.length() > 160 ? value.substring(0, 160) + "…" : value);
            prepend(item);
            textInput.setText("");
            renderHistory();
            Toast.makeText(this, "Texto depositado.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "No se pudo guardar: " + safe(e.getMessage()), Toast.LENGTH_LONG).show();
        }
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, PICK_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_FILE || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        String displayName = queryDisplayName(uri);
        Toast.makeText(this, "Guardando " + displayName + "…", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            try {
                JSONObject item = copyPickedFile(uri, displayName);
                runOnUiThread(() -> {
                    try {
                        prepend(item);
                        renderHistory();
                        Toast.makeText(this, "Archivo depositado.", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(this, "No se pudo registrar el archivo.", Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this,
                        "No se pudo guardar: " + safe(e.getMessage()), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private JSONObject copyPickedFile(Uri uri, String displayName) throws Exception {
        File dir = new File(getFilesDir(), "deposits");
        if (!dir.exists() && !dir.mkdirs()) throw new IllegalStateException("No se pudo crear el depósito local");
        String clean = displayName.replaceAll("[^A-Za-z0-9._-]", "_");
        if (clean.isEmpty()) clean = "archivo.bin";
        File output = new File(dir, System.currentTimeMillis() + "-" + clean);

        MessageDigest md = MessageDigest.getInstance("SHA-256");
        long total = 0L;
        try (InputStream input = getContentResolver().openInputStream(uri);
             FileOutputStream out = new FileOutputStream(output)) {
            if (input == null) throw new IllegalStateException("Android no entregó el archivo");
            byte[] buffer = new byte[128 * 1024];
            while (true) {
                int count = input.read(buffer);
                if (count <= 0) break;
                out.write(buffer, 0, count);
                md.update(buffer, 0, count);
                total += count;
            }
            out.getFD().sync();
        }
        if (total <= 0) {
            output.delete();
            throw new IllegalStateException("El archivo está vacío");
        }

        JSONObject item = new JSONObject();
        item.put("type", "file");
        item.put("name", displayName);
        item.put("created_at", System.currentTimeMillis());
        item.put("size_bytes", total);
        item.put("sha256", hex(md.digest()));
        item.put("local_file", output.getName());
        return item;
    }

    private void prepend(JSONObject item) throws Exception {
        JSONArray old = readItems();
        JSONArray fresh = new JSONArray();
        fresh.put(item);
        for (int i = 0; i < old.length() && fresh.length() < MAX_ITEMS; i++) {
            fresh.put(old.getJSONObject(i));
        }
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(KEY_ITEMS, fresh.toString()).apply();
    }

    private JSONArray readItems() {
        String raw = getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_ITEMS, "[]");
        try {
            return new JSONArray(raw == null ? "[]" : raw);
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    private void renderHistory() {
        JSONArray items = readItems();
        history.removeAllViews();
        summary.setText(items.length() + (items.length() == 1 ? " depósito guardado en este teléfono" : " depósitos guardados en este teléfono"));

        if (items.length() == 0) {
            TextView empty = text("Todavía no hay material. Guardá un texto o adjuntá un archivo.", 14, muted, false);
            empty.setPadding(0, dp(8), 0, dp(8));
            history.addView(empty);
            return;
        }

        int max = Math.min(items.length(), 20);
        for (int i = 0; i < max; i++) {
            JSONObject item = items.optJSONObject(i);
            if (item == null) continue;
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(dp(14), dp(12), dp(14), dp(12));
            row.setBackgroundColor(panel);

            String type = item.optString("type", "item").equals("file") ? "ARCHIVO" : "TEXTO";
            TextView kind = text(type, 11, cyan, true);
            row.addView(kind);

            TextView name = text(item.optString("name", "Depósito"), 16, Color.WHITE, true);
            name.setPadding(0, dp(4), 0, dp(4));
            row.addView(name);

            long size = item.optLong("size_bytes", 0L);
            String sha = item.optString("sha256", "");
            TextView meta = text(formatBytes(size) + " · SHA-256 " + shortSha(sha), 12, muted, false);
            row.addView(meta);

            String preview = item.optString("preview", "");
            if (!preview.isEmpty()) {
                TextView pv = text(preview, 13, Color.rgb(210, 220, 230), false);
                pv.setPadding(0, dp(7), 0, 0);
                row.addView(pv);
            }

            LinearLayout.LayoutParams rowLp = lpMatchWrap(dp(8));
            history.addView(row, rowLp);
        }
    }

    private String queryDisplayName(Uri uri) {
        String name = "archivo";
        try (Cursor cursor = getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) {
                    String value = cursor.getString(index);
                    if (value != null && !value.trim().isEmpty()) name = value.trim();
                }
            }
        } catch (Exception ignored) { }
        return name;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(view.getTypeface(), android.graphics.Typeface.BOLD);
        return view;
    }

    private Button button(String value) {
        Button b = new Button(this);
        b.setText(value);
        b.setTextSize(14f);
        b.setTextColor(Color.rgb(5, 22, 34));
        b.setBackgroundColor(cyan);
        return b;
    }

    private LinearLayout.LayoutParams lpMatchWrap(int topMargin) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        p.topMargin = topMargin;
        return p;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private String timestampLabel() {
        return new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(new Date());
    }

    private static String sha256(byte[] data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return hex(md.digest(data));
    }

    private static String hex(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 2);
        for (byte b : data) sb.append(String.format(Locale.US, "%02x", b & 0xff));
        return sb.toString();
    }

    private static String shortSha(String sha) {
        if (sha == null || sha.length() < 12) return sha == null ? "" : sha;
        return sha.substring(0, 12) + "…";
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024L * 1024L) return String.format(Locale.US, "%.1f KB", bytes / 1024.0);
        return String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private static String safe(String value) {
        return value == null || value.trim().isEmpty() ? "error desconocido" : value;
    }
}
