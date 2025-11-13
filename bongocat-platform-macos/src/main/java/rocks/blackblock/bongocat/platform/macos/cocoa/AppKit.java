package rocks.blackblock.bongocat.platform.macos.cocoa;

import com.sun.jna.Library;
import com.sun.jna.Native;

/**
 * JNA bindings for AppKit framework constants.
 * AppKit is accessed primarily through Objective-C runtime.
 */
public interface AppKit extends Library {
    AppKit INSTANCE = Native.load("AppKit", AppKit.class);

    // NSWindow style masks
    int NSWindowStyleMaskBorderless = 0;
    int NSWindowStyleMaskTitled = 1;
    int NSWindowStyleMaskClosable = 2;
    int NSWindowStyleMaskMiniaturizable = 4;
    int NSWindowStyleMaskResizable = 8;
    int NSWindowStyleMaskFullScreen = 1 << 14;

    // NSWindow backing store types
    int NSBackingStoreRetained = 0;
    int NSBackingStoreNonretained = 1;
    int NSBackingStoreBuffered = 2;

    // NSWindow collection behaviors
    long NSWindowCollectionBehaviorCanJoinAllSpaces = 1 << 0;
    long NSWindowCollectionBehaviorMoveToActiveSpace = 1 << 1;
    long NSWindowCollectionBehaviorManaged = 1 << 2;
    long NSWindowCollectionBehaviorTransient = 1 << 3;
    long NSWindowCollectionBehaviorStationary = 1 << 4;
    long NSWindowCollectionBehaviorParticipatesInCycle = 1 << 5;
    long NSWindowCollectionBehaviorIgnoresCycle = 1 << 6;
    long NSWindowCollectionBehaviorFullScreenPrimary = 1 << 7;
    long NSWindowCollectionBehaviorFullScreenAuxiliary = 1 << 8;
    long NSWindowCollectionBehaviorFullScreenNone = 1 << 9;

    // NSWindow levels
    int NSNormalWindowLevel = 0;
    int NSFloatingWindowLevel = 3;
    int NSStatusWindowLevel = 25;
    int NSPopUpMenuWindowLevel = 101;
    int NSScreenSaverWindowLevel = 1000;

    // NSApplication activation policies
    int NSApplicationActivationPolicyRegular = 0;
    int NSApplicationActivationPolicyAccessory = 1;
    int NSApplicationActivationPolicyProhibited = 2;
}
