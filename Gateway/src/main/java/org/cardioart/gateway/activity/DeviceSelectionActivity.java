package org.cardioart.gateway.activity;

import android.app.DialogFragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
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

import com.rbnb.sapi.Source;

import org.cardioart.gateway.R;
import org.cardioart.gateway.api.helper.bluetooth.BluetoothScanHelper;
import org.cardioart.gateway.fragment.DeviceModeDialogFragment;

import static org.cardioart.gateway.activity.DeviceSelectionActivity.OSDT_PORT;

public class DeviceSelectionActivity extends ActionBarActivity {
    private static final String TAG = "gateway";
    public static final String PREF_NAME = "BtG_Pref";
    public static final String OSDT_PORT = "3333";
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
        final String simNumber = telephonyManager.getLine1Number();

        // Set DeviceId using SimNumber and SimSerialNumber
        TextView textViewDeviceId = (TextView) findViewById(R.id.textViewDeviceID);
        textViewDeviceId.setText(simNumber);

        // load preferences from device
        SharedPreferences settings = getSharedPreferences(PREF_NAME, 0);
        String patientId = settings.getString("patientID", "1010-demo");
        final String serverIp = settings.getString("serverIP", "128.199.136.68");
        // Set Default PatientID
        final EditText editTextPatientId = (EditText) findViewById(R.id.editTextPatientID);
        editTextPatientId.setText(patientId);
        // Set Validation event when user edited PatientID
        editTextPatientId.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (!hasFocus) {
                    //validate patent id
                    String patientId = editTextPatientId.getText().toString();
                    if (patientId.matches("[\\w\\d\\-_]+")) {
                        editTextPatientId.setTextColor(Color.parseColor("#33DD00"));
                    } else {
                        editTextPatientId.setTextColor(Color.RED);
                    }
                }
            }
        });
        // Set Default ServerIP
        final EditText editTextServerIp = (EditText) findViewById(R.id.editTextServerIP);
        editTextServerIp.setText(serverIp);
        // Set Click event for Ping Server Button
        final Button pingServerButton = (Button) findViewById(R.id.buttonPingServer);
        pingServerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    Source source = new Source(2048, "none", 2048);
                    source.CloseRBNBConnection();
                    source.OpenRBNBConnection(serverIp + ":" + OSDT_PORT, simNumber);
                    if (source.VerifyConnection()) {
                        Toast.makeText(DeviceSelectionActivity.this, "Hello from " + source.GetServerName(), Toast.LENGTH_LONG).show();
                        editTextServerIp.setTextColor(Color.parseColor("#33DD00"));
                    } else {
                        editTextServerIp.setTextColor(Color.RED);
                    }
                } catch (Exception exception) {
                    pingServerButton.setText("Error");
                    Toast.makeText(DeviceSelectionActivity.this, exception.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e(TAG, exception.getMessage());
                }
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_gps) {
            // go to GPS test mode activity
            final EditText editText = (EditText) findViewById(R.id.editTextServerIP);
            String serverIp = editText.getText().toString();
            Intent intent = new Intent(this, GPSActivity.class);
            intent.putExtra("server_ip", serverIp + ":" + OSDT_PORT);
            startActivity(intent);
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
