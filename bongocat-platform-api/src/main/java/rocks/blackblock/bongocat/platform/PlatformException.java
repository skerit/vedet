package rocks.blackblock.bongocat.platform;

/**
 * Exception thrown when platform operations fail.
 */
public class PlatformException extends Exception {
    public PlatformException(String message) {
        super(message);
    }

    public PlatformException(String message, Throwable cause) {
        super(message, cause);
    }

    public PlatformException(Throwable cause) {
        super(cause);
    }
}
