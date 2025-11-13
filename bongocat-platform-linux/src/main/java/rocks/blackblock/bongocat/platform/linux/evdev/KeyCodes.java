package rocks.blackblock.bongocat.platform.linux.evdev;

import java.util.HashSet;
import java.util.Set;

/**
 * Linux input key codes and hand detection.
 *
 * Based on linux/input-event-codes.h
 * Divides keyboard into left and right hand sections.
 */
public class KeyCodes {

    // Common key codes from linux/input-event-codes.h
    public static final int KEY_ESC = 1;
    public static final int KEY_1 = 2;
    public static final int KEY_2 = 3;
    public static final int KEY_3 = 4;
    public static final int KEY_4 = 5;
    public static final int KEY_5 = 6;
    public static final int KEY_6 = 7;
    public static final int KEY_7 = 8;
    public static final int KEY_8 = 9;
    public static final int KEY_9 = 10;
    public static final int KEY_0 = 11;
    public static final int KEY_MINUS = 12;
    public static final int KEY_EQUAL = 13;
    public static final int KEY_BACKSPACE = 14;
    public static final int KEY_TAB = 15;
    public static final int KEY_Q = 16;
    public static final int KEY_W = 17;
    public static final int KEY_E = 18;
    public static final int KEY_R = 19;
    public static final int KEY_T = 20;
    public static final int KEY_Y = 21;
    public static final int KEY_U = 22;
    public static final int KEY_I = 23;
    public static final int KEY_O = 24;
    public static final int KEY_P = 25;
    public static final int KEY_LEFTBRACE = 26;
    public static final int KEY_RIGHTBRACE = 27;
    public static final int KEY_ENTER = 28;
    public static final int KEY_LEFTCTRL = 29;
    public static final int KEY_A = 30;
    public static final int KEY_S = 31;
    public static final int KEY_D = 32;
    public static final int KEY_F = 33;
    public static final int KEY_G = 34;
    public static final int KEY_H = 35;
    public static final int KEY_J = 36;
    public static final int KEY_K = 37;
    public static final int KEY_L = 38;
    public static final int KEY_SEMICOLON = 39;
    public static final int KEY_APOSTROPHE = 40;
    public static final int KEY_GRAVE = 41;
    public static final int KEY_LEFTSHIFT = 42;
    public static final int KEY_BACKSLASH = 43;
    public static final int KEY_Z = 44;
    public static final int KEY_X = 45;
    public static final int KEY_C = 46;
    public static final int KEY_V = 47;
    public static final int KEY_B = 48;
    public static final int KEY_N = 49;
    public static final int KEY_M = 50;
    public static final int KEY_COMMA = 51;
    public static final int KEY_DOT = 52;
    public static final int KEY_SLASH = 53;
    public static final int KEY_RIGHTSHIFT = 54;
    public static final int KEY_KPASTERISK = 55;
    public static final int KEY_LEFTALT = 56;
    public static final int KEY_SPACE = 57;
    public static final int KEY_CAPSLOCK = 58;
    public static final int KEY_F1 = 59;
    public static final int KEY_F2 = 60;
    public static final int KEY_F3 = 61;
    public static final int KEY_F4 = 62;
    public static final int KEY_F5 = 63;
    public static final int KEY_F6 = 64;
    public static final int KEY_F7 = 65;
    public static final int KEY_F8 = 66;
    public static final int KEY_F9 = 67;
    public static final int KEY_F10 = 68;
    public static final int KEY_F11 = 87;
    public static final int KEY_F12 = 88;

    // Left hand keys (roughly left half of keyboard)
    private static final Set<Integer> LEFT_HAND_KEYS = new HashSet<>();

    // Right hand keys (roughly right half of keyboard)
    private static final Set<Integer> RIGHT_HAND_KEYS = new HashSet<>();

