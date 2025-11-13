package rocks.blackblock.bongocat.platform.linux;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rocks.blackblock.bongocat.platform.DisplayManager;
import rocks.blackblock.bongocat.platform.InputMonitor;
import rocks.blackblock.bongocat.platform.PlatformException;
import rocks.blackblock.bongocat.platform.PlatformService;

/**
 * Linux platform service implementation.
 *
 * Provides factory methods for creating Linux-specific implementations
 * of the platform abstraction layer.
 */
public class LinuxPlatformService implements PlatformService {
    private static final Logger logger = LoggerFactory.getLogger(LinuxPlatformService.class);

    @Override
    public String getPlatformName() {
        String sessionType = System.getenv("XDG_SESSION_TYPE");
        if ("wayland".equalsIgnoreCase(sessionType)) {
            return "Linux/Wayland";
        } else if ("x11".equalsIgnoreCase(sessionType)) {
            return "Linux/X11 (unsupported)";
        } else {
            return "Linux (unknown session type)";
        }
    }

    @Override
    public boolean isSupported() {
        // Check if we're on Linux
        String osName = System.getProperty("os.name").toLowerCase();
        if (!osName.contains("linux")) {
            logger.debug("Not on Linux: {}", osName);
            return false;
        }

        // Check if we're on Wayland
        String sessionType = System.getenv("XDG_SESSION_TYPE");
        if (!"wayland".equalsIgnoreCase(sessionType)) {
            logger.warn("Not on Wayland session (XDG_SESSION_TYPE={}). Only Wayland is supported.", sessionType);
            return false;
        }

        // Check if WAYLAND_DISPLAY is set
        String waylandDisplay = System.getenv("WAYLAND_DISPLAY");
        if (waylandDisplay == null || waylandDisplay.trim().isEmpty()) {
            logger.warn("WAYLAND_DISPLAY environment variable not set");
            return false;
        }

        logger.info("Linux/Wayland platform detected and supported");
        return true;
    }

    @Override
    public DisplayManager createDisplayManager() throws PlatformException {
        if (!isSupported()) {
            throw new PlatformException("Linux/Wayland platform is not supported on this system");
        }

        return new LinuxDisplayManager();
    }

    @Override
    public InputMonitor createInputMonitor() throws PlatformException {
        if (!isSupported()) {
            throw new PlatformException("Linux/Wayland platform is not supported on this system");
        }

        return new LinuxInputMonitor();
    }

    /**
     * Detect if the current system supports this platform.
     *
     * @return platform service if supported, null otherwise
     */
    public static LinuxPlatformService detectAndCreate() {
        LinuxPlatformService service = new LinuxPlatformService();
        if (service.isSupported()) {
            return service;
        }
        return null;
    }
}
