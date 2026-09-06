package com.keytrins.btcnewssignal;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.json.JSONObject;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class AlarmReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        int before = intent.getIntExtra("minutesBefore", 60);
        String type = intent.getStringExtra("type");
        String title = intent.getStringExtra("title");
        if (before == 60) {
            NotificationHelper.show(context, (type + intent.getLongExtra("eventTime", 0)).hashCode(),
                    "Через час: " + type, "Откройте приложение. Начинается окно наблюдения за импульсом BTC.");
            return;
        }
        PendingResult pending = goAsync();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                JSONObject s = MarketData.fetchSignal();
                String signal = s.getString("signal");
                String action = "BUY".equals(signal) ? "BUY EUR/USD" : "SELL".equals(signal) ? "SELL EUR/USD" : "НЕТ ВХОДА";
                String body = String.format(Locale.US, "%s через %d мин. BTC 30м: %+.3f%%, 60м: %+.3f%%. %s",
                        title, before, s.getDouble("p30"), s.getDouble("p60"), action);
                NotificationHelper.show(context, (type + before + intent.getLongExtra("eventTime", 0)).hashCode(), action, body);
            } catch (Exception e) {
                NotificationHelper.show(context, (type + before).hashCode(), "Проверьте сигнал вручную", title + " через " + before + " мин. Не удалось получить BTC-котировки.");
            } finally {
                executor.shutdown();
                pending.finish();
            }
        });
    }
}
