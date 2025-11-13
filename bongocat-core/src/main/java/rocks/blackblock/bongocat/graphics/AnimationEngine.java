package rocks.blackblock.bongocat.graphics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rocks.blackblock.bongocat.config.Configuration;
import rocks.blackblock.bongocat.platform.InputEvent;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Animation state machine for bongo cat.
 *
 * Manages 4 animation frames:
 * - Frame 0: Idle (no keys pressed)
 * - Frame 1: Left hand active
 * - Frame 2: Right hand active
 * - Frame 3: Both hands active
 *
 * Transitions between frames based on keyboard input events.
 */
public class AnimationEngine {
    private static final Logger logger = LoggerFactory.getLogger(AnimationEngine.class);

    // Animation frames (original size)
    private final List<BufferedImage> frames;

    // Scaled frames (ready to render)
    private List<BufferedImage> scaledFrames;

    // Configuration
    private final Configuration config;

    // Current animation state
    private final AtomicInteger currentFrame = new AtomicInteger(0);
    private final AtomicLong lastLeftHandPress = new AtomicLong(0);
    private final AtomicLong lastRightHandPress = new AtomicLong(0);

    // Dimensions
    private int targetWidth;
    private int targetHeight;
    private boolean needsScaling = false;

    public AnimationEngine(List<BufferedImage> frames, Configuration config) {
        if (frames.size() != 4) {
            throw new IllegalArgumentException("Animation requires exactly 4 frames, got: " + frames.size());
        }

        this.frames = frames;
        this.config = config;
        this.scaledFrames = frames; // Initially use original frames

        // Set to idle frame
        currentFrame.set(config.getIdleFrame());

        logger.info("Animation engine initialized with {} frames", frames.size());
    }

    /**
     * Set target dimensions for scaling.
     * This will scale all frames to fit within the target height while maintaining aspect ratio.
     *
     * @param width target width (overlay width)
     * @param height target height (overlay height)
     */
    public void setTargetDimensions(int width, int height) {
        this.targetWidth = width;
        this.targetHeight = height;

        // Calculate scaled dimensions
        int[] originalDims = AssetLoader.getFrameDimensions(frames);
        int originalWidth = originalDims[0];
        int originalHeight = originalDims[1];

        // Scale to fit within cat_height (from config)
        int catHeight = config.getCatHeight();
        int[] scaledDims = ImageScaler.calculateFitDimensions(
            originalWidth, originalHeight, width, catHeight
        );

        int scaledWidth = scaledDims[0];
        int scaledHeight = scaledDims[1];

        logger.info("Scaling frames from {}x{} to {}x{}",
                   originalWidth, originalHeight, scaledWidth, scaledHeight);

        // Scale all frames
        this.scaledFrames = new java.util.ArrayList<>();
        boolean antiAlias = config.isEnableAntialiasing();

        for (int i = 0; i < frames.size(); i++) {
            BufferedImage scaled;
            if (antiAlias) {
                scaled = ImageScaler.scaleBilinear(frames.get(i), scaledWidth, scaledHeight);
            } else {
                scaled = ImageScaler.scaleNearestNeighbor(frames.get(i), scaledWidth, scaledHeight);
            }
            scaledFrames.add(scaled);
            logger.debug("Scaled frame {}: {}x{}", i, scaled.getWidth(), scaled.getHeight());
        }

        needsScaling = false;
        logger.info("All frames scaled successfully");
    }

    /**
     * Handle an input event and update animation state.
     *
     * @param event input event
     */
    public void handleInput(InputEvent event) {
        if (event.getType() != InputEvent.Type.KEY_PRESS) {
            return; // Only respond to key presses
        }

        long now = System.currentTimeMillis();

        boolean leftHand = event.isLeftHandKey();
        boolean rightHand = event.isRightHandKey();

        if (leftHand) {
            lastLeftHandPress.set(now);
            logger.trace("Left hand key pressed");
        }

        if (rightHand) {
            lastRightHandPress.set(now);
            logger.trace("Right hand key pressed");
        }

        updateAnimationState(now);
    }

    /**
     * Update animation state based on recent key presses.
     * Should be called periodically to handle key release timing.
     *
     * @param currentTime current time in milliseconds
     */
    public void updateAnimationState(long currentTime) {
        long keypressDuration = config.getKeypressDurationMs();

        boolean leftActive = (currentTime - lastLeftHandPress.get()) < keypressDuration;
        boolean rightActive = (currentTime - lastRightHandPress.get()) < keypressDuration;

        int newFrame;
        if (leftActive && rightActive) {
            newFrame = 3; // Both hands
        } else if (leftActive) {
            newFrame = 1; // Left hand
        } else if (rightActive) {
            newFrame = 2; // Right hand
        } else {
            newFrame = config.getIdleFrame(); // Idle
        }

        int oldFrame = currentFrame.getAndSet(newFrame);
        if (oldFrame != newFrame) {
            logger.debug("Animation state changed: frame {} -> {}", oldFrame, newFrame);
        }
    }

    /**
     * Get the current frame to render.
     *
     * @return current frame image (scaled)
     */
    public BufferedImage getCurrentFrame() {
        int frameIndex = currentFrame.get();
        return scaledFrames.get(frameIndex);
    }

    /**
     * Get the current frame index.
     *
     * @return frame index (0-3)
     */
    public int getCurrentFrameIndex() {
        return currentFrame.get();
    }

    /**
     * Force update the animation state.
     * Useful for periodic updates when no input is happening.
     */
    public void update() {
        updateAnimationState(System.currentTimeMillis());
    }

    /**
     * Reset animation to idle state.
     */
    public void reset() {
        currentFrame.set(config.getIdleFrame());
        lastLeftHandPress.set(0);
        lastRightHandPress.set(0);
        logger.debug("Animation reset to idle");
    }

    /**
     * Get the width of the current (scaled) frame.
     */
    public int getFrameWidth() {
        return scaledFrames.get(0).getWidth();
    }

    /**
     * Get the height of the current (scaled) frame.
     */
    public int getFrameHeight() {
        return scaledFrames.get(0).getHeight();
    }

    /**
     * Get the X offset for rendering (based on alignment).
     *
     * @param surfaceWidth width of the surface
     * @return X offset
     */
    public int getRenderX(int surfaceWidth) {
        int frameWidth = getFrameWidth();
        int baseX = config.getCatXOffset();

        switch (config.getCatAlign()) {
            case LEFT:
                return baseX;
            case RIGHT:
                return surfaceWidth - frameWidth + baseX;
            case CENTER:
            default:
                return (surfaceWidth - frameWidth) / 2 + baseX;
        }
    }

    /**
     * Get the Y offset for rendering.
     *
     * @return Y offset
     */
    public int getRenderY() {
        return config.getCatYOffset();
    }

    /**
     * Check if the animation is currently in idle state.
     */
    public boolean isIdle() {
        return currentFrame.get() == config.getIdleFrame();
    }
}
