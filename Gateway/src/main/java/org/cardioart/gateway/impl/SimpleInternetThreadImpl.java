package org.cardioart.gateway.impl;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.rbnb.sapi.ChannelMap;
import com.rbnb.sapi.SAPIException;
import com.rbnb.sapi.Source;

import org.cardioart.gateway.api.PacketReader;
import org.cardioart.gateway.api.constant.MyChannel;
import org.cardioart.gateway.api.constant.MyEvent;
import org.cardioart.gateway.api.constant.MyMessage;
import org.cardioart.gateway.api.thread.InternetThread;
import org.cardioart.gateway.impl.reader.Protocol2PacketReader;

import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class SimpleInternetThreadImpl extends Thread implements InternetThread {

    // used in debug message
    private final String TAG = "NETT";
    private final boolean DEBUG = true;

    // handler loop
    private final Handler mainHandler;  // main ui handler
    private final Handler mHandler;     // own handler for timer

    // constant parameters
    // TODO: edit this number of sampling time
    private final long hzSamplingFrequency = 600;
    private final double secSamplingTime = 1.0d / hzSamplingFrequency;

    // packet reader for parse raw data stream from incoming bluetooth device
    private final PacketReader packetReader;
    private final Object mLock = new Object();
    private final int MAX_CHANNEL = MyChannel.MAX;
    private ArrayList<ArrayList<Integer>> arrayChannel = new ArrayList<ArrayList<Integer>>(MAX_CHANNEL);

    // timer for send the packet to dataturbine server
    private Runnable mTimerForSendMessage;

    // Datatubine connection
    private String serverAddress;
    private String deviceName;
    private String patientId;

    // Dataturbine object
    private Source source;
    private ChannelMap sMap;

    // Message queue
    private final int QUEUE_LIMIT = 30;
    private BlockingQueue<MyMessage> blockingQueue = new LinkedBlockingDeque<MyMessage>(QUEUE_LIMIT);
    private long lastByteSend = 0;

    // Cached Index of data map
    private int[] channelIndexs = new int[MAX_CHANNEL];

    // For generate a timestamp for each channel
    private boolean isInitTimeSeries = false;
    private double[] secTimestamp = new double[MAX_CHANNEL];


    public SimpleInternetThreadImpl(Handler handler, String server, String device, String patient) throws SAPIException {
        if (DEBUG) Log.d(TAG, "create InternetThread()");

        //initial  handler for timer
        HandlerThread handlerThread = new HandlerThread("SimpleInternetHandlerThread");
        handlerThread.start();
        mHandler = new Handler(handlerThread.getLooper());
        mainHandler = handler;
        serverAddress = server;
        deviceName = device;
        patientId = patient;

        //initialize arrayChannel
        for(int i=0; i < MAX_CHANNEL; i++) {
            arrayChannel.add(new ArrayList<Integer>());
        }

        packetReader = new Protocol2PacketReader(MAX_CHANNEL, mLock, arrayChannel);
        startTimer();
    }
    @Override
    public void run() {
        Log.i(TAG, "BEGIN SimpleInternetThread");
        setName("SimpleInternetThread");

        try {
            initialDataturbineChannel(serverAddress, deviceName, patientId);
            mainHandler.obtainMessage(MyEvent.STATE_INTERNET_THREAD_START).sendToTarget();
        } catch (Exception e) {
            Log.e(TAG, "unable to connection DataTurbine server: " + serverAddress, e);
            cancel();
            return;
        }
        try {
            while (!interrupted()) {
                MyMessage message;
                message = blockingQueue.take();

                int channel_index = message.id;
                sMap.PutTimes(message.time);
                sMap.PutDataAsInt32(channelIndexs[channel_index], message.data);
                synchronized (this) {
                    source.Flush(sMap, true);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "unable to sent the data to server", e);

        } finally {
            cancel();
        }
    }

    public void cancel() {
        interrupt();
        Log.i(TAG, "END SimpleInternetThread");
        stopTimer();
        source.CloseRBNBConnection();
        mainHandler.obtainMessage(MyEvent.STATE_INTERNET_THREAD_STOP).sendToTarget();
    }
    public synchronized long getByteSend() {
        long buffer = lastByteSend;
        lastByteSend = source.BytesTransferred();
        return lastByteSend - buffer;
    }

    private void initialDataturbineChannel(String serverAddress, String deviceName, String patientId) throws SAPIException{
        source = new Source(2048, "none", 2048);
        source.CloseRBNBConnection();
        source.OpenRBNBConnection(serverAddress, deviceName);
        sMap = new ChannelMap();
        for (int i=0; i<MAX_CHANNEL; i++) {
            //TODO: this should be patient_Id/DATE_TYPE
            String channelName = String.format("%s/%s", patientId, MyChannel.getName(i));
            sMap.Add(channelName);
            channelIndexs[i] = sMap.GetIndex(channelName);
            sMap.PutUserInfo(channelIndexs[i], "units=mv, sampling_time=60Hz");
            sMap.PutMime(channelIndexs[i], "application/octet-stream");
        }
        source.Register(sMap);
        if (DEBUG) Log.d(TAG, "create dataturbine channel");
    }

    public boolean isNotFull() {
        return blockingQueue.remainingCapacity() > 0;
    }

    public boolean isEmpty() {
        return blockingQueue.isEmpty();
    }

    /**
     * send message to PacketReader
     * @param byteData array of raw data from incoming message
     * @return success or exception
     */
    public synchronized void sendMessage(byte[] byteData) {

        // if secTimestmp is not set; set it from current android timestamp
        if (!isInitTimeSeries) {
            isInitTimeSeries = true;
            double timestamp = (double)(System.currentTimeMillis())/1000.0d;
            // set timestamp in every channel
            for (int i=0; i < MAX_CHANNEL; i++) secTimestamp[i] = timestamp;
            if (DEBUG) Log.d(TAG, "Automatic Set Timestamp:" + Double.toString(timestamp));
        }
        // send to PacketReader waited the timer to send parse data to dataturbine
        packetReader.readByte(byteData);
    }

    /**
     * set timestamp in all channel
     * @param timestamp in seconds
     */
    public synchronized void setSecTimestamp(double timestamp) {
        for (int i=0; i < MAX_CHANNEL; i++) {
            secTimestamp[i] = timestamp;
        }
    }

    /**
     * This function send to parsed message to message queue running in main loop
     * @param message a parsed message ready to send
     * @return success or exception
     */
    private boolean sendMyMessage(MyMessage message) {
        try {
            blockingQueue.put(message);
            return true;
        } catch (InterruptedException e) {
            Log.e(TAG, "Cannot put message", e);
        }
        return false;
    }

    /**
     * This function start the timer to send the stored data in PacketReader to message queue
     */
    private synchronized void startTimer() {
        mTimerForSendMessage = new Runnable() {
            @Override
            public void run() {

                // foreach data channel for one patient
                for (int channel_index = 0; channel_index < MAX_CHANNEL; channel_index++) {

                    //Get data from PacketReader buffer
                    int length = arrayChannel.get(channel_index).size();
                    if (length > 0) {
                        Integer[] dataInt = arrayChannel.get(channel_index).toArray(new Integer[length]);

                        //clear PacketReader data on this channel
                        synchronized (mLock) {
                                arrayChannel.get(channel_index).clear();
                        }
                        //construct DataTurbine packet
                        MyMessage message = new MyMessage(channel_index, (byte) channel_index, (byte) 0);
                        message.data = new int[length];
                        message.time = new double[length];

                        // foreach data in this channel
                        for (int i = 0; i < length; i++) {
                            message.data[i] = dataInt[i];
                            message.time[i] = secTimestamp[channel_index];
                            secTimestamp[channel_index] += secSamplingTime;
                        }
                        sendMyMessage(message);
                    }
                }
                // re-run runnable function again every 100 msec
                mHandler.postDelayed(mTimerForSendMessage, 100);
            }
        };
        // first start runnable function in handler
        mHandler.postDelayed(mTimerForSendMessage, 100);
    }

    /**
     * This function stop timer to send the data from PacketReader to message queue
     */
    private synchronized void stopTimer() {
        mHandler.removeCallbacks(mTimerForSendMessage);
    }
}
