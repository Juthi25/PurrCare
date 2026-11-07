package com.purrcare.app.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Called by AlarmManager at the reminder time. It just shows the notification. */
public class ReminderReceiver extends BroadcastReceiver {
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_TEXT = "text";
    public static final String EXTRA_ID = "id";

    @Override
    public void onReceive(Context context, Intent intent) {
        String title = intent.getStringExtra(EXTRA_TITLE);
        String text = intent.getStringExtra(EXTRA_TEXT);
        int id = intent.getIntExtra(EXTRA_ID, 0);
        NotificationHelper.showReminder(context, id,
                title == null ? "PurrCare reminder" : title,
                text == null ? "" : text);
    }
}
