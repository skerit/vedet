# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with the Java implementation of Bongo Cat.

## Overview

This is a Java port of bongocat, a Wayland overlay application that displays an animated bongo cat reacting to keyboard input.
The implementation uses JNA (Java Native Access) to interface with native Wayland libraries.
It is meant to also work on MacOS and Windows in the future.

## Build Commands

```bash
# Build the project
./gradlew build

# Build without running tests
./gradlew build -x test

# Clean build artifacts
./gradlew clean

# Run the application
./gradlew run

# Or use the provided script
./run.sh
```

## Running the Application

```bash
# Basic usage
./run.sh

# With custom config (when implemented)
./run.sh --config path/to/bongocat.json

# Toggle mode (start if not running, stop if running)
./run.sh --toggle
```

## Project Structure

```
bongocat-java/
├── bongocat-app/           # Main application entry point
│   ├── assets/             # Bongo cat PNG animation frames
│   └── src/main/java/
│       └── rocks/blackblock/bongocat/
│           ├── Application.java      # Main class, initialization
│           └── cli/                  # Command-line argument parsing
│
├── bongocat-core/          # Core logic (platform-independent)
│   └── src/main/java/
│       └── rocks/blackblock/bongocat/
│           ├── config/               # Configuration system
│           ├── core/                 # Process management, PID files
│           └── graphics/             # Animation engine, rendering, scaling
│
├── bongocat-platform-api/  # Platform abstraction interfaces
│   └── src/main/java/
│       └── rocks/blackblock/bongocat/platform/
│           ├── DisplayManager.java   # Monitor and overlay management
│           ├── InputMonitor.java     # Keyboard input monitoring
│           ├── OverlaySurface.java   # Transparent overlay surface
│           └── PlatformService.java  # Platform detection
│
└── bongocat-platform-linux/ # Linux/Wayland implementation
    ├── native/                       # C helper libraries
    │   ├── wayland-protocols-jni.c   # Wayland protocol bindings
    │   └── build.sh                  # Native library build script
    └── src/main/java/
        └── rocks/blackblock/bongocat/platform/linux/
            ├── wayland/              # Wayland JNA bindings
            ├── evdev/                # Linux input device bindings
            ├── LinuxDisplayManager.java
            ├── LinuxInputMonitor.java
            ├── LinuxOverlaySurface.java
            └── NativeMemory.java     # Shared memory management
```

## Architecture Overview

### Platform Abstraction

The codebase uses a clean platform abstraction pattern:
- `PlatformService` interface defines platform detection and factory methods
- `DisplayManager`, `InputMonitor`, `OverlaySurface` are platform-agnostic interfaces
- Linux/Wayland implementation in `bongocat-platform-linux` module

This design allows for future macOS or Windows implementations without changing core logic.

### Wayland Integration

**Native Library (wayland-protocols-jni.so)**:
- Provides access to Wayland protocol extension interface structures
- Wrapper functions to avoid JNA varargs marshalling issues
- Critical functions:
  - `get_zwlr_layer_shell_v1_interface()` - Layer shell protocol
  - `create_layer_surface()` - Create overlay surface
  - `create_shm_pool()` - Shared memory for pixel buffers
  - `create_buffer_from_pool()` - Create Wayland buffer (fixes parameter order)

**JNA Bindings**:
- `WaylandLibrary.java` - Core libwayland-client functions
- `WaylandProtocolsLibrary.java` - Custom protocol extensions
- `LayerShellProtocol.java` - wlr-layer-shell-unstable-v1 constants

**Key Technical Details**:
- Uses `wlr-layer-shell-unstable-v1` for overlay rendering
- Implements `xdg-output-unstable-v1` for multi-monitor support
- ARGB8888 pixel format with proper alpha channel handling
- Shared memory via memfd_create or tmpfs

### Input Monitoring

**Linux evdev**:
- Direct monitoring of `/dev/input/eventX` devices
- Requires user to be in `input` group: `sudo usermod -a -G input $USER`
- Auto-detection of keyboard devices
- Separate thread for non-blocking input monitoring
- Uses `poll()` for efficient multi-device monitoring

**Key mapping**:
- Left-hand keys (Q, W, E, A, S, D, etc.) -> left paw down
- Right-hand keys (U, I, O, P, J, K, L, etc.) -> right paw down
- Both hands -> both paws down

### Animation System

**Frame Management**:
- 4 animation frames loaded from `assets/` directory:
  - `bongo-cat-both-up.png` - Idle state (both paws up)
  - `bongo-cat-left-down.png` - Left paw down
  - `bongo-cat-right-down.png` - Right paw down
  - `bongo-cat-both-down.png` - Both paws down

**Image Processing**:
- PNG loading with automatic conversion to TYPE_INT_ARGB
- Bilinear interpolation for smooth scaling (anti-aliasing)
- Proper alpha channel preservation for transparency
- Frames scaled to fit overlay height while maintaining aspect ratio

**Rendering**:
- Direct pixel buffer manipulation via ByteBuffer
- ARGB8888 format (little-endian: B, G, R, A)
- 60 FPS target frame rate
- Clear to transparent, render frame, commit to Wayland

## Critical JNA Limitations and Workarounds

### Varargs Marshalling Issue

