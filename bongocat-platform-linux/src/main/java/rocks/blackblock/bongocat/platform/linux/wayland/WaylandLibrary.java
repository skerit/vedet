package rocks.blackblock.bongocat.platform.linux.wayland;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;

/**
 * JNA bindings for libwayland-client.so
 *
 * This provides access to the core Wayland client library functions.
 * Wayland uses a protocol-based event system with callbacks.
 */
public interface WaylandLibrary extends Library {
    WaylandLibrary INSTANCE = Native.load("wayland-client", WaylandLibrary.class);

    // =========================================================================
    // Wayland Interface Symbols (global variables from libwayland)
    // =========================================================================

    /**
     * Helper to get Wayland interface pointer from the library.
     * These are global symbols exported by libwayland-client (core protocols only).
     * For protocol extensions, creates a minimal interface structure.
     */
    static Pointer getInterface(String name) {
        try {
            // Try to get from libwayland-client first (core protocols)
            Pointer ptr = NativeLibrary.getInstance("wayland-client").getGlobalVariableAddress(name);
            System.out.println("DEBUG: Found interface symbol: " + name);
            return ptr;
        } catch (UnsatisfiedLinkError e) {
            // Protocol extensions don't have symbols in libwayland-client
            // Try to get from our helper library with proper protocol structures
            System.out.println("DEBUG: Getting protocol extension interface: " + name);
            try {
                Pointer ptr = null;
                switch (name) {
                    case "zwlr_layer_shell_v1_interface":
                        ptr = WaylandProtocolsLibrary.INSTANCE.get_zwlr_layer_shell_v1_interface();
                        break;
                    case "zwlr_layer_surface_v1_interface":
                        ptr = WaylandProtocolsLibrary.INSTANCE.get_zwlr_layer_surface_v1_interface();
                        break;
                    case "zxdg_output_manager_v1_interface":
                        ptr = WaylandProtocolsLibrary.INSTANCE.get_zxdg_output_manager_v1_interface();
                        break;
                    case "zxdg_output_v1_interface":
                        ptr = WaylandProtocolsLibrary.INSTANCE.get_zxdg_output_v1_interface();
                        break;
                    default:
                        // Fall back to creating minimal interface structure
                        String interfaceName = name.replace("_interface", "");
                        System.out.println("DEBUG: Creating minimal interface for: " + interfaceName);
                        ptr = WaylandInterface.getInterfaceByName(interfaceName);
                }
                System.out.println("DEBUG: Got interface pointer: " + ptr);
                return ptr;
            } catch (UnsatisfiedLinkError e2) {
                System.err.println("ERROR: Failed to load wayland-protocols-jni library");
                throw new RuntimeException("Failed to load wayland-protocols-jni: " + e2.getMessage(), e2);
            }
        } catch (Exception e) {
            System.err.println("ERROR: Unexpected exception in getInterface: " + e);
            e.printStackTrace();
            throw new RuntimeException("Failed to get interface: " + name, e);
        }
    }

    // =========================================================================
    // Display Management
    // =========================================================================

    /**
     * Connect to the Wayland display (compositor).
     *
     * @param name display name, or NULL for default ($WAYLAND_DISPLAY)
     * @return display pointer, or NULL on failure
     */
    Pointer wl_display_connect(String name);

    /**
     * Disconnect from the Wayland display.
     *
     * @param display the display pointer
     */
    void wl_display_disconnect(Pointer display);

    /**
     * Dispatch pending events and read from the display.
     * Blocks if no events are available.
     *
     * @param display the display pointer
     * @return number of dispatched events, or -1 on error
     */
    int wl_display_dispatch(Pointer display);

    /**
     * Dispatch pending events without reading from the display.
     * Does not block.
     *
     * @param display the display pointer
     * @return number of dispatched events, or -1 on error
     */
    int wl_display_dispatch_pending(Pointer display);

    /**
     * Prepare to read events from the display.
     * Must be called before polling the display file descriptor.
     *
     * @param display the display pointer
     * @return 0 on success, -1 if events are already queued
     */
    int wl_display_prepare_read(Pointer display);

