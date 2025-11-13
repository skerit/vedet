package rocks.blackblock.bongocat.platform.linux.wayland;

import com.sun.jna.Callback;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

/**
 * Bindings for xdg-output-unstable-v1 protocol.
 *
 * This protocol provides additional output (monitor) information that's not
 * available in wl_output, particularly:
 * - Logical position and size (after scaling)
 * - Output name (e.g., "eDP-1", "HDMI-1")
 * - Output description
 */
public class XdgOutputProtocol {

    /**
     * Interface names for wl_registry_bind
     */
    public static final String MANAGER_INTERFACE_NAME = "zxdg_output_manager_v1";
    public static final int VERSION = 3;

    // =========================================================================
    // Request Opcodes
    // =========================================================================

    // zxdg_output_manager_v1 requests
    private static final int ZXDG_OUTPUT_MANAGER_V1_DESTROY = 0;
    private static final int ZXDG_OUTPUT_MANAGER_V1_GET_XDG_OUTPUT = 1;

    // zxdg_output_v1 requests
    private static final int ZXDG_OUTPUT_V1_DESTROY = 0;

    // =========================================================================
    // XDG Output Listener
    // =========================================================================

    /**
     * Listener for xdg output events.
     */
    @Structure.FieldOrder({"logical_position", "logical_size", "done", "name", "description"})
    public static class XdgOutputListener extends Structure {
        public LogicalPositionCallback logical_position;
        public LogicalSizeCallback logical_size;
        public DoneCallback done;
        public NameCallback name;
        public DescriptionCallback description;

        /**
         * Called when the logical position changes.
         */
        public interface LogicalPositionCallback extends Callback {
            void callback(Pointer data, Pointer xdg_output, int x, int y);
        }

        /**
         * Called when the logical size changes.
         */
        public interface LogicalSizeCallback extends Callback {
            void callback(Pointer data, Pointer xdg_output, int width, int height);
        }

        /**
         * Called when all changes are done (batch update complete).
         */
        public interface DoneCallback extends Callback {
            void callback(Pointer data, Pointer xdg_output);
        }

        /**
         * Called with the output name (e.g., "eDP-1").
         * Since version 2.
         */
        public interface NameCallback extends Callback {
            void callback(Pointer data, Pointer xdg_output, String name);
        }

        /**
         * Called with the output description.
         * Since version 2.
         */
        public interface DescriptionCallback extends Callback {
            void callback(Pointer data, Pointer xdg_output, String description);
        }
    }

    // =========================================================================
    // Helper Methods for Protocol Requests
    // =========================================================================

    /**
     * Get xdg output information for a wl_output.
     *
     * @param wl Wayland library instance
     * @param output_manager the xdg output manager pointer
     * @param output the wl_output pointer
     * @return xdg output pointer
     */
    public static Pointer getXdgOutput(WaylandLibrary wl, Pointer output_manager, Pointer output) {
        return wl.wl_proxy_marshal_constructor_versioned(
            output_manager,
            ZXDG_OUTPUT_MANAGER_V1_GET_XDG_OUTPUT,
            null,
            VERSION,
            null, // id (new xdg output)
            output
        );
    }

    /**
     * Destroy an xdg output object.
     *
     * @param wl Wayland library instance
     * @param xdg_output the xdg output pointer
     */
    public static void destroy(WaylandLibrary wl, Pointer xdg_output) {
        wl.wl_proxy_marshal(xdg_output, ZXDG_OUTPUT_V1_DESTROY);
        wl.wl_proxy_destroy(xdg_output);
    }

    /**
     * Destroy the xdg output manager.
     *
     * @param wl Wayland library instance
     * @param output_manager the xdg output manager pointer
     */
    public static void destroyManager(WaylandLibrary wl, Pointer output_manager) {
        wl.wl_proxy_marshal(output_manager, ZXDG_OUTPUT_MANAGER_V1_DESTROY);
        wl.wl_proxy_destroy(output_manager);
    }
}
