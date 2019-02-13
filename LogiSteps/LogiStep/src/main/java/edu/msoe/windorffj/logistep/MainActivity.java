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

import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class MainActivity extends AppCompatActivity {



    Integer stepGoal = 10000;
    Integer steps = 0;
    Double steps_per_hour = 0.0;
    Set<String> usernames;
    Set<String> passwords;
    Set<String> auths;
    int c_account;

    SharedPreferences sharedPref;
    SharedPreferences.Editor editor;

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

    // #defines for identifying shared types between calling functions
    public final static int REQUEST_ENABLE_BT = 1; // used to identify adding bluetooth names
    public final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update
    public static final int MESSAGE_WRITE = 3;
    public final static int CONNECTING_STATUS = 3; // used in bluetooth handler to identify message status


    public final static int RIGHT_FOOT = 0;
    public final static int LEFT_FOOT = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPref = this.getSharedPreferences(getString(R.string.shared_context),Context.MODE_PRIVATE);
        editor = sharedPref.edit();
        usernames = sharedPref.getStringSet(getString(R.string.username_save), new HashSet<String>());
        passwords = sharedPref.getStringSet(getString(R.string.pw_save), new HashSet<String>());
        auths = sharedPref.getStringSet(getString(R.string.auths_save),new HashSet<String>());
        c_account = sharedPref.getInt(getString(R.string.acc_save),0);
        steps = sharedPref.getInt(getString(R.string.step_save), 0);
        server_address = sharedPref.getString(getString(R.string.server_connect),"0.0.0.0");



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
                    EditText ip = findViewById(R.id.IP_Address);
                    if(!un.getText().toString().equals("") && !pw.getText().toString().equals("") && !ip.getText().toString().equals("")) {
                        server_address = ip.getText().toString();
                        server = new ServerConnect(MainActivity.this);
                        login(un.getText().toString(), pw.getText().toString());
                    } else {
                        CharSequence text = "Enter a Username, Password, and IP address";
                        int duration = Toast.LENGTH_SHORT;

                        Toast toast = Toast.makeText(MainActivity.this, text, duration);
                        toast.show();
                    }
                }
            });
            usernames = new HashSet<>();
            passwords = new HashSet<>();
        } else  {
            server = new ServerConnect(MainActivity.this);
            Iterator it = usernames.iterator();
            String un = it.next().toString();
            it = passwords.iterator();
            String pw = it.next().toString();
            login(un,pw);
        }

        right = new Foot(this,RIGHT_FOOT);
        left = new Foot(this,LEFT_FOOT);
        auths = new HashSet<>();

        mBTAdapter = BluetoothAdapter.getDefaultAdapter(); // get a handle on the bluetooth radio

        // Ask for location permission if not already allowed
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);


        mHandler = new Handler(new Handler.Callback(){
            public boolean handleMessage(android.os.Message msg){
                if(msg.what == MESSAGE_READ){
                    String foot = null;
                    if(msg.arg2 == RIGHT_FOOT) { // it is the right foot
                        foot = "right";
                    } else if(msg.arg2 == LEFT_FOOT) { //it is the left
                        foot = "left";
                    }
                    String readMessage;
                    //TODO: post a step to the server here using the message
                    server.post_step(foot,0,0); //default for now
                    //format:
                } else if(msg.what == MESSAGE_WRITE){ //do not need right now
                    //byte[] writeBuf = (byte[]) msg.obj;

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
                Toast.makeText(getApplicationContext(), "Discovery started", Toast.LENGTH_SHORT).show();
                IntentFilter filter = new IntentFilter();

                filter.addAction(BluetoothDevice.ACTION_FOUND);
                filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
                filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
                filter.addAction((BluetoothDevice.ACTION_NAME_CHANGED));

                registerReceiver(blReceiver, filter);

                mBTAdapter.startDiscovery();
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
            } else if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)){
                Toast.makeText(getApplicationContext(), "Bluetooth discovery started", Toast.LENGTH_SHORT).show();
            } else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                Toast.makeText(getApplicationContext(), "Bluetooth discovery finished", Toast.LENGTH_SHORT).show();
            }
        }
    };





    public static class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream outputStream;
        private String name;
        private Context context;

        ConnectedThread(BluetoothSocket socket, String name, Context context) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            this.name = name;
            this.context = context;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Toast.makeText(context,"IO Exception",Toast.LENGTH_SHORT).show();
            }

            mmInStream = tmpIn;
            outputStream = tmpOut;
        }

        public void run() {
            if(name.equals("Right")){
                MainActivity.right.run_data(mmInStream);
            } else if(name.equals("Left")){
                MainActivity.left.run_data(mmInStream);
            }
        }

        // write to OutputStream
        void write(byte[] buffer) {
            try {
                outputStream.write(buffer);
                mHandler.obtainMessage(MainActivity.MESSAGE_WRITE, -1, -1,
                        buffer).sendToTarget();
            } catch (IOException e) {
                Toast.makeText(context,"IO Exception",Toast.LENGTH_SHORT).show();
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
        Response r = server.getUser(m_username,m_password);
        if(r != null) {
            if (r.code() == 200) {
                try {
                    String body = r.body().string();
                    try {
                        JSONObject jso = new JSONObject(body);
                        stepGoal = (int)jso.get("step_goal");
                    } catch(JSONException e){
                        e.printStackTrace();
                    }

                    Response r1 = server.getSteps();
                    class runa implements Runnable {

                        volatile private String body1;
                        private Response r1;

                        private runa(Response r){
                            r1 = r;
                        }

                        @Override
                        public void run() {
                            try {
                                body1 = r1.body().string();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        private String get_response() {
                            return body1;
                        }

                    }

                    if(r1.code() == 200) {
                        runa ru = new runa(r1);

                        Thread thread = new Thread(ru);

                        thread.start();
                        String body1 = null;
                        try {
                            thread.join();
                            body1 = ru.get_response();
                            try {
                                JSONObject jso = new JSONObject(body1);
                                steps = (int)jso.get("steps");
                                steps_per_hour = (double)jso.get("steps_per_hour");
                            } catch(JSONException e){
                                e.printStackTrace();
                            }
                        } catch (InterruptedException e) {
                            Toast.makeText(this, "InterruptedException: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Error Code: " + r1.code() + r1.message(), Toast.LENGTH_SHORT).show();
                    }

                    if (r.code() == 200) {
                        String m_auth = server.getAuthentication();

                        if (!auths.contains(m_auth)) {
                            auths.add(m_auth);
                        }

                        if (usernames.contains(m_username)) {
                            int i = 0;
                            Iterator it = usernames.iterator();
                            while (it.hasNext()) {
                                String acc = (String) it.next();
                                if (acc.equals(m_username)) {
                                    c_account = i;
                                }
                                i++;
                            }
                        } else {
                            usernames.add(m_username);
                            passwords.add(m_password);
                            c_account = usernames.size() - 1;
                        }
                        setContentView(R.layout.activity_main);

                        TextView st = findViewById(R.id.Steps);
                        st.setText(steps.toString());
                        st.bringToFront();

                        TextView go = findViewById(R.id.Goal);
                        go.setText(stepGoal.toString());
                        go.bringToFront();

                        TextView ph = findViewById(R.id.per_hour);
                        ph.setText(steps_per_hour.toString());
                        ph.bringToFront();

                        Double projected;
                        ZonedDateTime zdt = ZonedDateTime.now();
                        int hour = zdt.getHour();
                        int left = 24 - hour;
                        projected = steps + (left * steps_per_hour);
                        TextView pr = findViewById(R.id.ProjectedSteps);
                        pr.setText(projected.toString());
                        pr.bringToFront();

                    } else {
                        Toast.makeText(this, "Error Code: " + r.code() + r.message(), Toast.LENGTH_SHORT).show();
                    }
                } catch (IOException e) {
                    Toast.makeText(this, "IO Exception: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            } else { //error code
                Toast.makeText(this, "Error Code: " + r.code() + r.message(), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "R is Null", Toast.LENGTH_SHORT).show();
        }
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
                EditText f_name = popupView.findViewById(R.id.f_name);
                EditText l_name = popupView.findViewById(R.id.l_name);
                EditText height = popupView.findViewById(R.id.height);
                EditText weight = popupView.findViewById(R.id.weight);
                EditText s_goal = popupView.findViewById(R.id.s_goal);
                if(!un.getText().toString().equals("") && !pw.getText().toString().equals("") && !email.getText().toString().equals("") && !lfoot.getText().toString().equals("") &&
                        !rfoot.getText().toString().equals("") && !f_name.getText().toString().equals("") && !l_name.getText().toString().equals("") && !height.getText().toString().equals("") &&
                        !weight.getText().toString().equals("") && !s_goal.getText().toString().equals("")) {
                    popupWindow.dismiss();
                    server.authenticate(un.getText().toString(),pw.getText().toString(),email.getText().toString(),Double.parseDouble(lfoot.getText().toString()),Double.parseDouble(rfoot.getText().toString()),
                            f_name.getText().toString(),l_name.getText().toString(),Integer.parseInt(height.getText().toString()),Integer.parseInt(weight.getText().toString()),Integer.parseInt(s_goal.getText().toString()));
                    login(un.getText().toString(), pw.getText().toString());
                } else {
                    CharSequence text = "Enter a All Fields";
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

        Button log = popupView.findViewById(R.id.go_server);
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
        server = new ServerConnect(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mConnectedThread.cancel();

        editor.putInt(getString(R.string.step_save), steps);
        editor.putInt(getString(R.string.acc_save), c_account);
        editor.putStringSet(getString(R.string.username_save), usernames);
        editor.putStringSet(getString(R.string.pw_save), passwords);
        editor.putStringSet(getString(R.string.auths_save),auths);
        editor.putString(getString(R.string.server_connect),server_address);
        editor.apply();

    }


    @Override
    protected void onStop() {
        super.onStop();

        try {
            mBTSocket.close();
        } catch(IOException e){
            Toast.makeText(this, "IO Exception: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        editor.putInt(getString(R.string.step_save), steps);
        editor.putInt(getString(R.string.acc_save), c_account);
        editor.putStringSet(getString(R.string.username_save), usernames);
        editor.putStringSet(getString(R.string.pw_save), passwords);
        editor.putStringSet(getString(R.string.auths_save),auths);
        editor.putString(getString(R.string.server_connect),server_address);
        editor.apply();
    }
}
