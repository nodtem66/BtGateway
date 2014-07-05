package org.cardioart.gateway.impl;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.rbnb.sapi.ChannelMap;
import com.rbnb.sapi.SAPIException;
import com.rbnb.sapi.Source;

import org.cardioart.gateway.activity.GatewayActivity;
import org.cardioart.gateway.api.InternetThread;
import org.cardioart.gateway.api.MyChannel;
import org.cardioart.gateway.api.MyMessage;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Created by jirawat on 06/07/2014.
 */
public class SimpleInternetThreadImpl extends Thread implements InternetThread {
    private final String TAG = "NETT";
    private final int LIMIT = 30;
    private final Handler mainHandler;

    private Source source;
    private ChannelMap sMap;
    private BlockingQueue<MyMessage> blockingQueue = new LinkedBlockingDeque<MyMessage>(LIMIT);
    private long lastByteSend = 0;
    private int[] channelIndexs = new int[1];

    public SimpleInternetThreadImpl(Handler handler) throws SAPIException {
        Log.d(TAG, "BEGIN InternetThread");
        mainHandler = handler;
    }
    @Override
    public void run() {
        try {
            initialDataturbineChannel();
            Looper.prepare();
            mainHandler.obtainMessage(GatewayActivity.STATE_INTERNET_THREAD_START).sendToTarget();
            MyMessage message;
            while (!interrupted()) {
                message = blockingQueue.take();
                sMap.PutDataAsInt16(channelIndexs[0], message.data);
                synchronized (this) {
                    source.Flush(sMap, true);
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "EXP: " + e.getLocalizedMessage());
        } finally {
            Log.d(TAG, "END InternetThread");
            source.CloseRBNBConnection();
            mainHandler.obtainMessage(GatewayActivity.STATE_INTERNET_THREAD_STOP).sendToTarget();
        }
    }
    @Override
    public void cancel() {
    }
    public double getSine(int x) {
        return Math.sin((Math.PI*x/5000)); // 10Hz
    }
    public synchronized long getByteSend() {
        long buffer = lastByteSend;
        lastByteSend = source.BytesTransferred();
        return lastByteSend - buffer;
    }

    private void initialDataturbineChannel() throws SAPIException{
        source = new Source(2048, "none", 2048);
        source.CloseRBNBConnection();
        source.OpenRBNBConnection("192.168.2.100:3333", "Android1");
        sMap = new ChannelMap();
        sMap.PutTimeAuto("timeofday");
        for (int i=0; i<1; i++) {
            sMap.Add(MyChannel.getName(i));
            channelIndexs[i] = sMap.GetIndex(MyChannel.getName(i));
            sMap.PutUserInfo(channelIndexs[i], "units=n, property=value");
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
        int id, shortLength;
        byte channel, opt;
        if (byteData.length >= 8) {
            id = byteData[0] << 24;
            id += byteData[1] << 16;
            id += byteData[2] << 8;
            id += byteData[3];
            channel = byteData[4];
            opt = byteData[5];
            MyMessage message = new MyMessage(id, channel, opt);
            shortLength = byteData.length - 6;
            message.data = new short[shortLength/2];
            for(int i=0; i<shortLength/2; i++) {
                message.data[i] = (short) (byteData[6 + 2 * i] << 8);
                message.data[i] += (short) byteData[7 + 2 * i];
            }
            return sendMyMessage(message);
        }
        return false;
    }
}
