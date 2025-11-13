package rocks.blackblock.bongocat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rocks.blackblock.bongocat.cli.BongoCatCommand;
import rocks.blackblock.bongocat.config.Configuration;
import rocks.blackblock.bongocat.config.ConfigurationException;
import rocks.blackblock.bongocat.config.ConfigurationLoader;
import rocks.blackblock.bongocat.core.ProcessManager;
import rocks.blackblock.bongocat.graphics.AnimationEngine;
import rocks.blackblock.bongocat.graphics.AssetLoader;
import rocks.blackblock.bongocat.graphics.FrameRenderer;
import rocks.blackblock.bongocat.platform.*;
import rocks.blackblock.bongocat.platform.InputMonitor;
import rocks.blackblock.bongocat.platform.linux.LinuxPlatformService;
import rocks.blackblock.bongocat.platform.macos.MacOSPlatformService;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.List;

/**
 * Main application entry point.
 */
public class Application {
    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    private final Configuration config;
    private final ProcessManager processManager;
    private final PlatformService platformService;

    private DisplayManager displayManager;
    private InputMonitor inputMonitor;
    private OverlaySurface surface;
    private AnimationEngine animationEngine;
    private volatile boolean running = true;

    public Application(Configuration config) {
        this.config = config;
        this.processManager = new ProcessManager();
        this.platformService = detectPlatform();
    }

    /**
     * Detect the current platform and return appropriate service.
     */
    private PlatformService detectPlatform() {
        // Try macOS first
        MacOSPlatformService macosService = MacOSPlatformService.detectAndCreate();
        if (macosService != null) {
            logger.info("Platform detected: {}", macosService.getPlatformName());
            return macosService;
        }

        // Try Linux/Wayland
        LinuxPlatformService linuxService = LinuxPlatformService.detectAndCreate();
        if (linuxService != null) {
            logger.info("Platform detected: {}", linuxService.getPlatformName());
            return linuxService;
        }

        throw new RuntimeException("No supported platform detected. " +
                                 "Supported platforms: macOS, Linux/Wayland");
    }

    /**
     * Initialize the application.
     */
    public void initialize() throws Exception {
        logger.info("Initializing Bongo Cat application");

        // Check for existing instance
        if (!processManager.createPidFile()) {
            logger.warn("Another instance is already running");
            throw new IllegalStateException("Another instance is already running");
        }

        // Initialize display manager
        displayManager = platformService.createDisplayManager();
        displayManager.initialize();

        // Get monitors
        List<Monitor> monitors = displayManager.getMonitors();
        logger.info("Found {} monitor(s)", monitors.size());
        for (Monitor monitor : monitors) {
            logger.info("  - {}: {}x{} at ({},{}) scale={} transform={}",
                       monitor.getName(), monitor.getWidth(), monitor.getHeight(),
                       monitor.getX(), monitor.getY(), monitor.getScale(), monitor.getTransform());
        }

        // Select monitor
        Monitor targetMonitor;
        if (config.getMonitorName() != null) {
            targetMonitor = displayManager.findMonitor(config.getMonitorName());
            if (targetMonitor == null) {
                logger.warn("Monitor '{}' not found, using primary", config.getMonitorName());
                targetMonitor = displayManager.getPrimaryMonitor();
            }
        } else {
            targetMonitor = displayManager.getPrimaryMonitor();
        }

        if (targetMonitor == null) {
            throw new PlatformException("No monitors found");
        }

        logger.info("Using monitor: {} ({}x{})", targetMonitor.getName(),
                   targetMonitor.getWidth(), targetMonitor.getHeight());

        // Create overlay surface
        surface = displayManager.createOverlay(
            targetMonitor,
            config.getOverlayPosition(),
            config.getOverlayHeight()
        );

        logger.info("Created overlay surface: {}x{}", surface.getWidth(), surface.getHeight());

        // Load animation frames from assets directory
        List<BufferedImage> frames;
        try {
            Path assetsDir = Path.of("assets");
            logger.debug("Loading bongo cat frames from: {}", assetsDir.toAbsolutePath());
            frames = AssetLoader.loadBongoCatFrames(assetsDir);
            logger.info("Loaded {} bongo cat frames", frames.size());
        } catch (Exception e) {
            logger.warn("Failed to load bongo cat frames, using test frames: {}", e.getMessage());
            frames = AssetLoader.createTestFrames(200, 160, 4);
        }

        // Create animation engine
        animationEngine = new AnimationEngine(frames, config);
        animationEngine.setTargetDimensions(surface.getWidth(), surface.getHeight());

        // Initialize input monitoring
        inputMonitor = platformService.createInputMonitor();
        List<String> keyboardDevices = config.getKeyboardDevices();

        // If no devices specified in config, try to find keyboards automatically
        if (keyboardDevices == null || keyboardDevices.isEmpty()) {
            logger.info("No keyboard devices specified in config, attempting auto-detection");
            keyboardDevices = inputMonitor.findKeyboardDevices();
            if (keyboardDevices.isEmpty()) {
                logger.warn("No keyboard devices found. Input monitoring will not work.");
                logger.warn("Please configure keyboard_device in config or ensure user is in 'input' group");
            }
        }

        // Start input monitoring if devices are available
        if (!keyboardDevices.isEmpty()) {
            logger.info("Starting input monitoring with {} device(s)", keyboardDevices.size());
            try {
                inputMonitor.start(keyboardDevices, animationEngine::handleInput);
            } catch (Exception e) {
                logger.warn("Failed to start input monitoring: {}", e.getMessage());
                logger.warn("The application will continue without input monitoring");
                logger.warn("To fix this, ensure your user is in the 'input' group: sudo usermod -a -G input $USER");
                inputMonitor = null; // Set to null so we don't try to stop it later
            }
        }

        logger.info("Application initialized successfully");
    }

