package rocks.blackblock.bongocat.platform.macos;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rocks.blackblock.bongocat.platform.*;
import rocks.blackblock.bongocat.platform.macos.cocoa.AppKit;
import rocks.blackblock.bongocat.platform.macos.cocoa.CoreGraphics;
import rocks.blackblock.bongocat.platform.macos.cocoa.ObjC;

import java.util.ArrayList;
import java.util.List;

/**
 * macOS implementation of DisplayManager.
 * Uses Core Graphics to enumerate displays and NSApplication for event processing.
 */
public class MacOSDisplayManager implements DisplayManager {
    private static final Logger logger = LoggerFactory.getLogger(MacOSDisplayManager.class);

    private final List<MacOSMonitor> monitors = new ArrayList<>();
    private Pointer nsApp;
    private Pointer pool;
    private boolean initialized = false;

    @Override
    public void initialize() throws PlatformException {
        if (initialized) {
            return;
        }

        logger.debug("Initializing macOS display manager");

        try {
            // Create autorelease pool
            pool = ObjC.autoreleasePool();

            // Initialize NSApplication
            nsApp = ObjC.sharedApplication();
            ObjC.sendVoid(nsApp, "setActivationPolicy:", AppKit.NSApplicationActivationPolicyProhibited);

            // Finish launching NSApplication - this allows window creation
            ObjC.sendVoid(nsApp, "finishLaunching");

            // Enumerate displays
            enumerateDisplays();

            initialized = true;
            logger.info("macOS display manager initialized with {} monitor(s)", monitors.size());

        } catch (Exception e) {
            throw new PlatformException("Failed to initialize display manager", e);
        }
    }

    private void enumerateDisplays() {
        // Get active displays
        IntByReference displayCountRef = new IntByReference();
        IntByReference displays = new IntByReference();

        // Query how many displays we have
        CoreGraphics.INSTANCE.CGGetActiveDisplayList(32, displays, displayCountRef);
        int displayCount = displayCountRef.getValue();

        if (displayCount == 0) {
            logger.warn("No active displays found");
            return;
        }

        // Get main display ID
        int mainDisplayId = CoreGraphics.INSTANCE.CGMainDisplayID();

        // For now, we'll just use the main display
        // In a full implementation, we would iterate through all displays
        MacOSMonitor primaryMonitor = new MacOSMonitor(mainDisplayId, true);
        monitors.add(primaryMonitor);

        logger.debug("Found primary display: {}", primaryMonitor);
    }

    @Override
    public List<Monitor> getMonitors() {
        return new ArrayList<>(monitors);
    }

    @Override
    public Monitor findMonitor(String name) {
        return monitors.stream()
                .filter(m -> m.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    @Override
    public Monitor getPrimaryMonitor() {
        return monitors.stream()
                .filter(Monitor::isPrimary)
                .findFirst()
                .orElse(monitors.isEmpty() ? null : monitors.get(0));
    }

    @Override
    public OverlaySurface createOverlay(Monitor monitor, Position position, int height)
            throws PlatformException {
        if (!(monitor instanceof MacOSMonitor)) {
            throw new PlatformException("Monitor must be a MacOSMonitor instance");
        }

        MacOSMonitor macMonitor = (MacOSMonitor) monitor;
        return new MacOSOverlaySurface(macMonitor, position, height);
    }

    @Override
    public boolean processEvents(int timeoutMs) throws PlatformException {
        try {
            // Process pending NSApplication events
            Pointer untilDate;
            if (timeoutMs == 0) {
                // Don't wait, process what's available
                untilDate = ObjC.send(ObjC.getClass("NSDate"), "distantPast");
            } else if (timeoutMs < 0) {
                // Wait indefinitely
                untilDate = ObjC.send(ObjC.getClass("NSDate"), "distantFuture");
            } else {
                // Wait for specified time
                double interval = timeoutMs / 1000.0;
                untilDate = ObjC.send(
                    ObjC.send(ObjC.getClass("NSDate"), "alloc"),
                    "initWithTimeIntervalSinceNow:",
                    interval
                );
            }

            Pointer event = ObjC.send(
                nsApp,
                "nextEventMatchingMask:untilDate:inMode:dequeue:",
                -1L,  // NSEventMaskAny
                untilDate,
                ObjC.nsString("kCFRunLoopDefaultMode"),
                true
            );

            if (event != null && Pointer.nativeValue(event) != 0) {
                ObjC.sendVoid(nsApp, "sendEvent:", event);
                return true;
            }

            return false;

        } catch (Exception e) {
            throw new PlatformException("Failed to process events", e);
        }
    }

    @Override
    public void close() {
        logger.debug("Closing macOS display manager");

        monitors.clear();

        if (pool != null) {
            ObjC.drainPool(pool);
            pool = null;
        }

        initialized = false;
    }
}
