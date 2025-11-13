package rocks.blackblock.bongocat.platform;

import java.util.List;
import java.util.function.Consumer;

/**
 * Monitors keyboard input events from the system.
 */
public interface InputMonitor extends AutoCloseable {
    /**
     * Start monitoring the specified input devices.
     *
     * @param devicePaths list of device paths (e.g., ["/dev/input/event3", "/dev/input/event4"])
     * @param eventHandler callback for input events
     * @throws PlatformException if monitoring fails to start
     */
    void start(List<String> devicePaths, Consumer<InputEvent> eventHandler) throws PlatformException;

    /**
     * Stop monitoring input events
     */
    void stop();

    /**
     * Check if the monitor is currently running
     */
    boolean isRunning();

    /**
     * Get a list of available keyboard devices on the system.
     * This is a helper method to discover input devices.
     *
     * @return list of device paths
     * @throws PlatformException if device enumeration fails
     */
    List<String> findKeyboardDevices() throws PlatformException;

    /**
     * Close the monitor and release resources
     */
    @Override
    void close();
}
