package com.example.jwo.ble_central;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by jwo on 29.06.17.
 */

public class BLEService extends Service {

    private BluetoothAdapter btAdapter;
    private BluetoothManager btManager;
    private BluetoothLeScanner btScanner;
    private ScanSettings settings;
    private List<ScanFilter> filters;
    private ArrayList<BluetoothGatt> gattList;
    private HashMap<BluetoothGatt, BluetoothGattCharacteristic> alarmMap;
    private BluetoothGatt mGatt;
    private ArrayList<BluetoothDevice> deviceList;
    private static final String TAG = "bleCentral: BLEService:";
    private final IBinder mBinder = new LocalBinder();
    private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_DEVICE_FOUND =
            "com.example.bluetooth.le.ACTION_DEVICE_FOUND";
    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_ALARM_TRIGGERED =
            "com.example.bluetooth.le.ACTION_ALARM_TRIGGERED";

    public final static String ALARM_VALUE = "ALARM_VALUE";


    public static final UUID ALARM_SERVICE_UUID = UUID.fromString("FF890198-9446-4E3A-B173-4E1161D6F59B");
    public static final UUID ALARM_CHARACTERISTIC_UUID = UUID.fromString("77B4350C-DEA3-4DBA-B650-251670F4B2B4");

    public boolean initialize() {
        Log.i(TAG, "Constructor()");
        filters = new ArrayList<ScanFilter>();
        deviceList = new ArrayList<BluetoothDevice>();
        gattList = new ArrayList<BluetoothGatt>();
        alarmMap = new HashMap<BluetoothGatt, BluetoothGattCharacteristic>();

        if(btManager == null) {
            btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

            if (btManager == null) {
                Log.i(TAG, "Initialize(): Cant init BT Manager");
                return false;
            }
        }

        btAdapter = btManager.getAdapter();
        if (btAdapter == null) {
            Log.i(TAG, "Initialize(): Cant init BT Adapter");
            return false;
        }

        return true;
    }

    public ArrayList<BluetoothGatt> getGattList() {
        return gattList;
    }
    public ArrayList<BluetoothDevice> getDeviceList() {
        return deviceList;
    }

    public void scanForDevices(final boolean enable) {
        Log.i(TAG, "scanForDevices()");
        btScanner = btAdapter.getBluetoothLeScanner();
        settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                .build();

        filters = Arrays.asList(
                new ScanFilter.Builder()
                        .build());

        if(enable) {
            btScanner.startScan(filters, settings, scanCallback);
        } else {
            btScanner.stopScan(scanCallback);
        }
    }

    private ScanCallback scanCallback = new ScanCallback() {

        @Override
        public void onScanResult(int callbackType, ScanResult result) {

            String intentAction;
            boolean newDevice = true;
            BluetoothDevice device = btAdapter.getRemoteDevice(result.getDevice().getAddress());

            for(BluetoothDevice d : deviceList) {
                if(d.getAddress().equals(device.getAddress())) {
                    Log.i(TAG, "onScanResult(): device already scanned");
                    newDevice = false;
                } else {
                    Log.i(TAG, "onScanResult(): found new device");
                    newDevice = true;
                }
            }

            if(newDevice) {
                deviceList.add(device);
                intentAction = ACTION_DEVICE_FOUND;
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult sr : results) {
                Log.i(TAG, "onBatchScanResults()");
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.i(TAG, "ScanCallback(): Scan Failed: Error Code: " + errorCode);
        }

    };

    public void connectGatt(int position) {
        BluetoothDevice selectedDevice = deviceList.get(position);
        mGatt = selectedDevice.connectGatt(this, false, gattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mConnectionState = STATE_CONNECTING;
    }

    public void disconnectGatt(int position) {
        for(BluetoothGatt g : gattList) {
            if(g.getDevice().equals(deviceList.get(position))) {
                g.disconnect();
            }
        }
    }

    public void clearList() {
        deviceList.clear();
    }


    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;

            Log.i("onConnectionStateChange", "Status: " + status);
            switch(newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    intentAction = ACTION_GATT_CONNECTED;
                    mConnectionState = STATE_CONNECTED;
                    broadcastUpdate(intentAction);
                    Log.i("gattCallback", "STATE_CONNECTED");
                    if(!gattList.contains(gatt)) {
                        gattList.add(gatt);
                    }
                    gatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    intentAction = ACTION_GATT_DISCONNECTED;
                    mConnectionState = STATE_DISCONNECTED;
                    gattList.remove(gatt);
                    broadcastUpdate(intentAction);
                    Log.i("gattCallback", "STATE_DISCONNECTED");
                    break;
                default:
                    Log.i("gattCallback", "STATE_OTHER");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if(status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "Services discovered");
                BluetoothGattCharacteristic alarm;
                List<BluetoothGattService> services = gatt.getServices();
                alarm = gatt.getService(ALARM_SERVICE_UUID).getCharacteristic(ALARM_CHARACTERISTIC_UUID);
                alarmMap.put(gatt, alarm);

                for (BluetoothGattDescriptor descriptor : alarm.getDescriptors()) {
                    //descriptor.setValue( BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                    gatt.writeDescriptor(descriptor);
                }
                gatt.setCharacteristicNotification(alarm, true);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.i("onCharacteristicRead", characteristic.getValue().toString());
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {

            broadcastUpdate(ACTION_ALARM_TRIGGERED, characteristic);
        }
    };

    public void setAlarm(boolean b, int position) {
        BluetoothGatt selectedGatt = null;

        for(BluetoothGatt g : gattList) {
            if(g.getDevice().equals(deviceList.get(position))) {
                selectedGatt = g;
            }
        }

        byte[] value = new byte[4];

        if(b) {
            value[0] = (byte) (01 & 0xFF);
        } else {
            value[0] = (byte) (00 & 0xFF);
        }

        if(selectedGatt != null) {
            BluetoothGattCharacteristic alarm = alarmMap.get(selectedGatt);
            alarm.setValue(value);
            boolean stat = selectedGatt.writeCharacteristic(alarm);

            if(stat) {
                Log.i("WriteCharStatus", "true");
            } else {
                Log.i("WriteCharStatus", "false");
            }
        }
    }


    public void close() {

        for(BluetoothGatt gatt : gattList) {
            gatt.close();
            gatt = null;
        }
        gattList.clear();
    }


    /*
        BINDER
     */

    public class LocalBinder extends Binder {
        BLEService getService() {
            return BLEService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }


    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        // This is special handling for the Heart Rate Measurement profile.  Data parsing is
        // carried out as per profile specifications:
        // http://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml
        if (ALARM_CHARACTERISTIC_UUID.equals(characteristic.getUuid())) {
            int flag = characteristic.getProperties();
            int format = -1;
            if ((flag & 0x01) != 0) {
                format = BluetoothGattCharacteristic.FORMAT_UINT16;
                Log.d(TAG, "Alarm format UINT16.");
            } else {
                format = BluetoothGattCharacteristic.FORMAT_UINT8;
                Log.d(TAG, "Alarm format UINT8.");
            }
            final int alarm = characteristic.getIntValue(format, 0);
            Log.d(TAG, String.format("Received Alarm: %d", alarm));

            intent.putExtra(ALARM_VALUE, String.valueOf(alarm));
            sendBroadcast(intent);
        }
    }
}
