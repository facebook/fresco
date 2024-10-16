#!/bin/sh
# Copyright (c) Meta Platforms, Inc. and affiliates.
#
# This source code is licensed under the MIT license found in the
# LICENSE file in the root directory of this source tree.

if [ -z "$ANDROID_HOME" ]; then
  echo "Environment variable ANDROID_HOME is not set"
  exit 1
fi

SCRIPT_DIR="$(readlink -f "$(dirname "$0")")"
cd "$SCRIPT_DIR" || exit

# Use the latest available AAPT binary
BUILD_TOOLS_DIR="$ANDROID_HOME/build-tools"
LATEST_TOOLS_DIR=$(find "$BUILD_TOOLS_DIR" -maxdepth 1 -type d -print | sort -rn --key=4.1 | head -1)

if [ ! -d "$LATEST_TOOLS_DIR" ]; then
  echo "Could not find build tools in $BUILD_TOOLS_DIR"
  exit 1
fi

# Use the latest available Android version
ANDROID_PLATFORMS_DIR="$ANDROID_HOME/platforms"
LATEST_PLATFORM_DIR=$(find "$ANDROID_PLATFORMS_DIR" -maxdepth 1 -type d -print | sort -rn --key=4.1 | head -1)

if [ ! -d "$LATEST_PLATFORM_DIR" ]; then
  echo "Could not find any platforms in $ANDROID_PLATFORMS_DIR"
  exit 1
fi

# Define a temporary directory for the APK
TMP_DIR=$(mktemp -d)
APK_OUTPUT="$TMP_DIR/app.apk"

# Build an APK with our raw resources
"$LATEST_TOOLS_DIR/aapt" package -f -m -M AndroidManifest.xml -S raw -0 "" -I "$LATEST_PLATFORM_DIR/android.jar" -F "$APK_OUTPUT" || exit 1

# Unzip all APK artifacts
cd "$TMP_DIR" || exit
unzip -q "app.apk"

# Copy all compiled resources from the base drawable folder
cd "$SCRIPT_DIR" || exit
rm -rf compiled
mkdir compiled
cp -R "$TMP_DIR/res/drawable/" ./compiled

# Remove the temporary folder
rm -rf "$TMP_DIR"
