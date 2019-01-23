package edu.msoe.windorffj.logistep;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import static edu.msoe.windorffj.logistep.MainActivity.mBTAdapter;
import static edu.msoe.windorffj.logistep.MainActivity.mBTSocket;

public class Foot {
    private static String bt_name;
    private static BluetoothDevice myDevice;
    public static Context context;
    public static boolean connected = false;
    public static int foot; //0 is right, and 1 is left

    private static final int MESSAGE_READ = 2;
    private static final int MESSAGE_WRITE = 3;


    /**
     * Initializer for the foot object.
     * starts the bluetooth.
     * @param context The application context form the MainActivity
     */
    Foot(Context context, int foot){
        this.foot = foot;
        this.context = context;
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

                    bt_name = myDevice.getName();

                    mBTAdapter.cancelDiscovery();


                    try {
                        mBTSocket = createBluetoothSocket(myDevice);
                    } catch (IOException e) {
                        fail = true;
                        Toast.makeText(context, "Socket creation failed", Toast.LENGTH_SHORT).show();
                    }
                    // Establish the Bluetooth socket connection.
                    try {

                        mBTSocket.connect();
                    } catch (IOException e) {
                        try {
                            try {
                                Log.e("","trying fallback...");

                                mBTSocket =(BluetoothSocket) myDevice.getClass().getMethod("createRfcommSocket", new Class[] {int.class}).invoke(myDevice,1);
                                mBTSocket.connect();

                                Log.e("","Connected");
                            } catch (Exception e2) {
                                Log.e("", "Couldn't establish Bluetooth connection!");
                                fail = true;
                                mBTSocket.close();
                                MainActivity.mHandler.obtainMessage(MainActivity.CONNECTING_STATUS, -1, -1)
                                        .sendToTarget();
                            }

                        } catch (IOException e2) {
                            //insert code to deal with this
                            Toast.makeText(context, "Socket creation failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                    if(!fail) {
                        connected = true;
                        sendMessage(Long.toString(System.currentTimeMillis()));
                        MainActivity.mConnectedThread = new MainActivity.ConnectedThread(mBTSocket,name,context);
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
                    MainActivity.mHandler.obtainMessage(MESSAGE_READ, bytes, foot, buffer)
                            .sendToTarget(); // Send the obtained bytes to the UI activity
                }
            } catch (IOException e) {
                e.printStackTrace();

                break;
            }
        }
    }

    public static void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (!connected) {
            Toast.makeText(context, "Cannot send. Bluetooth not connected", Toast.LENGTH_SHORT).show();
            return;
        }
        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            MainActivity.mConnectedThread.write(send);
        }
    }

    private static BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        while(mBTAdapter.isDiscovering());
        boolean tmp = device.fetchUuidsWithSdp();
        UUID SERIAL_UUID = null;
        if (tmp){
            SERIAL_UUID = device.getUuids()[0].getUuid();
        }
        return device.createRfcommSocketToServiceRecord(SERIAL_UUID);
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
        return bt_name;
    }

    /**
     * get the devices that are all on the connection list
     * @return the list of devices myDevices
     */
    public BluetoothDevice get_device(){
        return myDevice;
    }

}
