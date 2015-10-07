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
import org.cardioart.gateway.activity.DeviceSelectionActivity;
import org.cardioart.gateway.activity.GatewayActivity;
import org.cardioart.gateway.activity.GraphActivity;

import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;

public class DeviceModeDialogFragment extends DialogFragment {

    private boolean[] mShowDeviceModeMenu;
    private String mDeviceName;
    private Context context;

    public static DeviceModeDialogFragment newInstance(String deviceName, boolean[] showDeviceModeMenu) {
        DeviceModeDialogFragment fragment = new DeviceModeDialogFragment();
        Bundle args = new Bundle();
        args.putString("device_name", deviceName);
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
        context = getActivity().getApplicationContext();
        mDeviceName = getArguments().getString("device_name");
        builder.setTitle("Select Mode")
               .setItems(R.array.device_mode_array, new DialogInterface.OnClickListener() {
                   @Override
                   public void onClick(DialogInterface dialogInterface, int i) {
                       openActivityFromChoice(i);
                   }
               });
        return builder.create();
    }

    public void openActivityFromChoice(int i) {
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
        DeviceSelectionActivity activity = (DeviceSelectionActivity) getActivity();
        activity.onModeSelected(classActivity, mDeviceName);
        this.dismiss();
    }
}
