package rocks.blackblock.bongocat.graphics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.nio.ByteBuffer;

/**
 * Renders BufferedImage frames to pixel buffers.
 *
 * Handles conversion from BufferedImage to raw ARGB8888 format
 * suitable for Wayland shared memory buffers.
 */
public class FrameRenderer {
    private static final Logger logger = LoggerFactory.getLogger(FrameRenderer.class);

    /**
     * Clear a pixel buffer with a solid color.
     *
     * @param buffer pixel buffer (ARGB8888 format)
     * @param width buffer width
     * @param height buffer height
     * @param argb color in ARGB format (0xAARRGGBB)
     */
    public static void clear(ByteBuffer buffer, int width, int height, int argb) {
        int totalPixels = width * height;

        // Convert ARGB to bytes (little-endian)
        byte b = (byte) (argb & 0xFF);
        byte g = (byte) ((argb >> 8) & 0xFF);
        byte r = (byte) ((argb >> 16) & 0xFF);
        byte a = (byte) ((argb >> 24) & 0xFF);

        buffer.rewind();
        for (int i = 0; i < totalPixels; i++) {
            buffer.put(b);
            buffer.put(g);
            buffer.put(r);
            buffer.put(a);
        }
    }

    /**
     * Render an image to a pixel buffer at specified position.
     *
     * @param buffer target pixel buffer (ARGB8888 format)
     * @param bufferWidth buffer width
     * @param bufferHeight buffer height
     * @param image source image to render
     * @param x X position in buffer
     * @param y Y position in buffer
     */
    public static void renderImage(ByteBuffer buffer, int bufferWidth, int bufferHeight,
                                   BufferedImage image, int x, int y) {
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();

        // Clipping
        int startX = Math.max(0, x);
        int startY = Math.max(0, y);
        int endX = Math.min(bufferWidth, x + imageWidth);
        int endY = Math.min(bufferHeight, y + imageHeight);

        if (startX >= endX || startY >= endY) {
            return; // Completely outside buffer
        }

        // Get pixel data from BufferedImage
        int[] pixels = getPixels(image);

        // Render to buffer
        for (int row = startY; row < endY; row++) {
            for (int col = startX; col < endX; col++) {
                int imgX = col - x;
                int imgY = row - y;

                if (imgX >= 0 && imgX < imageWidth && imgY >= 0 && imgY < imageHeight) {
                    int argb = pixels[imgY * imageWidth + imgX];

                    // Extract ARGB components
                    int a = (argb >> 24) & 0xFF;
                    int r = (argb >> 16) & 0xFF;
                    int g = (argb >> 8) & 0xFF;
                    int b = argb & 0xFF;

                    // Write to buffer (ARGB8888 format, little-endian)
                    int bufferPos = (row * bufferWidth + col) * 4;
                    buffer.put(bufferPos, (byte) b);
                    buffer.put(bufferPos + 1, (byte) g);
                    buffer.put(bufferPos + 2, (byte) r);
                    buffer.put(bufferPos + 3, (byte) a);
                }
            }
        }
    }

    /**
     * Render an image centered horizontally in the buffer.
     *
     * @param buffer target pixel buffer
     * @param bufferWidth buffer width
     * @param bufferHeight buffer height
     * @param image source image
     * @param y Y position
     */
    public static void renderCentered(ByteBuffer buffer, int bufferWidth, int bufferHeight,
                                     BufferedImage image, int y) {
        int x = (bufferWidth - image.getWidth()) / 2;
        renderImage(buffer, bufferWidth, bufferHeight, image, x, y);
    }

