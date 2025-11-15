package rocks.blackblock.bongocat.platform.macos;

import rocks.blackblock.bongocat.platform.InputEvent;

import java.util.Set;

/**
 * macOS implementation of InputEvent.
 * Maps macOS keyboard events to the platform-agnostic interface.
 */
public class MacOSInputEvent implements InputEvent {
    private final Type type;
    private final int keyCode;
    private final long timestamp;

    // macOS keycodes for left-hand keys (based on US keyboard layout)
    private static final Set<Integer> LEFT_HAND_KEYS = Set.of(
        12, 13, 14, 15, 17,  // Q, W, E, R, T
        0, 1, 2, 3, 5,        // A, S, D, F, G
        6, 7, 8, 9,           // Z, X, C, V
        16, 4, 11, 45, 46,    // Y (on some layouts), B (sometimes), additional keys
        48, 56, 59, 55        // Tab, Shift, Ctrl, Cmd (left modifiers)
    );

    // macOS keycodes for right-hand keys (based on US keyboard layout)
    private static final Set<Integer> RIGHT_HAND_KEYS = Set.of(
        34, 31, 35, 32,       // I, O, P, [
        38, 40, 37, 41, 42,   // J, K, L, ;, '
        45, 46, 43, 47,       // N, M, ,, .
        123, 124, 125, 126,   // Arrow keys (left, right, down, up)
        60, 54, 61, 58        // Right modifiers (Shift, Cmd, Option, Ctrl)
    );

    public MacOSInputEvent(Type type, int keyCode, long timestamp) {
        this.type = type;
        this.keyCode = keyCode;
        this.timestamp = timestamp;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public int getKeyCode() {
        return keyCode;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public boolean isLeftHandKey() {
        return LEFT_HAND_KEYS.contains(keyCode);
    }

    @Override
    public boolean isRightHandKey() {
        return RIGHT_HAND_KEYS.contains(keyCode);
    }

    @Override
    public String toString() {
        return String.format("MacOSInputEvent{type=%s, keyCode=%d, timestamp=%d}",
                type, keyCode, timestamp);
    }
}
