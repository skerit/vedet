package rocks.blackblock.bongocat.platform.linux;

import com.sun.jna.Pointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rocks.blackblock.bongocat.platform.Monitor;
import rocks.blackblock.bongocat.platform.OverlaySurface;
import rocks.blackblock.bongocat.platform.PlatformException;
import rocks.blackblock.bongocat.platform.Position;
import rocks.blackblock.bongocat.platform.linux.wayland.LayerShellProtocol;
import rocks.blackblock.bongocat.platform.linux.wayland.WaylandLibrary;
import rocks.blackblock.bongocat.platform.linux.wayland.WaylandTypes;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Linux/Wayland implementation of OverlaySurface using layer-shell.
 *
 * Creates a transparent overlay bar at the top or bottom of the screen
 * and provides direct pixel buffer access for rendering.
 */
public class LinuxOverlaySurface implements OverlaySurface {
    private static final Logger logger = LoggerFactory.getLogger(LinuxOverlaySurface.class);

    private final LinuxDisplayManager displayManager;
    private final WaylandLibrary wl;
    private final LinuxMonitor monitor;
    private final Position position;
    private final int height;

    // Wayland objects
    private Pointer surface;
    private Pointer layerSurface;
    private Pointer shmPool;
    private Pointer buffer;

    // Shared memory
    private int shmFd = -1;
    private ByteBuffer pixelBuffer;

    // State
    private int width;
    private int configuredWidth;
    private int configuredHeight;
    private Layer currentLayer = Layer.OVERLAY;
    private boolean visible = false;
    private boolean configured = false;
    private boolean closed = false;

    // Listener (must be kept as instance variable to prevent GC)
    private LayerShellProtocol.LayerSurfaceListener layerSurfaceListener;

    public LinuxOverlaySurface(
        LinuxDisplayManager displayManager,
        LinuxMonitor monitor,
        Position position,
        int height
    ) {
        this.displayManager = displayManager;
        this.wl = displayManager.getWaylandLibrary();
        this.monitor = monitor;
        this.position = position;
        this.height = height;
        this.width = monitor.getWidth();
    }

    /**
     * Initialize the surface and create Wayland objects.
     */
    void initialize() throws PlatformException {
        logger.info("Initializing overlay surface: {}x{} at {}", width, height, position);

        try {
            // Create the base wl_surface
            surface = wl.wl_compositor_create_surface(displayManager.getCompositor());
            if (surface == null) {
                throw new PlatformException("Failed to create wl_surface");
            }
            logger.debug("Created wl_surface");

            // Create the layer surface
            int layer = currentLayer == Layer.TOP
                    ? LayerShellProtocol.ZWLR_LAYER_SHELL_V1_LAYER_TOP
                    : LayerShellProtocol.ZWLR_LAYER_SHELL_V1_LAYER_OVERLAY;

            layerSurface = LayerShellProtocol.createLayerSurface(
                wl,
                displayManager.getLayerShell(),
                surface,
                monitor.getOutputPointer(),
                layer,
                "bongocat"
            );

            if (layerSurface == null) {
                throw new PlatformException("Failed to create layer surface");
            }
            logger.debug("Created layer surface: {}", layerSurface);

            // Configure the layer surface
            configureSurface();

            // Set up layer surface listener
            setupLayerSurfaceListener();

            // Commit the surface to apply configuration
            wl.wl_surface_commit(surface);

            // Check for errors after commit
            int error = wl.wl_display_get_error(displayManager.getDisplay());
            if (error != 0) {
                throw new PlatformException("Error after surface commit: " + error);
            }

            logger.debug("Waiting for configure event from compositor...");
            // Wait for configure event
            int result = wl.wl_display_roundtrip(displayManager.getDisplay());
            if (result < 0) {
                error = wl.wl_display_get_error(displayManager.getDisplay());
                throw new PlatformException("Roundtrip failed with error: " + error);
            }
            logger.debug("Roundtrip completed. Configured: {}", configured);

            if (!configured) {
                throw new PlatformException("Layer surface was not configured by compositor");
            }

            logger.info("Overlay surface initialized successfully");

        } catch (Exception e) {
            cleanup();
            throw new PlatformException("Failed to initialize overlay surface", e);
        }
    }

