package edu.msoe.windorffj.logistep;


import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {



    Integer stepGoal = 10000;
    Integer steps = 0;
    Set<String> usernames;
    Set<String> passwords;
    int c_account;
    Date today;
    String pressed = null;
    Foot right;
    Foot left;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sharedPref = this.getSharedPreferences(getString(R.string.shared_context),Context.MODE_PRIVATE);
        usernames = sharedPref.getStringSet(getString(R.string.username_save), new HashSet<String>());
        passwords = sharedPref.getStringSet(getString(R.string.pw_save), new HashSet<String>());
        steps = sharedPref.getInt(getString(R.string.step_save), 0);

        //set the login if the username is not saved otherwise just log in
        if(usernames.size() == 0){
            c_account = 0;
            setContentView(R.layout.login);
            Button log = (Button)findViewById(R.id.LogIn);
            log.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    EditText un = (EditText)findViewById(R.id.Username);
                    EditText pw = (EditText)findViewById(R.id.Password);
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
            usernames = new HashSet<String>();
            passwords = new HashSet<String>();
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

        right = new Foot("Right",this);
        left = new Foot("Left",this);


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
                    if(v.getTag().equals("right foot")){
                        right.bt_connect();
                    } else if(v.getTag().equals("left foot")) {
                        left.bt_connect();
                    }
                } else {
                    if(v.getTag().equals("right foot")){
                        right.bt_disconnect(right.get_devices().get(0));
                    } else if(v.getTag().equals("left foot")) {
                        left.bt_disconnect(left.get_devices().get(0));
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
        //TODO: Add authentication with server for user.
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

        TextView st = (TextView)findViewById(R.id.Steps);
        st.setText(steps.toString());
        st.bringToFront();

        TextView go = (TextView)findViewById(R.id.Goal);
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
        boolean focusable = true; // lets taps outside the popup also dismiss it
        final PopupWindow popupWindow = new PopupWindow(popupView, width, height, focusable);

        // show the popup window
        // which view you pass in doesn't matter, it is only used for the window tolken
        popupWindow.showAtLocation(v, Gravity.CENTER, 0, 0);

        Button log = (Button)popupView.findViewById(R.id.create);
        log.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText un = (EditText)popupView.findViewById(R.id.new_username);
                EditText pw = (EditText)popupView.findViewById(R.id.new_password);
                if(!un.getText().toString().equals("") && !pw.getText().toString().equals("")) {
                    popupWindow.dismiss();
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

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        //TODO: figure out which foot is requesting
        right.activityResult(requestCode,resultCode,data);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Don't forget to unregister the ACTION_FOUND receiver.
        //only if the connect button was used.
        if(right.check_connect()) {
            right.unregister();
        }

        if(left.check_connect()) {
            left.unregister();
        }

        SharedPreferences sharedPref = this.getSharedPreferences(getString(R.string.shared_context),Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(getString(R.string.step_save), steps);
        editor.putInt(getString(R.string.acc_save), c_account);
        editor.putStringSet(getString(R.string.username_save), usernames);
        editor.putStringSet(getString(R.string.pw_save), passwords);
        editor.apply();

    }


}
