package org.cardioart.gateway.impl;

import android.content.Context;
import android.location.Location;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.rbnb.sapi.ChannelMap;
import com.rbnb.sapi.SAPIException;
import com.rbnb.sapi.Source;

import org.cardioart.gateway.api.constant.MyEvent;
import org.cardioart.gateway.api.helper.gps.GPSConnection;
import org.cardioart.gateway.api.helper.gps.GPSHelper;
import org.cardioart.gateway.api.thread.InternetThread;

/**
 * Created by jirawat on 06/10/2015.
 */
public class GPSInternetThreadImpl extends Thread implements InternetThread {

    // debug variables
    private final boolean DEBUG = true;
    private final String TAG = "GPS";

    // GPS connection
    private long byteSend = 0;
    private double secTimestamp = 0;
    private GPSConnection gpsConnection;

    // handler loop
    private Handler mHandler;
    private Handler mainHandler;
    private Runnable mTimerGPSSend;

    // Datatubine connection
    private String serverAddress;
    private String deviceName;
    private String patientId;

    // Dataturbine object
    private Source source;
    private ChannelMap sMap;
    private int[] channelIndexs = new int[2];

    /**
     * Initialize thread with own handler and GPSHelper
     * @param context main UI activity
     */
    public GPSInternetThreadImpl(Context context,Handler handler, String server, String device, String id) {
        HandlerThread handlerThread = new HandlerThread("GPSInternetHandlerThread");
        handlerThread.start();
        mHandler = new Handler(handlerThread.getLooper());
        gpsConnection = new GPSHelper(context);
        serverAddress = server;
        deviceName = device;
        patientId = id;
        mainHandler = handler;

        startTimer();
    }

    @Override
    public void run() {
        Log.i(TAG, "BEGIN GPSInternetThread");
        setName("GPSInternetThread");

        try {
            initialDataturbineChannel(serverAddress, deviceName, patientId);
            mainHandler.obtainMessage(MyEvent.STATE_GPS_UP).sendToTarget();
        } catch (Exception e) {
            Log.e(TAG, "unable to connect " + serverAddress, e);
            cancel();
            return;
        }

        try {
            while(!isInterrupted()) {
                if (gpsConnection.isLocationRequested() && gpsConnection.isConnected()) {
                    Location location = gpsConnection.getGPSLocation();
                    // send recent updated location to main ui
                    mainHandler.obtainMessage(MyEvent.STATE_GPS_MSG, location).sendToTarget();

                    // create packet
                    double timestamp = (double) (System.currentTimeMillis()) / 1000.0d;
                    double times[] = {timestamp};
                    sMap.PutTimes(times);
                    sMap.PutDataAsFloat64(channelIndexs[0], new double[] {location.getLatitude()});
                    sMap.PutDataAsFloat64(channelIndexs[1], new double[] {location.getLongitude()});
                    synchronized (this) {
                        source.Flush(sMap);
                    }
                }
                sleep(2500);
            }
        } catch (Exception e) {
            Log.e(TAG, "unable to send the message to DataTurbine", e);
        } finally {
            cancel();
        }
    }
    /**
     * Get total GPS bytes outgoing to server
     * @return number of bytes that outgoing to server
     */
    @Override
    public long getByteSend() {
        if (gpsConnection.isConnected() && gpsConnection.isLocationRequested()) {
            long tmpSource = source.BytesTransferred();
            long tmpSend = tmpSource - byteSend;
            byteSend = tmpSource;
            return tmpSend;
        }
        return 0;
    }

    @Override
    public void cancel() {
        interrupt();
        Log.i(TAG, "END SimpleInternetThread");
        stopTimer();
        source.CloseRBNBConnection();
        mainHandler.obtainMessage(MyEvent.STATE_GPS_DOWN).sendToTarget();
    }

    @Override
    public synchronized void sendMessage(byte[] message) {}

    @Override
    public synchronized void setSecTimestamp(double timestamp) {}

    /**
     * Start the timer to request to location from Google Location Service every 2000 seconds
     */
    private synchronized void startTimer() {
        mTimerGPSSend = new Runnable() {
            @Override
            public void run() {
                // Enable GPS from Google Location Service
                if (!gpsConnection.isLocationRequested() && gpsConnection.isConnected()) {
                    gpsConnection.startLocationUpdates();
                    mHandler.postDelayed(mTimerGPSSend, 1000);
                }
                if (!gpsConnection.isConnected()) {
                    mHandler.postDelayed(mTimerGPSSend, 1000);
                }
            }
        };
        mHandler.postDelayed(mTimerGPSSend, 10);
    }

    /**
     * Stop the timer to stop sending location request
     */
    private synchronized void stopTimer() {
        mHandler.removeCallbacks(mTimerGPSSend);
    }

    private void initialDataturbineChannel(String serverAddress, String deviceName, String patientId) throws SAPIException {
        source = new Source(2048, "none", 2048);
        source.CloseRBNBConnection();
        source.OpenRBNBConnection(serverAddress, deviceName);
        sMap = new ChannelMap();

        //TODO: this should be patient_Id/DATE_TYPE
        // put Latitude channel
        String channelName = String.format("%s/%s", patientId, "GPS_Lat");
        sMap.Add(channelName);
        channelIndexs[0] = sMap.GetIndex(channelName);
        sMap.PutUserInfo(channelIndexs[0], "units=degree");
        sMap.PutMime(channelIndexs[0], "application/octet-stream");
        source.Register(sMap);

        // put Longitude channel
        channelName = String.format("%s/%s", patientId, "GPS_Long");
        sMap.Add(channelName);
        channelIndexs[1] = sMap.GetIndex(channelName);
        sMap.PutUserInfo(channelIndexs[1], "units=degree");
        sMap.PutMime(channelIndexs[1], "application/octet-stream");
        source.Register(sMap);

        if (DEBUG) Log.d(TAG, "create dataturbine channel");
    }
}
