package edu.msoe.windorffj.logistep;


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
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    Integer stepGoal = 10000;
    Integer steps = 0;
    List<String> usernames;
    List<String> passwords;
    int c_account;
    Date today;
    String pressed = null;
    Foot right;
    Foot left;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //set the login if the username is not saved otherwise just log in
        if(usernames == null){
            c_account = 0;
            setContentView(R.layout.login);
            Button log = (Button)findViewById(R.id.LogIn);
            log.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    EditText un = (EditText)findViewById(R.id.Username);
                    EditText pw = (EditText)findViewById(R.id.Password);
                    if(un.getText().toString() != null && pw.getText().toString() != null) {
                        login(un.getText().toString(), pw.getText().toString());
                    } else {
                        CharSequence text = "Enter a Username and Password";
                        int duration = Toast.LENGTH_SHORT;

                        Toast toast = Toast.makeText(MainActivity.this, text, duration);
                        toast.show();
                    }
                }
            });
            usernames = new ArrayList<String>();
            passwords = new ArrayList<String>();
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
                    if(v.getTag().equals("RightFoot")){
                        right.bt_connect();
                    } else if(v.getTag().equals("LeftFoot")) {
                        left.bt_connect();
                    }
                } else {
                    if(v.getTag().equals("RightFoot")){
                        right.bt_disconnect(right.get_devices().get(0));
                    } else if(v.getTag().equals("LeftFoot")) {
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
        inflater.inflate(R.menu.shoe_connection, popup.getMenu());
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
                    inflater.inflate(R.menu.account_menu, popup.getMenu());
                    popup.show();
                    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            c_account = item.getItemId();
                            return true;
                        }
                    });
                    Menu mine = popup.getMenu();
                    for(int i = 0; i < usernames.size(); i++){
                        mine.add(usernames.get(i));
                    }
                }
                return true;
            }
        });
    }

    private void login(String m_username, String m_password){
        //TODO: Add authentication with server for user.
        if(usernames.contains(m_username)){
            for(int i = 0; i < usernames.size(); i++){
                if(usernames.get(i).equals(m_username)){
                    c_account = i;
                }
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

    private void change_account(String username){
        for(int i = 0; i < usernames.size(); i++){
            if(usernames.get(i).equals(username)){
                c_account = i;
            }
        }
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
    }
}
