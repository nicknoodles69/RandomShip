package com.example.randomship;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private boolean isPlayerTurn;

    private GridView opponentBoardGridView;
    private GridView playerBoardGridView;
    private Button startButton;
    private Button resetButton;
    private Button homeButton;

    private FloatingActionButton mFabInfo;

    private ArrayAdapter<String> opponentBoardAdapter;
    private ArrayAdapter<String> playerBoardAdapter;
    private ArrayList<String> oList;
    private ArrayList<String> pList;
    private int currentPlayerScore;
    private int currentOpponentScore;

    private int mapBlocks=10;
    private int ships=3;

    private ArrayList<String> displayedOpponentBoardList;
    private ArrayList<String> displayedPlayerBoardList;

    private static final String PREFS_NAME = "MyPrefsFile";
    private static final String KEY_WINS = "wins";
    private static final String KEY_LOSSES = "losses";
    private static final String KEY_PLAYED = "played";

    private SharedPreferences prefs;

    private HashSet opponentTriedPositions;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        isPlayerTurn = true;

        updateTurnIndicator();

        opponentTriedPositions = new HashSet<>();

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        mFabInfo = findViewById(R.id.fab_info);

        mFabInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create the game instructions dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Game Rules ");
                builder.setMessage("1) Each player gets a turn to guess the position of a ship on their map.\n\n" +
                        "2)  Hit = Ship Appears, Miss = (?) Disappears, and S = remaining ships at the end of the game\n\n" +
                        "3) To win you must find all 3 ships before the opponent \n\n" +
                        "4) Beware: Each time a player successfully hits a ship they gain an extra turn.. \n\n" +
                        "5) Have fun!");
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Dismiss the dialog when the OK button is clicked
                        dialog.dismiss();
                    }
                });

                // Show the dialog
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });

        opponentBoardGridView = findViewById(R.id.grid_view_opponent);
        playerBoardGridView = findViewById(R.id.grid_view_player);
        startButton = findViewById(R.id.button_start_game);
        resetButton = findViewById(R.id.button_reset_game);
        homeButton = findViewById(R.id.button_home);

        displayStats();

        // Initialize the player and opponent game boards
        oList = new ArrayList<>();
        pList = new ArrayList<>();
        for (int i = 0; i < mapBlocks; i++) {
            oList.add("");
            pList.add("");
        }

        // Initialize the Displayed ship positions
        displayedOpponentBoardList = new ArrayList<>();
        displayedPlayerBoardList = new ArrayList<>();
        for (int i = 0; i < mapBlocks; i++) {
            displayedOpponentBoardList.add("");
            displayedPlayerBoardList.add("");
        }

        opponentBoardAdapter = new GameBoardAdapter(this, R.layout.grid_item, R.id.text_view, displayedOpponentBoardList);
        opponentBoardGridView.setAdapter(opponentBoardAdapter);

        playerBoardAdapter = new GameBoardAdapter(this, R.layout.grid_item, R.id.text_view, displayedPlayerBoardList);
        playerBoardGridView.setAdapter(playerBoardAdapter);

        // Set a click listener for the opponent game board
        playerBoardGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (isPlayerTurn && displayedPlayerBoardList.get(position).equals("")) {
                if (displayedPlayerBoardList.get(position).equals("")) {
                    if (pList.get(position).equals("S")) {
                        displayedPlayerBoardList.set(position, "X");
                        playerBoardAdapter.notifyDataSetChanged();

                        currentPlayerScore++;

                        if (currentPlayerScore >= ships) {
                            Toast.makeText(MainActivity.this, "You won!", Toast.LENGTH_SHORT).show();
                            revealAllShips();
                            enableGameplay();
                            opponentBoardGridView.setEnabled(false);
                            playerBoardGridView.setEnabled(false);


                            // Increment the win count
                            int wins = prefs.getInt(KEY_WINS, 0);
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putInt(KEY_WINS, wins + 1);
                            editor.apply();

                            // Increment the played count
                            int played = prefs.getInt(KEY_PLAYED, 0);
                            editor.putInt(KEY_PLAYED, played + 1);
                            editor.apply();
                        }
                    } else {
                        displayedPlayerBoardList.set(position, "O");
                        isPlayerTurn = false;
                        updateTurnIndicator();
                        opponentTurn();
                        playerBoardAdapter.notifyDataSetChanged();

                    }
                }
              }
            }
        });

        // Set a click listener for the "Start Game" button
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Place the player's and opponent's ships randomly on the board
                placePlayerShips();
                placeOpponentShips();

                // Enable gameplay
                enableGameplay();
            }
        });

        // Set a click listener for the "Reset Game" button
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetGame();
            }
        });

        // Disable gameplay until the game is started
        disableGameplay();

        homeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                startActivity(intent);
            }
        });


    }

    public class GameBoardAdapter extends ArrayAdapter<String> {
        private Context context;
        private List<String> values;

        public GameBoardAdapter(@NonNull Context context, int resource, int textViewResourceId, @NonNull List<String> objects) {
            super(context, resource, textViewResourceId, objects);
            this.context = context;
            this.values = objects;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = inflater.inflate(R.layout.grid_item, parent, false);

            TextView textView = rowView.findViewById(R.id.text_view);
            ImageView imageView = rowView.findViewById(R.id.image_view);

            String value = values.get(position);
            textView.setText(value);

            if ("X".equals(value)) {
                textView.setVisibility(View.INVISIBLE);
                imageView.setImageResource(R.drawable.ship_piece); // Replace with your hit image
                imageView.setVisibility(View.VISIBLE);
                imageView.setAlpha(0f);
                imageView.animate().alpha(1f).setDuration(1000);
            } else if ("O".equals(value)) {

                textView.setAlpha(1f);
                textView.animate().alpha(0f).setDuration(2000);
                textView.setVisibility(View.INVISIBLE);
                updateTurnIndicator();
            }else {
                imageView.setVisibility(View.INVISIBLE);
            }

            return rowView;
        }
    }


    private void placeOpponentShips() {
        Random random = new Random();

        // Place a total of 5 ships
        int shipsPlaced = 0;
        while (shipsPlaced < ships) {
            int position = random.nextInt(mapBlocks);

            // Check if the selected position is already occupied by a ship
            if (!oList.get(position).equals("S")) {
                // If the position is not occupied, place a ship there
                oList.set(position, "S");
                shipsPlaced++;
            }
        }

        opponentBoardAdapter.notifyDataSetChanged();
    }

    private void placePlayerShips() {
        Random random = new Random();

        // Place a total of 5 ships
        int shipsPlaced = 0;
        while (shipsPlaced < ships) {
            int position = random.nextInt(mapBlocks);

            // Check if the selected position is already occupied by a ship
            if (!pList.get(position).equals("S")) {
                // If the position is not occupied, place a ship there
                pList.set(position, "S");
                shipsPlaced++;
            }
        }

        playerBoardAdapter.notifyDataSetChanged();
    }

    private void displayStats(){
        int wins = prefs.getInt(KEY_WINS, 0);
        int losses = prefs.getInt(KEY_LOSSES, 0);
        int played = prefs.getInt(KEY_PLAYED, 0);

        TextView statsTextView = findViewById(R.id.text_view_stats);
        String statsString = "Wins: " + wins + " | Losses: " + losses + " | Played: " + played;
        statsTextView.setText(statsString);
    }

    private void resetGame() {
        opponentTriedPositions.clear();
        disableGameplay();
        displayStats();
        isPlayerTurn = true;
        updateTurnIndicator();

        // Clear the player and opponent game boards
        oList.clear();
        pList.clear();
        displayedOpponentBoardList.clear();
        displayedPlayerBoardList.clear();

        for (int i = 0; i < mapBlocks; i++) {
            oList.add("");
            pList.add("");
            displayedOpponentBoardList.add(""); // Reset displayed player board
            displayedPlayerBoardList.add(""); // Reset displayed opponent board
        }

        opponentBoardAdapter.notifyDataSetChanged();
        playerBoardAdapter.notifyDataSetChanged();

        currentPlayerScore = 0;
        currentOpponentScore = 0;

        // Reset the alpha for all grid items
        for (int i = 0; i < opponentBoardGridView.getChildCount(); i++) {
            View view = opponentBoardGridView.getChildAt(i);
            if (view != null) {
                view.setAlpha(1f);
            }
        }
        for (int i = 0; i < playerBoardGridView.getChildCount(); i++) {
            View view = playerBoardGridView.getChildAt(i);
            if (view != null) {
                view.setAlpha(1f);
            }
        }
    }

    private void enableGameplay() {
        opponentBoardGridView.setEnabled(true);
        playerBoardGridView.setEnabled(true);
        startButton.setEnabled(false);
        resetButton.setEnabled(true);
    }

    private void disableGameplay() {
        opponentBoardGridView.setEnabled(false);
        playerBoardGridView.setEnabled(false);
        startButton.setEnabled(true); // Disable the Start button
        resetButton.setEnabled(false); // Enable the Reset button
    }

    private void opponentTurn() {
        final Handler handler = new Handler();
        Random random = new Random();
        boolean extraTurn;

        if (!isPlayerTurn) {

            do {
                int position = random.nextInt(mapBlocks);
                extraTurn = false;

                // Check if the position has not been tried before
                if (!opponentTriedPositions.contains(position)) {
                    opponentTriedPositions.add(position); // Add the position to the tried positions set

                if (displayedOpponentBoardList.get(position).equals("")) {
                    final int finalPosition = position;

                    if (oList.get(position).equals("S")) {
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                displayedOpponentBoardList.set(finalPosition, "X");
                                opponentBoardAdapter.notifyDataSetChanged();

                                currentOpponentScore++;

                                if (currentOpponentScore >= ships) {
                                    Toast.makeText(MainActivity.this, "You lost!", Toast.LENGTH_SHORT).show();
                                    revealAllShips();
                                    enableGameplay();
                                    opponentBoardGridView.setEnabled(false);
                                    playerBoardGridView.setEnabled(false);

                                    // Increment the loss count
                                    int losses = prefs.getInt(KEY_LOSSES, 0);
                                    SharedPreferences.Editor editor = prefs.edit();
                                    editor.putInt(KEY_LOSSES, losses + 1);
                                    editor.apply();

                                    // Increment the played count
                                    int played = prefs.getInt(KEY_PLAYED, 0);
                                    editor.putInt(KEY_PLAYED, played + 1);
                                    editor.apply();
                                } else {
                                    // Give the opponent an extra turn if they hit a ship
                                    opponentTurn();
                                    updateTurnIndicator();
                                    opponentBoardAdapter.notifyDataSetChanged();
                                }
                            }
                        }, 1500);
                    } else {
                        displayedOpponentBoardList.set(position, "O");
                        extraTurn = false;
                        isPlayerTurn = true;
                        updateTurnIndicator();
                        opponentBoardAdapter.notifyDataSetChanged();
                    }
                  }
                }else {
                    // If the opponent selects an already hit or missed position, give them another turn
                    extraTurn = true;
                    updateTurnIndicator();
                    opponentBoardAdapter.notifyDataSetChanged();
                }
            } while (extraTurn);
        }
    }

    private void revealAllShips() {
        for (int i = 0; i < mapBlocks; i++) {
            if (oList.get(i).equals("S") && displayedOpponentBoardList.get(i).equals("")) {
                displayedOpponentBoardList.set(i, "S");
            }
            if (pList.get(i).equals("S") && displayedPlayerBoardList.get(i).equals("")) {
                displayedPlayerBoardList.set(i, "S");
            }
        }
        opponentBoardAdapter.notifyDataSetChanged();
        playerBoardAdapter.notifyDataSetChanged();
    }

    private void updateTurnIndicator() {
        TextView pTurnIndicator = findViewById(R.id.player_view_turn);
        TextView oTurnIndicator = findViewById(R.id.opponent_view_turn);
        if (isPlayerTurn) {
            pTurnIndicator.setText("Player's Turn");
            pTurnIndicator.setVisibility(TextView.VISIBLE);
            oTurnIndicator.setVisibility(TextView.INVISIBLE);
        } else {
            oTurnIndicator.setText("Opponent's Turn");
            oTurnIndicator.setVisibility(TextView.VISIBLE);
            pTurnIndicator.setVisibility(TextView.INVISIBLE);
        }
    }

}