    /**
     * Configure the layer surface properties.
     */
    private void configureSurface() {
        // Set size
        LayerShellProtocol.setSize(wl, layerSurface, 0, height); // 0 width = full width

        // Set anchor
        int anchor = LayerShellProtocol.getBarAnchor(position == Position.TOP);
        LayerShellProtocol.setAnchor(wl, layerSurface, anchor);

        // Set exclusive zone to -1 (don't reserve space)
        LayerShellProtocol.setExclusiveZone(wl, layerSurface, -1);

        // Set keyboard interactivity to none
        LayerShellProtocol.setKeyboardInteractivity(wl, layerSurface,
                LayerShellProtocol.ZWLR_LAYER_SURFACE_V1_KEYBOARD_INTERACTIVITY_NONE);

        logger.debug("Configured layer surface: size={}x{}, anchor={}, position={}",
                    width, height, anchor, position);
    }

    /**
     * Set up the layer surface listener for configure events.
     */
    private void setupLayerSurfaceListener() {
        layerSurfaceListener = new LayerShellProtocol.LayerSurfaceListener();

        layerSurfaceListener.configure = (data, layerSurfacePtr, serial, width, height) -> {
            logger.info("CONFIGURE CALLBACK INVOKED: serial={}, size={}x{}", serial, width, height);

            configuredWidth = width;
            configuredHeight = height;

            // Update our dimensions
            if (width > 0) {
                this.width = width;
            }

            // Acknowledge the configure
            LayerShellProtocol.ackConfigure(wl, layerSurface, serial);

            // wlr-layer-shell protocol REQUIRES a commit after ack_configure
            // We commit without a buffer to acknowledge the configuration
            wl.wl_surface_commit(surface);

            //  Create the buffer for rendering
            try {
                if (!configured || pixelBuffer == null) {
                    createBuffer();
                    configured = true;
                    logger.debug("Buffer created in configure callback, ready for rendering");
                }
            } catch (Exception e) {
                logger.error("Failed to create buffer after configure: {}", e.getMessage(), e);
            }
        };

        layerSurfaceListener.closed = (data, layerSurfacePtr) -> {
            logger.info("Layer surface closed by compositor");
            closed = true;
        };

        layerSurfaceListener.write();
        wl.wl_proxy_add_listener(layerSurface, layerSurfaceListener.getPointer(), null);
    }

