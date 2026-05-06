package com.example.lab16_dev;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private TextView timeDisplay;
    private Button launchButton, terminateButton;
    private ChronoService timerService;
    private boolean connectedFlag = false;
    private Handler uiHandler = new Handler();
    
    private Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            if (connectedFlag && timerService != null && timerService.getOperationStatus()) {
                int currentSeconds = timerService.fetchCurrentTime();
                timeDisplay.setText(formatDisplay(currentSeconds));
                uiHandler.postDelayed(this, 1000);
            }
        }
    };

    private final ServiceConnection serviceLink = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ChronoService.IBLocal localBinder = (ChronoService.IBLocal) service;
            timerService = localBinder.retrieveService();
            connectedFlag = true;
            uiHandler.post(refreshRunnable);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            connectedFlag = false;
            timerService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        timeDisplay = findViewById(R.id.timeValue);
        launchButton = findViewById(R.id.startControl);
        terminateButton = findViewById(R.id.stopControl);

        launchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initiateTimer();
            }
        });

        terminateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                haltTimer();
            }
        });
    }

    private void initiateTimer() {
        Intent transferIntent = new Intent(this, ChronoService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(transferIntent);
        } else {
            startService(transferIntent);
        }
        bindService(transferIntent, serviceLink, Context.BIND_AUTO_CREATE);
    }

    private void haltTimer() {
        Intent haltIntent = new Intent(this, ChronoService.class);
        haltIntent.setAction("HALT");
        stopService(haltIntent);

        if (connectedFlag) {
            unbindService(serviceLink);
            connectedFlag = false;
            timerService = null;
        }
        timeDisplay.setText("00:00");
        uiHandler.removeCallbacks(refreshRunnable);
    }

    private String formatDisplay(int secondsValue) {
        int minsPortion = secondsValue / 60;
        int secsPortion = secondsValue % 60;
        return String.format("%02d:%02d", minsPortion, secsPortion);
    }

    @Override
    protected void onDestroy() {
        if (connectedFlag) {
            unbindService(serviceLink);
        }
        uiHandler.removeCallbacks(refreshRunnable);
        super.onDestroy();
    }
}