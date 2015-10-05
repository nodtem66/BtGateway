package org.cardioart.gateway.impl;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import org.cardioart.gateway.api.helper.bluetooth.BluetoothConnection;
import org.cardioart.gateway.api.constant.MyEvent;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Created by jirawat on 27/10/2014.
 */
public class BluetoothCommClient extends BluetoothConnection {

    // Debuging
    private static final UUID MY_UUID       = UUID.fromString("513a6c9c-ea27-4abd-90c0-6997dd532866");
    private static final String TAG         = "gateway";
    private static final boolean DEBUG      = true;

    // Member field
    private Handler mainHandler;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private BluetoothAdapter mAdapter;
    private CommState commState;
    private long rxByte = 0;
    private long txByte = 0;

    // unused constant
    private boolean allowInsecureConnections = true;

    /**
     * Contructor. prepare a new client for bluetooth session
     * @param handler The main UI handler
     */
    public  BluetoothCommClient(Handler handler) {
        mainHandler = handler;
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        commState = CommState.NONE;
    }

    /**
     * Set state of connection
     * @param state
     */
    private synchronized void setState(CommState state) {
        if (DEBUG) Log.d(TAG, "setState()" + commState + " -> " + state);
        commState = state;
    }

    /**
     * return local state
     * NONE: doing nothing (disconect)
     * LISTEN: listening for incoming connection
     * CONNECTING: initiating an outgoing connection
     * CONNECTED: connected to a remote device
     * @return local state of bluetooth connection
     */
    public synchronized CommState getState() {
        return commState;
    }

    /**
     * Start connecting thread
     */
    @Override
    public void start() {
        if (DEBUG) Log.d(TAG, "start()");

        // cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        setState(CommState.NONE);
    }

    @Override
    public synchronized void connect(BluetoothDevice device) {
        if (DEBUG) Log.d(TAG, "connect to: " + device.getName());

        // cancel any thread attempting to make a connection
        if (commState == CommState.CONNECTING) {
            if (mConnectThread != null) {mConnectThread.cancel();mConnectThread = null;}
        }
        // cancel any thread running a connnection
        if (mConnectedThread != null) {mConnectedThread.cancel();mConnectedThread = null;}

        // start a thread to connect with input device
        mConnectThread = new ConnectThread(this, device);
        mConnectThread.start();
        setState(commState.CONNECTING);
    }

    @Override
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        if (DEBUG) Log.d(TAG, "connected");

        // cancel the thread that attempting connection the device
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // cancel previous thread that running the connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // start a new thread handling the connection and data transmission
        mConnectedThread = new ConnectedThread(this, mainHandler, socket);
        mConnectedThread.start();

        // TODO: send the name of connected device to main ui
        mainHandler.obtainMessage(MyEvent.STATE_BT_RX_UP);

        setState(CommState.CONNECTED);
    }

    /**
     * stop all thread
     */
    @Override
    public synchronized void stop() {
        if (DEBUG) Log.d(TAG, "stop");


        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        mainHandler.obtainMessage(MyEvent.STATE_BT_RX_DOWN);
        setState(CommState.NONE);
    }

    /**
     * set the internal length of incoming buffer in bytes
     * used in main UI to display speed of incoming data (RX Speed)
     */
    public synchronized  void increaseRxByte(long bytes) {
        rxByte += bytes;
    }

    /**
     * get the total incoming message in bytes
     * @return number of byte
     */
    public synchronized long getRxSpeed() {
        long buffer = rxByte;
        rxByte = 0;
        return buffer;
    }

    /**
     * get the total outgoing message in bytes
     * @return number of byte
     */
    public synchronized long getTxSpeed() {
        return 0;
    }

    private void connectionFailed() {
        if (DEBUG) Log.d(TAG, "connection failed");
        mainHandler.obtainMessage(MyEvent.STATE_BT_RX_DOWN);
        setState(CommState.NONE);
    }

    private void connectionLost() {
        if (DEBUG) Log.d(TAG, "connection lost");
        mainHandler.obtainMessage(MyEvent.STATE_BT_RX_DOWN);
        setState(CommState.NONE);
    }

    /**
     * Thread handles to make a connection to bluetooth device
     * then report a success or fail
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        // for garbage collection help; see more: Efficient Android Thread textbook
        private WeakReference<BluetoothCommClient> commClient;

        public ConnectThread(BluetoothCommClient self, BluetoothDevice device) {
            if (DEBUG) Log.d(TAG, "create ConnectThread()");
            commClient = new WeakReference<BluetoothCommClient>(self);
            mmDevice = device;
            BluetoothSocket tmp = null;

            // get blueooth socket from device
            try {
                if ( allowInsecureConnections ) {
                    Method method;
                    method = device.getClass().getMethod("createRfcommSocket", new Class[] {int.class});
                    tmp = (BluetoothSocket) method.invoke(device, 1);
                } else {
                    tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
                }
            } catch (Exception e) {
                Log.e(TAG, "create() failed", e);
            }
            mmSocket = tmp;
        }
        public void run() {
            Log.i(TAG, "BEGIN connectThread");
            setName("ConnectThread");

            // always cancel discovery because it will slowdown a connection
            commClient.get().mAdapter.cancelDiscovery();

            // make a connection to the bluetooth socket
            try {
                // a blocking call and will return only success or exception
                mmSocket.connect();
            } catch (IOException e) {
                // report connection failed to main ui
                commClient.get().connectionFailed();

                // close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
                return;
            }

            // clear this thread
            synchronized (BluetoothCommClient.this) {
                commClient.get().mConnectThread = null;
            }

            // start connected thread
            commClient.get().connected(mmSocket, mmDevice);
        }
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "unable to close() socket during cancel() thread", e);
            }
        }
    }

    /**
     * This thread handles incoming message from bluetooth device
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mSocket;
        private final InputStream mInputStream;

        // for garbage collection help; see more: Efficient Android Thread textbook
        private WeakReference<Handler> mainHandler;
        private WeakReference<BluetoothCommClient> commClient;

        public ConnectedThread(BluetoothCommClient self, Handler handler, BluetoothSocket socket) {

            if (DEBUG) Log.d(TAG, "create ConnectedThread()");
            mSocket = socket;
            mainHandler = new WeakReference<Handler>(handler);
            commClient = new WeakReference<BluetoothCommClient>(self);
            InputStream tmpIn = null;

            // try to get input stream from socket
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "unable to get input steam from socket", e);
            }

            mInputStream = tmpIn;
        }

        public void run() {
            Log.i(TAG, "BEGIN ConnetedThread");
            byte[] buffer = new byte[2048];
            int bytes;

            // keep listening the data stream while connected
            while(true) {
                try {
                    // read from the input stream
                    bytes = mInputStream.read(buffer);

                    // send buffer to main ui
                    mainHandler.get().obtainMessage(MyEvent.STATE_PACKETREADER_THREAD_MSG, buffer).sendToTarget();
                    // increase size of total incoming data
                    commClient.get().increaseRxByte(bytes);
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    commClient.get().connectionLost();
                    break;
                }
            }
        }

        public void cancel() {
            try {
                mSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "unable to close socket", e);
            }
        }
    }
}
