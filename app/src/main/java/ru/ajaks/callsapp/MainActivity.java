package ru.ajaks.callsapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.CallLog;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    //TextView callview;
    //TextView loadview;
    //Button refreshbtn;

    Intent intentService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Context con = this;

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_PHONE_STATE,Manifest.permission.READ_CALL_LOG,Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
        }

        checkOptimization();

//        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG)
////                != PackageManager.PERMISSION_GRANTED){
////            ActivityCompat.requestPermissions(this,
////                    new String[]{Manifest.permission.READ_CALL_LOG},2);
////        }


//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
//            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_CALL_LOG)) {
//                ActivityCompat.requestPermissions( this, new String[]{Manifest.permission.READ_CALL_LOG}, 100);
//            }
//            else {
//                ActivityCompat.requestPermissions( this, new String[]{Manifest.permission.READ_CALL_LOG}, 100);
//            }
//        }

        if(loadText("address").isEmpty() && loadText("time").isEmpty())
        {
            setContentView(R.layout.startup_activity);
            Button addressbtn = findViewById(R.id.addressbtn);
            final EditText addressfield = findViewById(R.id.addressfield);
            final EditText timefield = findViewById(R.id.timefield);
            addressbtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(!addressfield.getText().toString().isEmpty() && !timefield.getText().toString().isEmpty()) {
                        saveText(addressfield.getText().toString(),"address");
                        saveText(timefield.getText().toString(),"time");
                        Toast.makeText(getApplicationContext(),"ГОТОВО",Toast.LENGTH_SHORT).show();

                        setContentView(R.layout.activity_main);
                        //Toast.makeText(getApplicationContext(),loadText().split("/")[0],Toast.LENGTH_SHORT).show();
                        if(!isMyServiceRunning(CallsCatcherService.class)) {
                            intentService = new Intent(getApplicationContext(), CallsCatcherService.class);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                startForegroundService(intentService);

                            } else {
                                startService(intentService);
                            }
                        }
                    }
                    else
                    {
                        Toast.makeText(getApplicationContext(),"НЕТ ДАННЫХ",Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
        else
        {
            setContentView(R.layout.activity_main);
            //Toast.makeText(getApplicationContext(),loadText().split("/")[0],Toast.LENGTH_SHORT).show();
            if(!isMyServiceRunning(CallsCatcherService.class)) {
                intentService = new Intent(getApplicationContext(), CallsCatcherService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intentService);

                } else {
                    startService(intentService);
                }
            }
        }



//        final Context con = this;
//
//        if (ContextCompat.checkSelfPermission(con, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
//            if (ActivityCompat.shouldShowRequestPermissionRationale((Activity) con, Manifest.permission.WRITE_CALENDAR)) {
//                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(con);
//                alertBuilder.setCancelable(true);
//                alertBuilder.setMessage("Write calendar permission is necessary to write event!!!");
//                alertBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
//                    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
//                    public void onClick(DialogInterface dialog, int which) {
//                        ActivityCompat.requestPermissions((Activity) con, new String[]{Manifest.permission.READ_CALL_LOG, Manifest.permission.WRITE_CALL_LOG}, 100);
//                    }
//                });
//            } else {
//                ActivityCompat.requestPermissions((Activity) con, new String[]{Manifest.permission.READ_CALL_LOG}, 100);
//            }
//        }
//
//        callview = findViewById(R.id.callview);
//        //loadview = findViewById(R.id.loadview);
//        refreshbtn = findViewById(R.id.refreshbtn);
//        final Handler handler = new Handler();
//
//        refreshbtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                callview.setGravity(Gravity.CENTER);
//                callview.setTextSize(40);
//                callview.setText("LOADING...");
//                handler.post(new Thread(){
//                    @Override
//                    public void run() {
//                        callview.setGravity(Gravity.NO_GRAVITY);
//                        callview.setTextSize(14);
//                        callview.setText(getCallDetails(con));
//                    }
//                });
//            }
//        });

    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @SuppressLint({"NewApi", "BatteryLife"})
    private void checkOptimization() {
        Intent intent = new Intent();
        String packageName = getPackageName();
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm.isIgnoringBatteryOptimizations(packageName))
            intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
        else {
            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + packageName));
        }
        startActivity(intent);
    }


//    private String getCallDetails(final Context context) {
//
//        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
//            if (ActivityCompat.shouldShowRequestPermissionRationale((Activity) context, Manifest.permission.WRITE_CALENDAR)) {
//                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(context);
//                alertBuilder.setCancelable(true);
//                alertBuilder.setMessage("Write calendar permission is necessary to write event!!!");
//                alertBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
//                    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
//                    public void onClick(DialogInterface dialog, int which) {
//                        ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.READ_CALL_LOG, Manifest.permission.WRITE_CALL_LOG}, 100);
//                    }
//                });
//            } else {
//                ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.READ_CALL_LOG}, 100);
//            }
//        }
//
//        StringBuffer stringBuffer = new StringBuffer();
//
//        Cursor cursor = context.getContentResolver().query(CallLog.Calls.CONTENT_URI,
//                null, null, null, CallLog.Calls.DATE + " DESC");
//        int number = cursor.getColumnIndex(CallLog.Calls.NUMBER);
//        int type = cursor.getColumnIndex(CallLog.Calls.TYPE);
//        int date = cursor.getColumnIndex(CallLog.Calls.DATE);
//        int duration = cursor.getColumnIndex(CallLog.Calls.DURATION);
//        while (cursor.moveToNext()) {
//            String phNumber = cursor.getString(number);
//            String callType = cursor.getString(type);
//            String callDate = cursor.getString(date);
//            Date callDayTime = new Date(Long.valueOf(callDate));
//            String callDuration = cursor.getString(duration);
//            String dir = null;
//            int dircode = Integer.parseInt(callType);
//            switch (dircode) {
//                case CallLog.Calls.OUTGOING_TYPE:
//                    dir = "OUTGOING";
//                    break;
//                case CallLog.Calls.INCOMING_TYPE:
//                    dir = "INCOMING";
//                    break;
//
//                case CallLog.Calls.MISSED_TYPE:
//                    dir = "MISSED";
//                    break;
//            }
//            if(dir == "MISSED") {
//                stringBuffer.append("\nPhone Number:--- " + phNumber + " \nCall Type:--- "
//                        + dir + " \nCall Date:--- " + callDayTime
//                        + " \nCall duration in sec :--- " + callDuration);
//                stringBuffer.append("\n----------------------------------");
//            }
//        }
//        cursor.close();
//        return stringBuffer.toString();
//    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    void saveText(String address,String name) {
        SharedPreferences sPref;
        sPref = getApplicationContext().getSharedPreferences(name,Context.MODE_PRIVATE);
        SharedPreferences.Editor ed = sPref.edit();
        ed.putString(name, address);
        ed.commit();

    }

    String loadText(String name) {
        SharedPreferences sPref;
        sPref = getApplicationContext().getSharedPreferences(name, MainActivity.MODE_PRIVATE);
        String savedText = sPref.getString(name, "");
        return savedText;
    }

}
