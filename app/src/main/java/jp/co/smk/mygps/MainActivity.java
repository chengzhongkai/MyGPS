package jp.co.smk.mygps;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import android.content.Context;
import android.content.ContentResolver;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Toast;
import android.database.Cursor;
import android.widget.ListView;
import android.net.Uri;
import android.widget.ArrayAdapter;
import android.location.LocationListener;

import java.util.ArrayList;

import android.telephony.SmsManager;
import android.widget.EditText;

import java.util.Timer;
import java.util.TimerTask;

import android.os.Handler;
import android.provider.Settings;
import android.content.Intent;

public class MainActivity extends AppCompatActivity implements OnClickListener, OnItemClickListener, LocationListener {

    private Timer mTimer;
    private static MainActivity inst;
    private ArrayList<String> smsList = new ArrayList<String>();
    private LocationManager mLocationManager;

    public static MainActivity instance() {
        return inst;
    }

    @Override
    public void onStart() {
        super.onStart();
        inst = this;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getGps();
                // Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                //         .setAction("Action", null).show();
            }
        });

        this.findViewById(R.id.UpdateList).setOnClickListener(this);
        // GPS
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean gpsFlg = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        Log.d("GPS Enabled", gpsFlg ? "OK" : "NG");
        turnGPSOn();

        Bundle bundle = getIntent().getExtras();
        if(bundle!=null && bundle.getString("address")!= null)
        {
            sentGps(bundle.getString("address"));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
        try {
            String[] splitted = smsList.get(pos).split("\n");
            String sender = splitted[0];
            String encryptedData = "";
            for (int i = 1; i < splitted.length; ++i) {
                encryptedData += splitted[i];
            }
            String data = sender + "\n" + StringCryptor.decrypt(new String(SmsReceiver.PASSWORD), encryptedData);
            Toast.makeText(this, data, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onClick(View v) {
        ContentResolver contentResolver = getContentResolver();
        Cursor cursor = contentResolver.query(Uri.parse("content://sms/inbox"), null, null, null, null);

        int indexBody = cursor.getColumnIndex(SmsReceiver.BODY);
        int indexAddr = cursor.getColumnIndex(SmsReceiver.ADDRESS);

        if (indexBody < 0 || !cursor.moveToFirst()) return;

        smsList.clear();

        do {
            String str = "Sender: " + cursor.getString(indexAddr) + "\n" + cursor.getString(indexBody);
            smsList.add(str);
        }
        while (cursor.moveToNext());


        ListView smsListView = (ListView) findViewById(R.id.SMSList);
        smsListView.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, smsList));
        smsListView.setOnItemClickListener(this);
    }

    public void getGps() {
        mLocationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, //LocationManager.NETWORK_PROVIDER,
                3000, // 通知のための最小時間間隔（ミリ秒）
                10, // 通知のための最小距離間隔（メートル）
                this
        );
        timeOutCount();
    }

    public void sentGps(String address) {
        ((EditText) findViewById(R.id.address)).setText(address);
        getGps();
    }

    protected void sendSMS(String toPhoneNumber, String smsMessage) {

        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(toPhoneNumber, null, smsMessage, null, null);
            Toast.makeText(getApplicationContext(), "SMS sent.",
                    Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(),
                    "Sending SMS failed.",
                    Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    /**
     * GPS取得のTimeOutを処理するスレッド
     */
    public void timeOutCount() {
        final Handler mHandler = new Handler();
        mTimer = new Timer(true);
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                mHandler.post(new Runnable() {


                    public void run() {

                        //onLocationChangedをStopする(GPS取得中断)
                        mLocationManager.removeUpdates(MainActivity.this);
                        sendSMS(((EditText) findViewById(R.id.address)).getText().toString(), "gps false!!!");

                        //以下にTimeOut時に行いたい処理を書く

                        //スレッドを止める
                        mTimer.cancel();
                    }


                });
            }


        }, 30000, 10000);
    }


    ////----------------------------------
    @Override
    public void onLocationChanged(Location location) {
        String msg = "Lat=" + location.getLatitude()
                + "\nLng=" + location.getLongitude();
        Log.d("GPS", msg);
        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
        sendSMS(((EditText) findViewById(R.id.address)).getText().toString(), "http://maps.google.com/maps?q=loc" + location.getLatitude() + "," + location.getLongitude());
        mLocationManager.removeUpdates(this);
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }
    ////-----------------------------------

    private void turnGPSOn() {
        //Intent intent = new Intent("android.location.GPS_ENABLED_CHANGE");
       // intent.putExtra("enabled", true);
       // sendBroadcast(intent);

        String provider = Settings.Secure.getString(getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
        if(!provider.contains("gps")){ //if gps is disabled
            final Intent poke = new Intent();
            poke.setClassName("com.android.settings", "com.android.settings.widget.SettingsAppWidgetProvider");
            poke.addCategory(Intent.CATEGORY_ALTERNATIVE);
            poke.setData(Uri.parse("3"));
            sendBroadcast(poke);


        }
    }
}
