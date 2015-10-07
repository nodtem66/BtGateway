package org.cardioart.gateway.activity;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphView.GraphViewData;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.LineGraphView;

import org.cardioart.gateway.R;
import org.cardioart.gateway.api.constant.MyEvent;
import org.cardioart.gateway.api.helper.bluetooth.BluetoothConnection;
import org.cardioart.gateway.api.helper.bluetooth.BluetoothScanHelper;
import org.cardioart.gateway.api.helper.gps.GPSConnection;
import org.cardioart.gateway.api.helper.gps.GPSHelper;
import org.cardioart.gateway.api.thread.PacketReaderThread;
import org.cardioart.gateway.impl.BluetoothCommClient;
import org.cardioart.gateway.impl.Protocol2ReaderThreadImpl;
import org.w3c.dom.Text;

import java.util.ArrayList;


public class GraphActivity extends AppCompatActivity implements Handler.Callback, AdapterView.OnItemSelectedListener {

    private static final int MAX_CHANNEL = 8;
    private static final int MAX_SAMPLE_LENGTH = 300;
    private static final String TAG = "gateway";
    private static String deviceName;
    private static String deviceAddress;


    private Handler mainHandler;
    private BluetoothConnection commHelper;
    private GPSConnection gpsHelper;
    private PacketReaderThread packetReader;
    private Runnable mTimerMonitorSpeed;
    private Runnable mTimerGraphPlot;
    private Runnable mTimerGPS;
    private TextView textViewRxSpeed;
    private TextView textViewRxPacket;
    private TextView textViewGPSLat;
    private TextView textViewGPSLong;

    private static final int GRAPHVIEW_SERIE_HISTORY_SIZE = 1000;
    private static final double REFERENCE_VOLTAGE = 5;
    private static final int ADC_RESOLUTION_BIT = 24;
    private static final double ADC_TO_VOLTAGE_UNIT_MULTIPLIER = REFERENCE_VOLTAGE / Math.pow(2, ADC_RESOLUTION_BIT);
    private GraphView graphView;
    private GraphViewSeries series1;
    private double data1Time = 0d;
    private ArrayList<GraphViewData> data1 = new ArrayList<GraphViewData>();

    private boolean isBluetoothActive = false;
    private boolean isPacketReaderActive = false;
    private int currentChannelId = 0;
    private long secRunningTime = 0;

