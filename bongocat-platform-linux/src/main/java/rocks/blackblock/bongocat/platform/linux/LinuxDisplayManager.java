package rocks.blackblock.bongocat.platform.linux;

import com.sun.jna.Pointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rocks.blackblock.bongocat.platform.*;
import rocks.blackblock.bongocat.platform.linux.wayland.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Linux/Wayland implementation of DisplayManager.
 *
 * Manages connection to the Wayland compositor and provides access to
 * monitors and overlay surfaces.
 */
public class LinuxDisplayManager implements DisplayManager {
    private static final Logger logger = LoggerFactory.getLogger(LinuxDisplayManager.class);

    private final WaylandLibrary wl = WaylandLibrary.INSTANCE;

    // Wayland objects
    private Pointer display;
    private Pointer registry;
    private Pointer compositor;
    private Pointer shm;
    private Pointer layerShell;
    private Pointer xdgOutputManager;

    // State tracking
    private final Map<Pointer, LinuxMonitor> monitors = new ConcurrentHashMap<>();
    private final Map<Pointer, Pointer> xdgOutputs = new ConcurrentHashMap<>();
    private final List<LinuxOverlaySurface> surfaces = new ArrayList<>();

    // Registry globals tracking
    private final Map<String, GlobalInfo> globals = new HashMap<>();

    private boolean initialized = false;
    private volatile boolean running = true;

    // Listeners (must be kept as instance variables to prevent GC)
    private WaylandTypes.RegistryListener registryListener;
    private final Map<Pointer, WaylandTypes.OutputListener> outputListeners = new HashMap<>();
    private final Map<Pointer, XdgOutputProtocol.XdgOutputListener> xdgOutputListeners = new HashMap<>();

    private static class GlobalInfo {
        int name;
        String interfaceName;
        int version;
        Pointer object;

        GlobalInfo(int name, String interfaceName, int version) {
            this.name = name;
            this.interfaceName = interfaceName;
            this.version = version;
        }
    }

    @Override
    public void initialize() throws PlatformException {
        if (initialized) {
            logger.warn("DisplayManager already initialized");
            return;
        }

        logger.info("Initializing Wayland display manager");

        // Connect to the Wayland display
        display = wl.wl_display_connect(null);
        if (display == null) {
            throw new PlatformException("Failed to connect to Wayland display. " +
                    "Make sure WAYLAND_DISPLAY is set and a compositor is running.");
        }

        logger.info("Connected to Wayland display");

        // Get the registry to discover global objects
        registry = wl.wl_display_get_registry(display);
        if (registry == null) {
            cleanup();
            throw new PlatformException("Failed to get Wayland registry");
        }

        // Set up registry listener
        setupRegistryListener();

        // Do initial roundtrip to get all globals
        if (wl.wl_display_roundtrip(display) < 0) {
            cleanup();
            throw new PlatformException("Failed to synchronize with Wayland display");
        }

        // Verify we got the required interfaces
        if (compositor == null) {
            cleanup();
            throw new PlatformException("Compositor interface not available");
        }
        if (shm == null) {
            cleanup();
            throw new PlatformException("Shared memory (wl_shm) interface not available");
        }
        if (layerShell == null) {
            cleanup();
            throw new PlatformException("Layer shell interface not available. " +
                    "This requires a wlroots-based compositor (Sway, Hyprland, etc.)");
        }

        logger.info("Found required Wayland interfaces:");
        logger.info("  - wl_compositor: yes");
        logger.info("  - wl_shm: yes");
        logger.info("  - zwlr_layer_shell_v1: yes");
        logger.info("  - zxdg_output_manager_v1: {}", xdgOutputManager != null ? "yes" : "no");
        logger.info("  - Monitors: {}", monitors.size());

        // Do another roundtrip to get monitor info
        wl.wl_display_roundtrip(display);

        initialized = true;
        logger.info("Display manager initialized successfully");
    }

