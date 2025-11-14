/**
 * JNA helper library for macOS Cocoa bindings.
 *
 * This library provides wrapper functions for Cocoa API calls that involve
 * structure-by-value passing, which JNA cannot properly marshal on arm64 macOS.
 *
 * Similar to wayland-protocols-jni.c, this avoids JNA varargs marshalling issues.
 */

#include <Cocoa/Cocoa.h>
#include <objc/runtime.h>
#include <objc/message.h>

/**
 * Create an NSWindow with the given frame rect.
 *
 * This wrapper avoids JNA's inability to pass NSRect by value through objc_msgSend.
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
void* create_nswindow(
    double x,
    double y,
    double width,
    double height,
    unsigned long styleMask,
    unsigned long backing,
    bool defer
) {
    NSRect frame = NSMakeRect(x, y, width, height);

    NSWindow *window = [[NSWindow alloc]
        initWithContentRect:frame
        styleMask:styleMask
        backing:backing
        defer:defer ? YES : NO];

    // Return the pointer directly - alloc already retains it
    // Caller must release when done
    return (void*)window;
}

/**
 * Create an NSImageView with the given frame rect.
 *
 * @param x X coordinate of the frame
 * @param y Y coordinate of the frame
 * @param width Width of the frame
 * @param height Height of the frame
 * @return The created NSImageView instance
 */
void* create_nsimageview(
    double x,
    double y,
    double width,
    double height
) {
    NSRect frame = NSMakeRect(x, y, width, height);

    NSImageView *imageView = [[NSImageView alloc] initWithFrame:frame];

    // Return the pointer directly - alloc already retains it
    // Caller must release when done
    return (void*)imageView;
}

/**
 * Get the bounds of a display as separate components.
 *
 * This avoids JNA structure-by-value return issues.
 *
 * @param displayId The display ID
 * @param outX Pointer to store X coordinate
 * @param outY Pointer to store Y coordinate
 * @param outWidth Pointer to store width
 * @param outHeight Pointer to store height
 */
void get_display_bounds(
    unsigned int displayId,
    double *outX,
    double *outY,
    double *outWidth,
    double *outHeight
) {
    CGRect bounds = CGDisplayBounds(displayId);

    if (outX) *outX = bounds.origin.x;
    if (outY) *outY = bounds.origin.y;
    if (outWidth) *outWidth = bounds.size.width;
    if (outHeight) *outHeight = bounds.size.height;
}

/**
 * Configure an NSWindow for overlay use.
 *
 * Sets all the necessary properties: transparent, ignores mouse events, floating level, etc.
 *
 * @param window The NSWindow to configure
 * @param windowLevel The window level (e.g., NSFloatingWindowLevel)
 */
void configure_overlay_window(void *window, int windowLevel) {
    NSWindow *nsWindow = (NSWindow*)window;

    // Set window to be transparent
    [nsWindow setOpaque:NO];

    // Create transparent background color
    NSColor *clearColor = [NSColor colorWithDeviceRed:0.0 green:0.0 blue:0.0 alpha:0.0];
    [nsWindow setBackgroundColor:clearColor];

    // Ignore mouse events
    [nsWindow setIgnoresMouseEvents:YES];

    // Set window level
    [nsWindow setLevel:windowLevel];

    // Set collection behavior
    [nsWindow setCollectionBehavior:
        NSWindowCollectionBehaviorCanJoinAllSpaces |
        NSWindowCollectionBehaviorStationary |
        NSWindowCollectionBehaviorIgnoresCycle];
}

/**
 * Show an NSWindow.
 *
 * @param window The NSWindow to show
 */
void show_window(void *window) {
    NSWindow *nsWindow = (NSWindow*)window;
    [nsWindow makeKeyAndOrderFront:nil];
}

/**
 * Hide an NSWindow.
 *
 * @param window The NSWindow to hide
 */
void hide_window(void *window) {
    NSWindow *nsWindow = (NSWindow*)window;
    [nsWindow orderOut:nil];
}

/**
 * Set the content view of an NSWindow.
 *
 * @param window The NSWindow
 * @param view The view to set as content view
 */
void set_content_view(void *window, void *view) {
    NSWindow *nsWindow = (NSWindow*)window;
    NSView *nsView = (NSView*)view;
    [nsWindow setContentView:nsView];
}

/**
 * Close and release an NSWindow.
 *
 * @param window The NSWindow to close
 */
void close_window(void *window) {
    NSWindow *nsWindow = (NSWindow*)window;
    [nsWindow close];
    [nsWindow release];
}

/**
 * Create an NSBitmapImageRep from pixel data and set it to an NSImageView.
 *
 * This function handles the entire rendering pipeline:
 * 1. Creates NSBitmapImageRep from ARGB pixel data
 * 2. Creates NSImage and adds the bitmap representation
 * 3. Sets the image to the NSImageView
 * 4. Releases the old bitmap if provided
 *
 * @param imageView The NSImageView to update
 * @param pixels Pointer to pixel data (ARGB8888 format)
 * @param width Image width
 * @param height Image height
 * @param oldBitmap Previous bitmap to release (can be NULL)
 * @return The new NSBitmapImageRep (caller should save for next call)
 */
void* render_pixels_to_imageview(
    void *imageView,
    void *pixels,
    int width,
    int height,
    void *oldBitmap
) {
    NSImageView *nsImageView = (NSImageView*)imageView;

    // Create NSBitmapImageRep with the pixel data
    NSBitmapImageRep *bitmapRep = [[NSBitmapImageRep alloc]
        initWithBitmapDataPlanes:NULL  // Let Cocoa allocate
        pixelsWide:width
        pixelsHigh:height
        bitsPerSample:8
        samplesPerPixel:4
        hasAlpha:YES
        isPlanar:NO
        colorSpaceName:NSDeviceRGBColorSpace
        bitmapFormat:NSBitmapFormatAlphaFirst
        bytesPerRow:width * 4
        bitsPerPixel:32];

    // Copy pixel data to the bitmap
    unsigned char *bitmapData = [bitmapRep bitmapData];
    if (bitmapData && pixels) {
        memcpy(bitmapData, pixels, width * height * 4);
    }

    // Create NSImage and add the representation
    NSImage *image = [[NSImage alloc] init];
    [image addRepresentation:bitmapRep];

    // Set the image to the image view
    [nsImageView setImage:image];

    // Release the image (imageView retains it)
    [image release];

    // Release old bitmap if provided
    if (oldBitmap) {
        NSBitmapImageRep *oldRep = (NSBitmapImageRep*)oldBitmap;
        [oldRep release];
    }

    // Return the new bitmap (caller should save it for next frame)
    return (void*)bitmapRep;
}

/**
 * Release a Cocoa object.
 *
 * @param obj The object to release
 */
void release_object(void *obj) {
    if (obj) {
        id object = (id)obj;
        [object release];
    }
}
