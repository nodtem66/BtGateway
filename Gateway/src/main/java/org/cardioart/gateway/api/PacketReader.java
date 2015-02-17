package org.cardioart.gateway.api;

import java.util.ArrayList;

/**
 * Created by jirawat on 30/01/2015.
 */
public abstract class PacketReader {
    public static final int STATE_START = 0;
    public static final int STATE_HEADER_0 = 1;
    public static final int STATE_HEADER_COUNTER = 2;
    public static final int STATE_HEADER_1 = 2;
    public static final int STATE_HEADER_2 = 3;
    public static final int STATE_STATUS_0 = 4;
    public static final int STATE_STATUS_1 = 5;
    public static final int STATE_STATUS_2 = 6;
    public static final int STATE_CH1_0 = 7;
    public static final int STATE_CH1_1 = 8;
    public static final int STATE_CH1_2 = 9;
    public static final int STATE_END = 31;

    abstract public void readByte(byte[] data);
    abstract public void setDataStorage(ArrayList<? extends ArrayList> arrayList);
    abstract public void setChannelLock(Object lock);
    abstract public long getTotalByteRead();
}
