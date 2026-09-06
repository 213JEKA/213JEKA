package com.keytrins.btcnewssignal;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class MarketData {
    private static final String URL_TEXT = "https://api.binance.com/api/v3/klines?symbol=BTCUSDT&interval=1m&limit=61";
    public static final double THRESHOLD = 0.15;

    private MarketData() {}

    public static JSONObject fetchSignal() throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(URL_TEXT).openConnection();
        c.setConnectTimeout(10_000);
        c.setReadTimeout(10_000);
        c.setRequestProperty("User-Agent", "BTC-News-Signal/0.1");
        if (c.getResponseCode() != 200) throw new IllegalStateException("Binance HTTP " + c.getResponseCode());
        StringBuilder text = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) text.append(line);
        }
        JSONArray a = new JSONArray(text.toString());
        if (a.length() < 60) throw new IllegalStateException("Недостаточно свечей BTC");
        int n = a.length();
        double last = a.getJSONArray(n - 1).getDouble(4);
        double open30 = a.getJSONArray(Math.max(0, n - 30)).getDouble(1);
        double open60 = a.getJSONArray(0).getDouble(1);
        double p30 = (last / open30 - 1.0) * 100.0;
        double p60 = (last / open60 - 1.0) * 100.0;
        boolean aligned = Math.signum(p30) == Math.signum(p60) && Math.signum(p30) != 0;
        String signal = "WAIT";
        if (Math.abs(p30) >= THRESHOLD && aligned) signal = p30 > 0 ? "BUY" : "SELL";

        JSONObject o = new JSONObject();
        o.put("price", last);
        o.put("p30", p30);
        o.put("p60", p60);
        o.put("aligned", aligned);
        o.put("threshold", THRESHOLD);
        o.put("signal", signal);
        o.put("updatedAt", System.currentTimeMillis());
        return o;
    }
}
