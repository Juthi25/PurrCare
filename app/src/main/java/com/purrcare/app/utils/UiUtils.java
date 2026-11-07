package com.purrcare.app.utils;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Calendar;
import java.util.Locale;

/** Small helpers used by many screens: messages, input validation, pickers. */
public final class UiUtils {
    private UiUtils() { }

    public static void snack(View anchor, String message) {
        if (anchor != null) Snackbar.make(anchor, message, Snackbar.LENGTH_LONG).show();
    }

    public static void toast(Context context, String message) {
        Toast.makeText(context.getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    /** null-safe string. */
    public static String nz(String s) {
        return s == null ? "" : s;
    }

    public static int dp(Context c, int dp) {
        return Math.round(dp * c.getResources().getDisplayMetrics().density);
    }

    /** "\u09F3 1,250.50" */
    public static String money(double value) {
        return Constants.CURRENCY + " " + String.format(Locale.US, "%,.2f", value);
    }

    // ------------------------------------------------------------------ validation

    /** Text of the field, trimmed ("" if empty). */
    public static String text(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    /**
     * INPUT VALIDATION: returns the text, or shows a red error with setError() and returns null
     * when the field is empty. The caller just checks "if (value == null) return;".
     */
    public static String requireText(TextInputEditText et, String errorMessage) {
        String value = text(et);
        if (value.isEmpty()) {
            et.setError(errorMessage);
            et.requestFocus();
            return null;
        }
        return value;
    }

    /** Returns a number greater than 0, or shows an error and returns null. */
    public static Double requirePositiveNumber(TextInputEditText et, String emptyMessage) {
        String value = requireText(et, emptyMessage);
        if (value == null) return null;
        try {
            double number = Double.parseDouble(value);
            if (number <= 0) throw new NumberFormatException();
            return number;
        } catch (NumberFormatException e) {
            et.setError("Enter a valid number greater than 0");
            et.requestFocus();
            return null;
        }
    }

    // ------------------------------------------------------------------ pickers

    /** Makes the field open a date picker when tapped; the field then contains yyyy-MM-dd. */
    public static void attachDatePicker(Context context, TextInputEditText et, boolean blockFuture) {
        et.setFocusable(false);
        et.setClickable(true);
        et.setOnClickListener(v -> {
            Calendar c = DateTimeUtils.parseIso(text(et));
            if (c == null) c = Calendar.getInstance();
            DatePickerDialog dialog = new DatePickerDialog(context, (view, year, month, day) -> {
                et.setText(DateTimeUtils.toIso(year, month, day));
                et.setError(null);
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
            if (blockFuture) dialog.getDatePicker().setMaxDate(System.currentTimeMillis());
            dialog.show();
        });
    }

    /** Makes the field open a time picker when tapped; the field then contains HH:mm. */
    public static void attachTimePicker(Context context, TextInputEditText et) {
        et.setFocusable(false);
        et.setClickable(true);
        et.setOnClickListener(v -> {
            Calendar now = Calendar.getInstance();
            String[] parts = text(et).matches("\\d{1,2}:\\d{2}") ? text(et).split(":") : null;
            int hour = parts != null ? Integer.parseInt(parts[0]) : now.get(Calendar.HOUR_OF_DAY);
            int minute = parts != null ? Integer.parseInt(parts[1]) : now.get(Calendar.MINUTE);
            new TimePickerDialog(context, (view, h, m) -> {
                et.setText(String.format(Locale.US, "%02d:%02d", h, m));
                et.setError(null);
            }, hour, minute, false).show();
        });
    }

    // ------------------------------------------------------------------ spinners

    public static void setupSpinner(Context context, Spinner spinner, String[] items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    public static int indexOf(String[] items, String value) {
        for (int i = 0; i < items.length; i++) {
            if (items[i].equals(value)) return i;
        }
        return 0;
    }
}
