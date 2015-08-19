package org.cardioart.gateway.activity;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
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
import android.widget.EditText;
import android.widget.TextView;

import com.rbnb.sapi.SAPIException;

import org.cardioart.gateway.R;
import org.cardioart.gateway.api.constant.MyEvent;
import org.cardioart.gateway.api.helper.bluetooth.BluetoothConnection;
import org.cardioart.gateway.api.helper.bluetooth.BluetoothScanHelper;
import org.cardioart.gateway.api.thread.InternetThread;
import org.cardioart.gateway.impl.BluetoothCommClient;
import org.cardioart.gateway.impl.SimpleInternetThreadImpl;

public class GatewayActivity extends ActionBarActivity implements Handler.Callback {

    private Handler mainHandler;
    private BluetoothConnection commHelper;
    private InternetThread internetThread;
    private EditText debugEditText;
    private TextView btServerStatus, btRxStatus, wifiClientStatus, wifiTxStatus;
    private TextView textViewTxSpeed, textViewRxSpeed, textViewConnectivityStatus;

    public static final String TAG = "BtG";
    public static final int REQUEST_ENABLE_BT = 2718;
    public static final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    private boolean isDebug = true;
    private boolean isBtEnable = false;
    private boolean isWifiEnable = false;
    private boolean isBtServerActive = false;
    private boolean isBtRxActive = false;
    private boolean isWifiClientActive = false;
    private boolean isWifiTxActive = false;
    private boolean isInternetActive = false;

    private Runnable mTimerMonitorSpeed;

    private String deviceName;
    private String deviceAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gateway);

        deviceName = getIntent().getStringExtra("device_name");
        deviceAddress = getIntent().getStringExtra("device_address");

        mainHandler = new Handler(this);
        commHelper = new BluetoothCommClient(mainHandler);

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BT);
        }

        debugEditText = (EditText) findViewById(R.id.editText);
        btServerStatus = (TextView) findViewById(R.id.btServerStatus);
        btRxStatus = (TextView) findViewById(R.id.btRxStatus);
        wifiClientStatus = (TextView) findViewById(R.id.wifiClientStatus);
        wifiTxStatus = (TextView) findViewById(R.id.wifiTxStatus);
        textViewRxSpeed = (TextView) findViewById(R.id.textViewRxSpeed);
        textViewTxSpeed = (TextView) findViewById(R.id.textViewTxSpeed);
        textViewConnectivityStatus = (TextView) findViewById(R.id.textViewConnectivity);

        btRxStatus.setTextColor(Color.DKGRAY);
        btServerStatus.setTextColor(Color.DKGRAY);
        wifiClientStatus.setTextColor(Color.DKGRAY);
        wifiTxStatus.setTextColor(Color.DKGRAY);

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
            case MyEvent.STATE_BT_SERVER_UP:
                if (!isBtServerActive) {
                    isBtServerActive = true;
                    btServerStatus.setTextColor(Color.GREEN);
                }
                break;
            case MyEvent.STATE_BT_SERVER_DOWN:
                if (isBtServerActive) {
                    isBtServerActive = false;
                    btServerStatus.setTextColor(Color.DKGRAY);
                }
                break;
            case MyEvent.STATE_BT_RX_UP:
                if (!isBtRxActive) {
                    isBtRxActive = true;
                    btRxStatus.setTextColor(Color.GREEN);
                    double time = System.currentTimeMillis()/1000.d;
                    internetThread.setSecTimestamp(time);
                }
                break;
            case MyEvent.STATE_BT_RX_DOWN:
                if (isBtRxActive) {
                    isBtRxActive = false;
                    btRxStatus.setTextColor(Color.DKGRAY);
                }
                break;
            case MyEvent.STATE_INTERNET_THREAD_START:
                if (!isWifiClientActive) {
                    isWifiClientActive = true;
                    wifiClientStatus.setTextColor(Color.GREEN);
                }
                if (!isWifiTxActive) {
                    isWifiTxActive = true;
                    wifiTxStatus.setTextColor(Color.GREEN);
                }
                break;
            case MyEvent.STATE_INTERNET_THREAD_STOP:
                if (isWifiClientActive) {
                    isWifiClientActive = false;
                    wifiClientStatus.setTextColor(Color.DKGRAY);
                }
                if (isWifiTxActive) {
                    isWifiTxActive = false;
                    wifiTxStatus.setTextColor(Color.DKGRAY);
                }
                if (isWifiEnable) {
                    isWifiEnable = false;
                }
                break;
            case MyEvent.STATE_INTERNET_THREAD_MSG:
                if (isWifiTxActive) {
                    internetThread.sendMyMessage((byte[]) message.obj);
                }
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
                debugMessage((CharSequence)message.obj);
                break;
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == BluetoothScanHelper.REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_CANCELED) {
                System.exit(2);
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
            internetThread.interrupt();
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
            //TODO: test
            commHelper.startConnection(mBluetoothAdapter.getRemoteDevice(deviceAddress));
        } else {
            isBtEnable = false;
            commHelper.stopConnection();
        }
    }
    public void enableWifi(View v) {
        if (!isWifiEnable) {
            isWifiEnable = true;
            if (isInternetActive) {
                if (internetThread != null) {
                    internetThread.cancel();
                    internetThread.interrupt();
                }
                try {
                    //internetThread = new TestInternetThreadImpl(mainHandler);
                    internetThread = new SimpleInternetThreadImpl(mainHandler);
                    internetThread.start();
                } catch (SAPIException e) {
                    Log.d(TAG, "SAPIExp: " + e.getLocalizedMessage());
                }
            }
            Log.d(TAG, "");
        } else {
            isWifiEnable =false;
            if (internetThread != null) {
                internetThread.cancel();
                internetThread.interrupt();
            }
            Log.d(TAG, "");
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
