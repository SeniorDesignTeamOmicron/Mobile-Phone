package edu.msoe.windorffj.logistep;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.widget.Toast;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Base64;

public class ServerConnect {

    private Context context;
    private String sUrl;
    private MediaType mediaType;
    private OkHttpClient client;
    private String authorization;

    ServerConnect(Context context) {
        this.context = context;
        this.sUrl = "http://" + MainActivity.server_address + "/api/";
        mediaType = MediaType.parse("application/json");
        client = new OkHttpClient();
    }

    public void post_step(String foot, double pressureB, double pressureT){
        ZonedDateTime zdt = ZonedDateTime.now();
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        String locationProvider = LocationManager.GPS_PROVIDER;
        double longitude = 0;
        double latitude = 0;
        try {
            Location lastKnownLocation = locationManager.getLastKnownLocation(locationProvider);
            latitude = lastKnownLocation.getLatitude();
            longitude = lastKnownLocation.getLongitude();
        } catch (SecurityException e){
            Toast.makeText(context,"Security Exception" + e.getMessage(),Toast.LENGTH_SHORT).show();
        } catch (NullPointerException e1){
            Toast.makeText(context,"Null Pointer Exception" + e1.getMessage(),Toast.LENGTH_SHORT).show();
        }
        RequestBody bodyB = RequestBody.create(mediaType, "[{\"datetime\": " + zdt + ",\"sensor_reading\":{\"location\":\"B\",\"pressure\":" + pressureB +
                ",\"shoe\":" + foot + "},\"location\":{\"longitude\": " + longitude + ",\"latitude\": " + latitude + "}]");
        Request requestB = new Request.Builder()
                .url(sUrl + "steps/")
                .post(bodyB)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", authorization)
                .addHeader("cache-control", "no-cache")
                .addHeader("Postman-Token", "cbaf5d7d-4259-4c10-aa79-5ce4ed07f187")
                .build();

        try {
            Response response = client.newCall(requestB).execute();
        } catch (IOException e){
            Toast.makeText(context,"IO Exception receiving JSON: " + e.getMessage(),Toast.LENGTH_SHORT).show();
        }

        RequestBody bodyT = RequestBody.create(mediaType, "[{\"datetime\": " + zdt + ",\"sensor_reading\":{\"location\":\"T\",\"pressure\":" + pressureT +
                ",\"shoe\":" + foot + "},\"location\":{\"longitude\": " + longitude + ",\"latitude\": " + latitude + "}]");
        Request requestT = new Request.Builder()
                .url(sUrl + "steps/")
                .post(bodyT)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", authorization)
                .addHeader("cache-control", "no-cache")
                .addHeader("Postman-Token", "cbaf5d7d-4259-4c10-aa79-5ce4ed07f187")
                .build();

        try {
            Response response = client.newCall(requestT).execute();
        } catch (IOException e){
            Toast.makeText(context,"IO Exception receiving JSON: " + e.getMessage(),Toast.LENGTH_SHORT).show();
        }
    }

    void authenticate(String username, String password, String email, double rShoeSize, double lShoeSize, String f_name, String l_name, int height, int weight, int goal){
        RequestBody body = RequestBody.create(mediaType, "{\"user\": {\"username\": " + username + ",\"email\": " + email + ",\"first_name\": " + f_name +
                ",\"last_name\": " + l_name + ",\"password\": " + password + "},\"right_shoe\": {\"foot\": \"R\",\"size\": " + rShoeSize + "},\"left_shoe\":{\"foot\": \"L\",\"size\": " +
                lShoeSize + "},\"height\": " + height + ",\"weight\": " + weight + ",\"step_goal\": "+ goal + "}");
        Request request = new Request.Builder()
                .url(sUrl + "user/")
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("cache-control", "no-cache")
                .addHeader("Postman-Token", "d74b7652-f01b-4dc8-8b2b-4cb91ee6525e")
                .build();
        String mid = username + ":" + password;
        Base64.Encoder encode = Base64.getEncoder();
        byte[] authEncBytes = encode.encode(mid.getBytes());
        authorization = new String (authEncBytes);
        try {
            Response response = client.newCall(request).execute();
            //TODO: use the response to determine if it is correct. and to send back to the main for user data
        } catch (IOException e){
            Toast.makeText(context,"IO Exception receiving JSON: " + e.getMessage(),Toast.LENGTH_SHORT).show();
        }
    }

    String getAuthentication(){
        return authorization;
    }

    void getUser(String un, String pw){
        String mid = un + ":" + pw;
        Base64.Encoder encode = Base64.getEncoder();
        byte[] authEncBytes = encode.encode(mid.getBytes());
        authorization = new String (authEncBytes);

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url("http://" + sUrl + "/api/users/")
                .get()
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Basic " + authorization)
                .addHeader("cache-control", "no-cache")
                .addHeader("Postman-Token", "06ed0396-e727-4a14-a44b-74b4cd40b74b")
                .build();

        try {
            Response response = client.newCall(request).execute();
            //TODO: use the response to set up the data for the user
        } catch (IOException e){
            Toast.makeText(context,"IO Exception receiving JSON: " + e.getMessage(),Toast.LENGTH_SHORT).show();
        }
    }

}
