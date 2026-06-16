#!/usr/bin/env bash

# Exit immediately if a command exits with a non-zero status
set -e

# Define colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}===================================================${NC}"
echo -e "${BLUE}        Android Build, Install & Run Script        ${NC}"
echo -e "${BLUE}===================================================${NC}"

# Find adb
if command -v adb &> /dev/null; then
    ADB="adb"
elif [ -f "/home/jonathan/Android/Sdk/platform-tools/adb" ]; then
    ADB="/home/jonathan/Android/Sdk/platform-tools/adb"
elif [ -n "$ANDROID_HOME" ] && [ -f "$ANDROID_HOME/platform-tools/adb" ]; then
    ADB="$ANDROID_HOME/platform-tools/adb"
else
    echo -e "${RED}Error: adb command not found. Please install Android SDK Platform-Tools or make sure it is in your PATH.${NC}"
    exit 1
fi

echo -e "${GREEN}✔ Found adb at: $ADB${NC}"

# Check for connected devices
echo -e "\n${YELLOW}Checking for connected devices...${NC}"
DEVICES=$($ADB devices | grep -v "List of devices" | grep "device" || true)

if [ -z "$DEVICES" ]; then
    echo -e "${RED}❌ Error: No connected devices or emulators found.${NC}"
    echo -e "${YELLOW}Please connect an Android device via USB with USB debugging enabled, or start an emulator.${NC}"
    exit 1
fi

echo -e "${GREEN}✔ Connected devices/emulators found:${NC}"
echo -e "$DEVICES" | sed 's/^/  - /'

# Build the project
echo -e "\n${YELLOW}Building debug APK...${NC}"
if [ -f "./gradlew" ]; then
    ./gradlew assembleDebug
else
    echo -e "${RED}❌ Error: gradlew not found in the current directory.${NC}"
    exit 1
fi

# Locate the APK
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
if [ ! -f "$APK_PATH" ]; then
    # Search fallback
    FOUND_APK=$(find app/build -name "*.apk" | head -n 1)
    if [ -n "$FOUND_APK" ]; then
        APK_PATH="$FOUND_APK"
    else
        echo -e "${RED}❌ Error: Could not locate the built APK file.${NC}"
        exit 1
    fi
fi

echo -e "${GREEN}✔ APK found: $APK_PATH${NC}"

# Install the APK
echo -e "\n${YELLOW}Installing APK...${NC}"
DEVICE_IDS=$(echo "$DEVICES" | awk '{print $1}')

for dev in $DEVICE_IDS; do
    echo -e "${BLUE}Installing on device: $dev...${NC}"
    if $ADB -s "$dev" install -r "$APK_PATH"; then
        echo -e "${GREEN}✔ Installation successful on $dev!${NC}"
        
        # Launch the app
        echo -e "${YELLOW}Starting application on $dev...${NC}"
        if $ADB -s "$dev" shell am start -n "com.example.myrecipes/com.example.myrecipes.MainActivity" &> /dev/null; then
            echo -e "${GREEN}✔ Application launched successfully on $dev!${NC}"
        else
            echo -e "${RED}⚠ Failed to auto-launch the app on $dev (you can still open it manually).${NC}"
        fi
    else
        echo -e "${RED}❌ Installation failed on $dev.${NC}"
    fi
done

echo -e "\n${GREEN}===================================================${NC}"
echo -e "${GREEN}              All tasks completed!                 ${NC}"
echo -e "${GREEN}===================================================${NC}"
