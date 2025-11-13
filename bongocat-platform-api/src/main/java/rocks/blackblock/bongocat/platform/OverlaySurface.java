package rocks.blackblock.bongocat.platform;

import java.nio.ByteBuffer;

/**
 * Represents a transparent overlay surface for rendering.
 */
public interface OverlaySurface extends AutoCloseable {
    /**
     * Get the surface width in pixels
     */
    int getWidth();

    /**
     * Get the surface height in pixels
     */
    int getHeight();

    /**
     * Get the monitor this surface is attached to
     */
    Monitor getMonitor();

    /**
     * Get the current layer (OVERLAY or TOP)
     */
    Layer getLayer();

    /**
     * Set the layer for this surface
     * OVERLAY = normal overlay (below fullscreen windows)
     * TOP = always on top (above fullscreen windows)
     *
     * @param layer the desired layer
     * @throws PlatformException if layer change fails
     */
    void setLayer(Layer layer) throws PlatformException;

    /**
     * Get a direct buffer for pixel data (ARGB8888 format).
     * The buffer size should be width * height * 4 bytes.
     * Changes to this buffer will be displayed on the next commit().
     *
     * @return byte buffer for pixel data
     */
    ByteBuffer getPixelBuffer();

    /**
     * Commit the current pixel buffer to the display.
     * This makes the rendered content visible.
     *
     * @throws PlatformException if commit fails
     */
    void commit() throws PlatformException;

    /**
     * Show the surface (make it visible)
     */
    void show();

    /**
     * Hide the surface (make it invisible)
     */
    void hide();

    /**
     * Check if the surface is currently visible
     */
    boolean isVisible();

    /**
     * Detect if there's a fullscreen window on this monitor.
     * Used to auto-hide the overlay during fullscreen apps.
     *
     * @return true if a fullscreen window is detected
     */
    boolean isFullscreenWindowPresent();

    /**
     * Close the surface and release resources
     */
    @Override
    void close();

    enum Layer {
        OVERLAY,  // Below fullscreen windows
        TOP       // Above everything
    }
}
