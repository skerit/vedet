package rocks.blackblock.bongocat.platform.macos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rocks.blackblock.bongocat.platform.DisplayManager;
import rocks.blackblock.bongocat.platform.InputMonitor;
import rocks.blackblock.bongocat.platform.PlatformException;
import rocks.blackblock.bongocat.platform.PlatformService;

/**
 * macOS platform service implementation.
 *
 * Provides factory methods for creating macOS-specific implementations
 * of the platform abstraction layer.
 */
public class MacOSPlatformService implements PlatformService {
    private static final Logger logger = LoggerFactory.getLogger(MacOSPlatformService.class);

    @Override
    public String getPlatformName() {
        String osVersion = System.getProperty("os.version", "unknown");
        return "macOS " + osVersion;
    }

    @Override
    public boolean isSupported() {
        // Check if we're on macOS
        String osName = System.getProperty("os.name").toLowerCase();
        if (!osName.contains("mac")) {
            logger.debug("Not on macOS: {}", osName);
            return false;
        }

        logger.info("macOS platform detected and supported");
        return true;
    }

    @Override
    public DisplayManager createDisplayManager() throws PlatformException {
        if (!isSupported()) {
            throw new PlatformException("macOS platform is not supported on this system");
        }

        return new MacOSDisplayManager();
    }

    @Override
    public InputMonitor createInputMonitor() throws PlatformException {
        if (!isSupported()) {
            throw new PlatformException("macOS platform is not supported on this system");
        }

        return new MacOSInputMonitor();
    }

    /**
     * Detect if the current system supports this platform.
     *
     * @return platform service if supported, null otherwise
     */
    public static MacOSPlatformService detectAndCreate() {
        MacOSPlatformService service = new MacOSPlatformService();
        if (service.isSupported()) {
            return service;
        }
        return null;
    }
}
