package org.cardioart.gateway.impl.reader;

import android.util.Log;

import org.cardioart.gateway.api.PacketReader;

import java.util.ArrayList;

/**
 * Created by jirawat on 30/01/2015.
 * This protocol from P' BAS
 * Format in byte
 * 0x01 0x01 COUNTER 0x01 XX XX XX CH1 CH1 CH1 CH2 CH2 CH2 ... CH8 CH8 CH8 0x04
 *   COUNTER: incremental flag
 *   CH1: 24-bit value of ECG channel 1 from ADC
 *   CHN: 24-bit value of ECG channel N from ADC
 *   XX: not consider
 * Total 32 byte
 */
public class  Protocol2PacketReader extends PacketReader {

    private int state;
    private int channel;
    private int channel_offset;
    private Integer byteTemp;
    private long totalByteRead = 0;

    private static final String TAG = "p2reader";
    private final int MAX_CHANNEL;
    private ArrayList<? extends ArrayList> arrayChannel;
    private Object mChannelLock;

    public Protocol2PacketReader(int maxChannel, Object lock, ArrayList<? extends ArrayList> arrayList) {
        this(maxChannel);
        setChannelLock(lock);
        setDataStorage(arrayList);
    }
    public Protocol2PacketReader(int maxChannel, Object lock) {
        this(maxChannel);
        setChannelLock(lock);
    }
    public Protocol2PacketReader(int maxChannel) {
        state = STATE_START;
        channel = 0;
        channel_offset = 16;
        byteTemp = 0;
        MAX_CHANNEL = maxChannel;
    }

    @Override
    public void readByte(byte[] data) {
        int length = data.length;

        synchronized (this) {
            totalByteRead += length;
        }

        //Parse data 3byte (8group) length
        for (int i = 0; i < length; i++) {
            //Check Header Protocol
            if (state == STATE_END && data[i] != 0x04) {continue;}
            else if (state == STATE_START && data[i] != 0x01) {continue;}
            else if (state == STATE_HEADER_0 && data[i] != 0x01) {continue;}
            else if (state == STATE_HEADER_2 && data[i] != 0x01) {continue;}

            //Skip status byte (3byte)
            if (state == STATE_END) {
                state = STATE_START;
                continue;
            } else if (state == STATE_STATUS_2) {
                channel_offset = 16;
                byteTemp = 0;
                channel = 0;
            } else if (state > STATE_STATUS_2) {
                if (channel_offset == 16) {
                    byteTemp = (data[i] & 0xFF) << 16;
                    channel_offset = 8;
                } else if (channel_offset == 8) {
                    byteTemp += (data[i] & 0xFF) << 8;
                    channel_offset = 0;
                } else if (channel_offset == 0) {
                    byteTemp += (data[i] & 0xFF);
                    // Debug Message
                    /*
                    if (byteTemp < 0)
                    {
                        Log.d(TAG, String.format("byteTemp: %d (%d,%d,%d)",
                                byteTemp, data[i], data[i - 1], data[i - 2]));
                    }*/
                    synchronized (mChannelLock) {
                        if (channel >= 0 && channel < MAX_CHANNEL) {
                            arrayChannel.get(channel).add(byteTemp);
                        }
                    }
                    channel++;
                    channel_offset = 16;
                }
            }
            state++;
        }

    }

    @Override
    public void setDataStorage(ArrayList<? extends ArrayList> arrayList) {
        arrayChannel = arrayList;
    }

    @Override
    public void setChannelLock(Object lock) {
        mChannelLock = lock;
    }

    @Override
    public long getTotalByteRead() {
        long buffer = totalByteRead;
        synchronized (this) {
            totalByteRead = 0;
        }
        return buffer;
    }
}
