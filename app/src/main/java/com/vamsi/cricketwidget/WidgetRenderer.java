package com.vamsi.cricketwidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.widget.RemoteViews;

public final class WidgetRenderer {
    private static final int LIVE_RED = Color.rgb(255, 82, 94);
    private static final int MUTED = Color.rgb(168, 174, 184);

    private WidgetRenderer() { }

    public static void render(Context context, MatchSnapshot snapshot) {
        render(context, snapshot, 0, 1);
    }

    public static void render(Context context, MatchSnapshot snapshot, int index, int count) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        update1x1(context, manager, snapshot);
        update2x1(context, manager, snapshot, index, count);
    }

    public static void renderMessage(Context context, String message) {
        render(context, new MatchSnapshot("CRIC", "", "—", "—", "unknown",
                message, "", System.currentTimeMillis()));
    }

    private static void update1x1(Context context, AppWidgetManager manager, MatchSnapshot s) {
        ComponentName component = new ComponentName(context, CricketWidget1x1Provider.class);
        int[] ids = manager.getAppWidgetIds(component);
        if (ids.length == 0) return;
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_cricket_1x1);
        views.setTextViewText(R.id.status_1x1, compactState(s));
        views.setTextViewText(R.id.line1_1x1, compactLineOne(s));
        views.setTextViewText(R.id.line2_1x1, compactLineTwo(s));
        views.setTextColor(R.id.status_1x1, s.isLive() ? LIVE_RED : MUTED);
        views.setOnClickPendingIntent(R.id.widget_root_1x1, openIntent(context));
        manager.updateAppWidget(ids, views);
    }

    private static void update2x1(Context context, AppWidgetManager manager, MatchSnapshot s, int index, int count) {
        ComponentName component = new ComponentName(context, CricketWidget2x1Provider.class);
        int[] ids = manager.getAppWidgetIds(component);
        if (ids.length == 0) return;
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_cricket_2x1);
        views.setTextViewText(R.id.match_title_2x1, matchTitle(s));
        views.setTextViewText(R.id.match_count_2x1, count > 1 ? (index + 1) + "/" + count : "");
        views.setTextViewText(R.id.flag1_2x1, s.flag1);
        views.setTextViewText(R.id.flag2_2x1, s.flag2);
        views.setTextViewText(R.id.team1_2x1, s.team1);
        views.setTextViewText(R.id.score1_2x1, primaryValue(s));
        views.setTextViewText(R.id.team2_2x1, s.team2);
        views.setTextViewText(R.id.score2_2x1, secondaryValue(s));
        views.setTextViewText(R.id.status_2x1, compactState(s));
        views.setTextColor(R.id.status_2x1, s.isLive() ? LIVE_RED : MUTED);
        views.setOnClickPendingIntent(R.id.widget_root_2x1, openIntent(context));
        views.setOnClickPendingIntent(R.id.refresh_2x1, refreshIntent(context));
        views.setOnClickPendingIntent(R.id.previous_2x1, cycleIntent(context, CricketWidgetProvider.ACTION_PREVIOUS, 22));
        views.setOnClickPendingIntent(R.id.next_2x1, cycleIntent(context, CricketWidgetProvider.ACTION_NEXT, 23));
        manager.updateAppWidget(ids, views);
    }

    private static PendingIntent openIntent(Context context) {
        Intent open = new Intent(context, MainActivity.class);
        return PendingIntent.getActivity(context, 20, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static PendingIntent refreshIntent(Context context) {
        Intent refresh = new Intent(context, CricketWidget2x1Provider.class);
        refresh.setAction(CricketWidgetProvider.ACTION_REFRESH);
        return PendingIntent.getBroadcast(context, 21, refresh,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static PendingIntent cycleIntent(Context context, String action, int requestCode) {
        Intent intent = new Intent(context, CricketWidget2x1Provider.class);
        intent.setAction(action);
        return PendingIntent.getBroadcast(context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    public static String compactState(MatchSnapshot s) {
        if (s.isLive()) return "● LIVE";
        if (s.isUpcoming()) return "UPCOMING";
        if ("post".equalsIgnoreCase(s.state)) return "FINAL";
        return "SCORE";
    }

    public static String compactLineOne(MatchSnapshot s) {
        if (s.isUpcoming()) return s.flag1 + " " + s.team1 + " vs " + s.team2;
        return s.flag1 + " " + s.team1 + "  " + trimScore(s.score1, 10);
    }

    public static String compactLineTwo(MatchSnapshot s) {
        if (s.isUpcoming()) return upcomingTime(s);
        return s.flag2 + " " + s.team2 + "  " + trimScore(s.score2, 10);
    }

    public static String matchTitle(MatchSnapshot s) {
        if (s.isLive()) return "LIVE  •  " + trimScore(s.competition, 22);
        if (s.isUpcoming()) return "UPCOMING  •  " + trimScore(s.competition, 18);
        if ("post".equalsIgnoreCase(s.state)) return "FINAL  •  " + trimScore(s.competition, 20);
        return trimScore(s.competition, 26);
    }

    public static String primaryValue(MatchSnapshot s) { return s.isUpcoming() ? "vs" : trimScore(s.score1, 17); }
    public static String secondaryValue(MatchSnapshot s) { return s.isUpcoming() ? upcomingTime(s) : trimScore(s.score2, 17); }

    public static String upcomingTime(MatchSnapshot s) {
        String candidate = s.detail;
        if (candidate == null || candidate.length() < 3 || candidate.equals("—")) candidate = s.summary;
        if (candidate == null || candidate.isEmpty()) return "Upcoming";
        return trimScore(candidate.replace("Starts ", ""), 18);
    }

    private static String trimScore(String value, int max) {
        if (value == null || value.isEmpty()) return "—";
        return value.length() <= max ? value : value.substring(0, max - 1) + "…";
    }
}