    /**
     * Set up the registry listener to discover global interfaces.
     */
    private void setupRegistryListener() {
        registryListener = new WaylandTypes.RegistryListener();

        // Global callback - called when a global interface is advertised
        registryListener.global = (data, registryPtr, name, interfaceName, version) -> {
            logger.debug("Registry global: {} (name={}, version={})", interfaceName, name, version);

            globals.put(interfaceName, new GlobalInfo(name, interfaceName, version));

            try {
                switch (interfaceName) {
                    case "wl_compositor":
                        compositor = wl.wl_registry_bind(registryPtr, name, "wl_compositor", Math.min(version, 4));
                        logger.debug("Bound wl_compositor");
                        break;

                    case "wl_shm":
                        shm = wl.wl_registry_bind(registryPtr, name, "wl_shm", 1);
                        logger.debug("Bound wl_shm");
                        break;

                    case "wl_output":
                        Pointer output = wl.wl_registry_bind(registryPtr, name, "wl_output", Math.min(version, 3));
                        if (output != null) {
                            LinuxMonitor monitor = new LinuxMonitor(output);
                            monitors.put(output, monitor);
                            setupOutputListener(output, monitor);
                            logger.debug("Bound wl_output (monitor)");

                            // TEMPORARILY DISABLED: XdgOutputProtocol causes SIGSEGV
                            // If we have xdg_output_manager, get xdg_output for this output
                            // if (xdgOutputManager != null) {
                            //     setupXdgOutput(output, monitor);
                            // }
                        }
                        break;

                    case LayerShellProtocol.INTERFACE_NAME:
                        layerShell = wl.wl_registry_bind(registryPtr, name, LayerShellProtocol.INTERFACE_NAME,
                                Math.min(version, LayerShellProtocol.VERSION));
                        logger.debug("Bound zwlr_layer_shell_v1");
                        break;

                    case XdgOutputProtocol.MANAGER_INTERFACE_NAME:
                        xdgOutputManager = wl.wl_registry_bind(registryPtr, name,
                                XdgOutputProtocol.MANAGER_INTERFACE_NAME,
                                Math.min(version, XdgOutputProtocol.VERSION));
                        logger.debug("Bound zxdg_output_manager_v1");

                        // TEMPORARILY DISABLED: XdgOutputProtocol causes SIGSEGV
                        // Set up xdg_output for existing monitors
                        // for (Map.Entry<Pointer, LinuxMonitor> entry : monitors.entrySet()) {
                        //     setupXdgOutput(entry.getKey(), entry.getValue());
                        // }
                        break;

                    default:
                        // Ignore other globals
                        break;
                }
            } catch (Exception e) {
                logger.error("Error handling global {}: {}", interfaceName, e.getMessage(), e);
            }
        };

        // Global remove callback
        registryListener.global_remove = (data, registryPtr, name) -> {
            logger.debug("Registry global removed: name={}", name);
            // Could handle monitor removal here
        };

        // Add the listener
        registryListener.write();
        wl.wl_registry_add_listener(registry, registryListener.getPointer(), null);
    }

    /**
     * Set up output listener for a monitor.
     */
    private void setupOutputListener(Pointer output, LinuxMonitor monitor) {
        WaylandTypes.OutputListener listener = new WaylandTypes.OutputListener();

        listener.geometry = (data, outputPtr, x, y, physicalWidth, physicalHeight,
                            subpixel, make, model, transform) -> {
            monitor.setPosition(x, y);
            monitor.setPhysicalSize(physicalWidth, physicalHeight);
            monitor.setTransform(WaylandTypes.transformFromWayland(transform));
            logger.debug("Output geometry: {}x{} at ({},{}), transform={}",
                        physicalWidth, physicalHeight, x, y, transform);
        };

        listener.mode = (data, outputPtr, flags, width, height, refresh) -> {
            if ((flags & WaylandTypes.WL_OUTPUT_MODE_CURRENT) != 0) {
                monitor.setSize(width, height);
                logger.debug("Output mode: {}x{} @ {}Hz (current)", width, height, refresh / 1000);
            }
        };

        listener.done = (data, outputPtr) -> {
            logger.debug("Output done: {}", monitor);
        };

        listener.scale = (data, outputPtr, factor) -> {
            monitor.setScale(factor);
            logger.debug("Output scale: {}", factor);
        };

        listener.write();
        outputListeners.put(output, listener);
        wl.wl_proxy_add_listener(output, listener.getPointer(), null);
    }

