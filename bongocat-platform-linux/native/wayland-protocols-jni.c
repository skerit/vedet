/*
 * JNI helper library to expose Wayland protocol interface structures.
 *
 * This library provides access to protocol extension interface structures
 * that are not exported by libwayland-client itself.
 */

#include <wayland-client.h>
#include "wlr-layer-shell-unstable-v1-client-protocol.h"

// Forward declarations for protocol interfaces
// These will be linked from the generated protocol code
extern const struct wl_interface zwlr_layer_shell_v1_interface;
extern const struct wl_interface zwlr_layer_surface_v1_interface;
extern const struct wl_interface zxdg_output_manager_v1_interface;
extern const struct wl_interface zxdg_output_v1_interface;

// Export interface pointers that JNA can access
__attribute__((visibility("default")))
const struct wl_interface *get_zwlr_layer_shell_v1_interface(void) {
    return &zwlr_layer_shell_v1_interface;
}

__attribute__((visibility("default")))
const struct wl_interface *get_zwlr_layer_surface_v1_interface(void) {
    return &zwlr_layer_surface_v1_interface;
}

__attribute__((visibility("default")))
const struct wl_interface *get_zxdg_output_manager_v1_interface(void) {
    return &zxdg_output_manager_v1_interface;
}

__attribute__((visibility("default")))
const struct wl_interface *get_zxdg_output_v1_interface(void) {
    return &zxdg_output_v1_interface;
}

// Wrapper function for creating layer surface
// This avoids JNA varargs marshalling issues
__attribute__((visibility("default")))
struct zwlr_layer_surface_v1 *create_layer_surface(
    struct zwlr_layer_shell_v1 *layer_shell,
    struct wl_surface *surface,
    struct wl_output *output,
    uint32_t layer,
    const char *namespace
) {
    return zwlr_layer_shell_v1_get_layer_surface(
        layer_shell,
        surface,
        output,
        layer,
        namespace
    );
}

// Wrapper function for creating SHM pool
// This properly handles file descriptor marshalling
__attribute__((visibility("default")))
struct wl_shm_pool *create_shm_pool(
    struct wl_shm *shm,
    int fd,
    int32_t size
) {
    return wl_shm_create_pool(shm, fd, size);
}

// Wrapper function for creating buffer from SHM pool
// This properly handles parameter marshalling
__attribute__((visibility("default")))
struct wl_buffer *create_buffer_from_pool(
    struct wl_shm_pool *pool,
    int32_t offset,
    int32_t width,
    int32_t height,
    int32_t stride,
    uint32_t format
) {
    return wl_shm_pool_create_buffer(pool, offset, width, height, stride, format);
}
