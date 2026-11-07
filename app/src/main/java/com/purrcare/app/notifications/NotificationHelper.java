package com.purrcare.app.notifications;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.purrcare.app.R;
import com.purrcare.app.activities.RemindersActivity;

/** Creates the notification channel and shows a reminder notification. */
public final class NotificationHelper {
    public static final String CHANNEL_ID = "purrcare_reminders";

    private NotificationHelper() { }

    /** Android 8+ requires a channel before any notification can be shown. */
    public static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Cat care reminders", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Feeding, medicine, vaccination and vet reminders");
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    public static void showReminder(Context context, int notificationId, String title, String text) {
        createChannel(context);
        // Tapping the notification opens the Reminders screen
        Intent open = new Intent(context, RemindersActivity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context, notificationId, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_paw)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(contentIntent);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        // If the user denied the notification permission, Android simply ignores this call.
        if (manager != null) manager.notify(notificationId, builder.build());
    }
}