    /**
     * Set up xdg_output for a monitor to get name and logical size.
     */
    private void setupXdgOutput(Pointer output, LinuxMonitor monitor) {
        if (xdgOutputManager == null) {
            return;
        }

        Pointer xdgOutput = XdgOutputProtocol.getXdgOutput(wl, xdgOutputManager, output);
        if (xdgOutput == null) {
            logger.warn("Failed to get xdg_output for monitor");
            return;
        }

        xdgOutputs.put(output, xdgOutput);

        XdgOutputProtocol.XdgOutputListener listener = new XdgOutputProtocol.XdgOutputListener();

        listener.logical_position = (data, xdgOutputPtr, x, y) -> {
            monitor.setPosition(x, y);
            logger.debug("XDG output logical position: ({}, {})", x, y);
        };

        listener.logical_size = (data, xdgOutputPtr, width, height) -> {
            monitor.setSize(width, height);
            logger.debug("XDG output logical size: {}x{}", width, height);
        };

        listener.done = (data, xdgOutputPtr) -> {
            logger.debug("XDG output done: {}", monitor);
        };

        listener.name = (data, xdgOutputPtr, name) -> {
            monitor.setName(name);
            logger.info("Monitor name: {}", name);
        };

        listener.description = (data, xdgOutputPtr, description) -> {
            logger.debug("Monitor description: {}", description);
        };

        listener.write();
        xdgOutputListeners.put(xdgOutput, listener);
        wl.wl_proxy_add_listener(xdgOutput, listener.getPointer(), null);
    }

    @Override
    public List<Monitor> getMonitors() {
        return new ArrayList<>(monitors.values());
    }

    @Override
    public Monitor findMonitor(String name) {
        if (name == null) {
            return null;
        }

        for (LinuxMonitor monitor : monitors.values()) {
            if (name.equals(monitor.getName())) {
                return monitor;
            }
        }
        return null;
    }

    @Override
    public Monitor getPrimaryMonitor() {
        // First try to find a monitor marked as primary
        for (LinuxMonitor monitor : monitors.values()) {
            if (monitor.isPrimary()) {
                return monitor;
            }
        }

        // Otherwise return the first monitor
        if (!monitors.isEmpty()) {
            return monitors.values().iterator().next();
        }

        return null;
    }

    @Override
    public OverlaySurface createOverlay(Monitor monitor, Position position, int height) throws PlatformException {
        if (!initialized) {
            throw new PlatformException("DisplayManager not initialized");
        }

        if (!(monitor instanceof LinuxMonitor)) {
            throw new PlatformException("Invalid monitor type");
        }

        LinuxMonitor linuxMonitor = (LinuxMonitor) monitor;

        logger.info("Creating overlay surface on monitor {} at {} with height {}",
                    linuxMonitor.getName(), position, height);

        LinuxOverlaySurface surface = new LinuxOverlaySurface(
            this,
            linuxMonitor,
            position,
            height
        );

        surface.initialize();
        surfaces.add(surface);

        return surface;
    }

    @Override
    public boolean processEvents(int timeoutMs) throws PlatformException {
        if (!initialized) {
            throw new PlatformException("DisplayManager not initialized");
        }

        // Check for display errors first
        int error = wl.wl_display_get_error(display);
        if (error != 0) {
            String errorMsg = getWaylandErrorMessage(error);
            throw new PlatformException("Wayland display error: " + errorMsg + " (errno=" + error + ")");
        }

        // Dispatch pending events without blocking
        int pending = wl.wl_display_dispatch_pending(display);
        if (pending < 0) {
            error = wl.wl_display_get_error(display);
            String errorMsg = getWaylandErrorMessage(error);
            throw new PlatformException("Error dispatching events: " + errorMsg + " (errno=" + error + ")");
        }

        if (timeoutMs == 0) {
            // Non-blocking - we're done
            return pending > 0;
        }

        // Prepare to read from display
        if (wl.wl_display_prepare_read(display) < 0) {
            // Events are queued, dispatch them
            return wl.wl_display_dispatch_pending(display) > 0;
        }

        // Flush any pending requests
        // Note: flush can return < 0 with EAGAIN (buffer full) which is not an error
        wl.wl_display_flush(display);

        // Poll the display file descriptor
        int fd = wl.wl_display_get_fd(display);
        boolean hasEvents = pollFileDescriptor(fd, timeoutMs);

        if (hasEvents) {
            // Read events from the display
            if (wl.wl_display_read_events(display) < 0) {
                throw new PlatformException("Failed to read events");
            }

            // Dispatch the events we just read
            return wl.wl_display_dispatch_pending(display) > 0;
        } else {
            // Timeout or error - cancel the read
            wl.wl_display_cancel_read(display);
            return false;
        }
    }