    public static final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    @Override
    public boolean handleMessage(Message message) {
        switch (message.what) {
            case MyEvent.STATE_DEBUG_RX:
                if (message.obj != null) {
                    debugRxSpeed((Long) message.obj);
                }
                break;
            case MyEvent.STATE_DEBUG_PACKET:
                if (message.obj != null) {
                    debugPacketReader((Long) message.obj);
                }
                break;
            case MyEvent.STATE_BT_RX_UP:
                isBluetoothActive = true;
                Toast.makeText(this, "connection start", Toast.LENGTH_SHORT).show();
                break;
            case MyEvent.STATE_BT_RX_DOWN:
                isBluetoothActive = false;
                break;
            case MyEvent.STATE_PACKETREADER_THREAD_MSG:
            case MyEvent.STATE_INTERNET_THREAD_MSG:
                packetReader.readPacket((byte[]) message.obj);
                //Log.d(TAG, MyStringConvert.bytesToHex((byte[])message.obj));
                break;
            case MyEvent.STATE_PACKETREADER_THREAD_START:
                isPacketReaderActive = true;
                break;
            case MyEvent.STATE_PACKETREADER_THREAD_STOP:
                isPacketReaderActive = false;
            default:
                break;
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);

        //Get ExtraParameter from DeviceSelectionActivity
        deviceName = getIntent().getStringExtra("device_name");
        deviceAddress = getIntent().getStringExtra("device_address");
        TextView textDevice = (TextView) findViewById(R.id.textDevice);
        textDevice.setText("Device: " + deviceName + " (" + deviceAddress + ")");

        //Initialize private variables
        mainHandler = new Handler(this);
        commHelper = new BluetoothCommClient(mainHandler);
        gpsHelper = new GPSHelper(this);
        textViewRxSpeed = (TextView) findViewById(R.id.textViewRxSpeed);
        textViewRxPacket = (TextView) findViewById(R.id.textViewRxPacket);
        textViewGPSLat = (TextView) findViewById(R.id.textViewGPSLat);
        textViewGPSLong = (TextView) findViewById(R.id.textViewGPSLong);

        //Enable bluetooth
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetoothIntent, MyEvent.REQUEST_ENABLE_BT);
        }

        //Change orientation
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);

        //Generate Series data
        for (; data1Time < GRAPHVIEW_SERIE_HISTORY_SIZE; data1Time++) {
            data1.add(new GraphViewData(data1Time, 0));
        }

        //Graph configuration
        series1 = new GraphViewSeries(data1.toArray(new GraphViewData[data1.size()]));
        graphView = new LineGraphView(this, "");
        graphView.setShowLegend(false);
        series1.getStyle().color = Color.GREEN;
        graphView.addSeries(series1);
        graphView.setHorizontalLabels(new String[]{});
        //graphView.setVerticalLabels(new String[]{"100", "75", "50", "25", "0"});
        graphView.getGraphViewStyle().setTextSize(getResources().getDimension(R.dimen.small));
        LinearLayout layout = (LinearLayout) findViewById(R.id.graph1);
        layout.addView(graphView);

        //Dropdown (Spinner) Configuration
        Spinner spinner = (Spinner) findViewById(R.id.spinner);
        spinner.setOnItemSelectedListener(this);
        //Start bluetooth thread
        if (mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.cancelDiscovery();
            commHelper.start();
            commHelper.connect(mBluetoothAdapter.getRemoteDevice(deviceAddress));
            //Start PacketReader thread
            packetReader = new Protocol2ReaderThreadImpl(mainHandler);
            packetReader.start();
        }
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
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        if (adapterView != null) {
            currentChannelId = i;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
    }

    @Override
    protected void onResume() {
        //Enable GPS from Google Location Service
        if (gpsHelper.isConnected()) {
            gpsHelper.startLocationUpdates();
        }
        // update speed every 1000 msec
        mTimerMonitorSpeed = new Runnable() {
            @Override
            public void run() {
                if (isBluetoothActive) {
                    mainHandler.obtainMessage(MyEvent.STATE_DEBUG_RX, commHelper.getRxSpeed()).sendToTarget();
                } else {
                    mainHandler.obtainMessage(MyEvent.STATE_DEBUG_RX, 0L).sendToTarget();
                }
                if (isPacketReaderActive) {
                    secRunningTime += 1;
                    mainHandler.obtainMessage(MyEvent.STATE_DEBUG_PACKET, packetReader.getTotalByteRead()).sendToTarget();
                } else {
                    secRunningTime = 0;
                    mainHandler.obtainMessage(MyEvent.STATE_DEBUG_PACKET, 27L).sendToTarget();
                }
                TextView textTime = (TextView) findViewById(R.id.textTime);
                textTime.setText(String.format("%d:%d:%d",
                        secRunningTime/3600, //For hour
                        (secRunningTime /60) % 60, //For minute
                        secRunningTime % 60)); // For seconds
                mainHandler.postDelayed(this, 1000);
            }
        };
        // update GPS every 3000 msec
        mTimerGPS = new Runnable() {
            @Override
            public void run() {
                if (gpsHelper.isLocationUpdated()) {
                    Location location = gpsHelper.getGPSLocation();
                    textViewGPSLat.setText(String.format("Lat: %.5f", location.getLatitude()));
                    textViewGPSLong.setText(String.format("Lng: %.5f", location.getLongitude()));
                }
                if (!gpsHelper.isLocationRequested() && gpsHelper.isConnected()) {
                    gpsHelper.startLocationUpdates();
                }
                mainHandler.postDelayed(this, 3000);
            }
        };
        // update graph every 100ms
        mTimerGraphPlot = new Runnable() {
            @Override
            public void run() {
                if (isPacketReaderActive) {
                    if (currentChannelId < 8) {
                        Integer[] newData = packetReader.getChannel(currentChannelId);

                        //Print array to graphview
                        for (int i = 0, length = newData.length; i < length; i++) {
                            data1Time += 1;
                            data1.remove(0);
                            data1.add(new GraphViewData(data1Time, newData[i].doubleValue() * ADC_TO_VOLTAGE_UNIT_MULTIPLIER));
                        }

                    } else if (currentChannelId == 8) {
                        //Get Channel3 and Channel2 Data
                        Integer[] channel3Data = packetReader.getChannel(2);
                        Integer[] channel2Data = packetReader.getChannel(1);
                        int length = channel2Data.length;
                        if (length > channel3Data.length) {
                            length = channel3Data.length;
                        }
                        for (int i = 0; i < length; i++) {
                            data1Time += 1;
                            data1.remove(0);
                            data1.add(new GraphViewData(data1Time, (channel3Data[i] - channel2Data[i]) * ADC_TO_VOLTAGE_UNIT_MULTIPLIER));
                        }
                    } else if (currentChannelId == 9) {
                        //Get Channel3 and Channel2 Data
                        Integer[] channel3Data = packetReader.getChannel(2);
                        Integer[] channel2Data = packetReader.getChannel(1);
                        int length = channel2Data.length;
                        if (length > channel3Data.length) {
                            length = channel3Data.length;
                        }
                        for (int i = 0; i < length; i++) {
                            data1Time += 1;
                            data1.remove(0);
                            data1.add(new GraphViewData(data1Time, (channel3Data[i] + channel2Data[i]) * 0.5 * ADC_TO_VOLTAGE_UNIT_MULTIPLIER));
                        }
                    } else if (currentChannelId == 10) {
                        //Get Channel3 and Channel2 Data
                        Integer[] channel3Data = packetReader.getChannel(2);
                        Integer[] channel2Data = packetReader.getChannel(1);
                        int length = channel2Data.length;
                        if (length > channel3Data.length) {
                            length = channel3Data.length;
                        }
                        for (int i = 0; i < length; i++) {
                            data1Time += 1;
                            data1.remove(0);
                            data1.add(new GraphViewData(data1Time, (channel2Data[i] - channel3Data[i] * 0.5) * ADC_TO_VOLTAGE_UNIT_MULTIPLIER));
                        }
                    } else if (currentChannelId == 11) {
                        //Get Channel3 and Channel2 Data
                        Integer[] channel3Data = packetReader.getChannel(2);
                        Integer[] channel1Data = packetReader.getChannel(0);
                        int length = channel1Data.length;
                        if (length > channel3Data.length) {
                            length = channel3Data.length;
                        }
                        for (int i = 0; i < length; i++) {
                            data1Time += 1;
                            data1.remove(0);
                            data1.add(new GraphViewData(data1Time, (channel3Data[i] - channel1Data[i] * 0.5) * ADC_TO_VOLTAGE_UNIT_MULTIPLIER));
                        }
                    }
                    //packetReader.clearAllChannel();
                    series1.resetData(data1.toArray(new GraphViewData[data1.size()]));

                }
                packetReader.clearAllChannel();
                mainHandler.postDelayed(this, 100);
            }
        };
        mainHandler.postDelayed(mTimerMonitorSpeed, 1000);
        mainHandler.postDelayed(mTimerGPS, 3000);
        mainHandler.postDelayed(mTimerGraphPlot, 100);

        super.onResume();
    }

    @Override
    protected void onPause() {
        //mainHandler.removeCallbacks(mTimerMonitorSpeed);
        mainHandler.removeCallbacks(mTimerGraphPlot);
        mainHandler.removeCallbacks(mTimerGPS);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        commHelper.stop();
        gpsHelper.stopLocationUpdates();
        super.onDestroy();
    }

    private void debugRxSpeed(long totalByte) {
        if (textViewRxSpeed != null) {

            double BPS = totalByte / 1024.0;
            if (totalByte < 1024) {
                textViewRxSpeed.setText(String.format("%d Bps", totalByte));
            } else if (BPS > 0 && BPS < 1024) {
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
    }

    private void debugPacketReader(long totalByte) {
        if (textViewRxPacket != null) {
            double BPS = totalByte / 1024.0;
            if (totalByte < 1024) {
                textViewRxPacket.setText(String.format("%d Bps", totalByte));
            } else if (BPS > 0 && BPS < 1024) {
                textViewRxPacket.setText(String.format("%.2f KBps", BPS));
            } else {
                BPS = BPS / 1024;
                if (BPS > 0 && BPS < 1024) {
                    textViewRxPacket.setText(String.format("%.2f MBps", BPS));
                } else {
                    BPS = BPS / 1024;
                    textViewRxPacket.setText(String.format("%.2f GBps", BPS));
                }
            }
        }
    }
}