    /**
     * Render an image with alpha blending.
     * This blends the image with the existing buffer content.
     *
     * @param buffer target pixel buffer
     * @param bufferWidth buffer width
     * @param bufferHeight buffer height
     * @param image source image
     * @param x X position
     * @param y Y position
     */
    public static void renderImageAlphaBlend(ByteBuffer buffer, int bufferWidth, int bufferHeight,
                                            BufferedImage image, int x, int y) {
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();

        // Clipping
        int startX = Math.max(0, x);
        int startY = Math.max(0, y);
        int endX = Math.min(bufferWidth, x + imageWidth);
        int endY = Math.min(bufferHeight, y + imageHeight);

        if (startX >= endX || startY >= endY) {
            return;
        }

        int[] pixels = getPixels(image);

        for (int row = startY; row < endY; row++) {
            for (int col = startX; col < endX; col++) {
                int imgX = col - x;
                int imgY = row - y;

                if (imgX >= 0 && imgX < imageWidth && imgY >= 0 && imgY < imageHeight) {
                    int argb = pixels[imgY * imageWidth + imgX];

                    int srcA = (argb >> 24) & 0xFF;
                    int srcR = (argb >> 16) & 0xFF;
                    int srcG = (argb >> 8) & 0xFF;
                    int srcB = argb & 0xFF;

                    if (srcA == 0) {
                        continue; // Fully transparent
                    }

                    int bufferPos = (row * bufferWidth + col) * 4;

                    if (srcA == 255) {
                        // Fully opaque - no blending needed
                        buffer.put(bufferPos, (byte) srcB);
                        buffer.put(bufferPos + 1, (byte) srcG);
                        buffer.put(bufferPos + 2, (byte) srcR);
                        buffer.put(bufferPos + 3, (byte) srcA);
                    } else {
                        // Alpha blending
                        int dstB = buffer.get(bufferPos) & 0xFF;
                        int dstG = buffer.get(bufferPos + 1) & 0xFF;
                        int dstR = buffer.get(bufferPos + 2) & 0xFF;
                        int dstA = buffer.get(bufferPos + 3) & 0xFF;

                        float alpha = srcA / 255f;
                        float invAlpha = 1f - alpha;

                        int outR = (int) (srcR * alpha + dstR * invAlpha);
                        int outG = (int) (srcG * alpha + dstG * invAlpha);
                        int outB = (int) (srcB * alpha + dstB * invAlpha);
                        int outA = Math.max(srcA, dstA);

                        buffer.put(bufferPos, (byte) outB);
                        buffer.put(bufferPos + 1, (byte) outG);
                        buffer.put(bufferPos + 2, (byte) outR);
                        buffer.put(bufferPos + 3, (byte) outA);
                    }
                }
            }
        }
    }

    /**
     * Get pixel array from BufferedImage.
     * Converts to TYPE_INT_ARGB if necessary.
     *
     * @param image source image
     * @return pixel array in ARGB format
     */
    private static int[] getPixels(BufferedImage image) {
        if (image.getType() == BufferedImage.TYPE_INT_ARGB) {
            // Fast path - direct access
            DataBufferInt dataBuffer = (DataBufferInt) image.getRaster().getDataBuffer();
            return dataBuffer.getData();
        } else {
            // Slow path - convert to ARGB
            int width = image.getWidth();
            int height = image.getHeight();
            int[] pixels = new int[width * height];
            image.getRGB(0, 0, width, height, pixels, 0, width);
            return pixels;
        }
    }

    /**
     * Fill a rectangle in the buffer with a solid color.
     *
     * @param buffer pixel buffer
     * @param bufferWidth buffer width
     * @param bufferHeight buffer height
     * @param x rectangle X position
     * @param y rectangle Y position
     * @param width rectangle width
     * @param height rectangle height
     * @param argb color in ARGB format
     */
    public static void fillRect(ByteBuffer buffer, int bufferWidth, int bufferHeight,
                               int x, int y, int width, int height, int argb) {
        // Clipping
        int startX = Math.max(0, x);
        int startY = Math.max(0, y);
        int endX = Math.min(bufferWidth, x + width);
        int endY = Math.min(bufferHeight, y + height);

        if (startX >= endX || startY >= endY) {
            return;
        }

        byte b = (byte) (argb & 0xFF);
        byte g = (byte) ((argb >> 8) & 0xFF);
        byte r = (byte) ((argb >> 16) & 0xFF);
        byte a = (byte) ((argb >> 24) & 0xFF);

        for (int row = startY; row < endY; row++) {
            for (int col = startX; col < endX; col++) {
                int bufferPos = (row * bufferWidth + col) * 4;
                buffer.put(bufferPos, b);
                buffer.put(bufferPos + 1, g);
                buffer.put(bufferPos + 2, r);
                buffer.put(bufferPos + 3, a);
            }
        }
    }
}
