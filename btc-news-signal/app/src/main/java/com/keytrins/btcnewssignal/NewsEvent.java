package com.keytrins.btcnewssignal;

import org.json.JSONException;
import org.json.JSONObject;

public final class NewsEvent {
    public final String type;
    public final String title;
    public final long timeMillis;

    public NewsEvent(String type, String title, long timeMillis) {
        this.type = type;
        this.title = title;
        this.timeMillis = timeMillis;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("type", type);
        o.put("title", title);
        o.put("timeMillis", timeMillis);
        o.put("quality", "PPI".equals(type) ? 90.9 : "NFP".equals(type) ? 77.8 : 66.7);
        o.put("hold", "PPI".equals(type) ? "10–60 сек" : "1–5 мин");
        return o;
    }
}
