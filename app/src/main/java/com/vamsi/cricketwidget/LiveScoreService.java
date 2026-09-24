package com.vamsi.cricketwidget;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

public class LiveScoreService extends Service {
    public static final String ACTION_STOP = "com.vamsi.cricketwidget.STOP_LIVE";
    private static final String CHANNEL_ID = "cricket_live_score";
    private static final int NOTIFICATION_ID = 73;
    private volatile boolean running;
    private Thread worker;

    @Override
    public void onCreate() {
        super.onCreate();
        createChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopLiveMode();
            return START_NOT_STICKY;
        }

        Prefs.setLiveMode(this, true);
        startForeground(NOTIFICATION_ID, buildNotification("Starting live score…"));
        startLoopIfNeeded();
        return START_STICKY;
    }

    private synchronized void startLoopIfNeeded() {
        if (worker != null && worker.isAlive()) return;
        running = true;
        worker = new Thread(() -> {
            while (running) {
                MatchSnapshot snapshot = CricketWidgetProvider.refreshBlocking(getApplicationContext());
                updateNotification(snapshot);
                long sleepMs = snapshot.isLive() ? 60_000L : 300_000L;
                try {
                    Thread.sleep(sleepMs);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "cricket-live-score");
        worker.start();
    }

    private void updateNotification(MatchSnapshot s) {
        String line = s.flag1 + " " + s.team1 + " " + s.score1 + "  •  " + s.flag2 + " " + s.team2 + " " + s.score2;
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) manager.notify(NOTIFICATION_ID, buildNotification(line));
    }

    private Notification buildNotification(String content) {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent openPi = PendingIntent.getActivity(
                this, 50, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent stop = new Intent(this, LiveScoreService.class);
        stop.setAction(ACTION_STOP);
        PendingIntent stopPi = PendingIntent.getService(
                this, 51, stop, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);

        return builder
                .setSmallIcon(R.drawable.ic_cricket)
                .setContentTitle("Cricket widget live mode")
                .setContentText(content)
                .setContentIntent(openPi)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .addAction(new Notification.Action.Builder(null, "Stop", stopPi).build())
                .build();
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Cricket live score",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Keeps the 2×1 cricket widget updated during live mode");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private void stopLiveMode() {
        running = false;
        if (worker != null) worker.interrupt();
        Prefs.setLiveMode(this, false);
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        running = false;
        if (worker != null) worker.interrupt();
        Prefs.setLiveMode(this, false);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
