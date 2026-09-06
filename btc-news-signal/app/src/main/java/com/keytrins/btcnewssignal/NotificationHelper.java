package com.keytrins.btcnewssignal;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public final class NotificationHelper {
    private static final String CHANNEL = "news_signals";
    private NotificationHelper() {}

    public static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(CHANNEL, "Сигналы перед новостями", NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("NFP, CPI и PPI: начало анализа и направление BTC");
            ch.enableVibration(true);
            context.getSystemService(NotificationManager.class).createNotificationChannel(ch);
        }
    }

    public static void show(Context context, int id, String title, String body) {
        ensureChannel(context);
        Intent open = new Intent(context, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi = PendingIntent.getActivity(context, 0, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? new Notification.Builder(context, CHANNEL) : new Notification.Builder(context);
        b.setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new Notification.BigTextStyle().bigText(body))
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_ALARM)
                .setPriority(Notification.PRIORITY_HIGH);
        context.getSystemService(NotificationManager.class).notify(id, b.build());
    }
}
