package com.navassist;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.os.*;
import android.speech.tts.*;
import android.util.Size;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.*;
import androidx.camera.lifecycle.*;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.*;
import com.google.mlkit.vision.objects.*;
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;
import com.google.mlkit.vision.text.*;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private PreviewView previewView;
    private TextView tvDetectedItems;
    private TextView tvScanStatus;
    private Button btnReadAloud;
    private Button btnBack;
    private Button btnSwitchMode;

    private TextToSpeech tts;
    private ExecutorService cameraExecutor;
    private ProcessCameraProvider cameraProvider;

    // ML Kit detectors
    private ObjectDetector objectDetector;
    private ImageLabeler imageLabeler;
    private TextRecognizer textRecognizer;

    private String disabilityMode;
    private List<String> detectedList = new ArrayList<>();
    private boolean isReadingText = false;  // toggle: object detect vs text read
    private Vibrator vibrator;
    private long lastSpeakTime = 0;

    // Overlay for drawing bounding boxes
    private DetectionOverlayView overlayView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        disabilityMode = getIntent().getStringExtra(SplashActivity.EXTRA_MODE);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        initViews();
        initMLKit();
        initTTS();

        cameraExecutor = Executors.newSingleThreadExecutor();
        startCamera();
    }

    private void initViews() {
        previewView = findViewById(R.id.camera_preview);
        overlayView = findViewById(R.id.overlay_view);
        tvDetectedItems = findViewById(R.id.tv_detected_items);
        tvScanStatus = findViewById(R.id.tv_scan_status);
        btnReadAloud = findViewById(R.id.btn_read_aloud);
        btnBack = findViewById(R.id.btn_back);
        btnSwitchMode = findViewById(R.id.btn_switch_mode);

        btnBack.setOnClickListener(v -> {
            speak("Going back.");
            finish();
        });

        btnReadAloud.setOnClickListener(v -> speakAllDetected());

        btnSwitchMode.setOnClickListener(v -> {
            isReadingText = !isReadingText;
            if (isReadingText) {
                btnSwitchMode.setText("🔍 Switch to Objects");
                tvScanStatus.setText("📝 TEXT READER MODE");
                speak("Text reader mode. Point camera at signs, labels or menus.");
            } else {
                btnSwitchMode.setText("📝 Switch to Text");
                tvScanStatus.setText("🔍 OBJECT DETECTION MODE");
                speak("Object detection mode. I will identify objects around you.");
            }
        });
    }

    // ============================
    // ML KIT INITIALIZATION
    // ============================
    private void initMLKit() {
        // Object Detector - detects and tracks objects in real time
        ObjectDetectorOptions objectOptions = new ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)  // real-time stream
            .enableMultipleObjects()
            .enableClassification()
            .build();
        objectDetector = ObjectDetection.getClient(objectOptions);

        // Image Labeler - labels scenes (food, outdoors, store, etc.)
        ImageLabelerOptions labelOptions = new ImageLabelerOptions.Builder()
            .setConfidenceThreshold(0.65f)
            .build();
        imageLabeler = ImageLabeling.getClient(labelOptions);

        // Text Recognizer - reads signs, labels, menus
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
    }

    private void initTTS() {
        tts = new TextToSpeech(this, this);
    }

    // ============================
    // CAMERA SETUP (CameraX)
    // ============================
    private void startCamera() {
        ProcessCameraProvider.getInstance(this).addListener(() -> {
            try {
                cameraProvider = ProcessCameraProvider.getInstance(this).get();

                // Preview
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // Image Analysis for ML Kit
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                    .setTargetResolution(new Size(1280, 720))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build();

                imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> {
                    analyzeFrame(imageProxy);
                });

                // Select back camera
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    // ============================
    // FRAME ANALYSIS - Core AI
    // ============================
    @androidx.camera.core.ExperimentalGetImage
    private void analyzeFrame(ImageProxy imageProxy) {
        if (imageProxy.getImage() == null) {
            imageProxy.close();
            return;
        }

        InputImage image = InputImage.fromMediaImage(
            imageProxy.getImage(),
            imageProxy.getImageInfo().getRotationDegrees()
        );

        if (isReadingText) {
            // TEXT RECOGNITION MODE
            textRecognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    String text = visionText.getText().trim();
                    if (!text.isEmpty() && !text.equals(lastRecognizedText)) {
                        lastRecognizedText = text;
                        runOnUiThread(() -> {
                            String display = "📝 TEXT FOUND:\n" + text;
                            tvDetectedItems.setText(display);
                        });
                        speakText("I can read: " + text, 4000);
                    }
                })
                .addOnCompleteListener(t -> imageProxy.close());

        } else {
            // OBJECT DETECTION MODE
            // Run both object detector and image labeler together
            objectDetector.process(image)
                .addOnSuccessListener(detectedObjects -> {
                    detectedList.clear();
                    List<RectF> boxes = new ArrayList<>();

                    for (DetectedObject obj : detectedObjects) {
                        String label = "Unknown object";
                        if (!obj.getLabels().isEmpty()) {
                            DetectedObject.Label topLabel = obj.getLabels().get(0);
                            label = topLabel.getText();
                            int confidence = (int)(topLabel.getConfidence() * 100);
                            label = label + " (" + confidence + "%)";
                        }

                        // Get position
                        Rect boundingBox = obj.getBoundingBox();
                        String position = getPosition(boundingBox, imageProxy.getWidth(), imageProxy.getHeight());
                        String fullLabel = "• " + label + " — " + position;
                        detectedList.add(fullLabel);

                        boxes.add(new RectF(boundingBox));
                    }

                    // Also run image labeler for scene context
                    imageLabeler.process(image)
                        .addOnSuccessListener(labels -> {
                            for (ImageLabel label : labels) {
                                if (label.getConfidence() > 0.75f) {
                                    detectedList.add("📍 Scene: " + label.getText());
                                    break; // only add top scene label
                                }
                            }
                            runOnUiThread(() -> updateUI(detectedList, boxes));
                            // Auto-speak new detections
                            speakTopDetection();
                        })
                        .addOnCompleteListener(t -> imageProxy.close());
                })
                .addOnFailureListener(e -> imageProxy.close());
        }
    }

    private String lastRecognizedText = "";
    private String lastSpokenDetection = "";

    private String getPosition(Rect box, int imageWidth, int imageHeight) {
        float centerX = box.centerX() / (float) imageWidth;
        float centerY = box.centerY() / (float) imageHeight;

        String horizontal = centerX < 0.33f ? "on your left" :
                           centerX > 0.66f ? "on your right" : "straight ahead";
        String vertical = centerY < 0.4f ? "above" :
                         centerY > 0.7f ? "below" : "at eye level";

        // Estimate distance by box size
        float boxArea = (float)(box.width() * box.height()) / (imageWidth * imageHeight);
        String distance = boxArea > 0.3f ? "very close" :
                         boxArea > 0.1f ? "nearby" : "in the distance";

        return horizontal + ", " + distance;
    }

    private void updateUI(List<String> items, List<RectF> boxes) {
        if (items.isEmpty()) {
            tvDetectedItems.setText("Scanning environment...");
        } else {
            StringBuilder sb = new StringBuilder();
            for (String item : items) {
                sb.append(item).append("\n");
            }
            tvDetectedItems.setText(sb.toString().trim());
        }

        if (overlayView != null && boxes != null) {
            overlayView.setBoxes(boxes);
        }
    }

    private void speakTopDetection() {
        long now = System.currentTimeMillis();
        if (now - lastSpeakTime < 3000) return; // throttle to every 3 seconds

        if (!detectedList.isEmpty()) {
            String top = detectedList.get(0)
                .replace("•", "").replace("(", "").replace(")", "").replace("%", " percent");
            if (!top.equals(lastSpokenDetection)) {
                lastSpokenDetection = top;
                lastSpeakTime = now;
                speakText(top, 0);
                vibrateShort();
            }
        }
    }

    private void speakAllDetected() {
        if (detectedList.isEmpty()) {
            speak("No objects detected yet. Point camera at something.");
            return;
        }
        StringBuilder sb = new StringBuilder("I can see: ");
        for (String item : detectedList) {
            sb.append(item.replace("•", "")).append(". ");
        }
        speak(sb.toString());
    }

    public void speak(String text) {
        if (tts != null && !disabilityMode.equals(SplashActivity.MODE_DEAF)) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "cam");
        }
    }

    private void speakText(String text, long delay) {
        if (delay > 0) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> speak(text), delay);
        } else {
            speak(text);
        }
    }

    private void vibrateShort() {
        if (vibrator != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(80);
            }
        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            tts.setLanguage(Locale.US);
            speak("Camera scanner ready. I will identify objects around you automatically.");
        }
    }

    @Override
    protected void onDestroy() {
        cameraExecutor.shutdown();
        if (tts != null) { tts.stop(); tts.shutdown(); }
        super.onDestroy();
    }
}
