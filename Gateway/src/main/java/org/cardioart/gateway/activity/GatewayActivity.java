package org.cardioart.gateway.activity;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Telephony;
import android.support.v7.app.ActionBarActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.rbnb.sapi.SAPIException;

import org.cardioart.gateway.R;
import org.cardioart.gateway.api.constant.MyEvent;
import org.cardioart.gateway.api.helper.bluetooth.BluetoothConnection;
import org.cardioart.gateway.api.helper.bluetooth.BluetoothScanHelper;
import org.cardioart.gateway.api.thread.InternetThread;
import org.cardioart.gateway.impl.BluetoothCommClient;
import org.cardioart.gateway.impl.GPSInternetThreadImpl;
import org.cardioart.gateway.impl.SimpleInternetThreadImpl;

public class GatewayActivity extends ActionBarActivity implements Handler.Callback {

    private Handler mainHandler;
    private BluetoothConnection commHelper;
    private InternetThread internetThread;
    private InternetThread gpsThread;
    private EditText debugEditText;
    private TextView btRxStatus, wifiTxStatus;
    private TextView textViewTxSpeed, textViewRxSpeed, textViewConnectivityStatus,
            textViewLat, textViewLong;
    private Button buttonBt, buttonWifi;

    public static final String TAG = "BtG";
    public static final int REQUEST_ENABLE_BT = 2718;
    public static final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    private boolean isDebug = true;
    private boolean isBtEnable = false;
    private boolean isWifiEnable = false;
    private boolean isBtRxActive = false;
    private boolean isWifiClientActive = false;
    private boolean isWifiTxActive = false;
    private boolean isInternetActive = false;

    private Runnable mTimerMonitorSpeed;

