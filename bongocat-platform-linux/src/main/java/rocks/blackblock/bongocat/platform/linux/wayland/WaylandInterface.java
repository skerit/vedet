package rocks.blackblock.bongocat.platform.linux.wayland;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import java.util.HashMap;
import java.util.Map;

/**
 * Wayland interface structure definitions for protocol extensions.
 *
 * These are needed for binding protocol extensions that don't have their
 * interface symbols in libwayland-client.
 */
public class WaylandInterface {

    /**
     * Minimal wl_interface structure from Wayland protocol.
     * The name field must be a native pointer to a C string.
     */
    @Structure.FieldOrder({"name", "version", "method_count", "methods", "event_count", "events"})
    public static class WlInterface extends Structure {
        public Pointer name;  // Pointer to C string, not Java String
        public int version;
        public int method_count;
        public Pointer methods;
        public int event_count;
        public Pointer events;

        public WlInterface() {
            // Allocate with malloc so it stays in native memory
            super();
            setAutoSynch(false); // We'll manage synchronization manually
        }

        public WlInterface(Pointer namePtr, int version) {
            this();
            this.name = namePtr;
            this.version = version;
            this.method_count = 0;
            this.methods = Pointer.NULL;
            this.event_count = 0;
            this.events = Pointer.NULL;
        }
    }

    // Cache for created interfaces - must keep Structure objects alive to prevent GC
    private static final Map<String, WlInterface> interfaceCache = new HashMap<>();

    /**
     * Create a minimal interface structure for a protocol extension.
     * This is sufficient for wl_registry_bind operations.
     */
    public static synchronized Pointer createInterface(String name, int version) {
        String key = name + "_v" + version;

        if (interfaceCache.containsKey(key)) {
            return interfaceCache.get(key).getPointer();
        }

        // Create native C string for the name - must stay alive!
        Memory nameMemory = new Memory(name.length() + 1);
        nameMemory.setString(0, name);

        // Create structure - JNA will allocate native memory automatically
        WlInterface iface = new WlInterface(nameMemory, version);
        iface.write();  // Write structure fields to native memory
        iface.read();   // Force synchronization

        // Cache the structure object itself to prevent GC
        interfaceCache.put(key, iface);

        return iface.getPointer();
    }

    /**
     * Get interface pointer for zwlr_layer_shell_v1
     */
    public static Pointer getLayerShellInterface() {
        return createInterface("zwlr_layer_shell_v1", 4);
    }

    /**
     * Get interface pointer for zxdg_output_manager_v1
     */
    public static Pointer getXdgOutputManagerInterface() {
        return createInterface("zxdg_output_manager_v1", 3);
    }

    /**
     * Get interface pointer for any protocol by name
     */
    public static Pointer getInterfaceByName(String name) {
        // Use reasonable default versions
        int version = 1;
        if (name.equals("zwlr_layer_shell_v1")) {
            version = 4;
        } else if (name.equals("zwlr_layer_surface_v1")) {
            version = 4;
        } else if (name.equals("zxdg_output_manager_v1")) {
            version = 3;
        } else if (name.equals("zxdg_output_v1")) {
            version = 3;
        } else if (name.equals("zwlr_foreign_toplevel_manager_v1")) {
            version = 3;
        }

        return createInterface(name, version);
    }
}
