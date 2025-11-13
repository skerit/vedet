package rocks.blackblock.bongocat.platform.macos.cocoa;

import com.sun.jna.FunctionMapper;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * JNA bindings for Objective-C runtime.
 * Provides low-level access to Objective-C classes, methods, and messaging.
 */
public interface ObjCRuntime extends Library {
    Map<String, Object> OPTIONS = new HashMap<>() {{
        put(Library.OPTION_FUNCTION_MAPPER, new FunctionMapper() {
            @Override
            public String getFunctionName(NativeLibrary library, Method method) {
                // Map all our msgSend variants to the same native function
                String name = method.getName();
                if (name.startsWith("msgSend")) {
                    return "objc_msgSend";
                }
                return name;
            }
        });
    }};

    ObjCRuntime INSTANCE = Native.load("objc", ObjCRuntime.class, OPTIONS);

    // Class and object management
    Pointer objc_getClass(String className);
    Pointer class_createInstance(Pointer cls, long extraBytes);
    Pointer objc_allocateClassPair(Pointer superclass, String name, long extraBytes);
    void objc_registerClassPair(Pointer cls);

    // Selectors
    Pointer sel_registerName(String name);

    // Messaging - all map to objc_msgSend but with different return types
    Pointer msgSend(Pointer receiver, Pointer selector, Object... args);
    long msgSend_long(Pointer receiver, Pointer selector, Object... args);
    double msgSend_double(Pointer receiver, Pointer selector, Object... args);
    void msgSend_void(Pointer receiver, Pointer selector, Object... args);
    boolean msgSend_bool(Pointer receiver, Pointer selector, Object... args);
}
