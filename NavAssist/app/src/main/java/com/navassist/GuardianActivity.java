package com.navassist;

import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import java.text.SimpleDateFormat;
import java.util.*;

public class GuardianActivity extends AppCompatActivity {

    private TextView tvUserStatus;
    private TextView tvUserLocation;
    private TextView tvLastSeen;
    private CardView cardSOSAlert;
    private TextView tvSOSTime;
    private Button btnCall;
    private Button btnMessage;
    private Button btnBack;
    private LinearLayout activityLog;

    private boolean sosReceived = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guardian);

        initViews();
        startMockLocationUpdates();
        checkForSOS();
    }

    private void initViews() {
        tvUserStatus = findViewById(R.id.tv_user_status);
        tvUserLocation = findViewById(R.id.tv_user_location);
        tvLastSeen = findViewById(R.id.tv_last_seen);
        cardSOSAlert = findViewById(R.id.card_sos_alert);
        tvSOSTime = findViewById(R.id.tv_sos_time);
        btnCall = findViewById(R.id.btn_call_user);
        btnMessage = findViewById(R.id.btn_message_user);
        btnBack = findViewById(R.id.btn_guardian_back);
        activityLog = findViewById(R.id.activity_log);

        btnBack.setOnClickListener(v -> finish());
        btnCall.setOnClickListener(v -> {
            Toast.makeText(this, "📞 Calling Ravi Kumar...", Toast.LENGTH_SHORT).show();
        });
        btnMessage.setOnClickListener(v -> {
            Toast.makeText(this, "💬 Message sent to Ravi!", Toast.LENGTH_SHORT).show();
        });

        cardSOSAlert.setVisibility(View.GONE);

        addActivityLog("✅ App opened — Home screen", "2 minutes ago", "#00C97B");
        addActivityLog("📸 Camera scanner used — Hospital entrance detected", "14 minutes ago", "#0057FF");
        addActivityLog("🗺 Navigation started — Apollo Hospital", "18 minutes ago", "#FFD600");
        addActivityLog("✅ Arrived at destination safely", "25 minutes ago", "#00C97B");
    }

    private void startMockLocationUpdates() {
        // Simulate live location updates (replace with Firebase real-time in production)
        String[] locations = {
            "Anna Nagar, Chennai • GPS Active",
            "Koyambedu, Chennai • Moving",
            "Arumbakkam, Chennai • Stopped",
            "Anna Nagar West, Chennai • GPS Active"
        };

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            int index = 0;
            @Override
            public void run() {
                if (!isFinishing()) {
                    tvUserLocation.setText("📍 " + locations[index % locations.length]);
                    tvLastSeen.setText("Updated " + new SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(new Date()));
                    index++;
                    new Handler(Looper.getMainLooper()).postDelayed(this, 5000);
                }
            }
        }, 5000);
    }

    // Called when Firebase push notification received (SOS)
    public void showSOSAlert(String message, String location) {
        runOnUiThread(() -> {
            cardSOSAlert.setVisibility(View.VISIBLE);
            tvSOSTime.setText("Received at " + new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date()));
            tvUserStatus.setText("🚨 SOS ALERT");
            tvUserStatus.setTextColor(getColor(R.color.sos_red));

            // Vibrate guardian's phone intensively
            Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (v != null) {
                long[] pattern = {0, 500, 200, 500, 200, 500, 200, 1000};
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createWaveform(pattern, 3)); // repeat 3 times
                } else {
                    v.vibrate(pattern, -1);
                }
            }

            addActivityLog("🚨 SOS ALERT SENT — " + location, "Just now", "#FF3C5F");
        });
    }

    private void checkForSOS() {
        // In production: listen to Firebase Realtime Database for SOS events
        // Example Firebase code:
        /*
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("sos_alerts");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String location = snapshot.child("location").getValue(String.class);
                    String message = snapshot.child("message").getValue(String.class);
                    showSOSAlert(message, location);
                }
            }
            @Override
            public void onCancelled(DatabaseError error) {}
        });
        */
    }

    private void addActivityLog(String text, String time, String colorHex) {
        if (activityLog == null) return;
        View item = getLayoutInflater().inflate(R.layout.item_activity, activityLog, false);
        TextView tvText = item.findViewById(R.id.tv_activity_text);
        TextView tvTime = item.findViewById(R.id.tv_activity_time);
        View dot = item.findViewById(R.id.activity_dot);

        tvText.setText(text);
        tvTime.setText(time);
        dot.setBackgroundColor(android.graphics.Color.parseColor(colorHex));

        activityLog.addView(item);
    }
}
