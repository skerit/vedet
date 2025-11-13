package rocks.blackblock.bongocat.platform.linux;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * JNA bindings for POSIX shared memory and mmap operations.
 * Used for creating Wayland shared memory buffers.
 */
public class NativeMemory {

    private static final CLibrary C_LIB = Native.load("c", CLibrary.class);

    public interface CLibrary extends Library {
        /**
         * Create an anonymous file (Linux 3.17+).
         *
         * @param name  name for debugging (not a path)
         * @param flags flags (e.g., MFD_CLOEXEC = 1)
         * @return file descriptor, or -1 on error
         */
        int memfd_create(String name, int flags);

        /**
         * Open a file.
         *
         * @param pathname file path
         * @param oflag    flags (O_RDWR | O_CREAT | O_EXCL)
         * @param mode     permissions (e.g., 0600)
         * @return file descriptor, or -1 on error
         */
        int open(String pathname, int oflag, int mode);

        /**
         * Delete a file.
         *
         * @param pathname file path
         * @return 0 on success, -1 on error
         */
        int unlink(String pathname);

        /**
         * Create and open a POSIX shared memory object.
         *
         * @param name name of the shared memory object
         * @param oflag flags (O_RDWR | O_CREAT | O_EXCL)
         * @param mode permissions (e.g., 0600)
         * @return file descriptor, or -1 on error
         */
        int shm_open(String name, int oflag, int mode);

        /**
         * Remove a shared memory object.
         *
         * @param name name of the shared memory object
         * @return 0 on success, -1 on error
         */
        int shm_unlink(String name);

        /**
         * Truncate a file to a specified length.
         *
         * @param fd file descriptor
         * @param length new length
         * @return 0 on success, -1 on error
         */
        int ftruncate(int fd, long length);

        /**
         * Map memory.
         *
         * @param addr starting address (usually NULL)
         * @param length number of bytes to map
         * @param prot protection flags (PROT_READ | PROT_WRITE)
         * @param flags mapping flags (MAP_SHARED)
         * @param fd file descriptor
         * @param offset offset in the file
         * @return pointer to mapped memory, or MAP_FAILED on error
         */
        Pointer mmap(Pointer addr, long length, int prot, int flags, int fd, long offset);

        /**
         * Unmap memory.
         *
         * @param addr pointer to mapped memory
         * @param length number of bytes
         * @return 0 on success, -1 on error
         */
        int munmap(Pointer addr, long length);

        /**
         * Close a file descriptor.
         *
         * @param fd file descriptor
         * @return 0 on success, -1 on error
         */
        int close(int fd);

        /**
         * Get the last error number.
         *
         * @return errno value
         */
        int errno();
    }

    // Constants for shm_open
    public static final int O_RDONLY = 0;
    public static final int O_WRONLY = 1;
    public static final int O_RDWR = 2;
    public static final int O_CREAT = 0100;
    public static final int O_EXCL = 0200;
    public static final int O_TRUNC = 01000;

    // Constants for mmap
    public static final int PROT_NONE = 0;
    public static final int PROT_READ = 1;
    public static final int PROT_WRITE = 2;
    public static final int PROT_EXEC = 4;

    public static final int MAP_SHARED = 1;
    public static final int MAP_PRIVATE = 2;
    public static final int MAP_ANONYMOUS = 0x20;

    public static final Pointer MAP_FAILED = new Pointer(-1);

    /**
     * Create a shared memory buffer for Wayland.
     *
     * @param size size in bytes
     * @return file descriptor for the shared memory
     * @throws IOException if creation fails
     */
    /**
     * Thread-local storage for shared memory names so we can unlink them later.
     */
    private static final ThreadLocal<String> shmNames = new ThreadLocal<>();

    public static int createSharedMemoryFile(int size) throws IOException {
        // Try memfd_create first (Linux 3.17+), fallback to temp file
        try {
            // Try to use memfd_create for anonymous shared memory
            int fd = C_LIB.memfd_create("bongocat-buffer", 1); // 1 = MFD_CLOEXEC
            if (fd >= 0) {
                // Set the size
                if (C_LIB.ftruncate(fd, size) < 0) {
                    C_LIB.close(fd);
                    throw new IOException("Failed to set memfd size: ftruncate failed");
                }
                return fd;
            }
        } catch (UnsatisfiedLinkError e) {
            // memfd_create not available, fall through to temp file approach
        }

        // Fallback: use a temporary file
        String tmpDir = System.getProperty("java.io.tmpdir", "/tmp");
        String name = tmpDir + "/bongocat-" + System.currentTimeMillis() + "-" + Thread.currentThread().getId();

        // Create and open the file
        int fd = C_LIB.open(name, O_RDWR | O_CREAT | O_EXCL, 0600);
        if (fd < 0) {
            throw new IOException("Failed to create temp file: open returned " + fd);
        }

        // Unlink immediately so it's deleted when closed
        C_LIB.unlink(name);

        // Set the size
        if (C_LIB.ftruncate(fd, size) < 0) {
            C_LIB.close(fd);
            throw new IOException("Failed to set file size: ftruncate failed");
        }

        return fd;
    }

    /**
     * Map shared memory to a ByteBuffer.
     *
     * @param fd file descriptor
     * @param size size in bytes
     * @return ByteBuffer wrapping the mapped memory
     * @throws IOException if mapping fails
     */
    public static ByteBuffer mapMemory(int fd, int size) throws IOException {
        Pointer ptr = C_LIB.mmap(null, size, PROT_READ | PROT_WRITE, MAP_SHARED, fd, 0);

        if (ptr == null || ptr.equals(MAP_FAILED)) {
            throw new IOException("Failed to map memory: mmap returned null or MAP_FAILED");
        }

        return ptr.getByteBuffer(0, size);
    }

    /**
     * Unmap memory.
     *
     * @param buffer the ByteBuffer that was mapped
     * @param size size in bytes
     * @throws IOException if unmapping fails
     */
    public static void unmapMemory(ByteBuffer buffer, int size) throws IOException {
        if (buffer.isDirect()) {
            // Get the native pointer from the direct buffer
            Pointer ptr = Native.getDirectBufferPointer(buffer);
            if (ptr != null) {
                if (C_LIB.munmap(ptr, size) < 0) {
                    throw new IOException("Failed to unmap memory: munmap failed");
                }
            }
        }
    }

    /**
     * Close a file descriptor.
     *
     * @param fd file descriptor
     */
    public static void close(int fd) {
        if (fd >= 0) {
            C_LIB.close(fd);
        }
    }
}
