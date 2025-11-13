package rocks.blackblock.bongocat.graphics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;

/**
 * Scales images using different interpolation algorithms.
 *
 * Supports:
 * - Bilinear interpolation (smooth, anti-aliased)
 * - Nearest neighbor (fast, pixelated)
 */
public class ImageScaler {
    private static final Logger logger = LoggerFactory.getLogger(ImageScaler.class);

    /**
     * Scale an image using bilinear interpolation.
     *
     * @param source source image
     * @param targetWidth target width
     * @param targetHeight target height
     * @return scaled image
     */
    public static BufferedImage scaleBilinear(BufferedImage source, int targetWidth, int targetHeight) {
        if (source.getWidth() == targetWidth && source.getHeight() == targetHeight) {
            return source; // No scaling needed
        }

        logger.debug("Scaling image from {}x{} to {}x{} (bilinear)",
                    source.getWidth(), source.getHeight(), targetWidth, targetHeight);

        BufferedImage result = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);

        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();

        double xRatio = (double) sourceWidth / targetWidth;
        double yRatio = (double) sourceHeight / targetHeight;

        for (int y = 0; y < targetHeight; y++) {
            for (int x = 0; x < targetWidth; x++) {
                // Calculate source coordinates
                double srcX = x * xRatio;
                double srcY = y * yRatio;

                // Get integer and fractional parts
                int x1 = (int) srcX;
                int y1 = (int) srcY;
                int x2 = Math.min(x1 + 1, sourceWidth - 1);
                int y2 = Math.min(y1 + 1, sourceHeight - 1);

                double xFrac = srcX - x1;
                double yFrac = srcY - y1;

                // Get four neighboring pixels
                int p11 = source.getRGB(x1, y1);
                int p21 = source.getRGB(x2, y1);
                int p12 = source.getRGB(x1, y2);
                int p22 = source.getRGB(x2, y2);

                // Bilinear interpolation for each color channel
                int a = interpolateChannel(p11, p21, p12, p22, xFrac, yFrac, 24);
                int r = interpolateChannel(p11, p21, p12, p22, xFrac, yFrac, 16);
                int g = interpolateChannel(p11, p21, p12, p22, xFrac, yFrac, 8);
                int b = interpolateChannel(p11, p21, p12, p22, xFrac, yFrac, 0);

                // Combine channels
                int rgb = (a << 24) | (r << 16) | (g << 8) | b;
                result.setRGB(x, y, rgb);
            }
        }

        return result;
    }

    /**
     * Interpolate a single color channel using bilinear interpolation.
     *
     * @param p11 top-left pixel
     * @param p21 top-right pixel
     * @param p12 bottom-left pixel
     * @param p22 bottom-right pixel
     * @param xFrac horizontal fraction (0.0 - 1.0)
     * @param yFrac vertical fraction (0.0 - 1.0)
     * @param shift bit shift for the channel (24 for alpha, 16 for red, 8 for green, 0 for blue)
     * @return interpolated channel value (0-255)
     */
    private static int interpolateChannel(int p11, int p21, int p12, int p22,
                                         double xFrac, double yFrac, int shift) {
        // Extract channel values
        int c11 = (p11 >> shift) & 0xFF;
        int c21 = (p21 >> shift) & 0xFF;
        int c12 = (p12 >> shift) & 0xFF;
        int c22 = (p22 >> shift) & 0xFF;

        // Interpolate horizontally
        double top = c11 * (1 - xFrac) + c21 * xFrac;
        double bottom = c12 * (1 - xFrac) + c22 * xFrac;

        // Interpolate vertically
        double result = top * (1 - yFrac) + bottom * yFrac;

        return (int) Math.round(result);
    }

    /**
     * Scale an image using nearest neighbor interpolation.
     * Fast but produces pixelated results.
     *
     * @param source source image
     * @param targetWidth target width
     * @param targetHeight target height
     * @return scaled image
     */
    public static BufferedImage scaleNearestNeighbor(BufferedImage source, int targetWidth, int targetHeight) {
        if (source.getWidth() == targetWidth && source.getHeight() == targetHeight) {
            return source; // No scaling needed
        }

        logger.debug("Scaling image from {}x{} to {}x{} (nearest neighbor)",
                    source.getWidth(), source.getHeight(), targetWidth, targetHeight);

        BufferedImage result = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);

        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();

        double xRatio = (double) sourceWidth / targetWidth;
        double yRatio = (double) sourceHeight / targetHeight;

        for (int y = 0; y < targetHeight; y++) {
            for (int x = 0; x < targetWidth; x++) {
                int srcX = (int) (x * xRatio);
                int srcY = (int) (y * yRatio);
                result.setRGB(x, y, source.getRGB(srcX, srcY));
            }
        }

        return result;
    }

    /**
     * Scale image to fit within target dimensions while maintaining aspect ratio.
     *
     * @param source source image
     * @param maxWidth maximum width
     * @param maxHeight maximum height
     * @param antiAlias true for bilinear, false for nearest neighbor
     * @return scaled image
     */
    public static BufferedImage scaleToFit(BufferedImage source, int maxWidth, int maxHeight, boolean antiAlias) {
        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();

        // Calculate scaling factor to fit within bounds
        double widthRatio = (double) maxWidth / sourceWidth;
        double heightRatio = (double) maxHeight / sourceHeight;
        double scale = Math.min(widthRatio, heightRatio);

        int targetWidth = (int) Math.round(sourceWidth * scale);
        int targetHeight = (int) Math.round(sourceHeight * scale);

        logger.debug("Scaling to fit: {}x{} -> {}x{} (scale={})",
                    sourceWidth, sourceHeight, targetWidth, targetHeight, scale);

        if (antiAlias) {
            return scaleBilinear(source, targetWidth, targetHeight);
        } else {
            return scaleNearestNeighbor(source, targetWidth, targetHeight);
        }
    }

    /**
     * Calculate dimensions to fit source within target while maintaining aspect ratio.
     *
     * @param sourceWidth source width
     * @param sourceHeight source height
     * @param maxWidth maximum width
     * @param maxHeight maximum height
     * @return array [width, height]
     */
    public static int[] calculateFitDimensions(int sourceWidth, int sourceHeight, int maxWidth, int maxHeight) {
        double widthRatio = (double) maxWidth / sourceWidth;
        double heightRatio = (double) maxHeight / sourceHeight;
        double scale = Math.min(widthRatio, heightRatio);

        int targetWidth = (int) Math.round(sourceWidth * scale);
        int targetHeight = (int) Math.round(sourceHeight * scale);

        return new int[]{targetWidth, targetHeight};
    }
}
