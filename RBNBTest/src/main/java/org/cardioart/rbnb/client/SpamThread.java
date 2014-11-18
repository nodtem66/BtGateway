package org.cardioart.rbnb.client;

import android.os.Handler;
import android.util.Log;

import com.rbnb.sapi.ChannelMap;
import com.rbnb.sapi.SAPIException;
import com.rbnb.sapi.Source;

/**
 * Created by jirawat on 06/07/2014.
 */
public class SpamThread extends Thread {
    private static int current_x = 0;
    private final String TAG = "SPAMT";
    private String[] channelNames = {
            "sine1", "sine2", "sine3", "sine4", "sine5", "sine6", "sine7", "sine8", "sine9", "sine10",
            "sine11", "sine12", "line"
    };
    private int[] channelIndexs = new int[13];
    private Source source;
    private ChannelMap sMap;
    private final Handler mainHandler;
    public double getSine(int x) {
        return Math.sin((Math.PI*x/5000)); // 10Hz
    }
    public SpamThread(Handler handler) throws  SAPIException {
        Log.d(TAG, "BEGIN SpamThread");
        mainHandler = handler;
    }
    @Override
    public void run() {
        try {
            initialDataturbineChannel();
            mainHandler.obtainMessage(0).sendToTarget();
            short[] values = new short[1000];
            while (!interrupted()) {

                for (int i=0; i<1000; i++) {
                    values[i] = (short) (32500 * getSine(current_x));
                    current_x++;
                }
                for (int i=0; i<13; i++) {
                    sMap.PutDataAsInt16(channelIndexs[i], values);
                }
                synchronized (this) {
                    source.Flush(sMap, true);
                }
                sleep(20);
            }
        } catch (Exception e) {
            Log.d(TAG, "EXP: " + e.getLocalizedMessage());
        } finally {
            Log.d(TAG, "END SpamThread");
            source.CloseRBNBConnection();
        }
    }
    public void cancel() {
    }
    public synchronized long getByteSend() {
        return source.BytesTransferred();
    }

    private void initialDataturbineChannel() throws SAPIException{
        source = new Source(2048, "none", 2048);
        source.CloseRBNBConnection();
        source.OpenRBNBConnection("128.199.182.116:3333", "HelloSine");
        sMap = new ChannelMap();
        sMap.PutTimeAuto("timeofday");
        for (int i=0; i<13; i++) {
            sMap.Add(channelNames[i]);
            channelIndexs[i] = sMap.GetIndex(channelNames[i]);
            sMap.PutUserInfo(channelIndexs[i], "units=n, property=value");
            sMap.PutMime(channelIndexs[i], "application/octet-stream");
        };
        source.Register(sMap);
        Log.d(TAG, "CHANNEL CREATE");
    }
}