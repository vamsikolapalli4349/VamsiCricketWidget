package com.vamsi.cricketwidget;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private EditText preferredTeam;
    private TextView preview1Status, preview1Line1, preview1Line2;
    private TextView preview2Team1, preview2Score1, preview2Team2, preview2Score2, preview2Status;
    private TextView stateText;
    private Switch liveToggle;
    private boolean settingToggle;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        preferredTeam = findViewById(R.id.preferredTeam);
        preview1Status = findViewById(R.id.preview1Status);
        preview1Line1 = findViewById(R.id.preview1Line1);
        preview1Line2 = findViewById(R.id.preview1Line2);
        preview2Team1 = findViewById(R.id.preview2Team1);
        preview2Score1 = findViewById(R.id.preview2Score1);
        preview2Team2 = findViewById(R.id.preview2Team2);
        preview2Score2 = findViewById(R.id.preview2Score2);
        preview2Status = findViewById(R.id.preview2Status);
        stateText = findViewById(R.id.stateText);
        liveToggle = findViewById(R.id.liveToggle);
        preferredTeam.setText(Prefs.getPreferredTeam(this));
        syncLiveToggle();
        findViewById(R.id.saveRefresh).setOnClickListener(v -> refreshPreview());
        liveToggle.setOnCheckedChangeListener((button, checked) -> { if (!settingToggle) setLiveMode(checked); });
    }

    @Override protected void onResume() { super.onResume(); syncLiveToggle(); }
    private void savePreference() { Prefs.setPreferredTeam(this, preferredTeam.getText().toString()); }

    private void setLiveMode(boolean enabled) {
        if (enabled) {
            requestNotificationPermissionIfNeeded();
            Intent start = new Intent(this, LiveScoreService.class);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(start); else startService(start);
            Prefs.setLiveMode(this, true);
            stateText.setText("Live updates on");
        } else {
            Intent stop = new Intent(this, LiveScoreService.class);
            stop.setAction(LiveScoreService.ACTION_STOP);
            startService(stop);
            Prefs.setLiveMode(this, false);
            stateText.setText("Live updates off");
        }
    }

    private void refreshPreview() {
        stateText.setText("Refreshing score…");
        executor.execute(() -> {
            MatchSnapshot snapshot;
            try {
                java.util.List<MatchSnapshot> matches = new CricketRepository().fetchMatches();
                MatchStore.save(this, matches, 0);
                snapshot = matches.get(0);
                WidgetRenderer.render(this, snapshot, 0, matches.size());
            } catch (Exception e) {
                snapshot = new MatchSnapshot("CRIC", "", "—", "—", "unknown", "Refresh failed", "", System.currentTimeMillis());
                WidgetRenderer.renderMessage(this, "Tap refresh to retry");
            }
            MatchSnapshot result = snapshot;
            runOnUiThread(() -> showSnapshot(result));
        });
    }

    private void showSnapshot(MatchSnapshot s) {
        String state = WidgetRenderer.compactState(s);
        preview1Status.setText(state); preview1Status.setTextColor(s.isLive() ? getColor(R.color.live) : getColor(R.color.text_secondary));
        preview1Line1.setText(WidgetRenderer.compactLineOne(s)); preview1Line2.setText(WidgetRenderer.compactLineTwo(s));
        preview2Team1.setText(s.team1); preview2Score1.setText(WidgetRenderer.primaryValue(s));
        preview2Team2.setText(s.team2); preview2Score2.setText(WidgetRenderer.secondaryValue(s));
        preview2Status.setText(state); preview2Status.setTextColor(s.isLive() ? getColor(R.color.live) : getColor(R.color.text_secondary));
        stateText.setText("Widget updated");
    }

    private void syncLiveToggle() { settingToggle = true; liveToggle.setChecked(Prefs.isLiveMode(this)); settingToggle = false; }
    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 90);
    }
    @Override protected void onDestroy() { executor.shutdownNow(); super.onDestroy(); }
}
