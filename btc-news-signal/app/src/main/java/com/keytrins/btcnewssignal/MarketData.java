package com.keytrins.btcnewssignal;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class MarketData {
    private static final String QUERY = "/v5/market/kline?category=linear&symbol=BTCUSDT&interval=1&limit=61";
    private static final String[] BASE_URLS = {
            "https://api.bybit.com",
            "https://api.bytick.com"
    };
    public static final double THRESHOLD = 0.15;

    private MarketData() {}

    public static JSONObject fetchSignal() throws Exception {
        Exception lastError = null;
        for (String baseUrl : BASE_URLS) {
            try {
                return fetchFrom(baseUrl);
            } catch (Exception e) {
                lastError = e;
            }
        }
        throw new IllegalStateException("Bybit недоступен", lastError);
    }

    private static JSONObject fetchFrom(String baseUrl) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(baseUrl + QUERY).openConnection();
        c.setConnectTimeout(10_000);
        c.setReadTimeout(10_000);
        c.setRequestProperty("User-Agent", "BTC-News-Signal/0.1.1");
        c.setRequestProperty("Accept", "application/json");
        if (c.getResponseCode() != 200) throw new IllegalStateException("Bybit HTTP " + c.getResponseCode());
        StringBuilder text = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) text.append(line);
        }
        JSONObject response = new JSONObject(text.toString());
        if (response.optInt("retCode", -1) != 0) {
            throw new IllegalStateException("Bybit: " + response.optString("retMsg", "ошибка API"));
        }
        JSONArray a = response.getJSONObject("result").getJSONArray("list");
        if (a.length() < 61) throw new IllegalStateException("Недостаточно свечей BTC");

        // Bybit возвращает свечи от новой к старой: [0] — текущая минута.
        double last = a.getJSONArray(0).getDouble(4);
        double open30 = a.getJSONArray(30).getDouble(1);
        double open60 = a.getJSONArray(60).getDouble(1);
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
        o.put("provider", "Bybit");
        o.put("market", "BTCUSDT perpetual");
        o.put("updatedAt", System.currentTimeMillis());
        return o;
    }
}
