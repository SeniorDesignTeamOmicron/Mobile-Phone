package edu.msoe.windorffj.logistep;


import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {



    Integer stepGoal = 10000;
    Integer steps = 0;
    Set<String> usernames;
    Set<String> passwords;
    Set<String> auths;
    int c_account;
    //Date today;
    public static Foot right;
    public static Foot left;
    private ServerConnect server;
    public static String server_address;


    // GUI Components
    public static BluetoothAdapter mBTAdapter;
    public static Set<BluetoothDevice> mPairedDevices;
    public static ArrayAdapter<String> mBTArrayAdapter;

    public static final String TAG = MainActivity.class.getSimpleName();
    public static Handler mHandler; // Our main handler that will receive callback notifications
    public static ConnectedThread mConnectedThread; // bluetooth background worker thread to send and receive data
    public static BluetoothSocket mBTSocket = null; // bi-directional client-to-client data path

    public static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // "random" unique identifier

    // #defines for identifying shared types between calling functions
    public final static int REQUEST_ENABLE_BT = 1; // used to identify adding bluetooth names
    public final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update
    public final static int CONNECTING_STATUS = 3; // used in bluetooth handler to identify message status



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sharedPref = this.getSharedPreferences(getString(R.string.shared_context),Context.MODE_PRIVATE);
        usernames = sharedPref.getStringSet(getString(R.string.username_save), new HashSet<String>());
        passwords = sharedPref.getStringSet(getString(R.string.pw_save), new HashSet<String>());
        steps = sharedPref.getInt(getString(R.string.step_save), 0);

        auths = new HashSet<>();

        //set the login if the username is not saved otherwise just log in
        if(auths.size() == 0){
            c_account = 0;
            setContentView(R.layout.login);
            Button log = findViewById(R.id.LogIn);
            log.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    EditText un = findViewById(R.id.Username);
                    EditText pw = findViewById(R.id.Password);
                    if(!un.getText().toString().equals("") && !pw.getText().toString().equals("")) {
                        login(un.getText().toString(), pw.getText().toString());
                    } else {
                        CharSequence text = "Enter a Username and Password";
                        int duration = Toast.LENGTH_SHORT;

                        Toast toast = Toast.makeText(MainActivity.this, text, duration);
                        toast.show();
                    }
                }
            });
            usernames = new HashSet<>();
            passwords = new HashSet<>();
        } else  {
            setContentView(R.layout.activity_main);
        }

        //check if it is still today and reset if not
        /*if(today != null){
            Date test = Calendar.getInstance().getTime();
            //if it is the same day
            //if not. reset step count
            if(today.getDay() != test.getDay() && today.getMonth() != test.getMonth()){
                steps = 0;
            }
        }*/

        right = new Foot(this);
        left = new Foot(this);

        mBTAdapter = BluetoothAdapter.getDefaultAdapter(); // get a handle on the bluetooth radio

        // Ask for location permission if not already allowed
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);


        mHandler = new Handler(new Handler.Callback(){
            public boolean handleMessage(android.os.Message msg){
                //TODO: use the message read to find which foot it is
                if(msg.what == MESSAGE_READ){
                    String readMessage;
                    try {
                        readMessage = new String((byte[]) msg.obj, "UTF-8");
                        Toast.makeText(getApplicationContext(),readMessage,Toast.LENGTH_SHORT).show();
                        //TODO: post a step to the server here using the message
                        //format:
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }

                if(msg.what == CONNECTING_STATUS){
                    if(msg.arg1 == 1) {
                        Toast.makeText(getApplicationContext(),"Connected to Device: " + (msg.obj),Toast.LENGTH_SHORT).show();
                    }else {
                        Toast.makeText(getApplicationContext(),"Connection Failed",Toast.LENGTH_SHORT).show();
                    }
                }
                return true;
            }
        });

        if (mBTAdapter == null) {
            // Device does not support Bluetooth
            Toast.makeText(getApplicationContext(),"Bluetooth device not found!",Toast.LENGTH_SHORT).show();
        }
        else {

            bluetoothOn();
        }
    }

    public void showBTDialog() {

        Button mDiscoverBtn;

        final AlertDialog.Builder popDialog = new AlertDialog.Builder(this);
        final LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
        final View Viewlayout = inflater.inflate(R.layout.bt_list, (ViewGroup) findViewById(R.id.bt_list));

        popDialog.setTitle("Paired Bluetooth Devices");
        popDialog.setView(Viewlayout);

        // create the arrayAdapter that contains the BTDevices, and set it to a ListView
        ListView myListView = Viewlayout.findViewById(R.id.BTList);
        mBTArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        myListView.setAdapter(mBTArrayAdapter);

        // get paired devices
        mPairedDevices = mBTAdapter.getBondedDevices();

        // put it's one to the adapter
        for(BluetoothDevice device : mPairedDevices)
            mBTArrayAdapter.add(device.getName()+ "\n" + device.getAddress());

        mDiscoverBtn = Viewlayout.findViewById(R.id.scan_button);

        mDiscoverBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                discover();
            }
        });

        myListView.setOnItemClickListener(Foot.mDeviceClickListener);

        // Create popup and show
        popDialog.create();
        popDialog.show();



    }

    private void bluetoothOn(){
        if (!mBTAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            Toast.makeText(getApplicationContext(),"Bluetooth Enabled",Toast.LENGTH_SHORT).show();

        }
        else{
            Toast.makeText(getApplicationContext(),"Bluetooth is already on", Toast.LENGTH_SHORT).show();
        }
    }

    // Enter here after user selects "yes" or "no" to enabling radio
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent Data){
        // Check which request we're responding to
        if (requestCode == REQUEST_ENABLE_BT) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                // The user picked a contact.
                // The Intent's data Uri identifies which contact was selected.
                Toast.makeText(getApplicationContext(), "Enabled", Toast.LENGTH_SHORT).show();
            }
            else {
                Toast.makeText(getApplicationContext(), "Disabled", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void discover(){
        // Check if the device is already discovering
        if(mBTAdapter.isDiscovering()){
            mBTAdapter.cancelDiscovery();
            Toast.makeText(getApplicationContext(),"Discovery stopped",Toast.LENGTH_SHORT).show();
        }
        else{
            if(mBTAdapter.isEnabled()) {
                mBTArrayAdapter.clear(); // clear items
                mBTAdapter.startDiscovery();
                Toast.makeText(getApplicationContext(), "Discovery started", Toast.LENGTH_SHORT).show();
                registerReceiver(blReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
            }
            else{
                Toast.makeText(getApplicationContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
            }
        }
    }

    final BroadcastReceiver blReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // add the name to the list
                mBTArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                mBTArrayAdapter.notifyDataSetChanged();
            }
        }
    };





    public static class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private String name;
        private Context context;

        ConnectedThread(BluetoothSocket socket, String name, Context context) {
            mmSocket = socket;
            InputStream tmpIn = null;
            this.name = name;
            this.context = context;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Toast.makeText(context,"IO Exception",Toast.LENGTH_SHORT).show();
            }

            mmInStream = tmpIn;
        }

        public void run() {
            if(name.equals("Right")){
                MainActivity.right.run_data(mmInStream);
            } else if(name.equals("Left")){
                MainActivity.left.run_data(mmInStream);
            }
        }

        /* Call this from the main activity to shutdown the connection */
        private void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Toast.makeText(context,"IO Exception",Toast.LENGTH_SHORT).show();
            }
        }
    }



    public void typePopup(final View v){
        PopupMenu popup = new PopupMenu(this, v);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.type_menu, popup.getMenu());
        popup.show();
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Button b = (Button) v;
                b.setText(item.getTitle().toString());
                return true;
            }
        });
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
                if(selected.equals("Connect")){
                    showBTDialog();
                } else {
                    if(v.getTag().equals("right foot")){
                        right.bt_disconnect();
                    } else if(v.getTag().equals("left foot")) {
                        left.bt_disconnect();
                    }
                }
                return true;
            }
        });
    }

    public void accountPopup(final View v){
        PopupMenu popup = new PopupMenu(this, v);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.account_menu, popup.getMenu());
        popup.show();
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                String selected = item.getTitle().toString();
                if(selected.equals("Logout")){
                    setContentView(R.layout.login);
                } else if(selected.equals("Change Account")){
                    PopupMenu popup = new PopupMenu(MainActivity.this, v);
                    MenuInflater inflater = popup.getMenuInflater();
                    inflater.inflate(R.menu.account_change, popup.getMenu());
                    popup.show();
                    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            c_account = item.getItemId();
                            change_account(item.toString());
                            return true;
                        }
                    });
                    Menu mine = popup.getMenu();
                    Iterator it = usernames.iterator();
                    while(it.hasNext()){
                        String un = (String)it.next();
                        mine.add(un);
                    }
                }
                return true;
            }
        });
    }

    private void login(String m_username, String m_password){
        server = new ServerConnect(this, m_username, m_password);
        String m_auth = server.getAuthentication();

        if(!auths.contains(m_auth)){
            auths.add(m_auth);
        }

        if(usernames.contains(m_username)){
            int i = 0;
            Iterator it = usernames.iterator();
            while(it.hasNext()){
                String acc = (String) it.next();
                if(acc.equals(m_username)){
                    c_account = i;
                }
                i++;
            }
        } else {
            usernames.add(m_username);
            passwords.add(m_password);
            c_account = usernames.size()-1;
        }
        setContentView(R.layout.activity_main);

        TextView st = findViewById(R.id.Steps);
        st.setText(steps.toString());
        st.bringToFront();

        TextView go = findViewById(R.id.Goal);
        go.setText(stepGoal.toString());
        go.bringToFront();
    }

    public void create_account(View v){
        // inflate the layout of the popup window
        LayoutInflater inflater = (LayoutInflater)
                getSystemService(LAYOUT_INFLATER_SERVICE);
        final View popupView = inflater.inflate(R.layout.popup_window, null);

        // create the popup window
        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        final PopupWindow popupWindow = new PopupWindow(popupView, width, height, true);

        // show the popup window
        // which view you pass in doesn't matter, it is only used for the window tolken
        popupWindow.showAtLocation(v, Gravity.CENTER, 0, 0);

        Button log = popupView.findViewById(R.id.create);
        log.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText un = popupView.findViewById(R.id.new_username);
                EditText pw = popupView.findViewById(R.id.new_password);
                EditText email = popupView.findViewById(R.id.email);
                EditText lfoot = popupView.findViewById(R.id.lfootsize);
                EditText rfoot = popupView.findViewById(R.id.rfootsize);
                if(!un.getText().toString().equals("") && !pw.getText().toString().equals("")) {
                    popupWindow.dismiss();
                    server.authenticate(un.getText().toString(),pw.getText().toString(),email.getText().toString(),Double.parseDouble(lfoot.getText().toString()),Double.parseDouble(rfoot.getText().toString()));
                    login(un.getText().toString(), pw.getText().toString());
                } else {
                    CharSequence text = "Enter a Username and Password";
                    int duration = Toast.LENGTH_SHORT;

                    Toast toast = Toast.makeText(MainActivity.this, text, duration);
                    toast.show();
                }
            }
        });

        // dismiss the popup window when touched
        popupView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                v.performClick();
                popupWindow.dismiss();
                return true;
            }
        });
    }

    private void change_account(String username){
        Iterator it = usernames.iterator();
        int i = 0;
        while(it.hasNext()){
            String acc = (String) it.next();
            if(acc.equals(username)){
                c_account = i;
            }
            i++;
        }
    }

    public void server_connect(View v) {
        // inflate the layout of the popup window
        LayoutInflater inflater = (LayoutInflater)
                getSystemService(LAYOUT_INFLATER_SERVICE);
        final View popupView = inflater.inflate(R.layout.server_popup, null);

        // create the popup window
        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        final PopupWindow popupWindow = new PopupWindow(popupView, width, height, true);

        // show the popup window
        // which view you pass in doesn't matter, it is only used for the window tolken
        popupWindow.showAtLocation(v, Gravity.CENTER, 0, 0);

        Button log = popupView.findViewById(R.id.create);
        log.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button btn = popupView.findViewById(R.id.go_server);
                btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        EditText ip = popupView.findViewById(R.id.ip_address);
                        server_address = ip.getText().toString();
                        popupWindow.dismiss();
                    }
                });
            }
        });

        // dismiss the popup window when touched
        popupView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                v.performClick();
                popupWindow.dismiss();
                return true;
            }
        });
        Iterator it_u = usernames.iterator();
        Iterator it_p = passwords.iterator();
        for (int i = 0; i < c_account; i++) {
            it_u.next();
            it_p.next();
        }
        server = new ServerConnect(this, it_u.next().toString(), it_p.next().toString());
        //TODO: get the ack and change the color of the Link button fi connected
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mConnectedThread.cancel();

        server.closeThread();

        SharedPreferences sharedPref = this.getSharedPreferences(getString(R.string.shared_context),Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(getString(R.string.step_save), steps);
        editor.putInt(getString(R.string.acc_save), c_account);
        editor.putStringSet(getString(R.string.username_save), usernames);
        editor.putStringSet(getString(R.string.pw_save), passwords);
        editor.apply();

    }

    


}
