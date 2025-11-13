#!/bin/bash
# Helper script to run BongoCat Java

# Build first
echo "Building project..."
./gradlew build -x test

if [ $? -ne 0 ]; then
    echo "Build failed!"
    exit 1
fi

# Check if user is in input group
if ! groups | grep -q input; then
    echo "WARNING: You are not in the 'input' group!"
    echo "Input monitoring will not work without proper permissions."
    echo "Run: sudo usermod -a -G input $USER"
    echo "Then log out and log back in."
    echo ""
fi

# Check for Wayland
if [ -z "$WAYLAND_DISPLAY" ]; then
    echo "WARNING: WAYLAND_DISPLAY is not set!"
    echo "This application requires a Wayland compositor."
    echo ""
fi

# Set library path to include our native library
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
export LD_LIBRARY_PATH="$SCRIPT_DIR/bongocat-platform-linux/src/main/resources/native/linux-x86-64:$LD_LIBRARY_PATH"

# Run the application
echo "Starting BongoCat..."
./gradlew run "$@"
