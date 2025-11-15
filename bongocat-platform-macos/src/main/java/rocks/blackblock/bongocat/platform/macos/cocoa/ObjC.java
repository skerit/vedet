package rocks.blackblock.bongocat.platform.macos.cocoa;

import com.sun.jna.Pointer;

/**
 * Helper class for Objective-C runtime interactions.
 * Provides convenience methods for common Objective-C operations.
 */
public class ObjC {
    private static final ObjCRuntime runtime = ObjCRuntime.INSTANCE;

    /**
     * Get an Objective-C class by name
     */
    public static Pointer getClass(String className) {
        return runtime.objc_getClass(className);
    }

    /**
     * Get a selector by name
     */
    public static Pointer selector(String name) {
        return runtime.sel_registerName(name);
    }

    /**
     * Send a message that returns a Pointer
     */
    public static Pointer send(Pointer receiver, String selectorName, Object... args) {
        Pointer selector = selector(selectorName);
        return runtime.objc_msgSend(receiver, selector, args);
    }

    /**
     * Send a message that returns void
     */
    public static void sendVoid(Pointer receiver, String selectorName, Object... args) {
        Pointer selector = selector(selectorName);
        runtime.objc_msgSend(receiver, selector, args);
    }

    /**
     * Send a message that returns a long/int
     */
    public static long sendLong(Pointer receiver, String selectorName, Object... args) {
        Pointer selector = selector(selectorName);
        // Use msgSend and get value from pointer
        Pointer result = runtime.objc_msgSend(receiver, selector, args);
        return Pointer.nativeValue(result);
    }

    /**
     * Send a message that returns a double
     */
    public static double sendDouble(Pointer receiver, String selectorName, Object... args) {
        Pointer selector = selector(selectorName);
        // Use msgSend and interpret as double
        Pointer result = runtime.objc_msgSend(receiver, selector, args);
        return Double.longBitsToDouble(Pointer.nativeValue(result));
    }

    /**
     * Send a message that returns a boolean
     */
    public static boolean sendBool(Pointer receiver, String selectorName, Object... args) {
        Pointer selector = selector(selectorName);
        // Use msgSend and check if result is non-zero
        Pointer result = runtime.objc_msgSend(receiver, selector, args);
        return Pointer.nativeValue(result) != 0;
    }

    /**
     * Allocate an instance of a class
     */
    public static Pointer alloc(String className) {
        Pointer cls = getClass(className);
        return send(cls, "alloc");
    }

    /**
     * Create a new autorelease pool
     */
    public static Pointer autoreleasePool() {
        return send(getClass("NSAutoreleasePool"), "new");
    }

    /**
     * Drain an autorelease pool
     */
    public static void drainPool(Pointer pool) {
        sendVoid(pool, "drain");
    }

    /**
     * Create an NSString from a Java string
     */
    public static Pointer nsString(String str) {
        Pointer nsStringClass = getClass("NSString");
        Pointer utf8Selector = selector("stringWithUTF8String:");
        return runtime.objc_msgSend(nsStringClass, utf8Selector, str);
    }

    /**
     * Get the shared application instance
     */
    public static Pointer sharedApplication() {
        Pointer nsAppClass = getClass("NSApplication");
        return send(nsAppClass, "sharedApplication");
    }
}
