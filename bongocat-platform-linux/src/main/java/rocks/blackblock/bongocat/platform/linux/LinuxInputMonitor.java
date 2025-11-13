package rocks.blackblock.bongocat.platform.linux;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rocks.blackblock.bongocat.platform.InputEvent;
import rocks.blackblock.bongocat.platform.InputMonitor;
import rocks.blackblock.bongocat.platform.PlatformException;
import rocks.blackblock.bongocat.platform.linux.evdev.EvdevLibrary;
import rocks.blackblock.bongocat.platform.linux.evdev.KeyCodes;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Linux implementation of InputMonitor using evdev.
 *
 * Monitors keyboard input devices via /dev/input/eventX using the Linux evdev interface.
 * Runs in a separate thread to avoid blocking the main application.
 */
public class LinuxInputMonitor implements InputMonitor {
    private static final Logger logger = LoggerFactory.getLogger(LinuxInputMonitor.class);

    private final EvdevLibrary evdev = EvdevLibrary.INSTANCE;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private List<String> devicePaths = new ArrayList<>();
    private List<Integer> deviceFds = new ArrayList<>();
    private Consumer<InputEvent> eventHandler;
    private Thread monitorThread;

    @Override
    public void start(List<String> devicePaths, Consumer<InputEvent> eventHandler) throws PlatformException {
        if (running.get()) {
            logger.warn("InputMonitor already running");
            return;
        }

        if (devicePaths == null || devicePaths.isEmpty()) {
            throw new PlatformException("No input devices specified");
        }

        if (eventHandler == null) {
            throw new PlatformException("Event handler cannot be null");
        }

        this.devicePaths = new ArrayList<>(devicePaths);
        this.eventHandler = eventHandler;

        logger.info("Starting input monitor for {} devices", devicePaths.size());

        // Open all device file descriptors
        for (String device : devicePaths) {
            try {
                int fd = openDevice(device);
                deviceFds.add(fd);
                logger.info("  - {} (fd={})", device, fd);
            } catch (PlatformException e) {
                logger.error("Failed to open device {}: {}", device, e.getMessage());
                // Close already opened devices
                closeAllDevices();
                throw e;
            }
        }

        // Start monitoring thread
        running.set(true);
        monitorThread = new Thread(this::monitorLoop, "InputMonitor");
        monitorThread.setDaemon(true);
        monitorThread.start();

        logger.info("Input monitoring started");
    }

    /**
     * Open an input device for reading.
     */
    private int openDevice(String devicePath) throws PlatformException {
        int fd = evdev.open(devicePath, EvdevLibrary.O_RDONLY | EvdevLibrary.O_NONBLOCK);
        if (fd < 0) {
            throw new PlatformException("Failed to open device " + devicePath +
                ": errno=" + fd + ". Check permissions (user must be in 'input' group)");
        }

        // Get device name for logging
        Memory nameBuffer = new Memory(256);
        int ret = evdev.ioctl(fd, EvdevLibrary.EVIOCGNAME, nameBuffer);
        if (ret >= 0) {
            String deviceName = nameBuffer.getString(0);
            logger.debug("Device name: {}", deviceName);
        }

        return fd;
    }

