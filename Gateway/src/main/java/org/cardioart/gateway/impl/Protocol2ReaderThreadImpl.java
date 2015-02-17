package org.cardioart.gateway.impl;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.cardioart.gateway.api.PacketReader;
import org.cardioart.gateway.api.constant.MyEvent;
import org.cardioart.gateway.api.thread.PacketReaderThread;
import org.cardioart.gateway.impl.reader.Protocol2PacketReader;

import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by jirawat on 09/09/2014.
 */
public class Protocol2ReaderThreadImpl extends PacketReaderThread {

    private static final String TAG = "streamgraph";
    private final int LIMIT = 30;
    private final int MAX_CHANNEL = 10;
    private final Handler mainHandler;
    private static final Lock mLock = new ReentrantLock();
    private static final Object mChannelLock = new Object();
    private boolean canChannelRead = false;
    private static final Condition notEmpty = mLock.newCondition();
    private static final Condition notFull = mLock.newCondition();

    private BlockingQueue<byte[]> blockingQueue = new LinkedBlockingDeque<byte[]>(LIMIT);
    private ArrayList<ArrayList<Integer>> arrayChannel = new ArrayList<ArrayList<Integer>>(MAX_CHANNEL);

    private final PacketReader packetReader;

    public Protocol2ReaderThreadImpl(Handler mHandler) {
        Log.d(TAG, "BEGIN PacketReaderThread");
        mainHandler = mHandler;
        for (int i=0; i < MAX_CHANNEL; i++) {
            arrayChannel.add(new ArrayList<Integer>());
        }
        packetReader = new Protocol2PacketReader(MAX_CHANNEL, mChannelLock, arrayChannel);
    }
    @Override
    public void run() {
        Looper.prepare();
        mLock.lock();
        try {
            mainHandler.obtainMessage(MyEvent.STATE_PACKETREADER_THREAD_START).sendToTarget();
            while (!isInterrupted()) {
                while (blockingQueue.size() == 0) {
                    notEmpty.await();
                }
                readByte(blockingQueue.take());
                notFull.signal();
            }
        } catch (Exception e) {
            Log.d(TAG, "EXP: " + e.getLocalizedMessage());
        } finally {
            Log.d(TAG, "END PacketReaderThread");
            mLock.unlock();
            mainHandler.obtainMessage(MyEvent.STATE_PACKETREADER_THREAD_STOP).sendToTarget();
        }
    }

    private void readByte(byte[] data) {
        packetReader.readByte(data);
    }
    public Integer[] getChannel(int index) {
        Integer[] result;
        if (index >= 0 && index < MAX_CHANNEL) {
            synchronized (mChannelLock) {
                int length = arrayChannel.get(index).size();
                result = arrayChannel.get(index).toArray(new Integer[length]);
                //TODO: may fix this
                for (int i=0; i< MAX_CHANNEL; i++) {
                    arrayChannel.get(i).clear();
                }
            }
        } else {
            result = new Integer[0];
        }
        return result;
    }
    public void readPacket(byte[] data) {
        mLock.lock();
        try {
            while (blockingQueue.remainingCapacity() == 0) {
                notFull.await();
            }
            blockingQueue.put(data);
            notEmpty.signal();
        } catch (Exception e) {
            Log.d(TAG, "EXP: " + e.getLocalizedMessage());
        } finally {
            mLock.unlock();
        }
    }
    public long getTotalByteRead() {
        return packetReader.getTotalByteRead();
    }
}
