package com.vamsi.cricketwidget;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/** Small on-device cache so match arrows work even after the widget process is restarted. */
public final class MatchStore {
    private static final String FILE = "cricket_match_store";
    private static final String KEY_MATCHES = "matches";
    private static final String KEY_INDEX = "index";
    private MatchStore() { }

    public static void save(Context context, List<MatchSnapshot> matches, int index) {
        JSONArray array = new JSONArray();
        for (MatchSnapshot s : matches) {
            JSONObject o = new JSONObject();
            try {
                o.put("id", s.id); o.put("competition", s.competition); o.put("t1", s.team1); o.put("t2", s.team2);
                o.put("f1", s.flag1); o.put("f2", s.flag2); o.put("s1", s.score1); o.put("s2", s.score2);
                o.put("state", s.state); o.put("summary", s.summary); o.put("detail", s.detail); o.put("at", s.updatedAt);
                array.put(o);
            } catch (Exception ignored) { }
        }
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
                .putString(KEY_MATCHES, array.toString()).putInt(KEY_INDEX, Math.max(0, index)).apply();
    }

    public static List<MatchSnapshot> load(Context context) {
        List<MatchSnapshot> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY_MATCHES, "[]"));
            for (int i = 0; i < a.length(); i++) { JSONObject o = a.getJSONObject(i); out.add(new MatchSnapshot(
                    o.optString("id"), o.optString("competition"), o.optString("t1"), o.optString("t2"),
                    o.optString("f1"), o.optString("f2"), o.optString("s1"), o.optString("s2"), o.optString("state"),
                    o.optString("summary"), o.optString("detail"), o.optLong("at"))); }
        } catch (Exception ignored) { }
        return out;
    }

    public static int index(Context context, int count) { return count == 0 ? 0 : Math.floorMod(context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getInt(KEY_INDEX, 0), count); }
}
