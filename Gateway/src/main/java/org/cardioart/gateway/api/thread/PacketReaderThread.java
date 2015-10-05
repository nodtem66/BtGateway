package org.cardioart.gateway.api.thread;

/**
 * Created by jirawat on 18/09/2014.
 */
public abstract class PacketReaderThread extends Thread implements Runnable{
    abstract public Integer[] getChannel(int index);
    abstract public void readPacket(byte[] data);
    abstract public void clearChannel(int index);
    abstract public void clearAllChannel();
    abstract public long getTotalByteRead();
}
