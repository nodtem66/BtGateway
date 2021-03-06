package org.cardioart.gateway.api.helper.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.cardioart.gateway.R;
import org.cardioart.gateway.activity.DeviceSelectionActivity;

import java.util.HashMap;
import java.util.Set;

/**
 * Created by jirawat on 10/05/2014.
 */
public class BluetoothScanHelper {
    public static final int REQUEST_ENABLE_BT = 2718;

    private BluetoothAdapter mBluetoothAdapter;
    private HashMap<String, String> foundDevices = new HashMap<String, String>();
    private Context context;
    private ArrayAdapter<String> PairedAdapter;
    private ArrayAdapter<String> DetectedAdapter;
    private Button textViewStatus;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice btDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (!foundDevices.containsKey(btDevice.getName())) {
                    if (btDevice.getBondState() != BluetoothDevice.BOND_BONDED) {
                        DetectedAdapter.add(btDevice.getName());
                        foundDevices.put(btDevice.getName(), btDevice.getAddress());
                        DetectedAdapter.notifyDataSetChanged();
                    }
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {

                mBluetoothAdapter.cancelDiscovery();
                if (foundDevices.isEmpty()) {
                    // No Device
                    Toast.makeText(context, "No Bluetooth Devices", Toast.LENGTH_LONG).show();
                    Log.d("BT", "No BT Devices");
                }
            }
        }
    };

    public BluetoothScanHelper(Context mcontext) {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.context = mcontext;
        if (mBluetoothAdapter == null) {
            Log.d("Bluetooth", "Device didn't support bluetooth");
            System.exit(2);
        }
    }
    public void enableBluetooth() {
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            DeviceSelectionActivity activity = (DeviceSelectionActivity) this.context;
            activity.startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BT);
        }
    }
    public void listPairedDevice() {
        PairedAdapter.clear();
        searchPairedDevice();
    }
    public void searchDevice() {
        mBluetoothAdapter.cancelDiscovery();
        mBluetoothAdapter.startDiscovery();
        //DetectedAdapter.clear();
        foundDevices.clear();
    }
    public void searchPairedDevice() {
        Set<BluetoothDevice> pairedDevice = mBluetoothAdapter.getBondedDevices();
        if (!pairedDevice.isEmpty()) {
            for (BluetoothDevice device : pairedDevice) {
                foundDevices.put(device.getName(), device.getAddress());
                PairedAdapter.add(device.getName());
            }
            PairedAdapter.notifyDataSetChanged();
        } else {
            Toast.makeText(context, "No Paired BT", Toast.LENGTH_SHORT).show();
        }
        Log.d("BT", "End search pair");
    }
    public BroadcastReceiver getReceiver() {
        return mReceiver;
    }
    public void setTextViewStatus(Button textViewStatus) {
        this.textViewStatus = textViewStatus;
    }
    public void setPairedAdapter(ArrayAdapter<String> adapter) {
        this.PairedAdapter = adapter;
    }
    public void setDetectedAdapter(ArrayAdapter<String> adapter) {
        this.DetectedAdapter = adapter;
    }
    public String getDeviceAddressFromName(String name) {
        if(foundDevices.containsKey(name)) {
            return foundDevices.get(name);
        }
        return null;
    }
    public BluetoothDevice getDeviceFromAddress(String address) {
        try {
            return mBluetoothAdapter.getRemoteDevice(address);
        } catch (Exception e) {
            return null;
        }
    }
    public BluetoothDevice getDeviceFromName(String name) {
        String address = getDeviceAddressFromName(name);
        return getDeviceFromAddress(address);
    }
}
