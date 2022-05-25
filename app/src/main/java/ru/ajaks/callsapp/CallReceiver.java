package ru.ajaks.callsapp;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class CallReceiver extends BroadcastReceiver {

//    TelephonyManager mTelephonyManager;
//    PhoneStateListener mPhoneListener;
//    private int prev_state;

    @Override
    public void onReceive(Context context, Intent intent) {

        Toast.makeText(context,"RING",Toast.LENGTH_SHORT).show();
        if(!isMyServiceRunning(CallsCatcherService.class,context))
        {
            context.startService(new Intent(context,CallsCatcherService.class));
        }

    }

    private boolean isMyServiceRunning(Class<?> serviceClass,Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

}
