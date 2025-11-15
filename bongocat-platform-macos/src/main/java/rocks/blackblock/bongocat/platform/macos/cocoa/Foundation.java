package rocks.blackblock.bongocat.platform.macos.cocoa;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

/**
 * JNA bindings for Core Foundation and Foundation framework.
 */
public interface Foundation extends Library {
    Foundation INSTANCE = Native.load("Foundation", Foundation.class);

    // Autorelease pool management
    Pointer objc_autoreleasePoolPush();
    void objc_autoreleasePoolPop(Pointer pool);

    // NSString functions
    Pointer CFStringCreateWithCString(Pointer allocator, String string, int encoding);
    void CFRelease(Pointer cf);

    // Constants
    int kCFStringEncodingUTF8 = 0x08000100;
}
