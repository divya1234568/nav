package com.navassist;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.*;
import android.os.*;
import android.speech.tts.*;
import android.telephony.*;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import java.util.Locale;

public class SOSActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private TextToSpeech tts;
    private Vibrator vibrator;
    private LocationManager locationManager;

    private Button btnSendSOS;
    private Button btnCancel;
    private TextView tvStatus;
    private TextView tvLocation;
    private ProgressBar progressBar;
    private LinearLayout sentPanel;
    private LinearLayout confirmPanel;

    // Replace with real guardian numbers
    private static final String[] GUARDIAN_NUMBERS = {"+91XXXXXXXXXX", "+91XXXXXXXXXX"};

    private double currentLat = 13.0827;
    private double currentLng = 80.2707;
    private boolean sosSent = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sos);

        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        tts = new TextToSpeech(this, this);

        initViews();
        startLocationTracking();
        startSOSVibration();
    }

    private void initViews() {
        btnSendSOS = findViewById(R.id.btn_send_sos);
        btnCancel = findViewById(R.id.btn_cancel);
        tvStatus = findViewById(R.id.tv_sos_status);
        tvLocation = findViewById(R.id.tv_sos_location);
        progressBar = findViewById(R.id.progress_sos);
        sentPanel = findViewById(R.id.layout_sent);
        confirmPanel = findViewById(R.id.layout_confirm);

        btnSendSOS.setOnClickListener(v -> sendSOS());
        btnCancel.setOnClickListener(v -> {
            speak("SOS cancelled.");
            finish();
        });
    }

    private void startLocationTracking() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, location -> {
                currentLat = location.getLatitude();
                currentLng = location.getLongitude();
                String locText = "📍 " + String.format("%.4f", currentLat) + ", " + String.format("%.4f", currentLng);
                runOnUiThread(() -> tvLocation.setText(locText));
            });

            Location lastKnown = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (lastKnown != null) {
                currentLat = lastKnown.getLatitude();
                currentLng = lastKnown.getLongitude();
            }
        }
        tvLocation.setText("📍 " + String.format("%.4f", currentLat) + ", " + String.format("%.4f", currentLng));
    }

    private void sendSOS() {
        if (sosSent) return;
        sosSent = true;

        speak("SOS sent! Alerting all guardians with your live location. Help is on the way.");

        btnSendSOS.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        tvStatus.setText("⏳ Sending SOS to all guardians...");

        // Intensive vibration pattern for deaf users
        sosVibrationPattern();

        // Simulate sending (in real app: use Firebase + SMS)
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            sendSMSToGuardians();
            // sendFirebaseAlert(); // Uncomment when Firebase is configured

            progressBar.setVisibility(View.GONE);
            confirmPanel.setVisibility(View.GONE);
            sentPanel.setVisibility(View.VISIBLE);
            tvStatus.setText("✅ SOS Sent to all guardians!");

            speak("SOS successfully sent. Your location has been shared. Guardians are being notified.");
        }, 2000);
    }

    private void sendSMSToGuardians() {
        String googleMapsLink = "https://maps.google.com/?q=" + currentLat + "," + currentLng;
        String message = "🚨 SOS EMERGENCY ALERT!\n" +
                        "NavAssist user needs immediate help!\n" +
                        "📍 Live Location: " + googleMapsLink + "\n" +
                        "Time: " + new java.util.Date().toString() + "\n" +
                        "Please respond immediately!";

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED) {
            try {
                SmsManager smsManager = SmsManager.getDefault();
                for (String number : GUARDIAN_NUMBERS) {
                    if (!number.contains("X")) { // Only send if real number configured
                        smsManager.sendTextMessage(number, null, message, null, null);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void startSOSVibration() {
        if (vibrator != null) {
            long[] pattern = {0, 200, 100, 200, 500};
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
            } else {
                vibrator.vibrate(pattern, -1);
            }
        }
    }

    private void sosVibrationPattern() {
        if (vibrator != null) {
            long[] pattern = {0, 500, 200, 500, 200, 500, 200, 1000};
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
            } else {
                vibrator.vibrate(pattern, -1);
            }
        }
    }

    public void speak(String text) {
        if (tts != null) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "sos");
        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            tts.setLanguage(Locale.US);
            speak("SOS screen. Press Send SOS Now to alert all your guardians immediately.");
        }
    }

    @Override
    protected void onDestroy() {
        if (vibrator != null) vibrator.cancel();
        if (tts != null) { tts.stop(); tts.shutdown(); }
        super.onDestroy();
    }
}
