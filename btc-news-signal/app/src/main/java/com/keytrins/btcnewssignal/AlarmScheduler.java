package com.keytrins.btcnewssignal;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.List;

public final class AlarmScheduler {
    private AlarmScheduler() {}

    public static void schedule(Context context, List<NewsEvent> events) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        long now = System.currentTimeMillis();
        for (NewsEvent e : events) {
            scheduleOne(context, am, e, 60, now);
            scheduleOne(context, am, e, 5, now);
            scheduleOne(context, am, e, 1, now);
        }
    }

    private static void scheduleOne(Context context, AlarmManager am, NewsEvent e, int minutesBefore, long now) {
        long trigger = e.timeMillis - minutesBefore * 60_000L;
        if (trigger <= now) return;
        Intent i = new Intent(context, AlarmReceiver.class);
        i.putExtra("type", e.type);
        i.putExtra("title", e.title);
        i.putExtra("eventTime", e.timeMillis);
        i.putExtra("minutesBefore", minutesBefore);
        int code = (e.type + e.timeMillis + minutesBefore).hashCode();
        PendingIntent pi = PendingIntent.getBroadcast(context, code, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi);
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi);
        }
    }
}
