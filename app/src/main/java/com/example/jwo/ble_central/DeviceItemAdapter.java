package com.example.jwo.ble_central;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.ClipData;
import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.ArrayList;

/**
 * Created by jwo on 01.07.17.
 */

public class DeviceItemAdapter extends ArrayAdapter {
    private ArrayList<BluetoothDevice> devices;
    private Context context;
    private boolean changedState;
    private States state;
    private ViewHolder mViewHolder;
    private LayoutInflater mInflater;
    private static final String TAG = "DeviceItemAdapter:";
    public enum States {
        STANDARD,
        CONNECTED,
        ALARM
    }

    public final static String ACTION_CONNECT_CLICKED =
            "com.example.bluetooth.le.ACTION_CONNECT_CLICKED";

    public final static String ACTION_DISCONNECT_CLICKED =
            "com.example.bluetooth.le.ACTION_DISCONNECT_CLICKED";

    public DeviceItemAdapter(Context context, ArrayList<BluetoothDevice> devices) {
        super(context, 0, devices);
        this.context = context;
        this.devices = devices;
        state = States.STANDARD;
        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        changedState = false;
    }

    public void addItem(BluetoothDevice device) {
        devices.add(device);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        // TODO Auto-generated method stub
        return devices.size();
    }

    @Override
    public BluetoothDevice getItem(int position) {
        // TODO Auto-generated method stub
        return devices.get(position);
    }

    @Override
    public long getItemId(int position) {
        // TODO Auto-generated method stub
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            // Inflate your view
            convertView = mInflater.inflate(R.layout.device, parent, false);
            mViewHolder = new ViewHolder();
            mViewHolder.deviceItem      = (LinearLayout) convertView.findViewById(R.id.deviceItem);
            mViewHolder.deviceName      = (TextView) convertView.findViewById(R.id.txtDeviceName);
            mViewHolder.deviceAdress    = (TextView) convertView.findViewById(R.id.txtDeviceAdress);
            mViewHolder.btnConnect      = (Button) convertView.findViewById(R.id.btnConnect);

            BluetoothDevice d = getItem(position);
            Log.i(TAG, d.getName());
            mViewHolder.deviceName.setText(d.getName());
            mViewHolder.deviceAdress.setText(d.getAddress());

            mViewHolder.btnConnect.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (state.equals(States.STANDARD)) {
                        ((MainActivity)context).connectDevice(position);
                    } else if (state.equals(States.CONNECTED) || state.equals(States.ALARM)) {
                        ((MainActivity)context).disconnectDevice(position);
                    }
                }
            });

            convertView.setTag(mViewHolder);
        } else {
            mViewHolder = (ViewHolder) convertView.getTag();
        }


        switch(state) {
            case STANDARD:
                mViewHolder.deviceItem.setBackground(ContextCompat.getDrawable(context,R.drawable.devicelayout_bg));
                mViewHolder.btnConnect.setText(R.string.connect);
                break;

            case CONNECTED:
                mViewHolder.deviceItem.setBackground(ContextCompat.getDrawable(context,R.drawable.devicelayout_connected_bg));
                mViewHolder.btnConnect.setText(R.string.disconnect);
                break;

            case ALARM:
                mViewHolder.deviceItem.setBackground(ContextCompat.getDrawable(context,R.drawable.devicelayout_alarm_bg));
                break;
            default:

        }
        mViewHolder.deviceItem.invalidate();

        // Return the completed view to render on screen
        return convertView;
    }

    public void setState(States state) {
        this.state = state;
    }

    private static class ViewHolder {
        LinearLayout deviceItem;
        TextView deviceName;
        TextView deviceAdress;
        Button btnConnect;
    }
}