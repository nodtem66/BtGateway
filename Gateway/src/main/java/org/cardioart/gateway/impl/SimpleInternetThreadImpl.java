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

    private final String TAG = "NETT";
    private final int LIMIT = 30;
    private final Handler mainHandler;

    //For packetreader
    private final Handler mHandler;
    private final int MAX_CHANNEL = MyChannel.MAX;
    private final PacketReader packetReader;
    // TODO: edit this number of sampling time
    private final double secSamplingTime = 1.0d/600;


    private final Object mLock = new Object();
    private ArrayList<ArrayList<Integer>> arrayChannel = new ArrayList<ArrayList<Integer>>(MAX_CHANNEL);

    // timer
    private Runnable mTimerForSendMessage;

    // Datatubine connection
    private String serverAddress;
    private String deviceName;
    private String patientId;

    // Dataturbine object
    private Source source;
    private ChannelMap sMap;
    // Queue-Message
    private BlockingQueue<MyMessage> blockingQueue = new LinkedBlockingDeque<MyMessage>(LIMIT);
    private long lastByteSend = 0;
    // Cached Index of data map
    private int[] channelIndexs = new int[MAX_CHANNEL];

    // For generate a timestamp
    private boolean isInitTimeSeries = false;
    private double secTimestamp;


    public SimpleInternetThreadImpl(Handler handler) throws SAPIException {
        Log.d(TAG, "BEGIN InternetThread");

        //initial  handler for timer
        HandlerThread handlerThread = new HandlerThread("SimpleInternetandlethread");
        handlerThread.start();
        mHandler = new Handler(handlerThread.getLooper());
        mainHandler = handler;

        //initialize arrayChannel
        for(int i=0; i < MAX_CHANNEL; i++) {
            arrayChannel.add(new ArrayList<Integer>());
        }

        packetReader = new Protocol2PacketReader(MAX_CHANNEL, mLock, arrayChannel);
        startTimer();
    }
    @Override
    public void run() {
        try {
            initialDataturbineChannel(serverAddress, deviceName, patientId);

            mainHandler.obtainMessage(MyEvent.STATE_INTERNET_THREAD_START).sendToTarget();
            MyMessage message;
            while (!interrupted()) {
                message = blockingQueue.take();
                //sMap.PutTime(message.getStart(), message.getDuration());
                int channel_index = message.id;
                //byte channel_index = message.channel;
                sMap.PutTimes(message.time);
                sMap.PutDataAsInt32(channelIndexs[channel_index], message.data);
                synchronized (this) {
                    source.Flush(sMap, true);
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "EXP: " + e.getLocalizedMessage());
            e.printStackTrace();
        } finally {
            Log.d(TAG, "END InternetThread");
            stopTimer();
            source.CloseRBNBConnection();
            mainHandler.obtainMessage(MyEvent.STATE_INTERNET_THREAD_STOP).sendToTarget();
        }
    }
    @Override
    public void cancel() {
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
        Log.d(TAG, "CHANNEL CREATE");
    }

    @Override
    public boolean isNotFull() {
        return blockingQueue.remainingCapacity() > 0;
    }

    @Override
    public boolean isEmpty() {
        return blockingQueue.isEmpty();
    }

    @Override
    public boolean sendMyMessage(MyMessage message) {
        try {
            blockingQueue.put(message);
            return true;
        } catch (InterruptedException e) {
            Log.d(TAG, "Cannot put message");
        }
        return false;
    }
    public boolean sendMyMessage(byte[] byteData) {

        if (!isInitTimeSeries) {
            isInitTimeSeries = true;
            secTimestamp = (double)(System.currentTimeMillis())/1000.0d;
            Log.d(TAG, "Time:" + Double.toString(secTimestamp));
        }
        packetReader.readByte(byteData);
        return true;

        /* below this a protocol I
        if (byteData.length >= 8) {
            id = byteData[0] << 24;
            id += byteData[1] << 16;
            id += byteData[2] << 8;
            id += byteData[3];
            channel = byteData[4];
            opt = byteData[5];

            MyMessage message = new MyMessage(id, channel, opt);
            shortLength = byteData.length - 6;
            message.data = new int[shortLength/2];
            message.time = new double[shortLength/2];
            for(int i=0,len = shortLength/2; i < len; i++) {
                message.data[i] = (byteData[6 + 2 * i] << 8);
                message.data[i] += byteData[7 + 2 * i];
                message.time[i] = secTimestamp;
                secTimestamp += secSamplingTime;
            }
            //Log.d(TAG, "shortLength: " + shortLength);
            //Log.d(TAG, "secSamplingTime: " + secSamplingTime);
            //Log.d(TAG, "secNextTimeFrame: " + secNextTimeFrame);
            return sendMyMessage(message);
        }*/
    }

    @Override
    public void setSecTimestamp(double timestamp) {
        secTimestamp = timestamp;
    }

    public void startTimer() {
        mTimerForSendMessage = new Runnable() {
            @Override
            public void run() {

                // foreach data channel for one patient
                for (int channel_index = 0; channel_index < MAX_CHANNEL; channel_index++) {

                    //Get data from packetreader buffer
                    int length = arrayChannel.get(channel_index).size();
                    if (length > 0) {
                        Integer[] dataInt = arrayChannel.get(channel_index).toArray(new Integer[length]);

                        //clear packetreader data
                        synchronized (mLock) {
                                arrayChannel.get(channel_index).clear();
                        }

                        //construct dataturbine packet
                        MyMessage message = new MyMessage(channel_index, (byte) channel_index, (byte) 0);
                        message.data = new int[length];
                        message.time = new double[length];

                        // foreach data in this channel
                        for (int i = 0; i < length; i++) {
                            message.data[i] = dataInt[i];
                            message.time[i] = secTimestamp;
                            secTimestamp += secSamplingTime;
                        }
                        sendMyMessage(message);
                    }
                }

                //rerun timer
                mHandler.postDelayed(mTimerForSendMessage, 100);
            }
        };
        mHandler.postDelayed(mTimerForSendMessage, 100);
    }

    public void stopTimer() {
        mHandler.removeCallbacks(mTimerForSendMessage);
    }
}
