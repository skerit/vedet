package rocks.blackblock.bongocat.platform.linux.wayland;

import com.sun.jna.Callback;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

/**
 * Common Wayland types, structures, and callback interfaces.
 */
public class WaylandTypes {

    // =========================================================================
    // Registry Listener
    // =========================================================================

    /**
     * Listener for registry global events.
     * Called when global interfaces are advertised or removed.
     */
    @Structure.FieldOrder({"global", "global_remove"})
    public static class RegistryListener extends Structure {
        public GlobalCallback global;
        public GlobalRemoveCallback global_remove;

        public interface GlobalCallback extends Callback {
            void callback(Pointer data, Pointer registry, int name, String interface_name, int version);
        }

        public interface GlobalRemoveCallback extends Callback {
            void callback(Pointer data, Pointer registry, int name);
        }
    }

    // =========================================================================
    // Callback Listener (for frame callbacks)
    // =========================================================================

    /**
     * Listener for frame callbacks.
     * Used for vsync and frame timing.
     */
    @Structure.FieldOrder({"done"})
    public static class CallbackListener extends Structure {
        public DoneCallback done;

        public interface DoneCallback extends Callback {
            void callback(Pointer data, Pointer callback, int callback_data);
        }
    }

    // =========================================================================
    // SHM Listener
    // =========================================================================

    /**
     * Listener for shared memory format events.
     */
    @Structure.FieldOrder({"format"})
    public static class ShmListener extends Structure {
        public FormatCallback format;

        public interface FormatCallback extends Callback {
            void callback(Pointer data, Pointer shm, int format);
        }
    }

    // =========================================================================
    // Output (Monitor) Listener
    // =========================================================================

    /**
     * Listener for output (monitor) events.
     * Provides geometry, mode, scale, and other display properties.
     */
    @Structure.FieldOrder({"geometry", "mode", "done", "scale"})
    public static class OutputListener extends Structure {
        public GeometryCallback geometry;
        public ModeCallback mode;
        public DoneCallback done;
        public ScaleCallback scale;

        public interface GeometryCallback extends Callback {
            void callback(Pointer data, Pointer output, int x, int y, int physical_width,
                         int physical_height, int subpixel, String make, String model, int transform);
        }

        public interface ModeCallback extends Callback {
            void callback(Pointer data, Pointer output, int flags, int width, int height, int refresh);
        }

        public interface DoneCallback extends Callback {
            void callback(Pointer data, Pointer output);
        }

        public interface ScaleCallback extends Callback {
            void callback(Pointer data, Pointer output, int factor);
        }
    }

    // =========================================================================
    // Constants
    // =========================================================================

    // Output transform values
    public static final int WL_OUTPUT_TRANSFORM_NORMAL = 0;
    public static final int WL_OUTPUT_TRANSFORM_90 = 1;
    public static final int WL_OUTPUT_TRANSFORM_180 = 2;
    public static final int WL_OUTPUT_TRANSFORM_270 = 3;
    public static final int WL_OUTPUT_TRANSFORM_FLIPPED = 4;
    public static final int WL_OUTPUT_TRANSFORM_FLIPPED_90 = 5;
    public static final int WL_OUTPUT_TRANSFORM_FLIPPED_180 = 6;
    public static final int WL_OUTPUT_TRANSFORM_FLIPPED_270 = 7;

    // Output mode flags
    public static final int WL_OUTPUT_MODE_CURRENT = 0x1;
    public static final int WL_OUTPUT_MODE_PREFERRED = 0x2;

    // Shared memory formats
    public static final int WL_SHM_FORMAT_ARGB8888 = 0;
    public static final int WL_SHM_FORMAT_XRGB8888 = 1;
    public static final int WL_SHM_FORMAT_RGB565 = 0x34315652;
    public static final int WL_SHM_FORMAT_RGBA8888 = 0x34325241;

    // Surface buffer transform
    public static final int WL_SURFACE_TRANSFORM_NORMAL = 0;

    /**
     * Helper to convert Wayland transform to Monitor.Transform enum.
     */
    public static rocks.blackblock.bongocat.platform.Monitor.Transform transformFromWayland(int waylandTransform) {
        switch (waylandTransform) {
            case WL_OUTPUT_TRANSFORM_NORMAL:
                return rocks.blackblock.bongocat.platform.Monitor.Transform.NORMAL;
            case WL_OUTPUT_TRANSFORM_90:
                return rocks.blackblock.bongocat.platform.Monitor.Transform.ROTATED_90;
            case WL_OUTPUT_TRANSFORM_180:
                return rocks.blackblock.bongocat.platform.Monitor.Transform.ROTATED_180;
            case WL_OUTPUT_TRANSFORM_270:
                return rocks.blackblock.bongocat.platform.Monitor.Transform.ROTATED_270;
            case WL_OUTPUT_TRANSFORM_FLIPPED:
                return rocks.blackblock.bongocat.platform.Monitor.Transform.FLIPPED;
            case WL_OUTPUT_TRANSFORM_FLIPPED_90:
                return rocks.blackblock.bongocat.platform.Monitor.Transform.FLIPPED_90;
            case WL_OUTPUT_TRANSFORM_FLIPPED_180:
                return rocks.blackblock.bongocat.platform.Monitor.Transform.FLIPPED_180;
            case WL_OUTPUT_TRANSFORM_FLIPPED_270:
                return rocks.blackblock.bongocat.platform.Monitor.Transform.FLIPPED_270;
            default:
                return rocks.blackblock.bongocat.platform.Monitor.Transform.NORMAL;
        }
    }
}
