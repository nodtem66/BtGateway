package org.cardioart.gateway.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.res.TypedArrayUtils;
import android.widget.Toast;

import org.cardioart.gateway.R;
import org.cardioart.gateway.activity.GatewayActivity;
import org.cardioart.gateway.activity.GraphActivity;

import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;

public class DeviceModeDialogFragment extends DialogFragment {

    private String deviceName;
    private String deviceAddress;
    private boolean[] mShowDeviceModeMenu;
    private Context context;

    public static DeviceModeDialogFragment newInstance(String deviceName, String deviceAddress, boolean[] showDeviceModeMenu) {
        DeviceModeDialogFragment fragment = new DeviceModeDialogFragment();
        Bundle args = new Bundle();
        args.putString("device_name", deviceName);
        args.putString("device_address", deviceAddress);
        args.putBooleanArray("enable_menu", showDeviceModeMenu);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity.getApplicationContext();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        deviceName = getArguments().getString("device_name");
        deviceAddress = getArguments().getString("device_address");
        mShowDeviceModeMenu = getArguments().getBooleanArray("enable_menu");
        context = getActivity().getApplicationContext();
        String[] menuString = getResources().getStringArray(R.array.device_mode_array);
        ArrayList<String> bufferMenuString = new ArrayList<String>();
        for (int i=0; i < mShowDeviceModeMenu.length; i++) {
            if (mShowDeviceModeMenu[i]) {
                bufferMenuString.add(menuString[i]);
            }
        }
        builder.setTitle("Select Mode")
               .setItems(bufferMenuString.toArray(new CharSequence[bufferMenuString.size()]), new DialogInterface.OnClickListener() {
                   @Override
                   public void onClick(DialogInterface dialogInterface, int i) {
                       if (mShowDeviceModeMenu[i]) {
                           openActivityFromChoice(i);
                       } else {
                           Toast.makeText(context, "Cannot select this mode", Toast.LENGTH_LONG).show();
                       }
                   }
               });
        return builder.create();
    }

    public void openActivityFromChoice(int i) {
        Toast.makeText(
                context,
                String.format("select %s",deviceAddress),
                Toast.LENGTH_LONG).show();
        Class<?> classActivity;
        switch (i) {
            case 0:
                classActivity = GraphActivity.class;
                break;
            case 1:
                classActivity = GatewayActivity.class;
                break;
            default:
                return;
        }
        Intent intent = new Intent(context, classActivity);
        intent.putExtra("device_name", deviceName);
        intent.putExtra("device_address", deviceAddress);
        startActivity(intent);
    }
}
