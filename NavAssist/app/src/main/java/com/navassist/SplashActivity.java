package com.navassist;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    public static final String MODE_BLIND = "blind";
    public static final String MODE_DEAF = "deaf";
    public static final String MODE_MOBILITY = "mobility";
    public static final String EXTRA_MODE = "disability_mode";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        LinearLayout cardBlind = findViewById(R.id.card_blind);
        LinearLayout cardDeaf = findViewById(R.id.card_deaf);
        LinearLayout cardMobility = findViewById(R.id.card_mobility);
        LinearLayout cardGuardian = findViewById(R.id.card_guardian);

        cardBlind.setOnClickListener(v -> launchMainApp(MODE_BLIND));
        cardDeaf.setOnClickListener(v -> launchMainApp(MODE_DEAF));
        cardMobility.setOnClickListener(v -> launchMainApp(MODE_MOBILITY));
        cardGuardian.setOnClickListener(v -> launchGuardian());
    }

    private void launchMainApp(String mode) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(EXTRA_MODE, mode);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void launchGuardian() {
        Intent intent = new Intent(this, GuardianActivity.class);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
