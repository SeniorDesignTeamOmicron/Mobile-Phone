package edu.msoe.windorffj.logistep;

import android.content.Context;
import android.widget.Toast;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;

public class ServerConnect {

    private Context context;
    private String sUrl;
    private MediaType mediaType;
    private OkHttpClient client;
    private String authorization;
    private serverThread st;

    public ServerConnect(Context context,String un, String pw) {
        this.context = context;
        this.sUrl = "http://" + MainActivity.server_address + "/api/";
        mediaType = MediaType.parse("application/json");
        client = new OkHttpClient();
        st = new serverThread();
        st.start();
        String mid = un + ":" + pw;
        Base64.Encoder encode = Base64.getEncoder();
        byte[] authEncBytes = encode.encode(mid.getBytes());
        authorization = new String (authEncBytes);
    }

    public void post_step(String foot, double pressureB, double pressureT){
        Date d = Calendar.getInstance().getTime();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        String ds = df.format(d);
        SimpleDateFormat tf = new SimpleDateFormat("HHmmss");
        String ts = tf.format(d);
        //TODO: use zoneddatetime
        double longitude = 0;
        double latitude = 0;
        RequestBody body = RequestBody.create(mediaType, "[{\"date\": " + ds + ",\"time\": " + ts + ",\"sensor_reading\": {\"shoe\": " + foot + ",\"pressure\": " + pressureT + ",\"location\": \"T\"" +
                "},\"location\": {\"latitude\": " + latitude + ",\"longitude\": " + longitude + "}},{\"date\": " + ds + ",\"time\": " + ts + ",\"sensor_reading\": {\"shoe\": " + foot +
                ",\"pressure\": " + pressureB + ",\"location\": \"B\"},\"location\": {\"latitude\": " + latitude + ",\"longitude\": " + longitude + "}}]");
        Request request = new Request.Builder()
                .url(sUrl + "steps/")
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", authorization)
                .addHeader("cache-control", "no-cache")
                .addHeader("Postman-Token", "cbaf5d7d-4259-4c10-aa79-5ce4ed07f187")
                .build();

        try {
            Response response = client.newCall(request).execute();
        } catch (IOException e){
            Toast.makeText(context,"IO Exception receiving JSON: " + e.getMessage(),Toast.LENGTH_SHORT).show();
        }
    }

    public void authenticate(String username, String password, String email, double rShoeSize, double lShoeSize){
        RequestBody body = RequestBody.create(mediaType, "{\"user\": {\"username\": \"" + username + "\",\"email\": \"" + email + "\",\"password\": \"" + password +
                "\"},\"right_shoe\": {\"foot\": \"R\",\"size\": " + rShoeSize + "},\"left_shoe\":{\"foot\": \"L\",\"size\": " + lShoeSize + "}}");
        Request request = new Request.Builder()
                .url(sUrl + "user/")
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("cache-control", "no-cache")
                .addHeader("Postman-Token", "1a1e0553-6c4e-451e-94a2-f19be604ea6d")
                .build();

        try {
            Response response = client.newCall(request).execute();
            //TODO: use the response to determine if it is correct
            //authorization = response.message();
        } catch (IOException e){
            Toast.makeText(context,"IO Exception receiving JSON: " + e.getMessage(),Toast.LENGTH_SHORT).show();
        }
    }

    public String getAuthentication(){
        return authorization;
    }

    private static class serverThread extends Thread {
        //TODO: make this take bluetooth data and send it to the server
        public serverThread(){

        }

        @Override
        public void run() {

        }

        private void cancel() {

        }
    }

    public void closeThread(){
        st.cancel();
    }

}
