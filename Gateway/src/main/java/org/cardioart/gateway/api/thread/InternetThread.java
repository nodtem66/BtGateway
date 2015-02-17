package org.cardioart.gateway.api.thread;

import org.cardioart.gateway.api.constant.MyMessage;

/**
 * Created by jirawat on 06/07/2014.
 */
public interface InternetThread extends Runnable{
    static final byte STATE_OK = 0;
    long getByteSend();
    void cancel();
    void start();
    void interrupt();
    boolean isNotFull();
    boolean isEmpty();
    boolean sendMyMessage(MyMessage message);
    boolean sendMyMessage(byte[] message);
    void setSecTimestamp(double timestamp);
}
