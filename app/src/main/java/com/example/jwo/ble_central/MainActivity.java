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
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Handler;
import android.os.IBinder;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static android.R.attr.value;

public class MainActivity extends Activity implements View.OnClickListener, AdapterView.OnItemClickListener {

    private Button btnScan;
    private Button btnClear;
    ArrayAdapter<BluetoothDevice> deviceAdapter;
    private ListView listView;
    private Context appContext;
    private BLEService mBLEService;
    private int selectedItemPosition;
    private static final String TAG = "BLE_CENTRAL";
    private boolean bAlarm;
    private States state;

    private static final UUID alarmUUid = UUID.fromString("FF890198-9446-4E3A-B173-4E1161D6F59B");
    private static final UUID charUUid = UUID.fromString("77B4350C-DEA3-4DBA-B650-251670F4B2B4");

    public enum States {
        STANDARD,
        SCANNING
    }

    private enum Klang
    {
        Ole
    }

    private SoundPool soundPool;
    private int soundId[] = new int[Klang.values().length];
    private final static int AUSGABE = AudioManager.STREAM_MUSIC;


    // manage Service Lifecycle
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBLEService = ((BLEService.LocalBinder) service).getService();
            if (!mBLEService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }

            deviceAdapter = new DeviceItemAdapter(appContext, mBLEService.getDeviceList());
            listView.setAdapter(deviceAdapter);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBLEService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BLEService.ACTION_GATT_CONNECTED.equals(action)) {
                ((DeviceItemAdapter)deviceAdapter).setState(DeviceItemAdapter.States.CONNECTED);
                deviceAdapter.notifyDataSetChanged();
                invalidateOptionsMenu();
            } else if (BLEService.ACTION_GATT_DISCONNECTED.equals(action)) {
                ((DeviceItemAdapter)deviceAdapter).setState(DeviceItemAdapter.States.STANDARD);
                deviceAdapter.notifyDataSetChanged();
                invalidateOptionsMenu();

            } else if (BLEService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.

            } else if (BLEService.ACTION_ALARM_TRIGGERED.equals(action)) {

                String alarmValue = intent.getStringExtra(BLEService.ALARM_VALUE);
                if(alarmValue.equals("0")) {
                    Log.i(TAG, "ALARM is 0");
                    ((DeviceItemAdapter)deviceAdapter).setState(DeviceItemAdapter.States.CONNECTED);
                    deviceAdapter.notifyDataSetChanged();
                    invalidateOptionsMenu();
                    bAlarm = false;
                } else {
                    Log.i(TAG, "ALARM is 1");
                    ((DeviceItemAdapter)deviceAdapter).setState(DeviceItemAdapter.States.ALARM);
                    deviceAdapter.notifyDataSetChanged();
                    invalidateOptionsMenu();

                    if(!bAlarm) {
                        Klang klang;
                        klang = Klang.Ole;

                        if (soundId[klang.ordinal()] != -1)
                        {
                            final float volL = 1;
                            final float volR = 1;
                            final int prio = 0;
                            final int n = 0;
                            final float rate = 1;
                            soundPool.play(soundId[klang.ordinal()], volL, volR, prio, n, rate);
                        }
                    }

                    bAlarm = true;
                }
            } else if(BLEService.ACTION_DEVICE_FOUND.equals(action)) {
                deviceAdapter.notifyDataSetChanged();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        System.out.print("oncreate");
        Log.v(TAG, "onCreate");
        appContext = this;
        state = States.STANDARD;

        btnScan = (Button) findViewById(R.id.btnScan);
        btnClear = (Button) findViewById(R.id.btnClear);
        btnScan.setOnClickListener(this);
        btnClear.setOnClickListener(this);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE not supported", Toast.LENGTH_SHORT).show();
            finish();
        }

        Intent gattServiceIntent = new Intent(this, BLEService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        listView = (ListView) findViewById(R.id.listView);
        listView.setOnItemClickListener(this);

        for (int i = 0; i < soundId.length; i++)
        {
            soundId[i] = -1;
        }

        setVolumeControlStream(AUSGABE);
        bAlarm = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        final int anzahlKanaele = 15;
        soundPool = new SoundPool(anzahlKanaele, AUSGABE, 0);

        try
        {
            AssetManager am = getAssets();
            soundId[Klang.Ole.ordinal()] = soundPool.load(am.openFd("audio/Ole.wav"), 1);
        }
        catch (IOException e)
        {
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
        for (int i : soundId)
        {
            if (i != -1)
            {
                soundPool.unload(i);
            }
        }
        soundPool.release();
        soundPool = null;
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.btnScan:
                Log.d("StartScan", "buttonClicked");
                if(state.equals(States.STANDARD)) {
                    mBLEService.scanForDevices(true);
                    btnScan.setText(R.string.scanStop);
                    state = States.SCANNING;
                } else if(state.equals(States.SCANNING)) {
                    mBLEService.scanForDevices(false);
                    btnScan.setText(R.string.scanStart);
                    state = States.STANDARD;
                }
                break;
            case R.id.btnClear:
                Log.d("StopScan", "buttonClicked");
                mBLEService.clearList();
                deviceAdapter.notifyDataSetChanged();
                break;
            default:
        }
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
         Log.i("OnItemClick", "item clicked");
        //RESET ALARM
        // SET DEVICE ITEM LAYOUT TO CONNECTED
       /* selectedItemPosition = position;
        mBLEService.setAlarm(false, position);*/
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BLEService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BLEService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BLEService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BLEService.ACTION_ALARM_TRIGGERED);
        intentFilter.addAction(BLEService.ACTION_DEVICE_FOUND);
        intentFilter.addAction(DeviceItemAdapter.ACTION_CONNECT_CLICKED);
        return intentFilter;
    }

    public void connectDevice(int position) {
        Log.i(TAG, "connect Button clicked " + position);
        selectedItemPosition = position;
        mBLEService.connectGatt(position);
    }

    public void disconnectDevice(int position) {
        Log.i(TAG, "disconnect Button clicked " + position);
        selectedItemPosition = position;
        mBLEService.disconnectGatt(position);
        deviceAdapter.notifyDataSetChanged();
        bAlarm = false;
    }
}
