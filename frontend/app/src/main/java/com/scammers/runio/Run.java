package com.scammers.runio;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Run {

    private final Context context;

    private final String playerId;

    private final double NEW_COORD_THRESHOLD = 5;

    private final double COMPLETE_LOOP_THRESHOLD = 100;

    // Mean radius of the Earth in kilometers
    private static final double EARTH_RADIUS_METERS = 6371000.0;
    // 6371 km * 1000 m/km

    ArrayList<LatLng> path;

    double distanceRan;

    double areaRan;

    // ChatGPT usage: NO
    public Run(String playerId, Context context) {
        this.playerId = playerId;
        this.path = new ArrayList<LatLng>();
        this.context = context;
    }

    // ChatGPT usage: NO
    public void addCoordinate(LatLng coord) {
        path.add(coord);
    }

    // ChatGPT usage: NO
    public boolean isNewCoord(LatLng coord) {
        return path.size() == 0 || distance(path.get(path.size() - 1), coord) >
                NEW_COORD_THRESHOLD;
    }

    // ChatGPT usage: NO
    public boolean isCompleteLoop() {
        if (path.size() <= 1) {
            return false;
        }
        LatLng start = path.get(0);
        LatLng end = path.get(path.size() - 1);
        return distance(start, end) < COMPLETE_LOOP_THRESHOLD;
    }


    // ChatGPT usage: YES
    public static double distance(LatLng c1, LatLng c2) {
        // Convert LatLng objects to radians
        double lat1Rad = Math.toRadians(c1.latitude);
        double lon1Rad = Math.toRadians(c1.longitude);
        double lat2Rad = Math.toRadians(c2.latitude);
        double lon2Rad = Math.toRadians(c2.longitude);

        // Calculate differences in latitude and longitude
        double deltaLat = lat2Rad - lat1Rad;
        double deltaLon = lon2Rad - lon1Rad;

        // Haversine formula
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                        Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        // Calculate the distance in meters
        double distance = EARTH_RADIUS_METERS * c;

        return distance;
    }

    // ChatGPT usage: NO
    public void end() {
        /*
         * This method will check if the player has run a valid loop and if so,
         * it will update the state of the map (Map.update()).  This state must
         * be sent to every player in the lobby. Each player will be notified
         *  when
         * someone finishes an activity.
         *
         * This can send API call to backend with the path
         * */

        // Connect path to starting point to complete polygon
        this.path.add(path.get(0));

        String url =
                "https://40.90.192.159:8081/run/player/" + this.playerId;
        MediaType mediaType =
                MediaType.parse("application/json; " +
                                        "charset=utf-8");
        Gson gson = new Gson();
        String pathJSON = gson.toJson(this.path);
        Log.d("Run", "path:" + pathJSON);
        RequestBody requestBody = RequestBody.create(pathJSON, mediaType);
        Request sendRun = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        MainActivity.client.newCall(sendRun).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.d("Run", "API fail");
            }

            @Override
            public void onResponse(@NonNull Call call,
                                   @NonNull Response response)
                    throws IOException {
                String responseBody = response.body().string();
                try {
                    JSONObject jsonObject = new JSONObject(responseBody);
                    DecimalFormat df = new DecimalFormat("#.##");
                    areaRan = Double.parseDouble(
                            df.format(jsonObject.getDouble("area")));
                    distanceRan = Double.parseDouble(
                            df.format(jsonObject.getDouble("distance")));
                    Log.d("Run", "run response:" + responseBody);
                    Log.d("Run",
                          "Area ran: " + areaRan + " km², Distance ran: " +
                                  distanceRan + " km");
                    Intent finishIntent =
                            new Intent(context, FinishActivity.class);
                    finishIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                                              Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    finishIntent.putParcelableArrayListExtra(
                            "path", path);
                    finishIntent.putExtra("distanceRan", distanceRan);
                    finishIntent.putExtra("areaRan", areaRan);
                    context.startActivity(finishIntent);
                } catch (JSONException e) {
                    Log.e("Run", "Error parsing JSON", e);

                }
            }
        });

    }
}
