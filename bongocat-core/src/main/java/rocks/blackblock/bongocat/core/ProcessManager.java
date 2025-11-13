package rocks.blackblock.bongocat.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * Manages process lifecycle including PID file and single-instance enforcement.
 */
public class ProcessManager implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(ProcessManager.class);

    private static final String PID_FILE_NAME = "bongocat.pid";

    private final Path pidFilePath;
    private FileChannel pidFileChannel;
    private FileLock pidFileLock;

    public ProcessManager() {
        this(getDefaultPidFilePath());
    }

    public ProcessManager(Path pidFilePath) {
        this.pidFilePath = pidFilePath;
    }

    /**
     * Get the default PID file path.
     * Uses /tmp/bongocat.pid on Unix systems, or user temp directory on others.
     */
    private static Path getDefaultPidFilePath() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            return Paths.get("/tmp", PID_FILE_NAME);
        } else {
            return Paths.get(System.getProperty("java.io.tmpdir"), PID_FILE_NAME);
        }
    }

    /**
     * Create PID file and acquire exclusive lock.
     * This ensures only one instance of the application can run.
     *
     * @return true if lock was acquired, false if another instance is running
     * @throws IOException if an error occurs
     */
    public boolean createPidFile() throws IOException {
        try {
            // Create or truncate the PID file
            pidFileChannel = FileChannel.open(
                pidFilePath,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING
            );

            // Try to acquire an exclusive lock (non-blocking)
            try {
                pidFileLock = pidFileChannel.tryLock();
            } catch (OverlappingFileLockException e) {
                // Another thread in this JVM has the lock
                logger.info("Another instance is already running (overlapping lock)");
                pidFileChannel.close();
                return false;
            }

            if (pidFileLock == null) {
                // Another process has the lock
                logger.info("Another instance is already running");
                pidFileChannel.close();
                return false;
            }

            // Write our PID to the file
            long pid = ProcessHandle.current().pid();
            String pidString = pid + "\n";
            ByteBuffer buffer = ByteBuffer.wrap(pidString.getBytes(StandardCharsets.UTF_8));
            pidFileChannel.write(buffer);
            pidFileChannel.force(true);

            logger.info("PID file created: {} (PID: {})", pidFilePath, pid);
            return true;

        } catch (IOException e) {
            logger.error("Failed to create PID file: {}", e.getMessage());
            if (pidFileChannel != null) {
                try {
                    pidFileChannel.close();
                } catch (IOException ex) {
                    // Ignore
                }
            }
            throw e;
        }
    }

    /**
     * Get the PID of the currently running instance (if any).
     *
     * @return PID of running instance, or -1 if no instance is running
     */
    public long getRunningPid() {
        if (!Files.exists(pidFilePath)) {
            return -1;
        }

        try {
            // Try to read the PID file
            String content = Files.readString(pidFilePath, StandardCharsets.UTF_8).trim();
            return Long.parseLong(content);
        } catch (IOException | NumberFormatException e) {
            logger.debug("Failed to read PID file: {}", e.getMessage());
            return -1;
        }
    }

    /**
     * Check if another instance is currently running.
     *
     * @return true if another instance is running
     */
    public boolean isRunning() {
        if (!Files.exists(pidFilePath)) {
            return false;
        }

        try (FileChannel channel = FileChannel.open(pidFilePath, StandardOpenOption.READ)) {
            try (FileLock lock = channel.tryLock(0, Long.MAX_VALUE, true)) {
                // We got a shared lock, which means no exclusive lock exists
                // So no instance is running
                return false;
            } catch (OverlappingFileLockException e) {
                // Shouldn't happen with shared lock
                return false;
            }
        } catch (IOException e) {
            // If we can't lock it, assume it's locked by another process
            logger.debug("Failed to check if running: {}", e.getMessage());
            return true;
        }
    }

    /**
     * Send a termination signal to the running instance.
     * On Unix, this sends SIGTERM to the process.
     *
     * @return true if signal was sent successfully
     */
    public boolean terminateRunningInstance() {
        long pid = getRunningPid();
        if (pid <= 0) {
            logger.warn("No running instance found");
            return false;
        }

        try {
            ProcessHandle handle = ProcessHandle.of(pid).orElse(null);
            if (handle == null) {
                logger.warn("Process {} not found", pid);
                return false;
            }

            if (!handle.isAlive()) {
                logger.info("Process {} is not alive, cleaning up stale PID file", pid);
                removePidFile();
                return false;
            }

            logger.info("Terminating process {}", pid);
            boolean destroyed = handle.destroy();

            if (destroyed) {
                // Wait a bit for the process to terminate
                for (int i = 0; i < 10; i++) {
                    if (!handle.isAlive()) {
                        logger.info("Process {} terminated", pid);
                        return true;
                    }
                    Thread.sleep(100);
                }

                // If still alive, try force destroy
                logger.warn("Process {} did not terminate gracefully, forcing", pid);
                handle.destroyForcibly();
            }

            return destroyed;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted while waiting for process termination");
            return false;
        } catch (Exception e) {
            logger.error("Failed to terminate process {}: {}", pid, e.getMessage());
            return false;
        }
    }

    /**
     * Remove the PID file.
     * This is called automatically when closing, but can be called manually if needed.
     */
    public void removePidFile() {
        try {
            if (Files.exists(pidFilePath)) {
                Files.delete(pidFilePath);
                logger.debug("PID file removed: {}", pidFilePath);
            }
        } catch (IOException e) {
            logger.warn("Failed to remove PID file: {}", e.getMessage());
        }
    }

    /**
     * Close the process manager and release the PID file lock.
     */
    @Override
    public void close() {
        // Release the lock
        if (pidFileLock != null) {
            try {
                pidFileLock.release();
                logger.debug("PID file lock released");
            } catch (IOException e) {
                logger.warn("Failed to release PID file lock: {}", e.getMessage());
            }
            pidFileLock = null;
        }

        // Close the channel
        if (pidFileChannel != null) {
            try {
                pidFileChannel.close();
            } catch (IOException e) {
                logger.warn("Failed to close PID file channel: {}", e.getMessage());
            }
            pidFileChannel = null;
        }

        // Remove the PID file
        removePidFile();
    }

    public Path getPidFilePath() {
        return pidFilePath;
    }
}
