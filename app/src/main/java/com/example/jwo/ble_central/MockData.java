package com.example.jwo.ble_central;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jwo on 08.06.17.
 */

public class MockData {

    private List<String> services;
    private List<String> devices;
    private List<String> characteristics;

    public MockData() {
        services = new ArrayList<String>();
        devices = new ArrayList<String>();
        characteristics = new ArrayList<String>();

        services.add("sampleService");
        services.add("sampleService2");

        devices.add("Galaxy S6");
        devices.add("Galaxy S7");

        characteristics.add("EINS");
        characteristics.add("ZWEI");
    }

    public List<String> getServices() {
        return services;
    }


    public List<String> getDevices() {
        return devices;
    }


    public List<String> getCharacteristics() {
        return characteristics;
    }
}
