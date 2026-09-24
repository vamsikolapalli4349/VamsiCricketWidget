package com.vamsi.cricketwidget;

import android.content.Context;
import android.content.SharedPreferences;

public final class Prefs {
    private static final String FILE = "cricket_widget_prefs";
    private static final String KEY_TEAM = "preferred_team";
    private static final String KEY_LIVE = "live_mode";

    private Prefs() {}

    public static String getPreferredTeam(Context context) {
        return prefs(context).getString(KEY_TEAM, "India");
    }

    public static void setPreferredTeam(Context context, String team) {
        prefs(context).edit().putString(KEY_TEAM, team == null ? "" : team.trim()).apply();
    }

    public static boolean isLiveMode(Context context) {
        return prefs(context).getBoolean(KEY_LIVE, false);
    }

    public static void setLiveMode(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_LIVE, enabled).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }
}
