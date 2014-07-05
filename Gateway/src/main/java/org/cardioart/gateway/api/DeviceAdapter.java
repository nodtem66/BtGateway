package org.cardioart.gateway.api;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.cardioart.gateway.R;

import java.util.ArrayList;

/**
 * Created by jirawat on 10/05/2014.
 */
public class DeviceAdapter extends ArrayAdapter<String> {
    private static class ViewHolder {
        TextView textName;
    }
    public DeviceAdapter(Context context, ArrayList<String> names) {
        super(context, R.layout.device_list, names);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        // get device name from position
        String device_name = getItem(position);
        ViewHolder viewHolder;

        // find resource id
        if (convertView == null) {
            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.device_list, parent, false);
            viewHolder.textName = (TextView) convertView.findViewById(R.id.textViewDeviceName);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        // set TextString
        viewHolder.textName.setText(device_name);
        return convertView;
    }
}
