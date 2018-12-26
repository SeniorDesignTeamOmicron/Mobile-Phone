package edu.msoe.windorffj.logistep;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.ParcelUuid;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.UUID;

import static edu.msoe.windorffj.logistep.MainActivity.BTMODULEUUID;
import static edu.msoe.windorffj.logistep.MainActivity.mBTAdapter;

public class Foot {
    private String name;
    private static BluetoothDevice myDevice;
    public static Context context;

    private static final int MESSAGE_READ = 2;


    /**
     * Initializer for the foot object.
     * starts the bluetooth.
     * @param name What the name of the foot is
     * @param context The application context form the MainActivity
     */
    Foot(String name, Context context){

        this.context = context;

        this.name = name;
    }

    //the connect button was pressed

    static AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {

            if(!mBTAdapter.isEnabled()) {
                Toast.makeText(context, "Bluetooth not on", Toast.LENGTH_SHORT).show();
                return;
            }

            //Toast.makeText(context, "Connecting...", Toast.LENGTH_SHORT).show();
            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            final String address = info.substring(info.length() - 17);
            final String name = info.substring(0,info.length() - 17);

            // Spawn a new thread to avoid blocking the GUI one
            new Thread()
            {
                public void run() {
                    boolean fail = false;

                    myDevice = mBTAdapter.getRemoteDevice(address);

                    mBTAdapter.cancelDiscovery();


                    try {
                        MainActivity.mBTSocket = createBluetoothSocket(myDevice);
                    } catch (IOException e) {
                        fail = true;
                        Toast.makeText(context, "Socket creation failed", Toast.LENGTH_SHORT).show();
                    }
                    // Establish the Bluetooth socket connection.
                    try {
                        MainActivity.mBTSocket.connect();
                    } catch (IOException e) {
                        try {
                            fail = true;
                            MainActivity.mBTSocket.close();
                            MainActivity.mHandler.obtainMessage(MainActivity.CONNECTING_STATUS, -1, -1)
                                    .sendToTarget();
                        } catch (IOException e2) {
                            //insert code to deal with this
                            Toast.makeText(context, "Socket creation failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                    if(!fail) {
                        MainActivity.mConnectedThread = new MainActivity.ConnectedThread(MainActivity.mBTSocket,name,context);
                        MainActivity.mConnectedThread.start();

                        MainActivity.mHandler.obtainMessage(MainActivity.CONNECTING_STATUS, 1, -1, name)
                                .sendToTarget();
                    }
                }

            }.start();
        }
    };

    void run_data(InputStream mmInStream){
        byte[] buffer = new byte[1024];  // buffer store for the stream
        int bytes; // bytes returned from read()
        // Keep listening to the InputStream until an exception occurs
        while (true) {
            try {
                // Read from the InputStream
                bytes = mmInStream.available();
                if(bytes != 0) {
                    buffer = new byte[1024];
                    SystemClock.sleep(100); //pause and wait for rest of data. Adjust this depending on your sending speed.
                    bytes = mmInStream.available(); // how many bytes are ready to be read?
                    bytes = mmInStream.read(buffer, 0, bytes); // record how many bytes we actually read
                    MainActivity.mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget(); // Send the obtained bytes to the UI activity
                }
            } catch (IOException e) {
                e.printStackTrace();

                break;
            }
        }
    }

    private static BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        //TODO: This is where the UUID is. Must find out why this is failing.
        UUID SERIAL_UUID;
        device.fetchUuidsWithSdp();
        final ParcelUuid[] temp = device.getUuids();
        if(temp != null) {
            SERIAL_UUID = temp[0].getUuid();
        } else {
            SERIAL_UUID = BTMODULEUUID;
        }
        try {
            final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", UUID.class);
            return (BluetoothSocket) m.invoke(device, SERIAL_UUID);

        } catch (Exception e) {
            Log.e(MainActivity.TAG, "Could not create Insecure RFComm Connection",e);
        }

        return  device.createRfcommSocketToServiceRecord(SERIAL_UUID);
    }

    /**
     * disconnect the bluetooth
     */
    void bt_disconnect(){
        myDevice = null;
    }

    /**
     * gets the name of the  foot object
     * @return our name
     */
    public String get_name(){
        return name;
    }

    /**
     * get the devices that are all on the connection list
     * @return the list of devices myDevices
     */
    public BluetoothDevice get_device(){
        return myDevice;
    }

}
