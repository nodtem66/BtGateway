package org.cardioart.gateway.api.helper.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

/**
 * Created by jirawat on 27/10/2014.
 */
public abstract class BluetoothConnection {
    public enum CommState {
        NONE,       // doing nothing
        LISTEN,     // used in server mode; listening for incoming connection
        CONNECTING, // initiating an outgoing connection
        CONNECTED,  // connected to a remote device
        DISCONNECTED; // used in server mode; when the client is disconnected

        public static CommState fromInt(int x) {
            switch(x) {
                case 0:
                    return NONE;
                case 1:
                    return LISTEN;
                case 2:
                    return CONNECTING;
                case 3:
                    return CONNECTED;
                case 4:
                    return DISCONNECTED;
            }
            return null;
        }
    }
    abstract public void start();
    abstract public void stop();
    abstract public void connect(BluetoothDevice device);
    abstract public void connected(BluetoothSocket socket, BluetoothDevice device);
    abstract public long getRxSpeed();
    abstract public long getTxSpeed();
}
