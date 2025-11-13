package rocks.blackblock.bongocat.platform.linux.wayland;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

/**
 * JNA bindings for our custom wayland-protocols-jni helper library.
 *
 * This library provides access to protocol extension interface structures
 * that are not exported by libwayland-client itself.
 */
public interface WaylandProtocolsLibrary extends Library {
    WaylandProtocolsLibrary INSTANCE = Native.load("wayland-protocols-jni", WaylandProtocolsLibrary.class);

    /**
     * Get the zwlr_layer_shell_v1 interface structure.
     *
     * @return pointer to the interface structure
     */
    Pointer get_zwlr_layer_shell_v1_interface();

    /**
     * Get the zwlr_layer_surface_v1 interface structure.
     *
     * @return pointer to the interface structure
     */
    Pointer get_zwlr_layer_surface_v1_interface();

    /**
     * Get the zxdg_output_manager_v1 interface structure.
     *
     * @return pointer to the interface structure
     */
    Pointer get_zxdg_output_manager_v1_interface();

    /**
     * Get the zxdg_output_v1 interface structure.
     *
     * @return pointer to the interface structure
     */
    Pointer get_zxdg_output_v1_interface();

    /**
     * Create a layer surface.
     * This wrapper avoids JNA varargs marshalling issues.
     *
     * @param layer_shell the layer shell pointer
     * @param surface the wl_surface pointer
     * @param output the wl_output pointer (can be NULL)
     * @param layer the layer value
     * @param namespace the namespace string
     * @return layer surface pointer
     */
    Pointer create_layer_surface(
        Pointer layer_shell,
        Pointer surface,
        Pointer output,
        int layer,
        String namespace
    );

    /**
     * Create a shared memory pool.
     * This wrapper properly handles file descriptor marshalling.
     *
     * @param shm the wl_shm pointer
     * @param fd the file descriptor
     * @param size the size in bytes
     * @return pool pointer
     */
    Pointer create_shm_pool(Pointer shm, int fd, int size);

    /**
     * Create a buffer from a SHM pool.
     * This wrapper properly handles parameter marshalling.
     *
     * @param pool the wl_shm_pool pointer
     * @param offset offset in the pool
     * @param width buffer width
     * @param height buffer height
     * @param stride bytes per row
     * @param format pixel format
     * @return buffer pointer
     */
    Pointer create_buffer_from_pool(Pointer pool, int offset, int width, int height, int stride, int format);
}
