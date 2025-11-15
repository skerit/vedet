package rocks.blackblock.bongocat.platform.macos;

import com.sun.jna.Pointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rocks.blackblock.bongocat.platform.InputEvent;
import rocks.blackblock.bongocat.platform.InputMonitor;
import rocks.blackblock.bongocat.platform.PlatformException;
import rocks.blackblock.bongocat.platform.macos.cocoa.CoreGraphics;

import java.util.List;
import java.util.function.Consumer;

/**
 * macOS implementation of InputMonitor.
 * Uses CGEventTap to monitor keyboard events globally.
 */
public class MacOSInputMonitor implements InputMonitor {
    private static final Logger logger = LoggerFactory.getLogger(MacOSInputMonitor.class);

    private Consumer<InputEvent> eventHandler;
    private Pointer eventTap;
    private Pointer runLoopSource;
    private Thread runLoopThread;
    private volatile boolean running = false;

    @Override
    public void start(List<String> devicePaths, Consumer<InputEvent> eventHandler)
            throws PlatformException {
        if (running) {
            logger.warn("Input monitor is already running");
            return;
        }

        this.eventHandler = eventHandler;

        logger.debug("Starting macOS input monitor");

        try {
            // Create event tap for keyboard events
            long eventMask = CoreGraphics.eventMask(
                CoreGraphics.kCGEventKeyDown,
                CoreGraphics.kCGEventKeyUp
            );

            CoreGraphics.CGEventTapCallBack callback = this::handleCGEvent;

            eventTap = CoreGraphics.INSTANCE.CGEventTapCreate(
                CoreGraphics.kCGSessionEventTap,
                CoreGraphics.kCGHeadInsertEventTap,
                CoreGraphics.kCGEventTapOptionListenOnly,
                eventMask,
                callback,
                Pointer.NULL
            );

            if (eventTap == null || Pointer.nativeValue(eventTap) == 0) {
                throw new PlatformException("Failed to create event tap. " +
                    "You may need to grant accessibility permissions to this application.");
            }

            // Create run loop source
            runLoopSource = CoreGraphics.INSTANCE.CFMachPortCreateRunLoopSource(
                Pointer.NULL,
                eventTap,
                0
            );

            if (runLoopSource == null || Pointer.nativeValue(runLoopSource) == 0) {
                throw new PlatformException("Failed to create run loop source");
            }

            // Start run loop in separate thread
            running = true;
            runLoopThread = new Thread(this::runEventLoop, "macOS-Input-Monitor");
            runLoopThread.setDaemon(true);
            runLoopThread.start();

            logger.info("macOS input monitor started successfully");

        } catch (Exception e) {
            running = false;
            cleanup();
            throw new PlatformException("Failed to start input monitor", e);
        }
    }

    /**
     * Handle CGEvent callback
     */
    private Pointer handleCGEvent(Pointer proxy, int type, Pointer event, Pointer refcon) {
        try {
            // Get keycode
            int keyCode = CoreGraphics.INSTANCE.CGEventGetIntegerValueField(
                event,
                CoreGraphics.kCGKeyboardEventKeycode
            );

            // Get timestamp (in nanoseconds)
            long timestamp = CoreGraphics.INSTANCE.CGEventGetTimestamp(event);

            // Determine event type
            InputEvent.Type eventType;
            if (type == CoreGraphics.kCGEventKeyDown) {
                eventType = InputEvent.Type.KEY_PRESS;
            } else if (type == CoreGraphics.kCGEventKeyUp) {
                eventType = InputEvent.Type.KEY_RELEASE;
            } else {
                return event; // Pass through unknown events
            }

            // Create and dispatch event
            MacOSInputEvent inputEvent = new MacOSInputEvent(eventType, keyCode, timestamp);

            if (eventHandler != null) {
                eventHandler.accept(inputEvent);
            }

        } catch (Exception e) {
            logger.error("Error handling CGEvent: {}", e.getMessage(), e);
        }

        // Return event to allow it to propagate
        return event;
    }

    /**
     * Run the Core Foundation run loop
     */
    private void runEventLoop() {
        logger.debug("Starting event loop thread");

        try {
            // Get current run loop
            Pointer runLoop = CoreGraphics.INSTANCE.CFRunLoopGetCurrent();

            // Add event tap source to run loop
            Pointer defaultMode = CoreGraphics.INSTANCE.CFRunLoopGetMain();
            CoreGraphics.INSTANCE.CFRunLoopAddSource(runLoop, runLoopSource, defaultMode);

            // Run the loop
            while (running) {
                CoreGraphics.INSTANCE.CFRunLoopRun();
            }

        } catch (Exception e) {
            logger.error("Error in event loop: {}", e.getMessage(), e);
        } finally {
            logger.debug("Event loop thread stopped");
        }
    }

    @Override
    public void stop() {
        if (!running) {
            return;
        }

        logger.debug("Stopping macOS input monitor");

        running = false;

        // Stop run loop
        if (runLoopThread != null && runLoopThread.isAlive()) {
            Pointer runLoop = CoreGraphics.INSTANCE.CFRunLoopGetCurrent();
            if (runLoop != null && Pointer.nativeValue(runLoop) != 0) {
                CoreGraphics.INSTANCE.CFRunLoopStop(runLoop);
            }

            try {
                runLoopThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        cleanup();

        logger.info("macOS input monitor stopped");
    }

    private void cleanup() {
        if (runLoopSource != null && Pointer.nativeValue(runLoopSource) != 0) {
            // CFRelease(runLoopSource) would go here if we had the binding
            runLoopSource = null;
        }

        if (eventTap != null && Pointer.nativeValue(eventTap) != 0) {
            // CFRelease(eventTap) would go here if we had the binding
            eventTap = null;
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public List<String> findKeyboardDevices() throws PlatformException {
        // On macOS, we use CGEventTap which monitors all keyboard input globally
        // So we don't need to enumerate specific devices
        // Return an empty list to indicate we'll monitor everything
        return List.of();
    }

    @Override
    public void close() {
        stop();
    }
}
