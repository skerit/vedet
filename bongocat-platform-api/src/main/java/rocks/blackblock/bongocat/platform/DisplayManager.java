package rocks.blackblock.bongocat.platform;

import java.util.List;

/**
 * Manages connection to the display server and monitor enumeration.
 */
public interface DisplayManager extends AutoCloseable {
    /**
     * Initialize connection to the display server (Wayland, X11, Win32, etc.)
     *
     * @throws PlatformException if connection fails
     */
    void initialize() throws PlatformException;

    /**
     * Get all available monitors
     *
     * @return list of monitors
     */
    List<Monitor> getMonitors();

    /**
     * Find a monitor by name
     *
     * @param name the monitor name (e.g., "eDP-1")
     * @return the monitor, or null if not found
     */
    Monitor findMonitor(String name);

    /**
     * Get the primary monitor
     *
     * @return the primary monitor
     */
    Monitor getPrimaryMonitor();

    /**
     * Create an overlay surface on the specified monitor
     *
     * @param monitor the target monitor
     * @param position overlay position (top or bottom)
     * @param height overlay height in pixels
     * @return the overlay surface
     * @throws PlatformException if creation fails
     */
    OverlaySurface createOverlay(Monitor monitor, Position position, int height) throws PlatformException;

    /**
     * Process pending events from the display server
     * This method should be called regularly from the main event loop
     *
     * @param timeoutMs maximum time to wait for events in milliseconds (0 = don't wait, -1 = wait indefinitely)
     * @return true if events were processed, false if timed out
     * @throws PlatformException if an error occurs
     */
    boolean processEvents(int timeoutMs) throws PlatformException;

    /**
     * Close the display connection and release resources
     */
    @Override
    void close();
}
