package ru.ajaks.callsapp;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.CallLog;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.Xml;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import org.xmlpull.v1.XmlSerializer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;


public class CallsCatcherService extends Service {
    private NotificationManager manager;
    TelephonyManager mTelephonyManager;
    PhoneStateListener mPhoneListener;
    private int prev_state;
    OpenHelper openHelper;
    SQLiteDatabase db;
    Cursor cur;
    ContentValues newValues;

    private PostInterface postInterface;
    //private static OkHttpClient.Builder httpClientBuilder = null;
    Handler handler = new Handler(Looper.getMainLooper());


    public void onCreate() {
        super.onCreate();
        showNotification(this,"Работает","Идет сбор данных о звонках",new Intent());

//        httpClientBuilder = new OkHttpClient.Builder().readTimeout(5, TimeUnit.SECONDS);
//        initHttpLogging();
//        initSSL(getApplicationContext());

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(loadText("address")+"/")
                //.client(httpClientBuilder.build())
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        postInterface = retrofit.create(PostInterface.class);

        openHelper = new OpenHelper(getApplicationContext(),"CALLS",null,1);
        db = openHelper.getWritableDatabase();

    }

//    private static void initHttpLogging() {
////        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
////        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
////        if (BuildConfig.DEBUG) httpClientBuilder.addInterceptor(logging);
////    }
////
////    private static void initSSL(Context context) {
////
////        SSLContext sslContext = null;
////        try {
////            sslContext = createCertificate();
////        } catch (CertificateException | IOException | KeyStoreException | KeyManagementException | NoSuchAlgorithmException e) {
////            e.printStackTrace();
////        }
////
////        if(sslContext!=null){
////            httpClientBuilder.sslSocketFactory(sslContext.getSocketFactory(), systemDefaultTrustManager());
////        }
////
////    }
////
////    private static SSLContext createCertificate() throws CertificateException, IOException, KeyStoreException, KeyManagementException, NoSuchAlgorithmException {
////
////        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
////            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
////                return new java.security.cert.X509Certificate[] {};
////            }
////
////            public void checkClientTrusted(X509Certificate[] chain,
////                                           String authType) throws CertificateException {
////            }
////
////            public void checkServerTrusted(X509Certificate[] chain,
////                                           String authType) throws CertificateException {
////            }
////        } };
////
////        // Install the all-trusting trust manager
////        SSLContext sc = null;
////        try {
////            sc = SSLContext.getInstance("TLS");
////            sc.init(null, trustAllCerts, new java.security.SecureRandom());
////            HttpsURLConnection
////                    .setDefaultSSLSocketFactory(sc.getSocketFactory());
////        } catch (Exception e) {
////            e.printStackTrace();
////        }
////
////        return sc;
////
////    }
////
////    private static X509TrustManager systemDefaultTrustManager() {
////
////        try {
////            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
////            trustManagerFactory.init((KeyStore) null);
////            TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
////            if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
////                throw new IllegalStateException("Unexpected default trust managers:" + Arrays.toString(trustManagers));
////            }
////            return (X509TrustManager) trustManagers[0];
////        } catch (GeneralSecurityException e) {
////            throw new AssertionError(); // The system has no TLS. Just give up.
////        }
////
////    }

    private void createPost(String value) throws IOException {
        Call<String> call = postInterface.createPost(loadText("address"),value);
        Response<String> res = call.execute();
        Log.d("RETROFIT", res.body());
//        call.enqueue(new Callback<String>() {
//            @Override
//            public void onResponse(Call<String> call, Response<String> response) {
//                String answer = response.body();
//                Log.d("RETROFIT",answer);
//            }
//            @Override
//            public void onFailure(Call<String> call, Throwable t) {
//
//            }
//        });
    }