    /**
     * Create shared memory buffer for pixel data.
     */
    private void createBuffer() throws IOException, PlatformException {
        // Clean up old buffer if it exists
        if (buffer != null) {
            wl.wl_buffer_destroy(buffer);
            buffer = null;
        }

        if (pixelBuffer != null && shmFd >= 0) {
            NativeMemory.unmapMemory(pixelBuffer, pixelBuffer.capacity());
            pixelBuffer = null;
        }

        if (shmPool != null) {
            wl.wl_shm_pool_destroy(shmPool);
            shmPool = null;
        }

        if (shmFd >= 0) {
            NativeMemory.close(shmFd);
            shmFd = -1;
        }

        // Calculate buffer size (ARGB8888 = 4 bytes per pixel)
        int stride = width * 4;
        int bufferSize = stride * height;

        logger.debug("Creating buffer: {}x{}, stride={}, size={}", width, height, stride, bufferSize);

        // Create shared memory
        shmFd = NativeMemory.createSharedMemoryFile(bufferSize);
        logger.debug("Created shared memory file descriptor: {}", shmFd);

        if (shmFd < 0) {
            throw new PlatformException("Invalid shared memory file descriptor: " + shmFd);
        }

        // Map the memory
        pixelBuffer = NativeMemory.mapMemory(shmFd, bufferSize);
        logger.debug("Mapped shared memory to ByteBuffer: {} bytes", pixelBuffer.capacity());

        // Clear buffer to transparent black
        for (int i = 0; i < bufferSize; i++) {
            pixelBuffer.put(i, (byte) 0);
        }

        // Create wl_shm_pool
        logger.debug("Creating wl_shm_pool: shm={}, fd={}, size={}", displayManager.getShm(), shmFd, bufferSize);
        shmPool = wl.wl_shm_create_pool(displayManager.getShm(), shmFd, bufferSize);
        if (shmPool == null) {
            throw new PlatformException("Failed to create wl_shm_pool");
        }
        logger.debug("Created wl_shm_pool: {}", shmPool);

        // Create wl_buffer from the pool
        buffer = wl.wl_shm_pool_create_buffer(
            shmPool,
            0, // offset
            width,
            height,
            stride,
            WaylandTypes.WL_SHM_FORMAT_ARGB8888
        );

        if (buffer == null) {
            throw new PlatformException("Failed to create wl_buffer");
        }

        logger.debug("Buffer created successfully");
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
    public Monitor getMonitor() {
        return monitor;
    }

    @Override
    public Layer getLayer() {
        return currentLayer;
    }

    @Override
    public void setLayer(Layer layer) throws PlatformException {
        if (layer == currentLayer) {
            return;
        }

        logger.info("Changing layer from {} to {}", currentLayer, layer);
        currentLayer = layer;

        if (layerSurface != null) {
            int waylandLayer = layer == Layer.TOP
                    ? LayerShellProtocol.ZWLR_LAYER_SHELL_V1_LAYER_TOP
                    : LayerShellProtocol.ZWLR_LAYER_SHELL_V1_LAYER_OVERLAY;

            LayerShellProtocol.setLayer(wl, layerSurface, waylandLayer);
            wl.wl_surface_commit(surface);
        }
    }

    @Override
    public ByteBuffer getPixelBuffer() {
        return pixelBuffer;
    }

    @Override
    public void commit() throws PlatformException {
        if (!configured || buffer == null) {
            throw new PlatformException("Surface not configured");
        }

        // Attach the buffer
        wl.wl_surface_attach(surface, buffer, 0, 0);

        // Mark the entire surface as damaged
        wl.wl_surface_damage(surface, 0, 0, width, height);

        // Commit the surface
        wl.wl_surface_commit(surface);

        // Flush to ensure the changes are sent
        // Note: flush can return -1 with EAGAIN (buffer full) which is not an error
        wl.wl_display_flush(displayManager.getDisplay());
    }

    @Override
    public void show() {
        if (visible) {
            return;
        }

        logger.debug("Showing overlay surface");
        visible = true;

        // Surface is visible by default once configured
        // If we previously hid it, we'd need to attach a buffer again
        if (configured && buffer != null) {
            try {
                commit();
            } catch (PlatformException e) {
                logger.error("Failed to show surface: {}", e.getMessage());
            }
        }
    }

    @Override
    public void hide() {
        if (!visible) {
            return;
        }

        logger.debug("Hiding overlay surface");
        visible = false;

        if (surface != null) {
            // Attach null buffer to hide the surface
            wl.wl_surface_attach(surface, null, 0, 0);
            wl.wl_surface_commit(surface);
            wl.wl_display_flush(displayManager.getDisplay());
        }
    }

    @Override
    public boolean isVisible() {
        return visible && configured && !closed;
    }

    @Override
    public boolean isFullscreenWindowPresent() {
        // TODO: Implement fullscreen detection using foreign-toplevel protocol
        // For now, always return false
        return false;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }

        logger.info("Closing overlay surface");
        closed = true;
        cleanup();
    }

    /**
     * Clean up all resources.
     */
    private void cleanup() {
        // Destroy buffer
        if (buffer != null) {
            try {
                wl.wl_buffer_destroy(buffer);
            } catch (Exception e) {
                logger.debug("Error destroying buffer: {}", e.getMessage());
            }
            buffer = null;
        }

        // Unmap and close shared memory
        if (pixelBuffer != null && shmFd >= 0) {
            try {
                NativeMemory.unmapMemory(pixelBuffer, pixelBuffer.capacity());
            } catch (Exception e) {
                logger.debug("Error unmapping memory: {}", e.getMessage());
            }
            pixelBuffer = null;
        }

        if (shmFd >= 0) {
            NativeMemory.close(shmFd);
            shmFd = -1;
        }

        // Destroy shm pool
        if (shmPool != null) {
            try {
                wl.wl_shm_pool_destroy(shmPool);
            } catch (Exception e) {
                logger.debug("Error destroying shm pool: {}", e.getMessage());
            }
            shmPool = null;
        }

        // Destroy layer surface
        if (layerSurface != null) {
            try {
                LayerShellProtocol.destroy(wl, layerSurface);
            } catch (Exception e) {
                logger.debug("Error destroying layer surface: {}", e.getMessage());
            }
            layerSurface = null;
        }

        // Destroy surface
        if (surface != null) {
            try {
                wl.wl_surface_destroy(surface);
            } catch (Exception e) {
                logger.debug("Error destroying surface: {}", e.getMessage());
            }
            surface = null;
        }

        logger.debug("Overlay surface cleanup complete");
    }
}
