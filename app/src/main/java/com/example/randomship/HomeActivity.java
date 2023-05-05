package com.example.randomship;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class HomeActivity extends AppCompatActivity implements Animation.AnimationListener {

    private TextView titleLabel;
    private ImageView shipIcon;

    private SharedPreferences prefs;

    String sharedPrefFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        titleLabel = findViewById(R.id.label_title);
        shipIcon = findViewById(R.id.imageview_ship);

        Animation rotate = AnimationUtils.loadAnimation(this, R.anim.rotate);
        rotate.setAnimationListener(this);
        titleLabel.startAnimation(rotate);

        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade);
        fadeIn.setAnimationListener(this);
        shipIcon.startAnimation(fadeIn);


        sharedPrefFile ="MyPrefsFile" ;

        prefs = getSharedPreferences(sharedPrefFile, MODE_PRIVATE);

        Button playButton = findViewById(R.id.button_play);

        Button resetButton = findViewById(R.id.button_reset);

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });

        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Clear preferences
                SharedPreferences.Editor preferencesEditor = prefs.edit();
                preferencesEditor.clear();
                preferencesEditor.apply();
                Toast.makeText(HomeActivity.this, "Reset Successful", Toast.LENGTH_SHORT).show();
            }
        });

    }

    @Override
    public void onAnimationStart(Animation animation) {
        titleLabel.setVisibility(View.VISIBLE);
        shipIcon.setVisibility(View.VISIBLE);
    }

    @Override
    public void onAnimationEnd(Animation animation) {

    }

    @Override
    public void onAnimationRepeat(Animation animation) {

    }
}