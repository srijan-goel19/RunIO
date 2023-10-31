package com.scammers.runio;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

public class LobbyStatsActivity extends AppCompatActivity {

    final static String TAG = "LobbyStatsActivity";
    private ImageButton profileActivityButton;
    private ImageButton homeActivityButton;
    private Button addPlayerButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby_stats);

        homeActivityButton = findViewById(R.id.home_button_lobby_stats);
        homeActivityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Go to new activity page where activity is live
                Intent runningIntent = new Intent(LobbyStatsActivity.this, HomeActivity.class);
                startActivity(runningIntent);
            }
        });

        profileActivityButton = findViewById(R.id.profile_image_button_lobby_stats);
        String photoUrl = MainActivity.photoUrlPublic;
        Glide.with(this).load(photoUrl).into(profileActivityButton);
        profileActivityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Go to new activity page where activity is live
                Intent profileIntent = new Intent(LobbyStatsActivity.this, ProfileActivity.class);
                startActivity(profileIntent);
            }
        });

        addPlayerButton = findViewById(R.id.add_player_button);

        // Retrieve the lobby ID from the intent's extras
        String lobbyId = getIntent().getStringExtra("lobbyStatsId");

        // GET request to get Lobby info
        String url = "https://40.90.192.159:8081/lobby/" + lobbyId;
        Request getLobby = new Request.Builder()
                .url(url)
                .build();
        MainActivity.client.newCall(getLobby).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if(response.code() != 200){
                    throw new IOException("Unexpected code " + response);
                }
                try {
                    JSONObject responseBody = new JSONObject(response.body().string());
                    Lobby currentLobby = new Lobby(responseBody);
                    TextView textView = findViewById(R.id.lobby_name_lobby_stats); // Replace with the ID of your TextView
                    textView.setText(currentLobby.lobbyName); // The text you want to set
                    if (!currentLobby.lobbyLeaderId.equals(MainActivity.currentPlayer.getPlayerId())) {
                        addPlayerButton.setVisibility(View.INVISIBLE);
                    } else {
                        addPlayerButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                Intent addPlayerIntent = new Intent(LobbyStatsActivity.this, AddPlayerActivity.class);
                                addPlayerIntent.putExtra("lobbyIdAddPlayer", lobbyId);
                                startActivity(addPlayerIntent);
                            }
                        });
                    }

                    //TODO get Player from API call using entry.getKey() -> which gives you playerID

                    //Display playerStats in screen
                    for(Map.Entry<String, PlayerLobbyStats> entry: currentLobby.playerMap.entrySet()) {
                        PlayerLobbyStats playerLobbyStats = entry.getValue();
                        double distanceCovered = playerLobbyStats.distanceCovered;
                        double totalArea = playerLobbyStats.totalArea;
                        int color = PlayerLobbyStats.lowerAlpha(playerLobbyStats.color);

                        LinearLayout parentLayout = findViewById(R.id.lobbyStatsLinearLayout);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // Create a new TextView
                                TextView textView = new TextView(LobbyStatsActivity.this);

                                // Set text properties
                                textView.setText("Kilometers ran: " + distanceCovered/1000 + "km\nArea Claimed: " + totalArea + "m^2");
                                textView.setTextSize(20); // Set text size

                                // Set background color
                                textView.setBackgroundColor(color); // Set your desired background color

                                // Set padding
                                int paddingInDp = 16; // Convert your padding in dp to pixels
                                float scale = getResources().getDisplayMetrics().density;
                                int paddingInPixels = (int) (paddingInDp * scale + 0.5f);
                                textView.setPadding(paddingInPixels, paddingInPixels, paddingInPixels, paddingInPixels);

                                // Add the TextView to your layout
                                parentLayout.addView(textView);
                            }
                        });

                    }
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }
}