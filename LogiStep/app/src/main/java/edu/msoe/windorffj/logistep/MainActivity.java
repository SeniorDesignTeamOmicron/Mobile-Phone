package edu.msoe.windorffj.logistep;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    BluetoothAdapter mBluetoothAdapter;
    Set<BluetoothDevice> pairedDevices;
    Map<String, BluetoothDevice> myDevices;
    boolean connectFlag;
    int stepGoal = 10000;
    int steps;
    String username;
    String password;
    Date today;
    String pressed = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //set the login if the username is not saved otherwise just log in
        if(username == null){
            setContentView(R.layout.login);
            Button log = (Button)findViewById(R.id.LogIn);
            log.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    EditText un = (EditText)findViewById(R.id.Username);
                    EditText pw = (EditText)findViewById(R.id.Password);
                    login(un.getText().toString(),pw.getText().toString());
                }
            });
        } else  {
            setContentView(R.layout.activity_main);
        }

        //check if it is still today and reset if not
        if(today != null){
            Date test = Calendar.getInstance().getTime();
            //if it is the same day
            //if not. reset step count
            if(today.getDay() != test.getDay() && today.getMonth() != test.getMonth()){
                steps = 0;
            }
        }

        TextView st = (TextView)findViewById(R.id.Steps);
        st.setText(steps);
        st.bringToFront();

        TextView go = (TextView)findViewById(R.id.Goal);
        go.setText(stepGoal);
        go.bringToFront();


        myDevices.put("")

        connectFlag = false;

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device doesn't support Bluetooth
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 10); //request code may need to change
        }

        pairedDevices = mBluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
            }
        }

    }

    public void typePopup(View v){
        PopupMenu popup = new PopupMenu(this, v);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.type_menu, popup.getMenu());
        popup.show();
    }

    public void shoeConnectionPopup(final View v){
        PopupMenu popup = new PopupMenu(this, v);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.shoe_connection, popup.getMenu());
        popup.show();
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                String selected = item.getTitle().toString();
                if(myDevices.get(v.getTag().toString()) != null){
                    //TODO: if it is connect, connect to this spot
                    // if disconnect, make the key null
                } else {
                    //TODO: if connect, connect
                    //if disconnect, do nothing
                }
                return true;
            }
        });
    }

    public void accountPopup(View v){
        PopupMenu popup = new PopupMenu(this, v);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.shoe_connection, popup.getMenu());
        popup.show();
    }

    private void login(String m_username, String m_password){
        username = m_username;
        password = m_password;
        setContentView(R.layout.activity_main);
    }

    //the connect button was pressed
    public void bt_connect(String foot){

        //start discovering

        pressed = foot;

        //first need to stop a current discovery
        if(mBluetoothAdapter.isDiscovering()){
            mBluetoothAdapter.cancelDiscovery();
        }

        //then start again
        mBluetoothAdapter.startDiscovery();

        while(mBluetoothAdapter.isDiscovering()); //wait while it is still discovering.

        // Register for broadcasts when a device is discovered.
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);
        connectFlag = true;
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
                addDevice(pressed,device);
            }
        }
    };

    private void addDevice(String key, BluetoothDevice device){
        myDevices.put(key,device);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Don't forget to unregister the ACTION_FOUND receiver.
        //only if the connect button was used.
        if(connectFlag) {
            unregisterReceiver(mReceiver);
        }
    }
}