    /**
     * Poll a file descriptor using Java's poll mechanism.
     * This is a simplified version - in production you'd use JNA poll() or select().
     */
    private boolean pollFileDescriptor(int fd, int timeoutMs) {
        // For now, we'll use a simple approach: dispatch with a small timeout
        // In a production implementation, you'd use actual poll() or select() via JNA
        try {
            if (timeoutMs > 0) {
                Thread.sleep(Math.min(timeoutMs, 16)); // Max 16ms sleep for responsive event loop
            }
            return true; // Assume events might be available
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Get the Wayland display pointer (for LinuxOverlaySurface).
     */
    Pointer getDisplay() {
        return display;
    }

    /**
     * Get the compositor pointer (for LinuxOverlaySurface).
     */
    Pointer getCompositor() {
        return compositor;
    }

    /**
     * Get the SHM pointer (for LinuxOverlaySurface).
     */
    Pointer getShm() {
        return shm;
    }

    /**
     * Get the layer shell pointer (for LinuxOverlaySurface).
     */
    Pointer getLayerShell() {
        return layerShell;
    }

    /**
     * Get the Wayland library instance.
     */
    WaylandLibrary getWaylandLibrary() {
        return wl;
    }

    /**
     * Get a human-readable error message for a Wayland error code.
     */
    private String getWaylandErrorMessage(int errno) {
        switch (errno) {
            case 1: return "EPERM (Operation not permitted)";
            case 2: return "ENOENT (No such file or directory)";
            case 3: return "ESRCH (No such process)";
            case 4: return "EINTR (Interrupted system call)";
            case 5: return "EIO (I/O error)";
            case 6: return "ENXIO (No such device or address)";
            case 9: return "EBADF (Bad file descriptor - protocol error occurred)";
            case 11: return "EAGAIN (Resource temporarily unavailable)";
            case 12: return "ENOMEM (Out of memory)";
            case 13: return "EACCES (Permission denied)";
            case 22: return "EINVAL (Invalid argument)";
            case 32: return "EPIPE (Broken pipe)";
            case 71: return "EPROTO (Protocol error - compositor detected protocol violation)";
            case 104: return "ECONNRESET (Connection reset by peer)";
            case 110: return "ETIMEDOUT (Connection timed out)";
            default: return "Unknown error";
        }
    }

    @Override
    public void close() {
        logger.info("Closing display manager");
        running = false;

        // Close all surfaces
        for (LinuxOverlaySurface surface : new ArrayList<>(surfaces)) {
            try {
                surface.close();
            } catch (Exception e) {
                logger.error("Error closing surface: {}", e.getMessage());
            }
        }
        surfaces.clear();

        cleanup();
    }

    /**
     * Clean up Wayland resources.
     */
    private void cleanup() {
        // Destroy xdg outputs
        for (Pointer xdgOutput : xdgOutputs.values()) {
            try {
                XdgOutputProtocol.destroy(wl, xdgOutput);
            } catch (Exception e) {
                logger.debug("Error destroying xdg_output: {}", e.getMessage());
            }
        }
        xdgOutputs.clear();

        // Destroy outputs (monitors)
        for (Pointer output : monitors.keySet()) {
            try {
                wl.wl_proxy_destroy(output);
            } catch (Exception e) {
                logger.debug("Error destroying output: {}", e.getMessage());
            }
        }
        monitors.clear();
        outputListeners.clear();
        xdgOutputListeners.clear();

        // Destroy xdg output manager
        if (xdgOutputManager != null) {
            try {
                XdgOutputProtocol.destroyManager(wl, xdgOutputManager);
            } catch (Exception e) {
                logger.debug("Error destroying xdg_output_manager: {}", e.getMessage());
            }
            xdgOutputManager = null;
        }

        // Destroy layer shell
        if (layerShell != null) {
            try {
                wl.wl_proxy_destroy(layerShell);
            } catch (Exception e) {
                logger.debug("Error destroying layer_shell: {}", e.getMessage());
            }
            layerShell = null;
        }

        // Destroy shm
        if (shm != null) {
            try {
                wl.wl_proxy_destroy(shm);
            } catch (Exception e) {
                logger.debug("Error destroying shm: {}", e.getMessage());
            }
            shm = null;
        }

        // Destroy compositor
        if (compositor != null) {
            try {
                wl.wl_proxy_destroy(compositor);
            } catch (Exception e) {
                logger.debug("Error destroying compositor: {}", e.getMessage());
            }
            compositor = null;
        }

        // Destroy registry
        if (registry != null) {
            try {
                wl.wl_proxy_destroy(registry);
            } catch (Exception e) {
                logger.debug("Error destroying registry: {}", e.getMessage());
            }
            registry = null;
        }

        // Disconnect from display
        if (display != null) {
            try {
                wl.wl_display_disconnect(display);
            } catch (Exception e) {
                logger.debug("Error disconnecting display: {}", e.getMessage());
            }
            display = null;
        }

        initialized = false;
        logger.info("Display manager closed");
    }
}
