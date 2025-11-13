package rocks.blackblock.bongocat.graphics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads animation frames from resources or file system.
 *
 * Supports PNG, JPEG, and other formats supported by ImageIO.
 */
public class AssetLoader {
    private static final Logger logger = LoggerFactory.getLogger(AssetLoader.class);

    /**
     * Load animation frames from resources.
     *
     * @param resourcePrefix resource path prefix (e.g., "animations/bongocat/frame")
     * @param frameCount number of frames to load
     * @param extension file extension (e.g., "png")
     * @return list of loaded frames
     * @throws IOException if loading fails
     */
    public static List<BufferedImage> loadFrames(String resourcePrefix, int frameCount, String extension)
            throws IOException {
        List<BufferedImage> frames = new ArrayList<>();

        for (int i = 0; i < frameCount; i++) {
            String resourcePath = resourcePrefix + i + "." + extension;
            BufferedImage frame = loadFromResource(resourcePath);
            frames.add(frame);
            logger.debug("Loaded frame {}: {} ({}x{})", i, resourcePath,
                        frame.getWidth(), frame.getHeight());
        }

        logger.info("Loaded {} animation frames from {}", frameCount, resourcePrefix);
        return frames;
    }

    /**
     * Load a single image from classpath resources.
     *
     * @param resourcePath path to resource (e.g., "animations/bongocat/frame0.png")
     * @return loaded image
     * @throws IOException if loading fails
     */
    public static BufferedImage loadFromResource(String resourcePath) throws IOException {
        // Try with leading slash
        InputStream is = AssetLoader.class.getClassLoader().getResourceAsStream(resourcePath);

        if (is == null) {
            // Try without leading slash
            is = AssetLoader.class.getResourceAsStream("/" + resourcePath);
        }

        if (is == null) {
            throw new IOException("Resource not found: " + resourcePath);
        }

        try (InputStream stream = is) {
            BufferedImage image = ImageIO.read(stream);
            if (image == null) {
                throw new IOException("Failed to decode image: " + resourcePath);
            }
            return image;
        }
    }

    /**
     * Create default frames for testing (simple colored rectangles).
     *
     * @param width frame width
     * @param height frame height
     * @param frameCount number of frames
     * @return list of test frames
     */
    public static List<BufferedImage> createTestFrames(int width, int height, int frameCount) {
        List<BufferedImage> frames = new ArrayList<>();

        int[] colors = {
            0xFF3498db, // Blue
            0xFF2ecc71, // Green
            0xFFf39c12, // Orange
            0xFFe74c3c  // Red
        };

        for (int i = 0; i < frameCount; i++) {
            BufferedImage frame = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            int color = colors[i % colors.length];

            // Fill with solid color
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    frame.setRGB(x, y, color);
                }
            }

            frames.add(frame);
            logger.debug("Created test frame {}: {}x{}, color=0x{}",
                        i, width, height, Integer.toHexString(color));
        }

        logger.info("Created {} test frames ({}x{})", frameCount, width, height);
        return frames;
    }

    /**
     * Get frame dimensions from a list of frames.
     * Assumes all frames have the same dimensions.
     *
     * @param frames list of frames
     * @return array [width, height]
     * @throws IllegalArgumentException if frames list is empty
     */
    public static int[] getFrameDimensions(List<BufferedImage> frames) {
        if (frames.isEmpty()) {
            throw new IllegalArgumentException("Frames list is empty");
        }

        BufferedImage firstFrame = frames.get(0);
        return new int[]{firstFrame.getWidth(), firstFrame.getHeight()};
    }

    /**
     * Validate that all frames have the same dimensions.
     *
     * @param frames list of frames to validate
     * @throws IllegalArgumentException if frames have different dimensions
     */
    public static void validateFrameDimensions(List<BufferedImage> frames) {
        if (frames.isEmpty()) {
            return;
        }

        int width = frames.get(0).getWidth();
        int height = frames.get(0).getHeight();

        for (int i = 1; i < frames.size(); i++) {
            BufferedImage frame = frames.get(i);
            if (frame.getWidth() != width || frame.getHeight() != height) {
                throw new IllegalArgumentException(
                    String.format("Frame %d has different dimensions: %dx%d (expected %dx%d)",
                                i, frame.getWidth(), frame.getHeight(), width, height)
                );
            }
        }
    }
}
