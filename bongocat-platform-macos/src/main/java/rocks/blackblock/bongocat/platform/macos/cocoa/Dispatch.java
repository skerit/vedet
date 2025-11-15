package rocks.blackblock.bongocat.platform.macos.cocoa;

import com.sun.jna.Callback;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

/**
 * JNA bindings for Grand Central Dispatch (libdispatch).
 * Provides access to dispatch queues for thread management.
 */
public interface Dispatch extends Library {
    Dispatch INSTANCE = Native.load("c", Dispatch.class);

    /**
     * Callback interface for dispatch blocks
     */
    interface DispatchBlock extends Callback {
        void invoke();
    }

    /**
     * Get the main dispatch queue (runs on main thread)
     */
    Pointer dispatch_get_main_queue();

    /**
     * Submit a block for asynchronous execution on a dispatch queue
     */
    void dispatch_async(Pointer queue, DispatchBlock block);

    /**
     * Submit a block for synchronous execution on a dispatch queue
     */
    void dispatch_sync(Pointer queue, DispatchBlock block);
}
