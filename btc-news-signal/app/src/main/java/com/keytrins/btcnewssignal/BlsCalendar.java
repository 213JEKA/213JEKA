package com.keytrins.btcnewssignal;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class BlsCalendar {
    private static final String ICS_URL = "https://www.bls.gov/schedule/news_release/bls.ics";
    private static final String PREFS = "btc_news_signal";
    private static final String CACHE = "events_json";

    private BlsCalendar() {}

    public static List<NewsEvent> download() throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(ICS_URL).openConnection();
        c.setConnectTimeout(12_000);
        c.setReadTimeout(12_000);
        c.setRequestProperty("User-Agent", "BTC-News-Signal/0.1");
        if (c.getResponseCode() != 200) throw new IllegalStateException("BLS HTTP " + c.getResponseCode());
        StringBuilder text = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) text.append(line).append('\n');
        }
        return parse(text.toString());
    }

    static List<NewsEvent> parse(String ics) {
        List<NewsEvent> result = new ArrayList<>();
        String[] blocks = ics.replace("\r", "").split("BEGIN:VEVENT");
        for (String block : blocks) {
            String summary = field(block, "SUMMARY:");
            String rawDate = dateField(block);
            if (summary == null || rawDate == null) continue;
            String type = classify(summary);
            if (type == null) continue;
            try {
                long millis;
                if (rawDate.endsWith("Z")) {
                    LocalDateTime d = LocalDateTime.parse(rawDate.substring(0, 15), DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"));
                    millis = d.atZone(ZoneId.of("UTC")).toInstant().toEpochMilli();
                } else {
                    LocalDateTime d = LocalDateTime.parse(rawDate.substring(0, 15), DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"));
                    millis = d.atZone(ZoneId.of("America/New_York")).toInstant().toEpochMilli();
                }
                if (millis > System.currentTimeMillis() - 3_600_000L) {
                    result.add(new NewsEvent(type, displayTitle(type), millis));
                }
            } catch (Exception ignored) {}
        }
        result.sort(Comparator.comparingLong(e -> e.timeMillis));
        return result;
    }

    private static String classify(String summary) {
        if (summary.equalsIgnoreCase("Employment Situation")) return "NFP";
        if (summary.equalsIgnoreCase("Consumer Price Index")) return "CPI";
        if (summary.equalsIgnoreCase("Producer Price Index")) return "PPI";
        return null;
    }

    private static String displayTitle(String type) {
        if ("NFP".equals(type)) return "Nonfarm Payrolls / Employment Situation";
        if ("CPI".equals(type)) return "Инфляция США — CPI";
        return "Инфляция производителей США — PPI";
    }

    private static String field(String block, String prefix) {
        for (String line : block.split("\n")) if (line.startsWith(prefix)) return line.substring(prefix.length()).trim();
        return null;
    }

    private static String dateField(String block) {
        for (String line : block.split("\n")) {
            if (line.startsWith("DTSTART")) {
                int p = line.indexOf(':');
                return p >= 0 ? line.substring(p + 1).trim() : null;
            }
        }
        return null;
    }

    public static String toJson(List<NewsEvent> events) {
        JSONArray a = new JSONArray();
        for (NewsEvent e : events) try { a.put(e.toJson()); } catch (Exception ignored) {}
        return a.toString();
    }

    public static List<NewsEvent> fromJson(String json) {
        List<NewsEvent> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(json);
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                long t = o.getLong("timeMillis");
                if (t > System.currentTimeMillis() - 3_600_000L) out.add(new NewsEvent(o.getString("type"), o.getString("title"), t));
            }
        } catch (Exception ignored) {}
        out.sort(Comparator.comparingLong(e -> e.timeMillis));
        return out;
    }

    public static void save(Context context, List<NewsEvent> events) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(CACHE, toJson(events)).apply();
    }

    public static List<NewsEvent> cached(Context context) {
        SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        List<NewsEvent> events = fromJson(p.getString(CACHE, "[]"));
        if (!events.isEmpty()) return events;
        return fallback();
    }

    private static List<NewsEvent> fallback() {
        List<NewsEvent> out = new ArrayList<>();
        addFallback(out, "PPI", "Инфляция производителей США — PPI", "2026-09-10T08:30:00", "America/New_York");
        addFallback(out, "CPI", "Инфляция США — CPI", "2026-09-11T08:30:00", "America/New_York");
        addFallback(out, "NFP", "Nonfarm Payrolls / Employment Situation", "2026-11-06T08:30:00", "America/New_York");
        addFallback(out, "CPI", "Инфляция США — CPI", "2026-11-10T08:30:00", "America/New_York");
        addFallback(out, "NFP", "Nonfarm Payrolls / Employment Situation", "2026-12-04T08:30:00", "America/New_York");
        out.removeIf(e -> e.timeMillis < System.currentTimeMillis() - 3_600_000L);
        return out;
    }

    private static void addFallback(List<NewsEvent> out, String type, String title, String local, String zone) {
        long t = LocalDateTime.parse(local).atZone(ZoneId.of(zone)).toInstant().toEpochMilli();
        out.add(new NewsEvent(type, title, t));
    }
}
