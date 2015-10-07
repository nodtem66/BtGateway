package org.cardioart.gateway.api.thread;

import org.cardioart.gateway.api.constant.MyMessage;

/**
 * Created by jirawat on 06/07/2014.
 */
public interface InternetThread extends Runnable{
    static final int STATE_OK = 0;

    long getByteSend();
    void cancel();
    void start();
    void sendMessage(byte[] message);
    void setSecTimestamp(double timestamp);
}
