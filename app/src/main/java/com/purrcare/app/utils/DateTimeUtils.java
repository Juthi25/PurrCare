package com.purrcare.app.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Dates are stored in Firebase as text "yyyy-MM-dd" (easy to sort) and times as "HH:mm".
 * These helpers convert them to friendly text for the screen.
 */
public final class DateTimeUtils {
    private DateTimeUtils() { }

    public static String todayString() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    /** "2026-11" - used to pick this month's expenses. */
    public static String currentMonthPrefix() {
        return new SimpleDateFormat("yyyy-MM", Locale.US).format(new Date());
    }

    public static String monthLabel() {
        return new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(new Date());
    }

    public static String toIso(int year, int monthZeroBased, int day) {
        return String.format(Locale.US, "%04d-%02d-%02d", year, monthZeroBased + 1, day);
    }

    /** Returns null if the text is not a valid date. */
    public static Calendar parseIso(String iso) {
        if (iso == null || iso.isEmpty()) return null;
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(iso);
            if (d == null) return null;
            Calendar c = Calendar.getInstance();
            c.setTime(d);
            return c;
        } catch (Exception e) {
            return null;
        }
    }

    /** "2026-03-14" -> "14 Mar 2026" */
    public static String pretty(String iso) {
        Calendar c = parseIso(iso);
        if (c == null) return iso == null ? "" : iso;
        return new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(c.getTime());
    }

    /** "2026-03-14" -> "14 Mar" (used on the weight chart). */
    public static String shortDate(String iso) {
        Calendar c = parseIso(iso);
        if (c == null) return "";
        return new SimpleDateFormat("dd MMM", Locale.getDefault()).format(c.getTime());
    }

    /** Age calculated automatically from the date of birth, e.g. "2 years 3 months". */
    public static String calculateAge(String dobIso) {
        Calendar dob = parseIso(dobIso);
        if (dob == null) return "Unknown";
        Calendar now = Calendar.getInstance();
        int years = now.get(Calendar.YEAR) - dob.get(Calendar.YEAR);
        int months = now.get(Calendar.MONTH) - dob.get(Calendar.MONTH);
        if (now.get(Calendar.DAY_OF_MONTH) < dob.get(Calendar.DAY_OF_MONTH)) months--;
        if (months < 0) {
            years--;
            months += 12;
        }
        if (years < 0) return "Not born yet";
        if (years == 0 && months == 0) return "Less than a month";
        StringBuilder sb = new StringBuilder();
        if (years > 0) sb.append(years).append(years == 1 ? " year" : " years");
        if (months > 0) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(months).append(months == 1 ? " month" : " months");
        }
        return sb.toString();
    }

    /** Days from today to the given date (negative = in the past). */
    public static long daysUntil(String iso) {
        Calendar target = parseIso(iso);
        Calendar today = parseIso(todayString());
        if (target == null || today == null) return Long.MAX_VALUE;
        return Math.round((target.getTimeInMillis() - today.getTimeInMillis()) / 86400000.0);
    }

    public static String dueText(String iso) {
        long d = daysUntil(iso);
        if (d == Long.MAX_VALUE) return "";
        if (d < 0) return "Overdue by " + (-d) + (d == -1 ? " day" : " days");
        if (d == 0) return "Due today";
        if (d == 1) return "Due tomorrow";
        return "Due in " + d + " days";
    }

    /** Combines "2026-03-14" and "18:30" into milliseconds. Returns -1 if invalid. */
    public static long toMillis(String isoDate, String hhmm) {
        Calendar c = parseIso(isoDate);
        if (c == null || hhmm == null || !hhmm.matches("\\d{1,2}:\\d{2}")) return -1;
        String[] p = hhmm.split(":");
        c.set(Calendar.HOUR_OF_DAY, Integer.parseInt(p[0]));
        c.set(Calendar.MINUTE, Integer.parseInt(p[1]));
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    public static String dateFromMillis(long millis) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date(millis));
    }

    public static String timeFromMillis(long millis) {
        return new SimpleDateFormat("HH:mm", Locale.US).format(new Date(millis));
    }

    public static String formatDateTime(long millis) {
        return new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(new Date(millis));
    }

    /** "18:30" -> "06:30 PM" */
    public static String formatTime12(String hhmm) {
        if (hhmm == null || !hhmm.matches("\\d{1,2}:\\d{2}")) return hhmm == null ? "" : hhmm;
        long millis = toMillis(todayString(), hhmm);
        return new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date(millis));
    }
}
