package rocks.blackblock.bongocat.platform.macos.cocoa;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

/**
 * JNA bindings for Objective-C runtime.
 * Provides low-level access to Objective-C classes, methods, and messaging.
 */
public interface ObjCRuntime extends Library {
    ObjCRuntime INSTANCE = Native.load("objc", ObjCRuntime.class);

    // Class and object management
    Pointer objc_getClass(String className);
    Pointer class_createInstance(Pointer cls, long extraBytes);
    Pointer objc_allocateClassPair(Pointer superclass, String name, long extraBytes);
    void objc_registerClassPair(Pointer cls);

    // Selectors
    Pointer sel_registerName(String name);

    // Messaging
    Pointer objc_msgSend(Pointer receiver, Pointer selector, Object... args);
    long objc_msgSend_long(Pointer receiver, Pointer selector, Object... args);
    double objc_msgSend_double(Pointer receiver, Pointer selector, Object... args);
    void objc_msgSend_void(Pointer receiver, Pointer selector, Object... args);
    boolean objc_msgSend_bool(Pointer receiver, Pointer selector, Object... args);
}