JNA's `wl_proxy_marshal_constructor` doesn't correctly marshal parameters for Wayland protocol functions. This was discovered via `WAYLAND_DEBUG=1`:

```
❌ Wrong: wl_shm_pool#10.create_buffer(new id wl_buffer#11, 1707, 100, 6828, 0, 0)
✅ Correct: wl_shm_pool#10.create_buffer(new id wl_buffer#11, 0, 1707, 100, 6828, 0)
```

**Solution**: Create C wrapper functions in `wayland-protocols-jni.c`:
```c
struct wl_buffer *create_buffer_from_pool(
    struct wl_shm_pool *pool,
    int32_t offset,
    int32_t width,
    int32_t height,
    int32_t stride,
    uint32_t format
) {
    return wl_shm_pool_create_buffer(pool, offset, width, height, stride, format);
}
```

### Structure Arrays

JNA Structure arrays must use contiguous memory:
```java
// ❌ Wrong - creates non-contiguous memory
for (int i = 0; i < size; i++) {
    pollFds[i] = new PollFd();
}

// ✅ Correct - uses JNA's toArray()
PollFd pollFd = new PollFd();
PollFd[] pollFds = (PollFd[]) pollFd.toArray(size);
```

## Dependencies

**Core Dependencies**:
- JNA (Java Native Access) 5.14.0 - Native library bindings
- SLF4J + Logback - Logging
- Jackson - JSON/YAML configuration parsing
- Picocli - Command-line argument parsing

**Native Libraries Required**:
- `libwayland-client.so` - Wayland client library
- `libwayland-protocols-jni.so` - Custom protocol bindings (built from `native/`)

**Build Dependencies**:
- Java 17+
- Gradle 8.14
- GCC (for native library compilation)
- wayland-protocols development files

## Platform Support

### Currently Supported
- ✅ **Linux/Wayland** - Tested on KDE Plasma, should work on Sway, Hyprland, Wayfire

### Not Supported
- ❌ **Linux/X11** - Would require different overlay mechanism (XShape extension)
- ❌ **macOS** - Would need Objective-C bridges and native window APIs
- ❌ **Windows** - Would need Win32 API for overlay windows
- ❌ **GNOME Wayland** - Lacks wlr-layer-shell protocol support

### Adding Platform Support

To add a new platform:
1. Create module `bongocat-platform-<platform>`
2. Implement `PlatformService` interface
3. Implement `DisplayManager`, `InputMonitor`, `OverlaySurface`
4. Update platform detection in `Application.java`

## Development Guidelines

### Code Standards

- **Java 17 features**: Use modern Java syntax (records, pattern matching, etc.)
- **No fully qualified class names**: Always use imports
- **Prefer editing over creating**: Modify existing files instead of creating new ones
- **Single responsibility**: Each class should have one clear purpose
- **Platform abstraction**: Keep platform-specific code in platform modules

### Native Library Development

When modifying `wayland-protocols-jni.c`:
1. Edit the C source file
2. Rebuild: `cd bongocat-platform-linux/native && ./build.sh`
3. Copy library: `cp build/libwayland-protocols-jni.so ../src/main/resources/native/linux-x86-64/`
4. Rebuild Java: `./gradlew build`

### Testing with WAYLAND_DEBUG

Debug Wayland protocol issues:
```bash
WAYLAND_DEBUG=1 ./run.sh 2>&1 | grep -E "error|wl_shm"
```

### Common Issues

**Protocol errors (EPROTO)**:
- Check parameter order in `wl_proxy_marshal_*` calls
- Verify interface pointers are correct
- Use C wrappers for complex marshalling

**Transparency not working**:
- Ensure PNG is converted to TYPE_INT_ARGB
- Check alpha channel is preserved during scaling
- Verify buffer format is ARGB8888

**Input monitoring fails**:
- User must be in `input` group
- Check `/dev/input/eventX` permissions
- Use `./bongocat-c/scripts/find_input_devices.sh` to identify keyboards

## Configuration

Configuration system is implemented but currently uses hardcoded defaults. The infrastructure supports:
- JSON/YAML config files
- Monitor selection
- Overlay position (TOP/BOTTOM) and height
- Animation settings (FPS, keypress duration)
- Input device selection

## Debugging Tips

**Enable debug logging**:
Edit `bongocat-app/src/main/resources/logback.xml`:
```xml
<logger name="rocks.blackblock.bongocat" level="DEBUG"/>
```

**Check Wayland compositor logs**:
```bash
journalctl --user -u plasma-kwin_wayland.service -f
```

**Verify native library loading**:
```bash
LD_DEBUG=libs ./run.sh 2>&1 | grep wayland
```

## Known Limitations

- Single monitor support only (primary monitor used)
- No fullscreen detection (always displays overlay)
- No configuration hot-reload
- No asset customization UI
- Requires compositor with wlr-layer-shell support

## Future Improvements

- [ ] Multi-monitor support with monitor selection
- [ ] Configuration file support with hot-reload
- [ ] Custom asset loading
- [ ] macOS platform implementation
- [ ] Fullscreen detection using foreign-toplevel protocol
- [ ] Packaging as native executable with GraalVM

## Warning

- Never use fully qualified classnames, always do proper imports
- After implementing any feature, ask a new subagent to perform a review
- Make sure to keep all the platform specific code in their respective sourcesets