    /**
     * Main monitoring loop (runs in separate thread).
     */
    private void monitorLoop() {
        logger.debug("Monitor loop started");

        // Prepare poll structures using JNA's toArray() for contiguous memory
        EvdevLibrary.PollFd pollFd = new EvdevLibrary.PollFd();
        EvdevLibrary.PollFd[] pollFds = (EvdevLibrary.PollFd[]) pollFd.toArray(deviceFds.size());
        for (int i = 0; i < deviceFds.size(); i++) {
            pollFds[i].fd = deviceFds.get(i);
            pollFds[i].events = EvdevLibrary.PollFd.POLLIN;
            pollFds[i].revents = 0;
        }

        // Event buffer (size of one input_event structure)
        int eventSize = new EvdevLibrary.InputEvent().size();
        Memory eventBuffer = new Memory(eventSize);

        while (running.get()) {
            try {
                // Poll all devices with 100ms timeout
                int ret = evdev.poll(pollFds, deviceFds.size(), 100);

                if (ret < 0) {
                    logger.error("poll() failed: {}", ret);
                    break;
                } else if (ret == 0) {
                    // Timeout - no events
                    continue;
                }

                // Check which devices have events
                for (int i = 0; i < pollFds.length; i++) {
                    if ((pollFds[i].revents & EvdevLibrary.PollFd.POLLIN) != 0) {
                        // Read event from this device
                        readAndProcessEvents(deviceFds.get(i), eventBuffer, eventSize);
                        pollFds[i].revents = 0; // Clear revents
                    }

                    if ((pollFds[i].revents & EvdevLibrary.PollFd.POLLERR) != 0) {
                        logger.warn("POLLERR on device fd={}", deviceFds.get(i));
                    }
                }

            } catch (Exception e) {
                if (running.get()) {
                    logger.warn("Error in monitor loop: {}", e.getMessage());
                    // Sleep to prevent tight error loop that causes OOM
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        logger.debug("Monitor loop stopped");
    }

    /**
     * Read and process all available events from a device.
     */
    private void readAndProcessEvents(int fd, Memory eventBuffer, int eventSize) {
        while (true) {
            int bytesRead = evdev.read(fd, eventBuffer, eventSize);

            if (bytesRead < 0) {
                // No more events (EAGAIN/EWOULDBLOCK)
                break;
            }

            if (bytesRead == eventSize) {
                EvdevLibrary.InputEvent event = new EvdevLibrary.InputEvent(eventBuffer);
                processEvent(event);
            } else {
                logger.warn("Partial read: {} bytes (expected {})", bytesRead, eventSize);
            }
        }
    }

    /**
     * Process a single input event.
     */
    private void processEvent(EvdevLibrary.InputEvent event) {
        // Only process key events
        if (event.type != EvdevLibrary.EV_KEY) {
            return;
        }

        // Convert evdev value to InputEvent type
        InputEvent.Type type;
        switch (event.value) {
            case EvdevLibrary.KEY_PRESS:
            case EvdevLibrary.KEY_REPEAT:
                // Treat repeats as presses for animation purposes
                type = InputEvent.Type.KEY_PRESS;
                break;
            case EvdevLibrary.KEY_RELEASE:
                type = InputEvent.Type.KEY_RELEASE;
                break;
            default:
                return; // Unknown event type
        }

        int keyCode = event.code;
        long timestamp = event.tv_sec * 1000 + event.tv_usec / 1000; // Convert to milliseconds

        // Create and dispatch event
        LinuxInputEvent inputEvent = new LinuxInputEvent(type, keyCode, timestamp);

        // Log key presses for debugging
        if (type == InputEvent.Type.KEY_PRESS) {
            String keyName = KeyCodes.getKeyName(keyCode);
            if (keyName != null) {
                logger.trace("Key pressed: {} (code={}, left={}, right={})",
                    keyName, keyCode, inputEvent.isLeftHandKey(), inputEvent.isRightHandKey());
            }
        }

        // Dispatch to handler
        try {
            eventHandler.accept(inputEvent);
        } catch (Exception e) {
            logger.error("Error in event handler", e);
        }
    }

    @Override
    public void stop() {
        if (!running.getAndSet(false)) {
            return;
        }

        logger.info("Stopping input monitor");

        // Wait for monitor thread to finish
        if (monitorThread != null) {
            try {
                monitorThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Close all devices
        closeAllDevices();

        logger.info("Input monitor stopped");
    }

    /**
     * Close all opened device file descriptors.
     */
    private void closeAllDevices() {
        for (int fd : deviceFds) {
            evdev.close(fd);
        }
        deviceFds.clear();
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public List<String> findKeyboardDevices() throws PlatformException {
        List<String> devices = new ArrayList<>();

        // Scan /dev/input for event devices
        Path inputDir = Paths.get("/dev/input");
        if (!Files.exists(inputDir) || !Files.isDirectory(inputDir)) {
            logger.warn("/dev/input directory not found");
            return devices;
        }

        try {
            Files.list(inputDir)
                .filter(path -> path.getFileName().toString().startsWith("event"))
                .filter(Files::exists)
                .sorted()
                .forEach(path -> devices.add(path.toString()));

            logger.debug("Found {} input devices", devices.size());

        } catch (Exception e) {
            throw new PlatformException("Failed to enumerate input devices", e);
        }

        return devices;
    }

    @Override
    public void close() {
        stop();
    }

    /**
     * Implementation of InputEvent for Linux.
     */
    private static class LinuxInputEvent implements InputEvent {
        private final Type type;
        private final int keyCode;
        private final long timestamp;

        LinuxInputEvent(Type type, int keyCode, long timestamp) {
            this.type = type;
            this.keyCode = keyCode;
            this.timestamp = timestamp;
        }

        @Override
        public Type getType() {
            return type;
        }

        @Override
        public int getKeyCode() {
            return keyCode;
        }

        @Override
        public long getTimestamp() {
            return timestamp;
        }

        @Override
        public boolean isLeftHandKey() {
            return KeyCodes.isLeftHandKey(keyCode);
        }

        @Override
        public boolean isRightHandKey() {
            return KeyCodes.isRightHandKey(keyCode);
        }
    }
}
