package com.example.jwo.ble_central;

import android.app.Activity;
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
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static android.R.attr.value;

public class MainActivity extends Activity implements View.OnClickListener, AdapterView.OnItemClickListener {

    private MockData mock;
    private boolean bMocking;
    private Button btnStartScan;
    private Button btnStopScan;
    private Button btnSetAlarm;
    private Button btnClearAlarm;
    private Button btnGetAlarm;
    private BluetoothAdapter btAdapter;
    private BluetoothManager btManager;
    private BluetoothLeScanner btScanner;
    private ScanSettings settings;
    private List<ScanFilter> filters;
    private BluetoothGatt gatt;
    private List<BluetoothDevice> deviceList;
    private ListView listView;
    private Context appContext;
    private int REQUEST_ENABLE_BT;
    private BluetoothGattService mAlarmService;
    private BluetoothGattCharacteristic mAlarmChar;
    private BluetoothGattDescriptor mAlarmWriteDescriptor;
    private BluetoothGattDescriptor mAlarmReadDescriptor;
    private static final String TAG = "BLE_CENTRAL";

    private static final UUID alarmUUid = UUID.fromString("FF890198-9446-4E3A-B173-4E1161D6F59B");
    private static final UUID charUUid = UUID.fromString("77B4350C-DEA3-4DBA-B650-251670F4B2B4");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        System.out.print("oncreate");
        Log.v(TAG, "onCreate");
        bMocking = false;
        mock = new MockData();
        appContext = this;
        listView = (ListView) findViewById(R.id.deviceList);
        listView.setOnItemClickListener(this);

        btnStartScan = (Button) findViewById(R.id.btnStartScan);
        btnStopScan = (Button) findViewById(R.id.btnStopScan);
        btnSetAlarm = (Button) findViewById(R.id.btnSetAlarm);
        btnClearAlarm = (Button) findViewById(R.id.btnClearAlarm);
        btnGetAlarm = (Button) findViewById(R.id.btnGetAlarm);

        btnStartScan.setOnClickListener(this);
        btnStopScan.setOnClickListener(this);
        btnSetAlarm.setOnClickListener(this);
        btnClearAlarm.setOnClickListener(this);
        btnGetAlarm.setOnClickListener(this);