    public int onStartCommand(Intent intent, int flags, int startId) {


        //Send Foreground Notification


        cur = db.query("CALLS", new String[] {"NUMBER", "TYPE", "DATE", "DURATION"}, null, null, null, null, null);


        mPhoneListener = new PhoneStateListener() {
            public void onCallStateChanged(int state, String incomingNumber) {
                super.onCallStateChanged(state, incomingNumber);

                switch(state){
                    case TelephonyManager.CALL_STATE_RINGING:
                        Log.d("CALL", "CALL_STATE_RINGING");
                        prev_state=state;
                        break;

                    case TelephonyManager.CALL_STATE_OFFHOOK:
                        Log.d("CALL", "CALL_STATE_OFFHOOK");
                        prev_state=state;
                        break;

                    case TelephonyManager.CALL_STATE_IDLE:

                        Log.d("CALL", "CALL_STATE_IDLE==>");

                        if((prev_state == TelephonyManager.CALL_STATE_OFFHOOK)){
                            prev_state=state;
                            //Answered Call which is ended
                            getCallDetails(getApplicationContext());
                        }
                        if((prev_state == TelephonyManager.CALL_STATE_RINGING)){
                            prev_state=state;
                            //Rejected or Missed call
                            getCallDetails(getApplicationContext());
                        }
                        break;
                }

            }



        };

        mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        mTelephonyManager.listen(mPhoneListener, PhoneStateListener.LISTEN_CALL_STATE);


        TimerTask tasknew = new TimerTask() {
            @Override
            public void run() {

                if(cur.getCount() > 0)
                {
                    CallInfo info = new CallInfo();
                    info.number = new String[cur.getCount()];
                    info.type = new String[cur.getCount()];
                    info.date = new String[cur.getCount()];
                    info.duration = new String[cur.getCount()];

                    for (int i = 0; i < cur.getCount(); i++) {
                        cur.moveToNext();

                        info.number[i] = cur.getString(0);
                        info.type[i] = cur.getString(1);
                        info.date[i] = cur.getString(2);
                        info.duration[i] = cur.getString(3);

                    }

                    StringWriter sw = new StringWriter();
                    XmlSerializer serializer = Xml.newSerializer();
                    try {
                        serializer.setOutput(sw);
                        serializer.startDocument(null, Boolean.valueOf(true));
                        serializer.startTag(null, "allcallsinfo");

                        for (int i = 0; i < cur.getCount(); i++) {
                            serializer.startTag(null,"callinfo");
                            serializer.startTag(null,"number");
                            serializer.text(info.number[i]);
                            serializer.endTag(null,"number");
                            serializer.startTag(null,"type");
                            serializer.text(info.type[i]);
                            serializer.endTag(null,"type");
                            serializer.startTag(null,"date");
                            serializer.text(info.date[i]);
                            serializer.endTag(null,"date");
                            serializer.startTag(null,"duration");
                            serializer.text(info.duration[i]);
                            serializer.endTag(null,"duration");
                            serializer.endTag(null,"callinfo");
                        }

                        serializer.endTag(null, "allcallsinfo");
                        serializer.endDocument();
                        serializer.flush();
                        String xmlstring = sw.toString();
                        Log.d("XML",xmlstring);
                        sw.close();

                        createPost(xmlstring);

                    } catch (final Exception e) {
                        if(e.getClass() == SSLHandshakeException.class)
                        {
                            Log.e("SSLERROR",e.getMessage());
                            e.printStackTrace();
                            String err = "";
                            for (int i = 0; i < e.getStackTrace().length; i++) {
                                err += e.getStackTrace()[i]+"\n";
                            }

                            appendLog(e.getMessage() + "\n" + err + "\n");
                            handler.post(new Runnable() {

                                @Override
                                public void run() {
                                    Toast.makeText(getApplicationContext(), e.getMessage()+"\n"+ "скиньте error.log разработчикам", Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    }


                }

                cur = db.query("CALLS", new String[] {"NUMBER", "TYPE", "DATE", "DURATION"}, null, null, null, null, null);
                db.execSQL("DELETE FROM CALLS");
            }
        };

        Timer timer = new Timer();
        timer.schedule(tasknew, (int)(Double.parseDouble(loadText("time")) * 60 * 60 * 1000), (int)(Double.parseDouble(loadText("time")) * 60 * 60 * 1000)); //43200000

        //return Service.START_STICKY;
        return START_STICKY;
    }

    String loadText(String name) {
        SharedPreferences sPref;
        sPref = getApplicationContext().getSharedPreferences(name, MainActivity.MODE_PRIVATE);
        String savedText = sPref.getString(name, "");
        return savedText;
    }

    public void appendLog(String text)
    {
        File logFile = new File("/storage/emulated/0/error.log");
        if (!logFile.exists())
        {
            try
            {
                logFile.createNewFile();
            }
            catch (IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        try
        {
            //BufferedWriter for performance, true to set append to file flag
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(text);
            buf.newLine();
            buf.close();
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


        private void getCallDetails(final Context context) {

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) { }

        StringBuffer stringBuffer = new StringBuffer();

        Cursor cursor = context.getContentResolver().query(CallLog.Calls.CONTENT_URI,
                null, null, null, CallLog.Calls.DATE + " DESC");
        int number = cursor.getColumnIndex(CallLog.Calls.NUMBER);
        int type = cursor.getColumnIndex(CallLog.Calls.TYPE);
        int date = cursor.getColumnIndex(CallLog.Calls.DATE);
        int duration = cursor.getColumnIndex(CallLog.Calls.DURATION);

        cursor.moveToFirst();

        String phNumber = cursor.getString(number);
        String callType = cursor.getString(type);
        String callDate = cursor.getString(date);
        Date callDayTime = new Date(Long.valueOf(callDate));
        String callDuration = cursor.getString(duration);
        String dir = "MISSED";
        int dircode = Integer.parseInt(callType);

            switch (dircode) {
                case CallLog.Calls.OUTGOING_TYPE:
                    dir = "OUTGOING";
                    break;
                case CallLog.Calls.INCOMING_TYPE:
                    dir = "INCOMING";
                    break;

                case CallLog.Calls.MISSED_TYPE:
                    dir = "MISSED";
                    break;
            }

        newValues = new ContentValues();
        newValues.put("NUMBER",phNumber);
        newValues.put("TYPE",dir);
        newValues.put("DATE",callDayTime.toString());
        newValues.put("DURATION",callDuration);
        db.insert("CALLS", null, newValues);



        stringBuffer.append("\nPhone Number:--- " + phNumber + " \nCall Type:--- "
                + dir + " \nCall Date:--- " + callDayTime
                + " \nCall duration in sec :--- " + callDuration);
        stringBuffer.append("\n----------------------------------");


        cursor.close();
    }


    //Send custom notification
    public void showNotification(Context context, String title, String body, Intent intent) {
        manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        int notificationId = 1;
        String channelId = "channel-01";
        String channelName = "Channel Name";
        int importance = NotificationManager.IMPORTANCE_HIGH;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel(
                    channelId, channelName, importance);
            manager.createNotificationChannel(mChannel);
        }

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, channelId)
                .setOngoing(true)
                .setSmallIcon(R.drawable.logo)
                .setContentTitle(title)
                .setContentText(body);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addNextIntent(intent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(
                0,
                PendingIntent.FLAG_UPDATE_CURRENT
        );
        mBuilder.setContentIntent(resultPendingIntent);

        manager.notify(notificationId, mBuilder.build());
        startForeground(notificationId,mBuilder.build());
    }

//    @RequiresApi(api = Build.VERSION_CODES.O)
//    private void startMyOwnForeground(){
//
//        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
//        Notification notification = notificationBuilder.setOngoing(true)
//                .setContentTitle("App is running in background")
//                .setPriority(NotificationManager.IMPORTANCE_MIN)
//                .setCategory(Notification.CATEGORY_SERVICE)
//                .build();
//        startForeground(2, notification);
//    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mTelephonyManager.listen(mPhoneListener, PhoneStateListener.LISTEN_NONE);

        //Removing any notifications
        manager.cancelAll();

        cur.close();
        db.close();

        //Disabling service
        stopSelf();
    }
}
