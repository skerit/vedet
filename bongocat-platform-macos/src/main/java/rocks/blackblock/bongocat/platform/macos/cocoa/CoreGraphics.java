package rocks.blackblock.bongocat.platform.macos.cocoa;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.IntByReference;

import java.util.Arrays;
import java.util.List;

/**
 * JNA bindings for Core Graphics framework.
 * Provides display and event management functions.
 */
public interface CoreGraphics extends Library {
    CoreGraphics INSTANCE = Native.load("CoreGraphics", CoreGraphics.class);

    // Display management
    int CGGetActiveDisplayList(int maxDisplays, IntByReference displays, IntByReference displayCount);
    int CGMainDisplayID();
    int CGDisplayPixelsWide(int display);
    int CGDisplayPixelsHigh(int display);
    CGRect.ByValue CGDisplayBounds(int display);
    double CGDisplayScreenSize(int display);

    /**
     * Callback interface for CGEventTap
     */
    interface CGEventTapCallBack extends com.sun.jna.Callback {
        Pointer callback(Pointer proxy, int type, Pointer event, Pointer refcon);
    }

    // Event tap management
    Pointer CGEventTapCreate(
        int tap,
        int place,
        int options,
        long eventsOfInterest,
        CGEventTapCallBack callback,
        Pointer refcon
    );
    void CFRunLoopAddSource(Pointer rl, Pointer source, Pointer mode);
    Pointer CFRunLoopGetCurrent();
    Pointer CFMachPortCreateRunLoopSource(Pointer allocator, Pointer port, long order);
    void CFRunLoopRun();
    void CFRunLoopStop(Pointer rl);
    Pointer CFRunLoopGetMain();

    // Event functions
    int CGEventGetIntegerValueField(Pointer event, int field);
    long CGEventGetFlags(Pointer event);
    long CGEventGetTimestamp(Pointer event);

    // Constants
    int kCGEventKeyDown = 10;
    int kCGEventKeyUp = 11;
    int kCGEventFlagsChanged = 12;
    int kCGKeyboardEventKeycode = 9;

    // Event tap constants
    int kCGSessionEventTap = 1;
    int kCGHeadInsertEventTap = 0;
    int kCGEventTapOptionDefault = 0;
    int kCGEventTapOptionListenOnly = 1;

    // Event mask calculation
    long CGEventMaskBit(int eventType);

    /**
     * CGRect structure for display bounds
     */
    @Structure.FieldOrder({"x", "y", "width", "height"})
    class CGRect extends Structure {
        public double x;
        public double y;
        public double width;
        public double height;

        public static class ByValue extends CGRect implements Structure.ByValue {}
    }

    /**
     * Helper function to create event mask
     */
    static long eventMask(int... eventTypes) {
        long mask = 0;
        for (int type : eventTypes) {
            mask |= (1L << type);
        }
        return mask;
    }
}
