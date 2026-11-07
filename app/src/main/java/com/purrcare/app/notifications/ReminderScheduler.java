package com.purrcare.app.notifications;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.purrcare.app.models.Reminder;
import com.purrcare.app.utils.DateTimeUtils;
import com.purrcare.app.utils.UiUtils;

import java.util.List;

/**
 * Schedules / cancels the alarm of a reminder with AlarmManager.
 * Each reminder uses its own request code (from its id) so it can be updated or cancelled later.
 */
public final class ReminderScheduler {
    private ReminderScheduler() { }

    private static PendingIntent buildIntent(Context context, Reminder r, int flags) {
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra(ReminderReceiver.EXTRA_ID, r.getId().hashCode());
        intent.putExtra(ReminderReceiver.EXTRA_TITLE, "\uD83D\uDC3E " + UiUtils.nz(r.getTitle()));
        intent.putExtra(ReminderReceiver.EXTRA_TEXT, UiUtils.nz(r.getCatName()) + " \u2022 " + UiUtils.nz(r.getType())
                + (UiUtils.nz(r.getNotes()).isEmpty() ? "" : "\n" + r.getNotes()));
        return PendingIntent.getBroadcast(context, r.getId().hashCode(), intent, flags | PendingIntent.FLAG_IMMUTABLE);
    }

    /** Sets an alarm for a pending reminder in the future (same reminder again = alarm is replaced). */
    public static void schedule(Context context, Reminder r) {
        if (r == null || r.getId() == null || r.isCompleted() || r.getDateTimeMillis() <= System.currentTimeMillis()) return;
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;
        PendingIntent pending = buildIntent(context, r, PendingIntent.FLAG_UPDATE_CURRENT);
        long time = r.getDateTimeMillis();
        try {
            // Exact alarm when Android allows it, otherwise a normal alarm that may be a few minutes late.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pending);
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pending);
            }
        } catch (SecurityException e) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pending);
        }
    }

    public static void cancel(Context context, Reminder r) {
        if (r == null || r.getId() == null) return;
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pending = buildIntent(context, r, PendingIntent.FLAG_UPDATE_CURRENT);
        if (alarmManager != null) alarmManager.cancel(pending);
    }

    /** Called whenever the reminder list loads, so alarms survive a phone restart as soon as the app is opened. */
    public static void rescheduleAll(Context context, List<Reminder> reminders) {
        for (Reminder r : reminders) schedule(context, r);
    }

    public static String describe(Reminder r) {
        return r.getTitle() + " - " + DateTimeUtils.formatDateTime(r.getDateTimeMillis());
    }
}
