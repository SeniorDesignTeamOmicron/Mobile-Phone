package edu.msoe.windorffj.logistep;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Toast;

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
    public static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 0;
    private BluetoothDataService mDataService = null;
    private String mConnectedDeviceName = null;

    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";


    public Foot(String name, Context context){

        this.context = context;

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device doesn't support Bluetooth
        }

        //if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            ((Activity)this.context).startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT); //request code may need to change
            Toast.makeText(this.context, "Turned on",Toast.LENGTH_LONG).show();
        //}

        pairedDevices = mBluetoothAdapter.getBondedDevices();

        this.name = name;
        myDevices = new ArrayList<BluetoothDevice>();

        mDataService = new BluetoothDataService(this.context,mHandler);
    }

    //the connect button was pressed
    public void bt_connect(){

        //TODO: check myDevices and connect if it is not empty
        //TODO: start the bluetooth service here

        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            context.startActivity(discoverableIntent);
        }

        Intent serverIntent = new Intent(context, DeviceListActivity.class);
        ((Activity)this.context).startActivityForResult(serverIntent, REQUEST_ENABLE_BT); //request code may need to change

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

    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    //TODO: do something with the message from bluetooth
                    CharSequence text = "Message received from Bluetooth " + readMessage;
                    int duration = Toast.LENGTH_SHORT;

                    Toast toast = Toast.makeText(context, text, duration);
                    toast.show();
                    break;
                case MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(context, "Connected to "
                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(context, msg.getData().getString(TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    public void activityResult(int requestCode, int resultCode, Intent data){
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    // Get the device MAC address
                    String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    // Get the BLuetoothDevice object
                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                    // Attempt to connect to the device
                    mDataService.connect(device);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    mDataService = new BluetoothDataService(this.context,mHandler);
                } else {
                    // User did not enable Bluetooth or an error occured
                    //Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                    //finish();
                }
        }
    }

}