    private String deviceName;
    private String deviceAddress;
    private String serverAddress;
    private String patientId;
    private String phoneName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gateway);

        // get intent parameters
        deviceName = getIntent().getStringExtra("device_name");
        deviceAddress = getIntent().getStringExtra("device_address");
        phoneName = getIntent().getStringExtra("phone_name");
        patientId = getIntent().getStringExtra("patient_id");
        serverAddress = getIntent().getStringExtra("server_address");

        // initial support helper
        mainHandler = new Handler(this);
        commHelper = new BluetoothCommClient(mainHandler);

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BT);
        }

        // cache EditText and TextView for later usage
        debugEditText = (EditText) findViewById(R.id.editText);
        btRxStatus = (TextView) findViewById(R.id.btRxStatus);
        wifiTxStatus = (TextView) findViewById(R.id.wifiTxStatus);
        textViewRxSpeed = (TextView) findViewById(R.id.textViewRxSpeed);
        textViewTxSpeed = (TextView) findViewById(R.id.textViewTxSpeed);
        textViewConnectivityStatus = (TextView) findViewById(R.id.textViewConnectivity);
        textViewLat = (TextView) findViewById(R.id.textViewLat);
        textViewLong = (TextView) findViewById(R.id.textViewLong);
        buttonBt = (Button) findViewById(R.id.buttonBt);
        buttonWifi = (Button) findViewById(R.id.buttonWifi);

        // set default color to text status
        btRxStatus.setTextColor(Color.DKGRAY);
        wifiTxStatus.setTextColor(Color.DKGRAY);

        // check the internet connectivity and change color of status
        if (isConnected()) {
            textViewConnectivityStatus.setText("Online");
            textViewConnectivityStatus.setBackgroundColor(Color.parseColor("#FF77E25F"));
            isInternetActive = true;
        } else {
            textViewConnectivityStatus.setText("Offline");
            textViewConnectivityStatus.setBackgroundColor(Color.parseColor("#FFD9542F"));
            isInternetActive = false;
        }
    }
    @Override
    public boolean handleMessage(Message message) {
        switch (message.what) {
            case MyEvent.STATE_BT_RX_UP:
                if (!isBtRxActive) {
                    isBtRxActive = true;
                    btRxStatus.setTextColor(Color.GREEN);
                    if (internetThread != null) {
                        double time = System.currentTimeMillis() / 1000.d;
                        internetThread.setSecTimestamp(time);
                    }
                    buttonBt.setText("Stop BT");
                }
                break;
            case MyEvent.STATE_BT_RX_DOWN:
                if (isBtRxActive) {
                    isBtRxActive = false;
                    btRxStatus.setTextColor(Color.DKGRAY);
                    buttonBt.setText("Start BT");
                }
                break;
            case MyEvent.STATE_INTERNET_THREAD_START:
                if (!isWifiTxActive) {
                    isWifiTxActive = true;
                    wifiTxStatus.setTextColor(Color.GREEN);
                    buttonWifi.setText("Stop Wifi");
                }
                break;
            case MyEvent.STATE_INTERNET_THREAD_STOP:
                if (isWifiTxActive) {
                    isWifiTxActive = false;
                    wifiTxStatus.setTextColor(Color.DKGRAY);
                    buttonWifi.setText("Start Wifi");
                }
                if (isWifiEnable) {
                    isWifiEnable = false;
                }
                break;
            case MyEvent.STATE_INTERNET_THREAD_MSG:
                if (isWifiTxActive) {
                    internetThread.sendMessage((byte[]) message.obj);
                }
                break;
            case MyEvent.STATE_GPS_MSG:
                Location location = (Location) message.obj;
                textViewLat.setText(String.format("%.5f", location.getLatitude()));
                textViewLong.setText(String.format("%.5f", location.getLongitude()));
                break;
            case MyEvent.STATE_DEBUG_RX:
                if (message.obj != null) {
                    debugRxSpeed((Long) message.obj);
                }
                break;
            case MyEvent.STATE_DEBUG_TX:
                if (message.obj != null) {
                    debugTxSpeed((Long) message.obj);
                }
                break;
            case MyEvent.STATE_DEBUG_MSG:
            default:
                if (message.obj != null) {
                    String text = new String((byte[]) message.obj);
                    debugMessage(text);
                }
                break;
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == BluetoothScanHelper.REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_CANCELED) {
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        mTimerMonitorSpeed = new Runnable() {
            @Override
            public void run() {
                if (isBtRxActive) {
                    mainHandler.obtainMessage(MyEvent.STATE_DEBUG_RX, commHelper.getRxSpeed()).sendToTarget();
                } else {
                    mainHandler.obtainMessage(MyEvent.STATE_DEBUG_RX, 0L).sendToTarget();
                }
                if (isWifiTxActive) {
                    mainHandler.obtainMessage(MyEvent.STATE_DEBUG_TX, internetThread.getByteSend()).sendToTarget();
                } else {
                    mainHandler.obtainMessage(MyEvent.STATE_DEBUG_TX, 0L).sendToTarget();
                }
                mainHandler.postDelayed(this, 1000);
            }
        };
        mainHandler.postDelayed(mTimerMonitorSpeed, 1000);
        super.onResume();
    }
    @Override
    protected void onPause() {
        mainHandler.removeCallbacks(mTimerMonitorSpeed);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        commHelper.stop();
        if (internetThread != null) {
            internetThread.cancel();
            internetThread = null;
        }
        if (gpsThread != null) {
            gpsThread.cancel();
            gpsThread = null;
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        return super.onOptionsItemSelected(item);
    }

    public void enableBluetooth(View v) {
        if (!isBtEnable) {
            isBtEnable = true;
            commHelper.start();
            commHelper.connect(mBluetoothAdapter.getRemoteDevice(deviceAddress));
        } else {
            isBtEnable = false;
            commHelper.stop();
        }
    }
    public void enableWifi(View v) {
        if (!isWifiEnable) {
            if (isInternetActive) {
                isWifiEnable = true;
                if (internetThread != null) {
                    internetThread.cancel();
                    internetThread = null;
                }
                if (gpsThread != null) {
                    gpsThread.cancel();
                    gpsThread = null;
                }
                // start new internet threads
                try {
                    //internetThread = new TestInternetThreadImpl(mainHandler);
                    internetThread = new SimpleInternetThreadImpl(mainHandler, serverAddress, phoneName, patientId);
                    internetThread.start();
                    gpsThread = new GPSInternetThreadImpl(this, mainHandler, serverAddress, phoneName, patientId);
                    gpsThread.start();
                } catch (Exception e) {
                    Log.e(TAG, "unable to create InternetThread", e);
                }
            } else {
                debugMessage("please connect internet before start Wifi");
            }
        } else {
            isWifiEnable =false;
            if (internetThread != null) {
                internetThread.cancel();
                internetThread = null;
            }
            if (gpsThread != null) {
                gpsThread.cancel();
                gpsThread = null;
            }
        }
    }
    public void debugMessage(CharSequence text) {
        if (isDebug) {
            debugEditText.append(text + "\n");
            Log.d(TAG, text.toString());
        }
    }
    public void debugRxSpeed(long bytePerSecond) {
        double BPS = bytePerSecond / 1024;
        if (bytePerSecond < 1024) {
            textViewRxSpeed.setText(String.format("%d Bps", bytePerSecond));
        }
        else if (BPS > 0 && BPS < 1024) {
            textViewRxSpeed.setText(String.format("%.2f KBps", BPS));
        } else {
            BPS = BPS / 1024;
            if (BPS > 0 && BPS < 1024) {
                textViewRxSpeed.setText(String.format("%.2f MBps", BPS));
            } else {
                BPS = BPS / 1024;
                textViewRxSpeed.setText(String.format("%.2f GBps", BPS));
            }
        }
    }
    public void debugTxSpeed(long bytePerSecond) {
        double BPS = bytePerSecond / 1024;
        if (bytePerSecond < 1024) {
            textViewTxSpeed.setText(String.format("%d Bps", bytePerSecond));
        }
        else if (BPS > 0 && BPS < 1024) {
            textViewTxSpeed.setText(String.format("%.2f KBps", BPS));
        } else {
            BPS = BPS / 1024;
            if (BPS > 0 && BPS < 1024) {
                textViewTxSpeed.setText(String.format("%.2f MBps", BPS));
            } else {
                BPS = BPS / 1024;
                textViewTxSpeed.setText(String.format("%.2f GBps", BPS));
            }
        }
    }
    public boolean isConnected() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }
}