    static {
        // Left hand: ESC, ~, Tab, Caps, Shift, Ctrl, Alt, and keys 1-5, Q-T, A-G, Z-B
        LEFT_HAND_KEYS.add(KEY_ESC);
        LEFT_HAND_KEYS.add(KEY_GRAVE);
        LEFT_HAND_KEYS.add(KEY_TAB);
        LEFT_HAND_KEYS.add(KEY_CAPSLOCK);
        LEFT_HAND_KEYS.add(KEY_LEFTSHIFT);
        LEFT_HAND_KEYS.add(KEY_LEFTCTRL);
        LEFT_HAND_KEYS.add(KEY_LEFTALT);

        // Numbers 1-5
        LEFT_HAND_KEYS.add(KEY_1);
        LEFT_HAND_KEYS.add(KEY_2);
        LEFT_HAND_KEYS.add(KEY_3);
        LEFT_HAND_KEYS.add(KEY_4);
        LEFT_HAND_KEYS.add(KEY_5);

        // Top row: Q, W, E, R, T
        LEFT_HAND_KEYS.add(KEY_Q);
        LEFT_HAND_KEYS.add(KEY_W);
        LEFT_HAND_KEYS.add(KEY_E);
        LEFT_HAND_KEYS.add(KEY_R);
        LEFT_HAND_KEYS.add(KEY_T);

        // Home row: A, S, D, F, G
        LEFT_HAND_KEYS.add(KEY_A);
        LEFT_HAND_KEYS.add(KEY_S);
        LEFT_HAND_KEYS.add(KEY_D);
        LEFT_HAND_KEYS.add(KEY_F);
        LEFT_HAND_KEYS.add(KEY_G);

        // Bottom row: Z, X, C, V, B
        LEFT_HAND_KEYS.add(KEY_Z);
        LEFT_HAND_KEYS.add(KEY_X);
        LEFT_HAND_KEYS.add(KEY_C);
        LEFT_HAND_KEYS.add(KEY_V);
        LEFT_HAND_KEYS.add(KEY_B);

        // F-keys F1-F6
        LEFT_HAND_KEYS.add(KEY_F1);
        LEFT_HAND_KEYS.add(KEY_F2);
        LEFT_HAND_KEYS.add(KEY_F3);
        LEFT_HAND_KEYS.add(KEY_F4);
        LEFT_HAND_KEYS.add(KEY_F5);
        LEFT_HAND_KEYS.add(KEY_F6);

        // Right hand: 6-0, Y-P, H-;, N-/, Enter, Backspace, Right Shift
        RIGHT_HAND_KEYS.add(KEY_BACKSPACE);
        RIGHT_HAND_KEYS.add(KEY_ENTER);
        RIGHT_HAND_KEYS.add(KEY_RIGHTSHIFT);

        // Numbers 6-0
        RIGHT_HAND_KEYS.add(KEY_6);
        RIGHT_HAND_KEYS.add(KEY_7);
        RIGHT_HAND_KEYS.add(KEY_8);
        RIGHT_HAND_KEYS.add(KEY_9);
        RIGHT_HAND_KEYS.add(KEY_0);
        RIGHT_HAND_KEYS.add(KEY_MINUS);
        RIGHT_HAND_KEYS.add(KEY_EQUAL);

        // Top row: Y, U, I, O, P, [, ]
        RIGHT_HAND_KEYS.add(KEY_Y);
        RIGHT_HAND_KEYS.add(KEY_U);
        RIGHT_HAND_KEYS.add(KEY_I);
        RIGHT_HAND_KEYS.add(KEY_O);
        RIGHT_HAND_KEYS.add(KEY_P);
        RIGHT_HAND_KEYS.add(KEY_LEFTBRACE);
        RIGHT_HAND_KEYS.add(KEY_RIGHTBRACE);
        RIGHT_HAND_KEYS.add(KEY_BACKSLASH);

        // Home row: H, J, K, L, ;, '
        RIGHT_HAND_KEYS.add(KEY_H);
        RIGHT_HAND_KEYS.add(KEY_J);
        RIGHT_HAND_KEYS.add(KEY_K);
        RIGHT_HAND_KEYS.add(KEY_L);
        RIGHT_HAND_KEYS.add(KEY_SEMICOLON);
        RIGHT_HAND_KEYS.add(KEY_APOSTROPHE);

        // Bottom row: N, M, ,, ., /
        RIGHT_HAND_KEYS.add(KEY_N);
        RIGHT_HAND_KEYS.add(KEY_M);
        RIGHT_HAND_KEYS.add(KEY_COMMA);
        RIGHT_HAND_KEYS.add(KEY_DOT);
        RIGHT_HAND_KEYS.add(KEY_SLASH);

        // F-keys F7-F12
        RIGHT_HAND_KEYS.add(KEY_F7);
        RIGHT_HAND_KEYS.add(KEY_F8);
        RIGHT_HAND_KEYS.add(KEY_F9);
        RIGHT_HAND_KEYS.add(KEY_F10);
        RIGHT_HAND_KEYS.add(KEY_F11);
        RIGHT_HAND_KEYS.add(KEY_F12);
    }

    /**
     * Check if a key code is a left hand key.
     */
    public static boolean isLeftHandKey(int keyCode) {
        // Space bar is special - treat as left hand by default
        if (keyCode == KEY_SPACE) {
            return true;
        }
        return LEFT_HAND_KEYS.contains(keyCode);
    }

    /**
     * Check if a key code is a right hand key.
     */
    public static boolean isRightHandKey(int keyCode) {
        return RIGHT_HAND_KEYS.contains(keyCode);
    }

    /**
     * Get a human-readable name for a key code.
     * Returns null if key code is unknown.
     */
    public static String getKeyName(int keyCode) {
        switch (keyCode) {
            case KEY_ESC: return "ESC";
            case KEY_1: return "1";
            case KEY_2: return "2";
            case KEY_3: return "3";
            case KEY_4: return "4";
            case KEY_5: return "5";
            case KEY_6: return "6";
            case KEY_7: return "7";
            case KEY_8: return "8";
            case KEY_9: return "9";
            case KEY_0: return "0";
            case KEY_Q: return "Q";
            case KEY_W: return "W";
            case KEY_E: return "E";
            case KEY_R: return "R";
            case KEY_T: return "T";
            case KEY_Y: return "Y";
            case KEY_U: return "U";
            case KEY_I: return "I";
            case KEY_O: return "O";
            case KEY_P: return "P";
            case KEY_A: return "A";
            case KEY_S: return "S";
            case KEY_D: return "D";
            case KEY_F: return "F";
            case KEY_G: return "G";
            case KEY_H: return "H";
            case KEY_J: return "J";
            case KEY_K: return "K";
            case KEY_L: return "L";
            case KEY_Z: return "Z";
            case KEY_X: return "X";
            case KEY_C: return "C";
            case KEY_V: return "V";
            case KEY_B: return "B";
            case KEY_N: return "N";
            case KEY_M: return "M";
            case KEY_SPACE: return "SPACE";
            case KEY_ENTER: return "ENTER";
            case KEY_BACKSPACE: return "BACKSPACE";
            case KEY_TAB: return "TAB";
            case KEY_LEFTSHIFT: return "LSHIFT";
            case KEY_RIGHTSHIFT: return "RSHIFT";
            case KEY_LEFTCTRL: return "LCTRL";
            case KEY_LEFTALT: return "LALT";
            default: return null;
        }
    }
}
