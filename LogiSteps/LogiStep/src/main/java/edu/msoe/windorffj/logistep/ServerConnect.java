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
import java.time.DayOfWeek;
import java.time.Month;
import java.time.Year;
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

    Response getUser(final String un, final String pw) {
        String mid = un + ":" + pw;
        Base64.Encoder encode = Base64.getEncoder();
        byte[] authEncBytes = encode.encode(mid.getBytes());
        authorization = new String(authEncBytes);

        class run implements Runnable {

            volatile Response response;

            @Override
            public void run() {
                try {
                    OkHttpClient client = new OkHttpClient();

                    Request request = new Request.Builder()
                            .url(sUrl + "user/" + un + "/")
                            .get()
                            .addHeader("Content-Type", "application/json")
                            .addHeader("Authorization", "Basic " + authorization)
                            .addHeader("cache-control", "no-cache")
                            .addHeader("Postman-Token", "06ed0396-e727-4a14-a44b-74b4cd40b74b")
                            .build();

                    try {
                        response = client.newCall(request).execute();
                        //TODO: use the response to set up the data for the user
                    } catch (IOException e) {
                        Toast.makeText(context, "IO Exception receiving JSON: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            private Response get_response() {
                return response;
            }

        }

        run r = new run();

        Thread thread = new Thread(r);

        thread.start();
        Response ret = null;
        try {
            thread.join();
            ret = r.get_response();
        } catch (InterruptedException e) {
            Toast.makeText(context, "InterruptedException: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        return ret;
    }

    public Response getSteps(){
        ZonedDateTime zdt = ZonedDateTime.now();
        Month m = zdt.getMonth();
        int mm = m.getValue();
        DayOfWeek d = zdt.getDayOfWeek();
        int dd = d.getValue();
        int yyyy = zdt.getYear();
        String date = mm + "-" + dd + "-" + yyyy;
        OkHttpClient client = new OkHttpClient();
        Response response = null;

        Request request = new Request.Builder()
                .url(sUrl + "steps/summary/?date=" + date)
                .get()
                .addHeader("Authorization", "Basic " + authorization)
                .addHeader("cache-control", "no-cache")
                .addHeader("Postman-Token", "f0460bcb-615b-4f31-adfc-83ac9b8d85d9")
                .build();

        try {
            response = client.newCall(request).execute();
            //TODO: use response to get steps for the day
        } catch (IOException e){
            Toast.makeText(context, "IO Exception receiving JSON: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        return response;
    }

}