    /**
     * Start the application event loop.
     */
    public void start() {
        logger.info("Starting Bongo Cat");

        // Show the surface
        surface.show();

        // Give compositor time to process configure/commit
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        // Main event loop
        while (running) {
            try {
                // Process display events FIRST to handle compositor responses
                displayManager.processEvents(16); // ~60 FPS

                // Update animation state
                animationEngine.update();

                // Render current frame
                renderFrame();

                // Small sleep to limit CPU usage
                Thread.sleep(16);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.info("Event loop interrupted");
                break;
            } catch (Exception e) {
                logger.warn("Error in event loop (continuing): {}", e.getMessage());
                // Don't break - just skip this iteration and continue
                try {
                    Thread.sleep(16);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        logger.info("Event loop stopped");
    }

    /**
     * Render the current animation frame to the surface.
     */
    private void renderFrame() {
        try {
            ByteBuffer buffer = surface.getPixelBuffer();

            // Clear to transparent
            FrameRenderer.clear(buffer, surface.getWidth(), surface.getHeight(), 0x00000000);

            // Get current frame
            BufferedImage frame = animationEngine.getCurrentFrame();

            // Calculate position
            int x = animationEngine.getRenderX(surface.getWidth());
            int y = animationEngine.getRenderY();

            // Render frame
            FrameRenderer.renderImage(buffer, surface.getWidth(), surface.getHeight(), frame, x, y);

            // Commit to display
            surface.commit();

        } catch (Exception e) {
            logger.error("Failed to render frame: {}", e.getMessage(), e);
        }
    }

    /**
     * Stop the application.
     */
    public void stop() {
        logger.info("Stopping Bongo Cat");
        running = false;
    }

    /**
     * Clean up resources.
     */
    public void cleanup() {
        logger.info("Cleaning up resources");

        if (inputMonitor != null) {
            inputMonitor.close();
        }

        if (surface != null) {
            surface.close();
        }

        if (displayManager != null) {
            displayManager.close();
        }

        processManager.close();

        logger.info("Cleanup complete");
    }

    /**
     * Main entry point.
     */
    public static void main(String[] args) {
        // Parse command line arguments
        BongoCatCommand command = BongoCatCommand.parse(args);
        if (command == null) {
            // Help or version was shown
            return;
        }

        // Set up shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutdown hook triggered");
        }));

        Application app = null;
        try {
            // Load configuration
            ConfigurationLoader configLoader = new ConfigurationLoader();
            Configuration config;

            if (command.getConfigFile() != null) {
                logger.info("Loading configuration from: {}", command.getConfigFile());
                config = configLoader.loadFromPath(command.getConfigFile());
            } else {
                logger.info("Loading default configuration");
                config = configLoader.loadDefault();
            }

            // Override debug if specified on command line
            if (command.isDebug()) {
                config.setDebug(true);
            }

            logger.info("Configuration loaded: {}", config);

            // Handle toggle mode
            if (command.isToggle()) {
                ProcessManager pm = new ProcessManager();
                if (pm.isRunning()) {
                    logger.info("Toggle mode: stopping existing instance");
                    pm.terminateRunningInstance();
                    System.exit(0);
                } else {
                    logger.info("Toggle mode: no instance running, starting new instance");
                    // Continue with normal startup
                }
            }

            // Create and initialize application
            app = new Application(config);
            app.initialize();

            // Start event loop
            app.start();

        } catch (ConfigurationException e) {
            logger.error("Configuration error: {}", e.getMessage());
            System.err.println("Configuration error: " + e.getMessage());
            System.exit(1);
        } catch (PlatformException e) {
            logger.error("Platform error: {}", e.getMessage(), e);
            System.err.println("Platform error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } catch (IllegalStateException e) {
            logger.error("Error: {}", e.getMessage());
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            logger.error("Fatal error: {}", e.getMessage(), e);
            System.err.println("Fatal error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } finally {
            if (app != null) {
                app.cleanup();
            }
        }
    }
}
