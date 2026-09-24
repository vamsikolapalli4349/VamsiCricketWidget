package com.vamsi.cricketwidget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.List;

/** Shared receiver behavior for both widget footprints. */
public class CricketWidgetProvider extends AppWidgetProvider {
    public static final String ACTION_REFRESH = "com.vamsi.cricketwidget.REFRESH";
    public static final String ACTION_PREVIOUS = "com.vamsi.cricketwidget.PREVIOUS_MATCH";
    public static final String ACTION_NEXT = "com.vamsi.cricketwidget.NEXT_MATCH";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        super.onUpdate(context, manager, ids);
        refreshAsync(context);
    }

    @Override public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION_REFRESH.equals(intent.getAction())) {
            final PendingResult pending = goAsync();
            EXECUTOR.execute(() -> { try { refreshBlocking(context.getApplicationContext()); } finally { pending.finish(); } });
        } else if (ACTION_PREVIOUS.equals(intent.getAction()) || ACTION_NEXT.equals(intent.getAction())) {
            List<MatchSnapshot> matches = MatchStore.load(context);
            if (matches.isEmpty()) { refreshAsync(context); return; }
            int change = ACTION_NEXT.equals(intent.getAction()) ? 1 : -1;
            int index = Math.floorMod(MatchStore.index(context, matches.size()) + change, matches.size());
            MatchStore.save(context, matches, index);
            WidgetRenderer.render(context, matches.get(index), index, matches.size());
        }
    }

    public static void refreshAsync(Context context) {
        Context app = context.getApplicationContext();
        EXECUTOR.execute(() -> refreshBlocking(app));
    }

    public static MatchSnapshot refreshBlocking(Context context) {
        try {
            WidgetRenderer.renderMessage(context, "Refreshing…");
            List<MatchSnapshot> matches = new CricketRepository().fetchMatches();
            List<MatchSnapshot> previous = MatchStore.load(context);
            int oldIndex = MatchStore.index(context, previous.size());
            String selectedId = previous.isEmpty() ? "" : previous.get(oldIndex).id;
            int index = 0;
            for (int i = 0; i < matches.size(); i++) if (matches.get(i).id.equals(selectedId)) { index = i; break; }
            MatchStore.save(context, matches, index);
            MatchSnapshot snapshot = matches.get(index);
            WidgetRenderer.render(context, snapshot, index, matches.size());
            return snapshot;
        } catch (Exception e) {
            WidgetRenderer.renderMessage(context, "Tap refresh to retry");
            return new MatchSnapshot("CRIC", "", "—", "—", "unknown",
                    e.getMessage() == null ? "Refresh failed" : e.getMessage(), "", System.currentTimeMillis());
        }
    }
}