    /**
     * Read events from the display after polling.
     *
     * @param display the display pointer
     * @return 0 on success, -1 on error
     */
    int wl_display_read_events(Pointer display);

    /**
     * Cancel a read operation.
     *
     * @param display the display pointer
     */
    void wl_display_cancel_read(Pointer display);

    /**
     * Get the file descriptor for the display connection.
     * Can be used with poll()/select().
     *
     * @param display the display pointer
     * @return file descriptor
     */
    int wl_display_get_fd(Pointer display);

    /**
     * Flush pending requests to the display.
     *
     * @param display the display pointer
     * @return number of bytes sent, or -1 on error
     */
    int wl_display_flush(Pointer display);

    /**
     * Round-trip the display (wait for server response).
     *
     * @param display the display pointer
     * @return 0 on success, -1 on error
     */
    int wl_display_roundtrip(Pointer display);

    /**
     * Get the last error that occurred.
     *
     * @param display the display pointer
     * @return error code
     */
    int wl_display_get_error(Pointer display);

    // =========================================================================
    // Registry & Global Objects
    // =========================================================================

    /**
     * Get the registry object to discover global interfaces.
     * Implemented as a default method using wl_proxy_marshal_flags
     * since wl_display_get_registry is an inline function, not an exported symbol.
     *
     * @param display the display pointer
     * @return registry pointer
     */
    default Pointer wl_display_get_registry(Pointer display) {
        // WL_DISPLAY_GET_REGISTRY opcode is 1
        Pointer registryInterface = getInterface("wl_registry_interface");
        int version = wl_proxy_get_version(display);
        return wl_proxy_marshal_flags(
            display,
            1,  // WL_DISPLAY_GET_REGISTRY opcode
            registryInterface,  // wl_registry_interface from libwayland
            version,
            0  // flags
        );
    }

    /**
     * Add a listener to the registry.
     * Implemented as default method using wl_proxy_add_listener.
     *
     * @param registry the registry pointer
     * @param listener the listener callbacks
     * @param data user data passed to callbacks
     * @return 0 on success, -1 on error
     */
    default int wl_registry_add_listener(Pointer registry, Pointer listener, Pointer data) {
        return wl_proxy_add_listener(registry, listener, data);
    }

    /**
     * Bind a global interface.
     * Implemented as default method using wl_proxy_marshal_flags.
     *
     * @param registry the registry pointer
     * @param name the global name (from registry event)
     * @param interface_name the interface name string
     * @param version the interface version
     * @return bound interface pointer
     */
    default Pointer wl_registry_bind(Pointer registry, int name, String interface_name, int version) {
        // WL_REGISTRY_BIND opcode is 0
        // Get the interface pointer from the library (for core protocols)
        Pointer interfacePtr = getInterface(interface_name + "_interface");

        // Use wl_proxy_marshal_constructor_versioned which works for both core and extension protocols
        return wl_proxy_marshal_constructor_versioned(
            registry,
            0,  // WL_REGISTRY_BIND opcode
            interfacePtr,  // Can be NULL for protocol extensions
            version,
            name,
            interface_name,
            version
        );
    }

    // =========================================================================
    // Proxy Management (base type for all Wayland objects)
    // =========================================================================

    /**
     * Add a listener to a proxy (Wayland object).
     *
     * @param proxy the proxy pointer
     * @param listener the listener callbacks
     * @param data user data passed to callbacks
     * @return 0 on success, -1 on error
     */
    int wl_proxy_add_listener(Pointer proxy, Pointer listener, Pointer data);

    /**
     * Destroy a proxy.
     *
     * @param proxy the proxy pointer
     */
    void wl_proxy_destroy(Pointer proxy);

    /**
     * Get user data from a proxy.
     *
     * @param proxy the proxy pointer
     * @return user data pointer
     */
    Pointer wl_proxy_get_user_data(Pointer proxy);

