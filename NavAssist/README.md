# 🧭 NavAssist — Smart Navigation App for Disabled Persons

A complete Android app with:
- 📸 **Real-time AI camera** object detection (ML Kit — works offline!)
- 🔊 **Text-to-Speech** voice guidance for blind users
- 🎤 **Voice commands** recognition
- 📳 **Haptic vibration** alerts for deaf users
- ⚡ **Flash screen alerts** for deaf users
- 🆘 **SOS Emergency** — sends SMS + live location to guardians
- 🛡️ **Guardian App** — real-time monitoring dashboard
- 🔥 **Firebase** push notifications for SOS alerts

---

## 📱 BUILD THE APK (3 Steps)

### Option A — Easiest (Android Studio)
1. Download & install **Android Studio**: https://developer.android.com/studio
2. Open Android Studio → `Open Project` → select this `NavAssist` folder
3. Wait for Gradle sync (~2 minutes first time)
4. Click **▶ Run** button or go to `Build → Build APK(s)`
5. APK appears at `app/build/outputs/apk/debug/app-debug.apk`

### Option B — Command Line (Linux/Mac)
```bash
chmod +x build-apk.sh
./build-apk.sh
```

### Option C — Command Line (Windows)
```cmd
gradlew.bat assembleDebug
```
APK: `app\build\outputs\apk\debug\app-debug.apk`

---

## ⚙️ BEFORE BUILDING — Configuration

### 1. Add Firebase (for SOS push notifications)
1. Go to https://console.firebase.google.com
2. Create project `NavAssist`
3. Add Android app with package `com.navassist`
4. Download `google-services.json`
5. Place it in `app/google-services.json`

### 2. Add Guardian Phone Numbers (for SMS SOS)
Open `app/src/main/java/com/navassist/SOSActivity.java`:
```java
private static final String[] GUARDIAN_NUMBERS = {
    "+91XXXXXXXXXX",  // ← Replace with real numbers
    "+91XXXXXXXXXX"
};
```

### 3. Add Google Maps API Key (for map in Guardian app)
In `app/src/main/AndroidManifest.xml`, add inside `<application>`:
```xml
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="YOUR_GOOGLE_MAPS_API_KEY"/>
```
Get key: https://console.cloud.google.com → Maps SDK for Android

---

## 📲 INSTALL ON PHONE

### Via USB (Developer mode):
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Manually:
1. Copy `app-debug.apk` to phone
2. Settings → Security → **Install Unknown Apps** → ON
3. Open file manager → tap APK → Install

---

## 🏗️ PROJECT STRUCTURE

```
NavAssist/
├── app/src/main/
│   ├── java/com/navassist/
│   │   ├── SplashActivity.java       ← Mode selection screen
│   │   ├── MainActivity.java         ← Main hub (voice, navigate)
│   │   ├── CameraActivity.java       ← 🔑 Real-time AI camera
│   │   ├── DetectionOverlayView.java ← Bounding box drawing
│   │   ├── SOSActivity.java          ← Emergency SOS + SMS
│   │   ├── GuardianActivity.java     ← Guardian monitoring
│   │   └── services/
│   │       ├── NavAssistFirebaseService.java  ← Push notifications
│   │       └── LocationTrackingService.java   ← Background GPS
│   ├── res/layout/
│   │   ├── activity_splash.xml       ← Mode selection UI
│   │   ├── activity_main.xml         ← Main screen UI
│   │   ├── activity_camera.xml       ← Camera + detection UI
│   │   ├── activity_sos.xml          ← SOS screen UI
│   │   ├── activity_guardian.xml     ← Guardian dashboard UI
│   │   └── item_activity.xml         ← Activity log item
│   └── AndroidManifest.xml
├── build.gradle
├── build-apk.sh                      ← One-command builder
└── README.md
```

---

## 🔬 HOW AI CAMERA WORKS

The camera uses **Google ML Kit** (100% offline, on-device):

| Feature | ML Kit API | What it does |
|---------|-----------|--------------|
| Object Detection | `ObjectDetection` | Detects chairs, doors, people, products with bounding boxes |
| Image Labeling | `ImageLabeling` | Identifies scenes: store, hospital, outdoor, food |
| Text Recognition | `TextRecognition` | Reads signs, menus, product labels, notices |

All 3 run on-device — **no internet needed** for camera AI.

---

## 🆘 SOS FLOW

```
User presses SOS
       ↓
Location captured (GPS)
       ↓
SMS sent to guardian numbers (with Google Maps link)
       ↓
Firebase push notification → Guardian's phone
       ↓
Guardian app shows alert banner + vibrates + sound
```

---

## 📋 PERMISSIONS REQUIRED

| Permission | Why |
|-----------|-----|
| CAMERA | Real-time object detection |
| ACCESS_FINE_LOCATION | Live GPS for SOS + navigation |
| RECORD_AUDIO | Voice commands |
| SEND_SMS | SOS alerts to guardians |
| VIBRATE | Haptic feedback for deaf users |
| CALL_PHONE | Emergency call from guardian |
| POST_NOTIFICATIONS | SOS push alerts |

---

## 🛠️ TECH STACK

- **Language**: Java (Android)
- **Min SDK**: Android 7.0 (API 24)
- **Camera**: CameraX 1.3.1
- **AI**: Google ML Kit (Object Detection + Image Labeling + OCR)
- **Location**: Google Play Services Location
- **Notifications**: Firebase Cloud Messaging (FCM)
- **Database**: Firebase Realtime Database
- **TTS**: Android Built-in TextToSpeech
- **Voice**: Android SpeechRecognizer
