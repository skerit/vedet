package rocks.blackblock.bongocat.platform.macos;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rocks.blackblock.bongocat.platform.Monitor;
import rocks.blackblock.bongocat.platform.OverlaySurface;
import rocks.blackblock.bongocat.platform.PlatformException;
import rocks.blackblock.bongocat.platform.Position;
import rocks.blackblock.bongocat.platform.macos.cocoa.AppKit;
import rocks.blackblock.bongocat.platform.macos.cocoa.CocoaJNI;
import rocks.blackblock.bongocat.platform.macos.cocoa.CoreGraphics;
import rocks.blackblock.bongocat.platform.macos.cocoa.ObjC;

import java.nio.ByteBuffer;

/**
 * macOS implementation of OverlaySurface.
 * Creates a transparent, always-on-top NSWindow for overlay rendering.
 */
public class MacOSOverlaySurface implements OverlaySurface {
    private static final Logger logger = LoggerFactory.getLogger(MacOSOverlaySurface.class);

    private final MacOSMonitor monitor;
    private final Position position;
    private final int width;
    private final int height;

    private Pointer window;
    private Pointer imageView;
    private Pointer bitmap;
    private ByteBuffer pixelBuffer;
    private Layer layer = Layer.OVERLAY;
    private boolean visible = false;

    public MacOSOverlaySurface(MacOSMonitor monitor, Position position, int height)
            throws PlatformException {
        this.monitor = monitor;
        this.position = position;
        this.height = height;
        this.width = monitor.getWidth();

        logger.debug("Creating macOS overlay surface: {}x{} at {} on {}",
                width, height, position, monitor.getName());

        try {
            // All AppKit operations must run on the main thread
            // Since we're already on the main thread (Java main), we can call directly
            // NSApplication.finishLaunching was called in MacOSDisplayManager
            createWindow();
            createImageView();
            allocatePixelBuffer();
        } catch (Exception e) {
            throw new PlatformException("Failed to create overlay surface", e);
        }
    }

    private void createWindow() {
        // Calculate window position
        double windowX = monitor.getX();
        double windowY;
        if (position == Position.TOP) {
            windowY = monitor.getY();
        } else {
            windowY = monitor.getY() + monitor.getHeight() - height;
        }

        // Use native wrapper to create NSWindow (avoids JNA structure-by-value issue)
        window = CocoaJNI.INSTANCE.create_nswindow(
            windowX,
            windowY,
            width,
            height,
            AppKit.NSWindowStyleMaskBorderless,
            AppKit.NSBackingStoreBuffered,
            false
        );

        // Configure window using native wrapper (avoids JNA objc_msgSend issues)
        CocoaJNI.INSTANCE.configure_overlay_window(window, AppKit.NSFloatingWindowLevel);

        logger.debug("Created NSWindow at ({}, {}) with size {}x{}", windowX, windowY, width, height);
    }

    private void createImageView() {
        // Use native wrapper to create NSImageView (avoids JNA structure-by-value issue)
        imageView = CocoaJNI.INSTANCE.create_nsimageview(0, 0, width, height);

        // Use native wrapper to set content view (avoids JNA objc_msgSend issues)
        CocoaJNI.INSTANCE.set_content_view(window, imageView);

        logger.debug("Created NSImageView");
    }

    private void allocatePixelBuffer() {
        // Allocate pixel buffer (ARGB8888 format, 4 bytes per pixel)
        int bufferSize = width * height * 4;
        Memory memory = new Memory(bufferSize);
        pixelBuffer = memory.getByteBuffer(0, bufferSize);

        // Clear to transparent
        clearBuffer();

        logger.debug("Allocated pixel buffer: {} bytes", bufferSize);
    }

    private void clearBuffer() {
        pixelBuffer.clear();
        for (int i = 0; i < width * height * 4; i++) {
            pixelBuffer.put((byte) 0);
        }
        pixelBuffer.flip();
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public Monitor getMonitor() {
        return monitor;
    }

    @Override
    public Layer getLayer() {
        return layer;
    }

    @Override
    public void setLayer(Layer layer) throws PlatformException {
        this.layer = layer;

        // Update window level
        int windowLevel = (layer == Layer.TOP)
            ? AppKit.NSScreenSaverWindowLevel
            : AppKit.NSFloatingWindowLevel;

        ObjC.sendVoid(window, "setLevel:", (long) windowLevel);

        logger.debug("Set window layer to {}", layer);
    }

    @Override
    public ByteBuffer getPixelBuffer() {
        return pixelBuffer;
    }

    @Override
    public void commit() throws PlatformException {
        try {
            // Reset buffer position for reading
            pixelBuffer.rewind();

            // Copy pixel data out of the buffer
            byte[] pixels = new byte[width * height * 4];
            pixelBuffer.get(pixels);
            pixelBuffer.rewind();

            // Create NSBitmapImageRep from pixel buffer
            Pointer bitmapClass = ObjC.getClass("NSBitmapImageRep");
            Pointer newBitmap = ObjC.send(
                ObjC.send(bitmapClass, "alloc"),
                "initWithBitmapDataPlanes:pixelsWide:pixelsHigh:bitsPerSample:samplesPerPixel:hasAlpha:isPlanar:colorSpaceName:bitmapFormat:bytesPerRow:bitsPerPixel:",
                null,  // Let Cocoa allocate the buffer
                width,
                height,
                8,     // bits per sample
                4,     // samples per pixel (ARGB)
                true,  // has alpha
                false, // not planar
                ObjC.nsString("NSDeviceRGBColorSpace"),
                1,     // NSAlphaPremultipliedFirstBitmapFormat
                width * 4,  // bytes per row
                32     // bits per pixel
            );

            // Copy pixel data
            Pointer bitmapData = ObjC.send(newBitmap, "bitmapData");
            if (bitmapData != null) {
                bitmapData.write(0, pixels, 0, pixels.length);
            }

            // Create NSImage and set it to the image view
            Pointer image = ObjC.send(ObjC.alloc("NSImage"), "init");
            ObjC.sendVoid(image, "addRepresentation:", newBitmap);
            ObjC.sendVoid(imageView, "setImage:", image);

            // Release old bitmap if it exists
            if (bitmap != null) {
                ObjC.sendVoid(bitmap, "release");
            }
            bitmap = newBitmap;

        } catch (Exception e) {
            throw new PlatformException("Failed to commit pixel buffer", e);
        }
    }

    @Override
    public void show() {
        if (!visible) {
            CocoaJNI.INSTANCE.show_window(window);
            visible = true;
            logger.debug("Showing overlay window");
        }
    }

    @Override
    public void hide() {
        if (visible) {
            CocoaJNI.INSTANCE.hide_window(window);
            visible = false;
            logger.debug("Hiding overlay window");
        }
    }

    @Override
    public boolean isVisible() {
        return visible;
    }

    @Override
    public boolean isFullscreenWindowPresent() {
        // TODO: Implement fullscreen detection using CGWindowListCopyWindowInfo
        // For now, always return false
        return false;
    }

    @Override
    public void close() {
        logger.debug("Closing overlay surface");

        if (window != null) {
            hide();
            CocoaJNI.INSTANCE.close_window(window);

            if (bitmap != null) {
                CocoaJNI.INSTANCE.release_object(bitmap);
            }

            window = null;
            bitmap = null;
        }

        pixelBuffer = null;
    }
}
