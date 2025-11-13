package rocks.blackblock.bongocat.platform;

/**
 * Factory for creating platform-specific implementations.
 * This is the main entry point for platform abstraction.
 */
public interface PlatformService {
    /**
     * Get the platform name (e.g., "Linux/Wayland", "Windows", "macOS")
     */
    String getPlatformName();

    /**
     * Check if this platform is supported on the current system
     */
    boolean isSupported();

    /**
     * Create a display manager instance
     *
     * @return new display manager
     * @throws PlatformException if display manager creation fails
     */
    DisplayManager createDisplayManager() throws PlatformException;

    /**
     * Create an input monitor instance
     *
     * @return new input monitor
     * @throws PlatformException if input monitor creation fails
     */
    InputMonitor createInputMonitor() throws PlatformException;

    /**
     * Detect the current platform and return appropriate service.
     * This method uses ServiceLoader to discover platform implementations.
     *
     * @return platform service for the current system
     * @throws PlatformException if no supported platform is found
     */
    static PlatformService detect() throws PlatformException {
        // Try to detect platform based on system properties
        String osName = System.getProperty("os.name").toLowerCase();
        String sessionType = System.getenv("XDG_SESSION_TYPE");

        // For now, we'll return null and let the implementation be discovered via ServiceLoader
        // In the future, this can use java.util.ServiceLoader to auto-discover implementations
        throw new PlatformException("Platform detection not yet implemented. " +
                "OS: " + osName + ", Session: " + sessionType);
    }
}
