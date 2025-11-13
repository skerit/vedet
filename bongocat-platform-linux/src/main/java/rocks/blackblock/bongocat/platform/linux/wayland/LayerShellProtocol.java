package rocks.blackblock.bongocat.platform.linux.wayland;

import com.sun.jna.Callback;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

/**
 * Bindings for wlr-layer-shell-unstable-v1 protocol.
 *
 * This protocol allows creation of "layer surfaces" that exist at specific
 * layers in the compositor's rendering stack (background, bottom, top, overlay).
 * It's used for panels, docks, overlays, and wallpapers.
 *
 * Provided by wlroots-based compositors (Sway, Hyprland, etc).
 */
public class LayerShellProtocol {

    /**
     * Layer shell interface name (for wl_registry_bind)
     */
    public static final String INTERFACE_NAME = "zwlr_layer_shell_v1";
    public static final int VERSION = 1;  // Use v1 for maximum compatibility

    // =========================================================================
    // Layer Values
    // =========================================================================

    public static final int ZWLR_LAYER_SHELL_V1_LAYER_BACKGROUND = 0;
    public static final int ZWLR_LAYER_SHELL_V1_LAYER_BOTTOM = 1;
    public static final int ZWLR_LAYER_SHELL_V1_LAYER_TOP = 2;
    public static final int ZWLR_LAYER_SHELL_V1_LAYER_OVERLAY = 3;

    // =========================================================================
    // Anchor Values (bitfield for positioning)
    // =========================================================================

    public static final int ZWLR_LAYER_SURFACE_V1_ANCHOR_TOP = 1;
    public static final int ZWLR_LAYER_SURFACE_V1_ANCHOR_BOTTOM = 2;
    public static final int ZWLR_LAYER_SURFACE_V1_ANCHOR_LEFT = 4;
    public static final int ZWLR_LAYER_SURFACE_V1_ANCHOR_RIGHT = 8;

    // =========================================================================
    // Keyboard Interactivity
    // =========================================================================

    public static final int ZWLR_LAYER_SURFACE_V1_KEYBOARD_INTERACTIVITY_NONE = 0;
    public static final int ZWLR_LAYER_SURFACE_V1_KEYBOARD_INTERACTIVITY_EXCLUSIVE = 1;
    public static final int ZWLR_LAYER_SURFACE_V1_KEYBOARD_INTERACTIVITY_ON_DEMAND = 2;

    // =========================================================================
    // Request Opcodes
    // =========================================================================

    // zwlr_layer_shell_v1 requests
    private static final int ZWLR_LAYER_SHELL_V1_GET_LAYER_SURFACE = 0;
    private static final int ZWLR_LAYER_SHELL_V1_DESTROY = 1;

    // zwlr_layer_surface_v1 requests
    private static final int ZWLR_LAYER_SURFACE_V1_SET_SIZE = 0;
    private static final int ZWLR_LAYER_SURFACE_V1_SET_ANCHOR = 1;
    private static final int ZWLR_LAYER_SURFACE_V1_SET_EXCLUSIVE_ZONE = 2;
    private static final int ZWLR_LAYER_SURFACE_V1_SET_MARGIN = 3;
    private static final int ZWLR_LAYER_SURFACE_V1_SET_KEYBOARD_INTERACTIVITY = 4;
    private static final int ZWLR_LAYER_SURFACE_V1_GET_POPUP = 5;
    private static final int ZWLR_LAYER_SURFACE_V1_ACK_CONFIGURE = 6;
    private static final int ZWLR_LAYER_SURFACE_V1_DESTROY = 7;
    private static final int ZWLR_LAYER_SURFACE_V1_SET_LAYER = 8;

    // =========================================================================
    // Layer Surface Listener
    // =========================================================================

    /**
     * Listener for layer surface events.
     */
    @Structure.FieldOrder({"configure", "closed"})
    public static class LayerSurfaceListener extends Structure {
        public ConfigureCallback configure;
        public ClosedCallback closed;

        public interface ConfigureCallback extends Callback {
            void callback(Pointer data, Pointer layer_surface, int serial, int width, int height);
        }

        public interface ClosedCallback extends Callback {
            void callback(Pointer data, Pointer layer_surface);
        }
    }

    // =========================================================================
    // Helper Methods for Protocol Requests
    // =========================================================================

    /**
     * Create a layer surface.
     *
     * @param wl Wayland library instance
     * @param layer_shell the layer shell pointer
     * @param surface the wl_surface pointer
     * @param output the wl_output pointer (can be NULL for any output)
     * @param layer the layer value (BACKGROUND, BOTTOM, TOP, OVERLAY)
     * @param namespace a namespace string (for compositor identification)
     * @return layer surface pointer
     */
    public static Pointer createLayerSurface(
        WaylandLibrary wl,
        Pointer layer_shell,
        Pointer surface,
        Pointer output,
        int layer,
        String namespace
    ) {
        // Use the C wrapper function to avoid JNA varargs marshalling issues
        return WaylandProtocolsLibrary.INSTANCE.create_layer_surface(
            layer_shell,
            surface,
            output,
            layer,
            namespace
        );
    }

