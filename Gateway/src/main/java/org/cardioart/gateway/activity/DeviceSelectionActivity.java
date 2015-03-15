package org.cardioart.gateway.activity;

import android.app.DialogFragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.cardioart.gateway.R;
import org.cardioart.gateway.api.helper.bluetooth.BluetoothScanHelper;
import org.cardioart.gateway.fragment.DeviceModeDialogFragment;

public class DeviceSelectionActivity extends ActionBarActivity {
    private static final String TAG = "gateway";
    public static final String PREF_NAME = "BtG_Pref";
    public BluetoothScanHelper bluetoothScanHelper;
    private ArrayAdapter<String> adapterPaired;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);
        bluetoothScanHelper = new BluetoothScanHelper(this);
        bluetoothScanHelper.setTextViewStatus((Button) findViewById(R.id.buttonSearch));
        bluetoothScanHelper.enableBluetooth();

        //ListView listViewDetected = (ListView) findViewById(R.id.listViewDetected);
        ListView listViewPaired = (ListView) findViewById(R.id.listViewPaired);
        Button buttonSearch = (Button) findViewById(R.id.buttonSearch);

        //adapterDetected = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        //listViewDetected.setAdapter(adapterDetected);
        //bluetoothScanHelper.setDetectedAdapter(adapterDetected);
        adapterPaired = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        listViewPaired.setAdapter(adapterPaired);
        bluetoothScanHelper.setPairedAdapter(adapterPaired);

        listViewPaired.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String device_name = (String) adapterView.getItemAtPosition(i);
                String device_address = (String) bluetoothScanHelper.getDeviceAddressFromName(device_name);
                DialogFragment dialogFragment = DeviceModeDialogFragment.newInstance(device_name, device_address);
                dialogFragment.show(getFragmentManager(), "devicemode");
                /*
                Toast.makeText(
                        getApplicationContext(),
                        bluetoothScanHelper.getDeviceAddressFromName(device_name) + " select",
                        Toast.LENGTH_SHORT
                ).show();
                Intent graphIntent = new Intent(getApplicationContext(), GraphActivity.class);
                graphIntent.putExtra("device_name", device_name);
                graphIntent.putExtra("device_address", device_address);
                startActivity(graphIntent);
                */
            }
        });

        buttonSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bluetoothScanHelper.searchDevice();
            }
        });

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(bluetoothScanHelper.getReceiver(), filter);
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(bluetoothScanHelper.getReceiver(), filter);

        bluetoothScanHelper.searchDevice();

        // Get Phone number from SIM CARD
        // this can view manually by going to setting > phone > sim card manager
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        String simNumber = telephonyManager.getLine1Number();

        // Set DeviceId using SimNumber and SimSerialNumber
        TextView textViewDeviceId = (TextView) findViewById(R.id.textViewDeviceID);
        textViewDeviceId.setText(simNumber);

        // load preferences from device
        SharedPreferences settings = getSharedPreferences(PREF_NAME, 0);
        String patientId = settings.getString("patientID", "1010-demo");
        // Set Default PatientID
        EditText editTextPatientId = (EditText) findViewById(R.id.editTextPatientID);
        editTextPatientId.setText(patientId);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Toast.makeText(this, "Setting", Toast.LENGTH_LONG).show();
            Log.d(TAG, "Setting");
            return true;
        } else if (id == R.id.action_refresh) {
            bluetoothScanHelper.searchDevice();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == BluetoothScanHelper.REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_CANCELED) {
                System.exit(2);
            }
        }
    }

    @Override
    protected void onDestroy() {
        this.unregisterReceiver(bluetoothScanHelper.getReceiver());
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        super.onStop();

        // load local settings from textview
        EditText editTextPatientId = (EditText) findViewById(R.id.editTextPatientID);
        EditText editTextServerIp = (EditText) findViewById(R.id.editTextServerIP);
        String patientId = editTextPatientId.getText().toString();
        String serverIp = editTextServerIp.getText().toString();

        // save preferences to settings file
        SharedPreferences settings = getSharedPreferences(PREF_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        if (!patientId.isEmpty()) {
            editor.putString("patientID", patientId);
        }
        if (!serverIp.isEmpty()) {
            editor.putString("serverIP", serverIp);
        }
        editor.commit();
    }
}
