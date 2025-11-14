package rocks.blackblock.bongocat.platform.macos.cocoa;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.DoubleByReference;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * JNA bindings for the libcocoa-jni native library.
 *
 * This library provides wrapper functions for Cocoa API calls that involve
 * structure-by-value passing, which JNA cannot properly marshal on arm64 macOS.
 */
public interface CocoaJNI extends Library {

    /**
     * Load the native library from resources
     */
    CocoaJNI INSTANCE = loadLibrary();

    /**
     * Create an NSWindow with the given frame rect.
     *
     * @param x X coordinate of the window
     * @param y Y coordinate of the window
     * @param width Width of the window
     * @param height Height of the window
     * @param styleMask Window style mask (NSWindowStyleMask)
     * @param backing Backing store type (NSBackingStoreType)
     * @param defer Whether to defer window creation
     * @return The created NSWindow instance
     */
    Pointer create_nswindow(
        double x,
        double y,
        double width,
        double height,
        long styleMask,
        long backing,
        boolean defer
    );

    /**
     * Create an NSImageView with the given frame rect.
     *
     * @param x X coordinate of the frame
     * @param y Y coordinate of the frame
     * @param width Width of the frame
     * @param height Height of the frame
     * @return The created NSImageView instance
     */
    Pointer create_nsimageview(
        double x,
        double y,
        double width,
        double height
    );

    /**
     * Get the bounds of a display as separate components.
     *
     * @param displayId The display ID
     * @param outX Pointer to store X coordinate
     * @param outY Pointer to store Y coordinate
     * @param outWidth Pointer to store width
     * @param outHeight Pointer to store height
     */
    void get_display_bounds(
        int displayId,
        DoubleByReference outX,
        DoubleByReference outY,
        DoubleByReference outWidth,
        DoubleByReference outHeight
    );

    /**
     * Configure an NSWindow for overlay use.
     *
     * @param window The NSWindow to configure
     * @param windowLevel The window level
     */
    void configure_overlay_window(Pointer window, int windowLevel);

    /**
     * Show an NSWindow.
     *
     * @param window The NSWindow to show
     */
    void show_window(Pointer window);

    /**
     * Hide an NSWindow.
     *
     * @param window The NSWindow to hide
     */
    void hide_window(Pointer window);

    /**
     * Set the content view of an NSWindow.
     *
     * @param window The NSWindow
     * @param view The view to set as content view
     */
    void set_content_view(Pointer window, Pointer view);

    /**
     * Close and release an NSWindow.
     *
     * @param window The NSWindow to close
     */
    void close_window(Pointer window);

    /**
     * Render pixel data to an NSImageView.
     *
     * This handles the complete rendering pipeline:
     * - Creates NSBitmapImageRep from pixel data
     * - Creates NSImage and adds the representation
     * - Sets the image to the NSImageView
     * - Releases the old bitmap
     *
     * @param imageView The NSImageView to render to
     * @param pixels Pointer to pixel data (ARGB8888 format)
     * @param width Image width
     * @param height Image height
     * @param oldBitmap Previous bitmap to release (can be null)
     * @return New NSBitmapImageRep pointer to pass on next call
     */
    Pointer render_pixels_to_imageview(
        Pointer imageView,
        Pointer pixels,
        int width,
        int height,
        Pointer oldBitmap
    );

    /**
     * Release a Cocoa object.
     *
     * @param obj The object to release
     */
    void release_object(Pointer obj);

    /**
     * Load the native library, extracting it from resources if necessary
     */
    static CocoaJNI loadLibrary() {
        String osName = System.getProperty("os.name").toLowerCase();
        String osArch = System.getProperty("os.arch").toLowerCase();

        if (!osName.contains("mac")) {
            throw new UnsatisfiedLinkError("cocoa-jni is only supported on macOS");
        }

        // Determine architecture folder name
        String archFolder;
        if (osArch.equals("aarch64") || osArch.equals("arm64")) {
            archFolder = "aarch64";
        } else if (osArch.equals("x86_64") || osArch.equals("amd64")) {
            archFolder = "x86-64";
        } else {
            throw new UnsatisfiedLinkError("Unsupported macOS architecture: " + osArch);
        }

        String libraryPath = "/native/macos-" + archFolder + "/libcocoa-jni.dylib";

        try {
            // Try to extract library from resources to temp directory
            InputStream is = CocoaJNI.class.getResourceAsStream(libraryPath);
            if (is == null) {
                throw new UnsatisfiedLinkError(
                    "Native library not found in resources: " + libraryPath +
                    "\nPlease build the native library first: cd bongocat-platform-macos/native && ./build.sh"
                );
            }

            // Create temp file
            File tempLib = File.createTempFile("libcocoa-jni", ".dylib");
            tempLib.deleteOnExit();

            // Copy library to temp file
            Files.copy(is, tempLib.toPath(), StandardCopyOption.REPLACE_EXISTING);
            is.close();

            // Load the library
            return Native.load(tempLib.getAbsolutePath(), CocoaJNI.class);

        } catch (Exception e) {
            throw new UnsatisfiedLinkError("Failed to load native library: " + e.getMessage());
        }
    }
}
