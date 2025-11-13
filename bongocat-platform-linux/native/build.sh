#!/bin/bash
set -e

# Build script for wayland-protocols-jni library

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BUILD_DIR="$SCRIPT_DIR/build"
PROTOCOLS_DIR="$SCRIPT_DIR/protocols"

# Clean and create directories
rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR"
mkdir -p "$PROTOCOLS_DIR"

# Copy protocol XML files from bongocat-c
if ls "../../../bongocat-c/protocols/"*.xml 1> /dev/null 2>&1; then
    cp "../../../bongocat-c/protocols/"*.xml "$PROTOCOLS_DIR/"
else
    echo "Warning: bongocat-c protocols not found, assuming they're already in $PROTOCOLS_DIR"
fi

echo "Generating protocol code..."

# First, generate xdg-shell from system protocols (required dependency)
XDG_SHELL_XML=$(pkg-config --variable=pkgdatadir wayland-protocols)/stable/xdg-shell/xdg-shell.xml
if [ -f "$XDG_SHELL_XML" ]; then
    echo "  Processing xdg-shell (system protocol)..."
    wayland-scanner client-header "$XDG_SHELL_XML" "$BUILD_DIR/xdg-shell-client-protocol.h"
    wayland-scanner private-code "$XDG_SHELL_XML" "$BUILD_DIR/xdg-shell-protocol.c"
fi

# Generate C code from protocol XMLs using wayland-scanner
if ls "$PROTOCOLS_DIR"/*.xml 1> /dev/null 2>&1; then
    for xml in "$PROTOCOLS_DIR"/*.xml; do
        basename=$(basename "$xml" .xml)
        echo "  Processing $basename..."

        # Generate client header
        wayland-scanner client-header "$xml" "$BUILD_DIR/${basename}-client-protocol.h"

        # Generate code
        wayland-scanner private-code "$xml" "$BUILD_DIR/${basename}-protocol.c"
    done
else
    echo "No protocol XMLs found in $PROTOCOLS_DIR"
    echo "Please copy protocol XML files to $PROTOCOLS_DIR first"
    exit 1
fi

echo "Compiling library..."

# Compile all generated protocol C files plus our JNI helper
gcc -shared -fPIC \
    -o "$BUILD_DIR/libwayland-protocols-jni.so" \
    "$BUILD_DIR"/*-protocol.c \
    "$SCRIPT_DIR/wayland-protocols-jni.c" \
    $(pkg-config --cflags --libs wayland-client) \
    -I"$BUILD_DIR"

echo "Build complete: $BUILD_DIR/libwayland-protocols-jni.so"

# Copy to resources so Gradle can package it
RESOURCES_DIR="$SCRIPT_DIR/../src/main/resources/native/linux-x86-64"
mkdir -p "$RESOURCES_DIR"
cp "$BUILD_DIR/libwayland-protocols-jni.so" "$RESOURCES_DIR/"

echo "Copied to resources: $RESOURCES_DIR/libwayland-protocols-jni.so"
