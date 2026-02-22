package com.navassist;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.*;
import android.speech.*;
import android.speech.tts.*;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.*;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private static final int PERMISSIONS_REQUEST = 100;
    private static final int SPEECH_REQUEST = 101;

    private TextToSpeech tts;
    private String disabilityMode;
    private Vibrator vibrator;

    private TextView tvModeBadge;
    private TextView tvLocation;
    private Button btnSOS;
    private Button btnCamera;
    private Button btnVoice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        disabilityMode = getIntent().getStringExtra(SplashActivity.EXTRA_MODE);
        if (disabilityMode == null) disabilityMode = SplashActivity.MODE_BLIND;

        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        // Init TTS
        tts = new TextToSpeech(this, this);

        initViews();
        requestPermissions();
        configureForMode();
    }

    private void initViews() {
        tvModeBadge = findViewById(R.id.tv_mode_badge);
        tvLocation = findViewById(R.id.tv_location);
        btnSOS = findViewById(R.id.btn_sos);
        btnCamera = findViewById(R.id.btn_camera);
        btnVoice = findViewById(R.id.btn_voice_command);

        btnSOS.setOnClickListener(v -> openSOS());
        btnCamera.setOnClickListener(v -> openCamera());
        btnVoice.setOnClickListener(v -> startVoiceRecognition());

        findViewById(R.id.btn_navigate).setOnClickListener(v -> startNavigation());
        findViewById(R.id.btn_nearby).setOnClickListener(v -> showNearbyPlaces());
        findViewById(R.id.btn_guardian).setOnClickListener(v -> openGuardian());
    }

    private void configureForMode() {
        switch (disabilityMode) {
            case SplashActivity.MODE_BLIND:
                tvModeBadge.setText("👁 BLIND MODE");
                speak("Welcome to NavAssist. Blind mode activated. Tap anywhere to hear options. " +
                      "Double tap camera button to scan surroundings. Tap SOS for emergency.");
                // Large touch targets, high contrast already in XML
                break;

            case SplashActivity.MODE_DEAF:
                tvModeBadge.setText("👂 DEAF MODE");
                // Flash screen as alert
                flashScreen();
                // Hide TTS button, show visual cues
                btnVoice.setVisibility(View.GONE);
                break;

            case SplashActivity.MODE_MOBILITY:
                tvModeBadge.setText("♿ MOBILITY MODE");
                speak("Mobility mode. Accessible routes and elevator guidance activated.");
                break;
        }
    }

    // ============================
    // CAMERA - Real Object Detection
    // ============================
    private void openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            speak("Opening camera scanner. Point camera at objects to identify them.");
            Intent intent = new Intent(this, CameraActivity.class);
            intent.putExtra(SplashActivity.EXTRA_MODE, disabilityMode);
            startActivity(intent);
        } else {
            ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA}, PERMISSIONS_REQUEST);
        }
    }

    // ============================
    // VOICE COMMANDS
    // ============================
    private void startVoiceRecognition() {
        vibrate(100);
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say a command...");
        try {
            startActivityForResult(intent, SPEECH_REQUEST);
        } catch (Exception e) {
            speak("Voice recognition not available.");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SPEECH_REQUEST && resultCode == RESULT_OK) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                String command = results.get(0).toLowerCase();
                handleVoiceCommand(command);
            }
        }
    }

    private void handleVoiceCommand(String command) {
        if (command.contains("camera") || command.contains("scan") || command.contains("surroundings")) {
            speak("Opening camera scanner.");
            openCamera();
        } else if (command.contains("sos") || command.contains("help") || command.contains("emergency")) {
            speak("Sending SOS alert.");
            openSOS();
        } else if (command.contains("home") || command.contains("navigate")) {
            speak("Starting navigation to home.");
            startNavigation();
        } else if (command.contains("guardian") || command.contains("call")) {
            speak("Connecting to guardian.");
            openGuardian();
        } else if (command.contains("hospital") || command.contains("nearby")) {
            speak("Searching for nearby hospitals and accessible places.");
            showNearbyPlaces();
        } else if (command.contains("where am i") || command.contains("location")) {
            speak("You are in Anna Nagar, Chennai, Tamil Nadu. GPS is active.");
        } else {
            speak("Command not recognized. Try saying: camera scan, SOS help, navigate home, or call guardian.");
        }
    }

    // ============================
    // SOS
    // ============================
    private void openSOS() {
        vibrate(500);
        if (disabilityMode.equals(SplashActivity.MODE_DEAF)) {
            flashScreen();
        }
        Intent intent = new Intent(this, SOSActivity.class);
        startActivity(intent);
    }

    // ============================
    // NAVIGATION
    // ============================
    private void startNavigation() {
        speak("Navigation started. Turn right in 50 meters. Haptic feedback enabled.");
        vibrate(200);
        Toast.makeText(this, "🗺 Navigation started!", Toast.LENGTH_SHORT).show();
    }

    private void showNearbyPlaces() {
        speak("Searching for accessible places nearby.");
        Toast.makeText(this, "Finding accessible places...", Toast.LENGTH_SHORT).show();
    }

    private void openGuardian() {
        startActivity(new Intent(this, GuardianActivity.class));
    }

    // ============================
    // HELPERS
    // ============================
    public void speak(String text) {
        if (tts != null && !disabilityMode.equals(SplashActivity.MODE_DEAF)) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "nav");
        }
    }

    private void vibrate(long ms) {
        if (vibrator != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(ms);
            }
        }
    }

    private void flashScreen() {
        View rootView = getWindow().getDecorView().getRootView();
        rootView.setBackgroundColor(0xFFFFD600);
        new Handler(Looper.getMainLooper()).postDelayed(() ->
            rootView.setBackgroundColor(0xFF0A0F1E), 300);
    }

    private void requestPermissions() {
        String[] permissions = {
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.VIBRATE,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.SEND_SMS
        };
        ActivityCompat.requestPermissions(this, permissions, PERMISSIONS_REQUEST);
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            tts.setLanguage(Locale.US);
            configureForMode();
        }
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}
