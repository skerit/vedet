package rocks.blackblock.bongocat.platform;

/**
 * Represents a physical monitor/display.
 */
public interface Monitor {
    /**
     * Get the monitor's name (e.g., "eDP-1", "HDMI-1")
     */
    String getName();

    /**
     * Get the monitor's width in pixels
     */
    int getWidth();

    /**
     * Get the monitor's height in pixels
     */
    int getHeight();

    /**
     * Get the monitor's X position in the global coordinate space
     */
    int getX();

    /**
     * Get the monitor's Y position in the global coordinate space
     */
    int getY();

    /**
     * Get the monitor's scale factor (e.g., 1.0 for standard, 2.0 for HiDPI)
     */
    double getScale();

    /**
     * Get the monitor's rotation/transform
     */
    Transform getTransform();

    /**
     * Check if this monitor is the primary monitor
     */
    boolean isPrimary();

    enum Transform {
        NORMAL,
        ROTATED_90,
        ROTATED_180,
        ROTATED_270,
        FLIPPED,
        FLIPPED_90,
        FLIPPED_180,
        FLIPPED_270
    }
}
