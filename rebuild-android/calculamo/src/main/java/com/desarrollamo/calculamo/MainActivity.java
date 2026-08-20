package com.desarrollamo.calculamo;

import android.app.Activity;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Locale;

public class MainActivity extends Activity {
    private EditText a, b;
    private TextView result, history;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 48, 32, 32);
        TextView title = new TextView(this);
        title.setText("CalculAMO"); title.setTextSize(30f);
        root.addView(title);
        a = input("Primer número"); b = input("Segundo número");
        root.addView(a); root.addView(b);
        LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL);
        addOp(row, "+"); addOp(row, "−"); addOp(row, "×"); addOp(row, "÷");
        root.addView(row);
        result = new TextView(this); result.setText("Resultado: —"); result.setTextSize(24f); result.setPadding(0,24,0,16); root.addView(result);
        history = new TextView(this); history.setText("Historial de esta sesión"); history.setTextSize(16f); root.addView(history);
        Button clear = new Button(this); clear.setText("Limpiar"); clear.setOnClickListener(v -> { a.setText(""); b.setText(""); result.setText("Resultado: —"); history.setText("Historial de esta sesión"); }); root.addView(clear);
        setContentView(root);
    }

    private EditText input(String hint) {
        EditText e = new EditText(this); e.setHint(hint); e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED); return e;
    }

    private void addOp(LinearLayout row, String op) {
        Button btn = new Button(this); btn.setText(op); btn.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)); btn.setOnClickListener(v -> calculate(op)); row.addView(btn);
    }

    private void calculate(String op) {
        try {
            double x = Double.parseDouble(a.getText().toString().replace(',', '.'));
            double y = Double.parseDouble(b.getText().toString().replace(',', '.'));
            double z;
            switch (op) {
                case "+": z = x + y; break;
                case "−": z = x - y; break;
                case "×": z = x * y; break;
                default: if (y == 0) { result.setText("No se puede dividir por cero"); return; } z = x / y;
            }
            String line = String.format(Locale.US, "%s %s %s = %s", trim(x), op, trim(y), trim(z));
            result.setText("Resultado: " + trim(z));
            history.setText(line + "\n" + history.getText());
        } catch (Exception ex) { result.setText("Ingresá dos números válidos"); }
    }

    private String trim(double v) { return v == Math.rint(v) ? Long.toString((long)v) : Double.toString(v); }
}
