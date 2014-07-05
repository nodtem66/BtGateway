package org.cardioart.gateway.impl;

import android.os.Handler;
import android.util.Log;

import com.rbnb.sapi.ChannelMap;
import com.rbnb.sapi.SAPIException;
import com.rbnb.sapi.Source;

import org.cardioart.gateway.activity.GatewayActivity;
import org.cardioart.gateway.api.InternetThread;
import org.cardioart.gateway.api.MyMessage;

/**
 * Created by jirawat on 06/07/2014.
 */
public class TestInternetThreadImpl extends Thread implements InternetThread {
    private static int current_x = 0;

    private final String TAG = "NETT";
    private final Handler mainHandler;

    private String[] channelNames = {
            "ecg1"
    };
    private int[] channelIndexs = new int[channelNames.length];
    private Source source;
    private ChannelMap sMap;
    private long lastByteSend = 0;

    public TestInternetThreadImpl(Handler handler) throws  SAPIException {
        Log.d(TAG, "BEGIN InternetThread");
        mainHandler = handler;
    }
    @Override
    public void run() {
        try {
            initialDataturbineChannel();
            mainHandler.obtainMessage(GatewayActivity.STATE_INTERNET_THREAD_START).sendToTarget();
            short[] values = new short[1000];
            while (!interrupted()) {

                for (int i=0; i<1000; i++) {
                    values[i] = (short) (32500 * getSine(current_x));
                    current_x++;
                }
                for (int i=0; i<channelIndexs.length; i++) {
                    sMap.PutDataAsInt16(channelIndexs[i], values);
                }
                synchronized (this) {
                    source.Flush(sMap, true);
                }
                sleep(100);
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
        for (int i=0; i<channelNames.length; i++) {
            sMap.Add(channelNames[i]);
            channelIndexs[i] = sMap.GetIndex(channelNames[i]);
            sMap.PutUserInfo(channelIndexs[i], "units=n, property=value");
            sMap.PutMime(channelIndexs[i], "application/octet-stream");
        }
        source.Register(sMap);
        Log.d(TAG, "CHANNEL CREATE");
    }

    @Override
    public boolean isNotFull() {
        return false;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public boolean sendMyMessage(MyMessage message) {
        return true;
    }
    public boolean sendMyMessage(byte[] data) {
        return true;
    }
}