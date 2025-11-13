package rocks.blackblock.bongocat.platform.linux.evdev;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

/**
 * JNA bindings for Linux evdev (event device) interface.
 *
 * Provides access to /dev/input/eventX devices for reading keyboard input.
 */
public interface EvdevLibrary extends Library {
    EvdevLibrary INSTANCE = Native.load("c", EvdevLibrary.class);

    // File operation constants
    int O_RDONLY = 0x0000;
    int O_NONBLOCK = 0x0800;

    // ioctl request codes for evdev
    long EVIOCGNAME = 0x80FF4506L;  // Get device name
    long EVIOCGID = 0x80084502L;     // Get device ID
    long EVIOCGBIT = 0x80FF4520L;    // Get event bits

    // Event types
    int EV_SYN = 0x00;      // Synchronization events
    int EV_KEY = 0x01;      // Key/button events
    int EV_REL = 0x02;      // Relative axis events
    int EV_ABS = 0x03;      // Absolute axis events
    int EV_MSC = 0x04;      // Miscellaneous events

    // Key event values
    int KEY_RELEASE = 0;
    int KEY_PRESS = 1;
    int KEY_REPEAT = 2;

    /**
     * Input event structure from linux/input.h
     */
    @Structure.FieldOrder({"tv_sec", "tv_usec", "type", "code", "value"})
    class InputEvent extends Structure {
        public long tv_sec;      // seconds
        public long tv_usec;     // microseconds
        public short type;       // event type (EV_KEY, EV_REL, etc.)
        public short code;       // event code (key code, axis, etc.)
        public int value;        // event value

        public InputEvent() {}

        public InputEvent(com.sun.jna.Pointer p) {
            super(p);
            read();
        }

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("tv_sec", "tv_usec", "type", "code", "value");
        }

        @Override
        public String toString() {
            return String.format("InputEvent{type=%d, code=%d, value=%d}", type, code, value);
        }
    }

    /**
     * Device ID structure
     */
    @Structure.FieldOrder({"bustype", "vendor", "product", "version"})
    class InputId extends Structure {
        public short bustype;
        public short vendor;
        public short product;
        public short version;

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("bustype", "vendor", "product", "version");
        }
    }

    /**
     * Open a file descriptor.
     */
    int open(String pathname, int flags);

    /**
     * Close a file descriptor.
     */
    int close(int fd);

    /**
     * Read from a file descriptor.
     */
    int read(int fd, com.sun.jna.Pointer buffer, int count);

    /**
     * ioctl system call for device control.
     */
    int ioctl(int fd, long request, com.sun.jna.Pointer argp);

    /**
     * poll() for waiting on file descriptors.
     */
    int poll(PollFd[] fds, int nfds, int timeout);

    /**
     * pollfd structure for poll()
     */
    @Structure.FieldOrder({"fd", "events", "revents"})
    class PollFd extends Structure {
        public int fd;
        public short events;
        public short revents;

        public static final short POLLIN = 0x0001;
        public static final short POLLPRI = 0x0002;
        public static final short POLLOUT = 0x0004;
        public static final short POLLERR = 0x0008;
        public static final short POLLHUP = 0x0010;
        public static final short POLLNVAL = 0x0020;

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("fd", "events", "revents");
        }
    }
}
