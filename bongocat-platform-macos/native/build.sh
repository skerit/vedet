#!/bin/bash
# Build script for cocoa-jni native library

set -e

# Detect architecture
ARCH=$(uname -m)
if [ "$ARCH" = "arm64" ]; then
    TARGET_ARCH="aarch64"
elif [ "$ARCH" = "x86_64" ]; then
    TARGET_ARCH="x86-64"
else
    echo "Unsupported architecture: $ARCH"
    exit 1
fi

echo "Building cocoa-jni for macOS ($ARCH)..."

# Create build directory
mkdir -p build

# Compile the library (without ARC for manual memory management)
clang -dynamiclib \
    -o build/libcocoa-jni.dylib \
    -framework Cocoa \
    -framework CoreGraphics \
    -framework Foundation \
    -fno-objc-arc \
    -fPIC \
    cocoa-jni.m

echo "Build complete: build/libcocoa-jni.dylib"

# Create resources directory
RESOURCES_DIR="../src/main/resources/native/macos-${TARGET_ARCH}"
mkdir -p "$RESOURCES_DIR"

# Copy library to resources
cp build/libcocoa-jni.dylib "$RESOURCES_DIR/"

echo "Library copied to: $RESOURCES_DIR/libcocoa-jni.dylib"
echo ""
echo "To use this library, rebuild the Java project:"
echo "  cd ../.."
echo "  ./gradlew clean build"
