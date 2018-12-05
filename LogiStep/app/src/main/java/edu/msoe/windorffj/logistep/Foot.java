package edu.msoe.windorffj.logistep;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Foot {
    private BluetoothAdapter mBluetoothAdapter;
    private Set<BluetoothDevice> pairedDevices;
    private boolean connectFlag = false;
    private String name;
    private List<BluetoothDevice> myDevices;
    private Context context;


    public Foot(String name, Context context){

        this.context = context;

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device doesn't support Bluetooth
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            ((Activity)this.context).startActivityForResult(enableBtIntent, 10); //request code may need to change
        }

        pairedDevices = mBluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
            }
        }

        this.name = name;
        myDevices = new ArrayList<BluetoothDevice>();
    }

    //the connect button was pressed
    public void bt_connect(){

        //TODO: check myDevices and connect if it is not empty
        if(myDevices.size() != 0){

        } else {
            //start discovering

            //first need to stop a current discovery
            if (mBluetoothAdapter.isDiscovering()) {
                mBluetoothAdapter.cancelDiscovery();
            }

            //then start again
            mBluetoothAdapter.startDiscovery();

            while (mBluetoothAdapter.isDiscovering()) ; //wait while it is still discovering.

            // Register for broadcasts when a device is discovered.
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            context.registerReceiver(mReceiver, filter);
        }
    }

    public void bt_disconnect(BluetoothDevice device){
        myDevices.remove(device);
    }

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                addDevice(device);

            }
        }
    };

    private void addDevice(BluetoothDevice device){
        myDevices.add(device);
        connectFlag = true;
    }

    public boolean check_connect(){
        return connectFlag;
    }

    public String get_name(){
        return name;
    }

    public void unregister(){
        context.unregisterReceiver(mReceiver);
    }

    public List<BluetoothDevice> get_devices(){
        return myDevices;
    }
}