    /**
     * Set user data on a proxy.
     *
     * @param proxy the proxy pointer
     * @param user_data the user data pointer
     */
    void wl_proxy_set_user_data(Pointer proxy, Pointer user_data);

    /**
     * Get the version of a proxy's interface.
     *
     * @param proxy the proxy pointer
     * @return interface version
     */
    int wl_proxy_get_version(Pointer proxy);

    /**
     * Marshal a request with flags (modern Wayland API).
     *
     * @param proxy the proxy pointer
     * @param opcode the operation code
     * @param interface_ptr the interface pointer for new objects (or NULL)
     * @param version interface version
     * @param flags marshal flags (0 for normal operation)
     * @param args variable arguments
     * @return new proxy pointer for constructor methods, NULL otherwise
     */
    Pointer wl_proxy_marshal_flags(Pointer proxy, int opcode, Pointer interface_ptr, int version, int flags, Object... args);

    /**
     * Marshal a request (call a Wayland protocol method).
     * This is a variadic function - use wl_proxy_marshal_flags instead.
     *
     * @param proxy the proxy pointer
     * @param opcode the operation code
     * @param args variable arguments
     */
    void wl_proxy_marshal(Pointer proxy, int opcode, Object... args);

    /**
     * Marshal a request and get a new proxy for the result.
     *
     * @param proxy the proxy pointer
     * @param opcode the operation code
     * @param interface_ptr the interface pointer for the new proxy
     * @param args variable arguments
     * @return new proxy pointer, or NULL
     */
    Pointer wl_proxy_marshal_constructor(Pointer proxy, int opcode, Pointer interface_ptr, Object... args);

    /**
     * Marshal a request with explicit version.
     *
     * @param proxy the proxy pointer
     * @param opcode the operation code
     * @param interface_ptr the interface pointer for the new proxy
     * @param version the interface version
     * @param args variable arguments
     * @return new proxy pointer, or NULL
     */
    Pointer wl_proxy_marshal_constructor_versioned(
        Pointer proxy, int opcode, Pointer interface_ptr, int version, Object... args
    );

    // =========================================================================
    // Compositor Interface
    // =========================================================================

    /**
     * Create a surface.
     * Implemented as default method using wl_proxy_marshal_constructor.
     *
     * @param compositor the compositor pointer
     * @return surface pointer
     */
    default Pointer wl_compositor_create_surface(Pointer compositor) {
        // WL_COMPOSITOR_CREATE_SURFACE opcode is 0
        Pointer surfaceInterface = getInterface("wl_surface_interface");
        return wl_proxy_marshal_constructor(
            compositor,
            0,  // WL_COMPOSITOR_CREATE_SURFACE
            surfaceInterface
        );
    }

    /**
     * Create a region.
     *
     * @param compositor the compositor pointer
     * @return region pointer
     */
    Pointer wl_compositor_create_region(Pointer compositor);

    // =========================================================================
    // Surface Interface
    // =========================================================================

    /**
     * Destroy a surface.
     * Implemented as default method using wl_proxy_marshal and wl_proxy_destroy.
     *
     * @param surface the surface pointer
     */
    default void wl_surface_destroy(Pointer surface) {
        // WL_SURFACE_DESTROY opcode is 0
        wl_proxy_marshal(surface, 0);
        wl_proxy_destroy(surface);
    }

    /**
     * Attach a buffer to a surface.
     * Implemented as default method using wl_proxy_marshal.
     *
     * @param surface the surface pointer
     * @param buffer the buffer pointer (can be NULL to detach)
     * @param x x offset
     * @param y y offset
     */
    default void wl_surface_attach(Pointer surface, Pointer buffer, int x, int y) {
        // WL_SURFACE_ATTACH opcode is 1
        wl_proxy_marshal(surface, 1, buffer, x, y);
    }

    /**
     * Mark a region as damaged (needs redraw).
     * Implemented as default method using wl_proxy_marshal.
     *
     * @param surface the surface pointer
     * @param x x coordinate
     * @param y y coordinate
     * @param width damage width
     * @param height damage height
     */
    default void wl_surface_damage(Pointer surface, int x, int y, int width, int height) {
        // WL_SURFACE_DAMAGE opcode is 2
        wl_proxy_marshal(surface, 2, x, y, width, height);
    }

