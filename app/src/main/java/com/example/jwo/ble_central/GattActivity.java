package com.example.jwo.ble_central;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

/**
 * Created by jwo on 08.06.17.
 */

public class GattActivity extends Activity {

    private TextView viewGattService;
    private TextView viewGattCharacteristic;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gatt);

        viewGattService = (TextView) findViewById(R.id.gattService);
        viewGattCharacteristic = (TextView) findViewById(R.id.gattCharacteristic);


        Intent intent = getIntent();

        String service = intent.getStringExtra("service");
        String characteristic = intent.getStringExtra("characteristic");


        viewGattService.setText(service);
        viewGattCharacteristic.setText(characteristic);
    }

}