        filters = new ArrayList<ScanFilter>();
        deviceList = new ArrayList<BluetoothDevice>();

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE not supported", Toast.LENGTH_SHORT).show();
            finish();
        }

        btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

        if(btManager == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            finish();
        }

        btAdapter = btManager.getAdapter();

        if (btAdapter != null || !btAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, REQUEST_ENABLE_BT);
        }

    }

    @Override
    public void onDestroy() {
        deviceList.clear();
        deviceList = null;
        super.onDestroy();
    }


    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.btnStartScan:
                Log.d("StartScan", "buttonClicked");
                scanForDevices(true);
                break;
            case R.id.btnStopScan:
                Log.d("StopScan", "buttonClicked");
                scanForDevices(false);
                break;
            case R.id.btnSetAlarm:
                Log.d("SetAlarm", "buttonClicked");
                setAlarm(true);
                break;
            case R.id.btnClearAlarm:
                Log.d("ClearAlarm", "buttonClicked");
                setAlarm(false);
                break;
            case R.id.btnGetAlarm:
                Log.d("ClearAlarm", "buttonClicked");
                getAlarm();
                break;
            default:
        }
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Log.i("OnItemClick", "item clicked");

        if(bMocking) {
            Intent intent = new Intent(this, GattActivity.class);
            intent.putExtra("service", mock.getServices().get(position));
            intent.putExtra("characteristic", mock.getCharacteristics().get(position));
            startActivity(intent);
        } else {
            BluetoothDevice selectedDevice = deviceList.get(position);
            gatt = selectedDevice.connectGatt(MainActivity.this, false, gattCallback);
            Log.i(TAG, "connecting to gatt");
           /* if(gatt.getDevice() == selectedDevice) {

                    List<BluetoothGattService> services = new ArrayList<BluetoothGattService>();
                    List<BluetoothGattCharacteristic> chars = new ArrayList<BluetoothGattCharacteristic>();

                    for(BluetoothGattService s : gatt.getServices()) {
                        services.add(s);
                        for(BluetoothGattCharacteristic c : s.getCharacteristics()){
                            chars.add(c);
                        }
                    }

                    Intent intent = new Intent(this, GattActivity.class);

                    if(services != null && services.size() > 0) {
                        intent.putExtra("service", services.get(0).getUuid().toString());
                        Log.v(TAG, "Service found");
                    }

                    if(chars != null && chars.size() > 0) {
                        if(chars.get(0).getValue() != null) {
                            intent.putExtra("characteristics", chars.get(0).getValue().toString());
                        }
                        Log.v(TAG, "Characteristic found");
                    }

                    startActivity(intent);
                }*/
            }
        }

    private void scanForDevices(final boolean enable) {
        Log.v("ScanForDevices", "enter");
        btScanner = btAdapter.getBluetoothLeScanner();
        settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        filters = Arrays.asList(
                new ScanFilter.Builder()
                    .build());

        if(enable) {
            if(bMocking) {
                dataMocking();
            }
            btScanner.startScan(filters, settings, scanCallback);
        } else {
            btScanner.stopScan(scanCallback);
        }
    }

    private void dataMocking() {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                appContext,
                android.R.layout.simple_list_item_1,
                mock.getDevices());
        listView.setAdapter(adapter);
    }

    private ScanCallback scanCallback = new ScanCallback() {

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = btAdapter.getRemoteDevice(result.getDevice().getAddress());

            if(!deviceList.contains(device)) {
                deviceList.add(device);
            }

            ArrayAdapter<BluetoothDevice> adapter = new ArrayAdapter<BluetoothDevice>(
                    appContext,
                    android.R.layout.simple_list_item_1,
                    deviceList);
            listView.setAdapter(adapter);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult sr : results) {
                Log.i("ScanResult - Results", sr.toString());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e("Scan Failed", "Error Code: " + errorCode);
        }


    };

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.i("onConnectionStateChange", "Status: " + status);
            switch(newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.i("gattCallback", "STATE_CONNECTED");
                    btAdapter.getBluetoothLeScanner().stopScan(scanCallback);
                    gatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.i("gattCallback", "STATE_DISCONNECTED");
                    break;
                default:
                    Log.i("gattCallback", "STATE_OTHER");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if(status == BluetoothGatt.GATT_SUCCESS) {
                //String value = "TEST_NEW_VALUE";

                List<BluetoothGattService> services = gatt.getServices();
                mAlarmService = gatt.getService(alarmUUid);
                mAlarmChar = mAlarmService.getCharacteristic(charUUid);

                Log.i(TAG, "alarmCharacteristic Value: " + mAlarmChar.getValue());
                Log.i("Service discovered", gatt.getServices().toString());


                // See Whatsapp
                // Write via GattDescriptor
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.i("onCharacteristicRead", characteristic.getValue().toString());
        }
    };

    public void setAlarm(boolean b) {
        byte[] value = new byte[4];

        if(b) {
            value[0] = (byte) (01 & 0xFF);
        } else {
            value[0] = (byte) (00 & 0xFF);
        }

        mAlarmChar.setValue(value);
        boolean stat = gatt.writeCharacteristic(mAlarmChar);

        if(stat) {
            Log.i("WriteCharStatus", "true");
        } else {
            Log.i("WriteCharStatus", "false");
        }
    }

    public void getAlarm() {

        byte[] value = mAlarmChar.getValue();
        Log.i("getAlarm(): ", "value" + value.toString());
        Toast.makeText(this, "getAlarm Value" + value.toString(), Toast.LENGTH_LONG).show();
    }

}
