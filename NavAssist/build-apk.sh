#!/bin/bash
# =========================================================
#  NavAssist APK Builder
#  Run this script on your PC to build the APK in one step
# =========================================================

set -e
echo ""
echo "🧭 NavAssist APK Builder"
echo "========================"
echo ""

# Check Java
if ! command -v java &> /dev/null; then
    echo "❌ Java not found. Installing..."
    if [[ "$OSTYPE" == "darwin"* ]]; then
        brew install --cask temurin
    else
        sudo apt-get install -y openjdk-17-jdk
    fi
fi

JAVA_VERSION=$(java -version 2>&1 | head -1)
echo "✅ Java: $JAVA_VERSION"

# Install Android command-line tools if not present
if [ -z "$ANDROID_HOME" ]; then
    export ANDROID_HOME="$HOME/android-sdk"
    echo "📦 Setting ANDROID_HOME to $ANDROID_HOME"
fi

if [ ! -d "$ANDROID_HOME/cmdline-tools" ]; then
    echo "📥 Downloading Android SDK Command Line Tools..."
    mkdir -p "$ANDROID_HOME"
    
    if [[ "$OSTYPE" == "darwin"* ]]; then
        SDK_URL="https://dl.google.com/android/repository/commandlinetools-mac-11076708_latest.zip"
    else
        SDK_URL="https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
    fi
    
    curl -o /tmp/cmdline-tools.zip "$SDK_URL"
    mkdir -p "$ANDROID_HOME/cmdline-tools"
    unzip -q /tmp/cmdline-tools.zip -d "$ANDROID_HOME/cmdline-tools"
    mv "$ANDROID_HOME/cmdline-tools/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest" 2>/dev/null || true
    rm /tmp/cmdline-tools.zip
    echo "✅ SDK tools downloaded"
fi

export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"

# Accept licenses and install build tools
if [ ! -d "$ANDROID_HOME/build-tools/34.0.0" ]; then
    echo "📦 Installing Android build tools (one-time, ~500MB)..."
    yes | sdkmanager --licenses > /dev/null 2>&1 || true
    sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"
    echo "✅ Build tools installed"
fi

# Make gradlew executable
chmod +x gradlew

# Build Debug APK (installable without signing)
echo ""
echo "🔨 Building NavAssist APK..."
./gradlew assembleDebug --no-daemon

APK_PATH="app/build/outputs/apk/debug/app-debug.apk"

if [ -f "$APK_PATH" ]; then
    echo ""
    echo "✅ ============================================="
    echo "   APK BUILT SUCCESSFULLY!"
    echo "   📱 File: $APK_PATH"
    APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
    echo "   📦 Size: $APK_SIZE"
    echo "=============================================="
    echo ""
    echo "📲 HOW TO INSTALL:"
    echo "   1. Copy app-debug.apk to your Android phone"
    echo "   2. On phone: Settings → Security → Unknown Sources → ON"
    echo "   3. Tap the APK file to install"
    echo ""
    echo "📲 OR install directly via USB:"
    echo "   adb install $APK_PATH"
else
    echo "❌ Build failed. Check errors above."
    exit 1
fi
