# Bongo Cat - macOS Platform Implementation

This module provides macOS support for Bongo Cat using Cocoa/AppKit and Core Graphics frameworks via JNA (Java Native Access).

## Features

- ✅ **Transparent Overlay Window**: Creates a borderless, transparent NSWindow that displays over other applications
- ✅ **Keyboard Input Monitoring**: Uses CGEventTap to monitor keyboard events globally
- ✅ **Display Management**: Enumerates displays using Core Graphics API
- ✅ **Always-on-top Support**: Window can be configured to stay above fullscreen applications

## Architecture

### Platform Components

1. **MacOSPlatformService** - Main entry point for platform detection and factory methods
2. **MacOSDisplayManager** - Manages NSApplication and display enumeration
3. **MacOSOverlaySurface** - Creates and manages transparent NSWindow for rendering
4. **MacOSInputMonitor** - Monitors keyboard input using CGEventTap
5. **MacOSMonitor** - Represents display information
6. **MacOSInputEvent** - Platform-agnostic keyboard event wrapper

### JNA Bindings

Located in `rocks.blackblock.bongocat.platform.macos.cocoa`:

- **ObjCRuntime** - Low-level Objective-C runtime bindings
- **ObjC** - High-level helper class for Objective-C messaging
- **Foundation** - Core Foundation framework bindings
- **AppKit** - AppKit framework constants
- **CoreGraphics** - Core Graphics API for display and event management

## Requirements

### System Requirements

- macOS 10.14 (Mojave) or later
- Java 17 or later

### Permissions

**Accessibility Access Required**: The application needs accessibility permissions to monitor keyboard input via CGEventTap.

To grant permissions:
1. Open System Settings (or System Preferences)
2. Go to Privacy & Security → Accessibility
3. Add and enable your terminal application or the Bongo Cat application

Without this permission, keyboard monitoring will fail with an error message.

## Technical Details

### Transparent Overlay Window

The overlay is created using NSWindow with:
- `NSWindowStyleMaskBorderless` - No title bar or borders
- Transparent background using `[NSColor clearColor]`
- `setIgnoresMouseEvents:` - Allows clicks to pass through
- `setLevel:` - Controls window layering (NSFloatingWindowLevel or NSScreenSaverWindowLevel)
- Collection behaviors to appear on all spaces and ignore window cycling

### Keyboard Input Monitoring

Uses CGEventTap to create a passive event tap that monitors:
- `kCGEventKeyDown` - Key press events
- `kCGEventKeyUp` - Key release events

The event tap runs in a separate thread with a Core Foundation run loop.

**Key Mapping**:
- Left-hand keys: Q, W, E, R, T, A, S, D, F, G, Z, X, C, V, and left modifiers
- Right-hand keys: I, O, P, J, K, L, N, M, arrow keys, and right modifiers

Keycodes are based on the US keyboard layout.

### Display Management

Uses Core Graphics to:
- Enumerate active displays with `CGGetActiveDisplayList`
- Get display bounds with `CGDisplayBounds`
- Identify primary display with `CGMainDisplayID`

Currently implements single-display support using the primary monitor.

### Pixel Buffer Format

The overlay uses **ARGB8888** pixel format:
- 4 bytes per pixel: Alpha, Red, Green, Blue
- Direct ByteBuffer for efficient rendering
- Converted to NSBitmapImageRep for display

## Building

The module is automatically included when building the main project:

```bash
./gradlew build
```

## Running

On macOS, the application will automatically detect and use the macOS platform:

```bash
./gradlew run
```

Or use the run script:

```bash
./run.sh
```

## Limitations

- **Single monitor support**: Currently only uses the primary display
- **No fullscreen detection**: `isFullscreenWindowPresent()` always returns false (TODO: implement using CGWindowListCopyWindowInfo)
- **No HiDPI scaling**: Display scale factor is hardcoded to 1.0 (could query NSScreen.backingScaleFactor)
- **US keyboard layout**: Key mapping assumes US layout

## Future Improvements

- [ ] Multi-monitor support with per-monitor overlays
- [ ] Fullscreen window detection using CGWindowListCopyWindowInfo
- [ ] HiDPI/Retina display support with proper scaling
- [ ] International keyboard layout support
- [ ] Configurable window level and positioning
- [ ] Memory management improvements (proper CFRelease calls)

## Troubleshooting

### "Failed to create event tap" Error

**Problem**: Application cannot monitor keyboard input

**Solution**: Grant Accessibility permissions:
1. Open System Settings → Privacy & Security → Accessibility
2. Add your terminal or the application
3. Restart the application

### Overlay Window Not Visible

**Problem**: Window doesn't appear or appears in wrong location

**Debugging steps**:
1. Check logs for window creation messages
2. Verify display detection: should show "Found primary display"
3. Try changing window level in MacOSOverlaySurface
4. Check that `show()` is called after creating the surface

### Keyboard Input Not Detected

**Problem**: Bongo cat doesn't respond to keypresses

**Solutions**:
1. Verify accessibility permissions are granted
2. Check logs for "macOS input monitor started successfully"
3. Ensure run loop thread is running
4. Test with different keys (try both left and right hand keys)

## macOS API References

- [NSWindow Documentation](https://developer.apple.com/documentation/appkit/nswindow)
- [CGEventTap Documentation](https://developer.apple.com/documentation/coregraphics/1454426-cgeventtapcreate)
- [Core Graphics Display API](https://developer.apple.com/documentation/coregraphics/core_graphics_display_services)
- [Objective-C Runtime](https://developer.apple.com/documentation/objectivec/objective-c_runtime)

## License

Same license as the main Bongo Cat project.