    /**
     * Set the size of a layer surface.
     *
     * @param wl Wayland library instance
     * @param layer_surface the layer surface pointer
     * @param width desired width (0 = full width)
     * @param height desired height (0 = full height)
     */
    public static void setSize(WaylandLibrary wl, Pointer layer_surface, int width, int height) {
        wl.wl_proxy_marshal(layer_surface, ZWLR_LAYER_SURFACE_V1_SET_SIZE, width, height);
    }

    /**
     * Set the anchor edges for positioning.
     *
     * @param wl Wayland library instance
     * @param layer_surface the layer surface pointer
     * @param anchor bitfield of anchor values (TOP, BOTTOM, LEFT, RIGHT)
     */
    public static void setAnchor(WaylandLibrary wl, Pointer layer_surface, int anchor) {
        wl.wl_proxy_marshal(layer_surface, ZWLR_LAYER_SURFACE_V1_SET_ANCHOR, anchor);
    }

    /**
     * Set the exclusive zone (area reserved by this surface).
     *
     * @param wl Wayland library instance
     * @param layer_surface the layer surface pointer
     * @param zone exclusive zone in pixels (-1 = no exclusive zone)
     */
    public static void setExclusiveZone(WaylandLibrary wl, Pointer layer_surface, int zone) {
        wl.wl_proxy_marshal(layer_surface, ZWLR_LAYER_SURFACE_V1_SET_EXCLUSIVE_ZONE, zone);
    }

    /**
     * Set margins around the surface.
     *
     * @param wl Wayland library instance
     * @param layer_surface the layer surface pointer
     * @param top top margin in pixels
     * @param right right margin in pixels
     * @param bottom bottom margin in pixels
     * @param left left margin in pixels
     */
    public static void setMargin(WaylandLibrary wl, Pointer layer_surface, int top, int right, int bottom, int left) {
        wl.wl_proxy_marshal(layer_surface, ZWLR_LAYER_SURFACE_V1_SET_MARGIN, top, right, bottom, left);
    }

    /**
     * Set keyboard interactivity mode.
     *
     * @param wl Wayland library instance
     * @param layer_surface the layer surface pointer
     * @param interactivity interactivity mode (NONE, EXCLUSIVE, ON_DEMAND)
     */
    public static void setKeyboardInteractivity(WaylandLibrary wl, Pointer layer_surface, int interactivity) {
        wl.wl_proxy_marshal(layer_surface, ZWLR_LAYER_SURFACE_V1_SET_KEYBOARD_INTERACTIVITY, interactivity);
    }

    /**
     * Acknowledge a configure event.
     *
     * @param wl Wayland library instance
     * @param layer_surface the layer surface pointer
     * @param serial serial from the configure event
     */
    public static void ackConfigure(WaylandLibrary wl, Pointer layer_surface, int serial) {
        wl.wl_proxy_marshal(layer_surface, ZWLR_LAYER_SURFACE_V1_ACK_CONFIGURE, serial);
    }

    /**
     * Set the layer of the surface.
     * Available since version 2.
     *
     * @param wl Wayland library instance
     * @param layer_surface the layer surface pointer
     * @param layer the new layer value
     */
    public static void setLayer(WaylandLibrary wl, Pointer layer_surface, int layer) {
        wl.wl_proxy_marshal(layer_surface, ZWLR_LAYER_SURFACE_V1_SET_LAYER, layer);
    }

    /**
     * Destroy a layer surface.
     *
     * @param wl Wayland library instance
     * @param layer_surface the layer surface pointer
     */
    public static void destroy(WaylandLibrary wl, Pointer layer_surface) {
        wl.wl_proxy_marshal(layer_surface, ZWLR_LAYER_SURFACE_V1_DESTROY);
        wl.wl_proxy_destroy(layer_surface);
    }

    /**
     * Helper to calculate anchor value for top/bottom bar positioning.
     *
     * @param isTop true for top, false for bottom
     * @return anchor bitfield
     */
    public static int getBarAnchor(boolean isTop) {
        if (isTop) {
            return ZWLR_LAYER_SURFACE_V1_ANCHOR_TOP |
                   ZWLR_LAYER_SURFACE_V1_ANCHOR_LEFT |
                   ZWLR_LAYER_SURFACE_V1_ANCHOR_RIGHT;
        } else {
            return ZWLR_LAYER_SURFACE_V1_ANCHOR_BOTTOM |
                   ZWLR_LAYER_SURFACE_V1_ANCHOR_LEFT |
                   ZWLR_LAYER_SURFACE_V1_ANCHOR_RIGHT;
        }
    }
}
