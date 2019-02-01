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
import java.text.SimpleDateFormat;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.Date;
import java.util.TimeZone;

class ServerConnect {

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

    void post_step(String foot, double pressureB, double pressureT) {
        class runa implements Runnable {
            private String foot;
            private double pressureB;
            private double pressureT;

            private runa(String foot, double pressureB, double pressureT){
                this.foot = foot;
                this.pressureB = pressureB;
                this.pressureT = pressureT;
            }

            @Override
            public void run() {
                try {
                    ZonedDateTime zdt = ZonedDateTime.now();
                    LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                    String locationProvider = LocationManager.GPS_PROVIDER;
                    double longitude = 0;
                    double latitude = 0;
                    try {
                        Location lastKnownLocation = locationManager.getLastKnownLocation(locationProvider);
                        latitude = lastKnownLocation.getLatitude();
                        longitude = lastKnownLocation.getLongitude();
                    } catch (SecurityException e) {
                        Toast.makeText(context, "Security Exception" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    } catch (NullPointerException e1) {
                        Toast.makeText(context, "Null Pointer Exception" + e1.getMessage(), Toast.LENGTH_SHORT).show();
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
                    } catch (IOException e) {
                        Toast.makeText(context, "IO Exception receiving JSON: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
                    } catch (IOException e) {
                        Toast.makeText(context, "IO Exception receiving JSON: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
        runa r = new runa(foot, pressureB, pressureT);

        Thread thread = new Thread(r);

        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            Toast.makeText(context, "InterruptedException: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    Response authenticate(String username, String password, String email, double rShoeSize, double lShoeSize, String f_name, String l_name, int height, int weight, int goal) {

        class runa implements Runnable {

            volatile private Response response;
            private String un;
            private String pw;
            private String em;
            private double rs;
            private double ls;
            private String fn;
            private String ln;
            private int height;
            private int weight;
            private int goal;

            private runa(String username, String password, String email, double rShoeSize, double lShoeSize, String f_name, String l_name, int height, int weight, int goal){
                un = username;
                pw = password;
                em = email;
                rs = rShoeSize;
                ls = lShoeSize;
                fn = f_name;
                ln = l_name;
                this.height = height;
                this.weight = weight;
                this.goal = goal;
            }

            @Override
            public void run() {
                try {
                    RequestBody body = RequestBody.create(mediaType, "{\"user\": {\"username\": " + un + ",\"email\": " + em + ",\"first_name\": " + fn +
                            ",\"last_name\": " + ln + ",\"password\": " + pw + "},\"right_shoe\": {\"foot\": \"R\",\"size\": " + rs + "},\"left_shoe\":{\"foot\": \"L\",\"size\": " +
                            ls + "},\"height\": " + height + ",\"weight\": " + weight + ",\"step_goal\": " + goal + "}");
                    Request request = new Request.Builder()
                            .url(sUrl + "user/")
                            .post(body)
                            .addHeader("Content-Type", "application/json")
                            .addHeader("cache-control", "no-cache")
                            .addHeader("Postman-Token", "d74b7652-f01b-4dc8-8b2b-4cb91ee6525e")
                            .build();
                    String mid = un + ":" + pw;
                    Base64.Encoder encode = Base64.getEncoder();
                    byte[] authEncBytes = encode.encode(mid.getBytes());
                    authorization = new String(authEncBytes);
                    try {
                        response = client.newCall(request).execute();
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
        runa r = new runa(username, password, email, rShoeSize, lShoeSize, f_name, l_name, height, weight, goal);

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

    String getAuthentication() {
        return authorization;
    }

    Response getUser(final String un, final String pw) {
        String mid = un + ":" + pw;
        Base64.Encoder encode = Base64.getEncoder();
        byte[] authEncBytes = encode.encode(mid.getBytes());
        authorization = new String(authEncBytes);

        class runa implements Runnable {

            volatile private Response response;

            @Override
            public void run() {
                try {
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

        runa r = new runa();

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

    Response getSteps() {
        Date d = new Date(System.currentTimeMillis());

        //conversion to +0:00
        SimpleDateFormat sdf;
        sdf = new SimpleDateFormat("MM-dd-yyyy");
        sdf.setTimeZone(TimeZone.getTimeZone("CET"));
        String date = sdf.format(d);

        class runa implements Runnable {

            volatile private Response response;
            private String date;

            private runa(String d){
                date = d;
            }

            @Override
            public void run() {
                try {
                    Request request = new Request.Builder()
                            .url(sUrl + "steps/summary/?date=" + date)
                            .get()
                            .addHeader("Authorization", "Basic " + authorization)
                            .addHeader("cache-control", "no-cache")
                            .addHeader("Postman-Token", "f0460bcb-615b-4f31-adfc-83ac9b8d85d9")
                            .build();
                    try {
                        response = client.newCall(request).execute();
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
        runa r = new runa(date);

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


}