    /**
     * Commit surface changes.
     * Implemented as default method using wl_proxy_marshal.
     *
     * @param surface the surface pointer
     */
    default void wl_surface_commit(Pointer surface) {
        // WL_SURFACE_COMMIT opcode is 6
        wl_proxy_marshal(surface, 6);
    }

    /**
     * Set the surface opaque region.
     *
     * @param surface the surface pointer
     * @param region the region pointer (can be NULL)
     */
    void wl_surface_set_opaque_region(Pointer surface, Pointer region);

    /**
     * Set the surface input region.
     *
     * @param surface the surface pointer
     * @param region the region pointer (can be NULL for full surface)
     */
    void wl_surface_set_input_region(Pointer surface, Pointer region);

    // =========================================================================
    // Shared Memory (SHM) Interface
    // =========================================================================

    /**
     * Create a shared memory pool.
     * Uses C wrapper to properly handle file descriptor marshalling.
     *
     * @param shm the shm pointer
     * @param fd file descriptor for shared memory
     * @param size size in bytes
     * @return pool pointer
     */
    default Pointer wl_shm_create_pool(Pointer shm, int fd, int size) {
        // Use the C wrapper from wayland-protocols-jni to properly handle FD marshalling
        return WaylandProtocolsLibrary.INSTANCE.create_shm_pool(shm, fd, size);
    }

    /**
     * Destroy a shared memory pool.
     * Implemented as default method using wl_proxy_marshal and wl_proxy_destroy.
     *
     * @param pool the pool pointer
     */
    default void wl_shm_pool_destroy(Pointer pool) {
        // WL_SHM_POOL_DESTROY opcode is 1
        wl_proxy_marshal(pool, 1);
        wl_proxy_destroy(pool);
    }

    /**
     * Create a buffer from a pool.
     * Uses C wrapper to properly handle parameter marshalling.
     *
     * @param pool the pool pointer
     * @param offset offset in the pool
     * @param width buffer width
     * @param height buffer height
     * @param stride bytes per row
     * @param format pixel format (WL_SHM_FORMAT_*)
     * @return buffer pointer
     */
    default Pointer wl_shm_pool_create_buffer(Pointer pool, int offset, int width, int height, int stride, int format) {
        // Use the C wrapper from wayland-protocols-jni to properly handle parameter marshalling
        // JNA's varargs marshalling doesn't work correctly for wl_proxy_marshal_constructor
        return WaylandProtocolsLibrary.INSTANCE.create_buffer_from_pool(pool, offset, width, height, stride, format);
    }

    /**
     * Resize a shared memory pool.
     *
     * @param pool the pool pointer
     * @param size new size in bytes
     */
    void wl_shm_pool_resize(Pointer pool, int size);

    /**
     * Destroy a buffer.
     * Implemented as default method using wl_proxy_marshal and wl_proxy_destroy.
     *
     * @param buffer the buffer pointer
     */
    default void wl_buffer_destroy(Pointer buffer) {
        // WL_BUFFER_DESTROY opcode is 0
        wl_proxy_marshal(buffer, 0);
        wl_proxy_destroy(buffer);
    }

    // =========================================================================
    // Callback Interface
    // =========================================================================

    /**
     * Add a listener to a callback.
     *
     * @param callback the callback pointer
     * @param listener the listener callbacks
     * @param data user data
     * @return 0 on success, -1 on error
     */
    int wl_callback_add_listener(Pointer callback, Pointer listener, Pointer data);

    /**
     * Get a frame callback (for vsync).
     *
     * @param surface the surface pointer
     * @return callback pointer
     */
    Pointer wl_surface_frame(Pointer surface);

    // =========================================================================
    // Constants
    // =========================================================================

    // Shared memory formats (subset)
    int WL_SHM_FORMAT_ARGB8888 = 0;
    int WL_SHM_FORMAT_XRGB8888 = 1;
}
