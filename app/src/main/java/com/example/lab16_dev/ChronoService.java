package com.example.lab16_dev;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ChronoService extends Service {

    private final IBLocal binder = new IBLocal();
    
    private int elapsedSeconds = 0;
    private boolean activeFlag = false;
    private ScheduledExecutorService scheduledExecutor;
    private static final int ALERT_ID = 2002;
    private NotificationManager notifManager;

    public class IBLocal extends Binder {
        public ChronoService retrieveService() {
            return ChronoService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        notifManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        buildNotifChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String actionCommand = (intent != null) ? intent.getAction() : null;

        if ("HALT".equals(actionCommand)) {
            stopSelf();
            return START_NOT_STICKY;
        }

        if (!activeFlag) {
            activeFlag = true;
            startForeground(ALERT_ID, generateNotification());
            launchTimer();
        }
        return START_STICKY;
    }

    private void launchTimer() {
        scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                elapsedSeconds++;
                refreshNotification();
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    private void buildNotifChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "chronometer_channel_id",
                    "Timer Monitoring Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            notifManager.createNotificationChannel(channel);
        }
    }

    private Notification generateNotification() {
        return new NotificationCompat.Builder(this, "chronometer_channel_id")
                .setContentTitle("Timer Active")
                .setContentText("Duration: " + formatDuration(elapsedSeconds))
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void refreshNotification() {
        notifManager.notify(ALERT_ID, generateNotification());
    }

    private String formatDuration(int seconds) {
        int minutesCount = seconds / 60;
        int remainingSecs = seconds % 60;
        return String.format("%02d:%02d", minutesCount, remainingSecs);
    }

    public int fetchCurrentTime() {
        return elapsedSeconds;
    }

    public boolean getOperationStatus() {
        return activeFlag;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        activeFlag = false;
        if (scheduledExecutor != null) {
            scheduledExecutor.shutdown();
        }
        stopForeground(true);
        super.onDestroy();
    }
}