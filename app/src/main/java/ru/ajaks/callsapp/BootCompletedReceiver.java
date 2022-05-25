package ru.ajaks.callsapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class BootCompletedReceiver extends BroadcastReceiver {
    public BootCompletedReceiver() {
    }
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(new Intent(context, CallsCatcherService.class));

            } else {
                context.startService(new Intent(context, CallsCatcherService.class));
            }
        }

    }
}
