package rocks.blackblock.bongocat.platform.macos;

import rocks.blackblock.bongocat.platform.Monitor;
import rocks.blackblock.bongocat.platform.macos.cocoa.CoreGraphics;

/**
 * macOS implementation of the Monitor interface.
 * Wraps Core Graphics display information.
 */
public class MacOSMonitor implements Monitor {
    private final int displayId;
    private final String name;
    private final int width;
    private final int height;
    private final int x;
    private final int y;
    private final boolean primary;

    public MacOSMonitor(int displayId, boolean isPrimary) {
        this.displayId = displayId;
        this.primary = isPrimary;

        // Get display bounds
        CoreGraphics.CGRect bounds = CoreGraphics.INSTANCE.CGDisplayBounds(displayId);
        this.x = (int) bounds.x;
        this.y = (int) bounds.y;
        this.width = (int) bounds.width;
        this.height = (int) bounds.height;

        // Create a name for the display
        this.name = "Display-" + displayId + (isPrimary ? " (Primary)" : "");
    }

    @Override
    public String getName() {
        return name;
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
    public int getX() {
        return x;
    }

    @Override
    public int getY() {
        return y;
    }

    @Override
    public double getScale() {
        // macOS handles scaling internally, return 1.0 for now
        // Could be extended to query NSScreen backingScaleFactor
        return 1.0;
    }

    @Override
    public Transform getTransform() {
        return Transform.NORMAL;
    }

    @Override
    public boolean isPrimary() {
        return primary;
    }

    public int getDisplayId() {
        return displayId;
    }

    @Override
    public String toString() {
        return String.format("MacOSMonitor{name='%s', size=%dx%d, pos=(%d,%d), primary=%s}",
                name, width, height, x, y, primary);
    }
}
