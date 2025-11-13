package rocks.blackblock.bongocat.platform;

/**
 * Represents a keyboard input event.
 */
public interface InputEvent {
    /**
     * Get the type of input event
     */
    Type getType();

    /**
     * Get the key code (platform-specific)
     */
    int getKeyCode();

    /**
     * Get the timestamp in nanoseconds
     */
    long getTimestamp();

    /**
     * Check if the key is a left-hand key
     * (keys typically pressed with the left hand: QWER, ASDF, ZXCV, Tab, Shift, Ctrl, Alt)
     */
    boolean isLeftHandKey();

    /**
     * Check if the key is a right-hand key
     * (keys typically pressed with the right hand: UIOP, JKL, NM, arrows, etc.)
     */
    boolean isRightHandKey();

    enum Type {
        KEY_PRESS,
        KEY_RELEASE
    }
}
