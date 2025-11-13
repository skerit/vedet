package rocks.blackblock.bongocat.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import rocks.blackblock.bongocat.platform.Position;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Application configuration model.
 * Uses Jackson for JSON/YAML serialization.
 */
public class Configuration {

    // Display settings
    @JsonProperty("monitor_name")
    private String monitorName;

    @JsonProperty("overlay_height")
    private int overlayHeight = 100;

    @JsonProperty("overlay_position")
    private Position overlayPosition = Position.TOP;

    @JsonProperty("overlay_opacity")
    private int overlayOpacity = 255; // 0-255

    @JsonProperty("layer")
    private LayerType layer = LayerType.OVERLAY;

    // Cat appearance
    @JsonProperty("cat_height")
    private int catHeight = 80;

    @JsonProperty("cat_x_offset")
    private int catXOffset = 0;

    @JsonProperty("cat_y_offset")
    private int catYOffset = 0;

    @JsonProperty("cat_align")
    private Alignment catAlign = Alignment.CENTER;

    @JsonProperty("mirror_horizontal")
    private boolean mirrorHorizontal = false;

    @JsonProperty("mirror_vertical")
    private boolean mirrorVertical = false;

    // Animation settings
    @JsonProperty("idle_frame")
    private int idleFrame = 0; // Which frame to show when idle

    @JsonProperty("keypress_duration_ms")
    private int keypressDurationMs = 100; // How long to show keypress animation

    @JsonProperty("fps")
    private int fps = 60;

    @JsonProperty("enable_antialiasing")
    private boolean enableAntialiasing = true; // Bilinear interpolation

    // Input devices
    @JsonProperty("keyboard_devices")
    private List<String> keyboardDevices = new ArrayList<>();

    // Test animation (for debugging)
    @JsonProperty("test_animation_duration_ms")
    private int testAnimationDurationMs = 0;

    @JsonProperty("test_animation_interval_ms")
    private int testAnimationIntervalMs = 0;

    // Sleep mode (deferred to v2, but keeping the fields)
    @JsonProperty("enable_scheduled_sleep")
    private boolean enableScheduledSleep = false;

    @JsonProperty("sleep_begin")
    private Time sleepBegin = new Time(0, 0);

    @JsonProperty("sleep_end")
    private Time sleepEnd = new Time(6, 0);

    @JsonProperty("idle_sleep_timeout_sec")
    private int idleSleepTimeoutSec = 0;

    // Debug
    @JsonProperty("debug")
    private boolean debug = false;

    // Asset paths (will be loaded from resources in Java)
    private transient List<Path> assetPaths = new ArrayList<>();

    public Configuration() {
        // Default constructor for Jackson
    }

    // Validation and bounds checking
    public void validate() throws IllegalArgumentException {
        if (overlayHeight < 10 || overlayHeight > 1000) {
            throw new IllegalArgumentException("overlay_height must be between 10 and 1000, got: " + overlayHeight);
        }

        if (catHeight < 10 || catHeight > overlayHeight) {
            throw new IllegalArgumentException("cat_height must be between 10 and overlay_height, got: " + catHeight);
        }

        if (overlayOpacity < 0 || overlayOpacity > 255) {
            throw new IllegalArgumentException("overlay_opacity must be between 0 and 255, got: " + overlayOpacity);
        }

        if (fps < 1 || fps > 144) {
            throw new IllegalArgumentException("fps must be between 1 and 144, got: " + fps);
        }

        if (idleFrame < 0 || idleFrame >= 4) {
            throw new IllegalArgumentException("idle_frame must be between 0 and 3, got: " + idleFrame);
        }

        if (keypressDurationMs < 10 || keypressDurationMs > 5000) {
            throw new IllegalArgumentException("keypress_duration_ms must be between 10 and 5000, got: " + keypressDurationMs);
        }

        // keyboard_devices is optional - will auto-detect if empty
    }

    // Getters and setters

    public String getMonitorName() {
        return monitorName;
    }

    public void setMonitorName(String monitorName) {
        this.monitorName = monitorName;
    }

    public int getOverlayHeight() {
        return overlayHeight;
    }

    public void setOverlayHeight(int overlayHeight) {
        this.overlayHeight = overlayHeight;
    }

    public Position getOverlayPosition() {
        return overlayPosition;
    }

    public void setOverlayPosition(Position overlayPosition) {
        this.overlayPosition = overlayPosition;
    }

    public int getOverlayOpacity() {
        return overlayOpacity;
    }

    public void setOverlayOpacity(int overlayOpacity) {
        this.overlayOpacity = overlayOpacity;
    }

    public LayerType getLayer() {
        return layer;
    }

    public void setLayer(LayerType layer) {
        this.layer = layer;
    }

    public int getCatHeight() {
        return catHeight;
    }

    public void setCatHeight(int catHeight) {
        this.catHeight = catHeight;
    }

    public int getCatXOffset() {
        return catXOffset;
    }

    public void setCatXOffset(int catXOffset) {
        this.catXOffset = catXOffset;
    }

    public int getCatYOffset() {
        return catYOffset;
    }

    public void setCatYOffset(int catYOffset) {
        this.catYOffset = catYOffset;
    }

    public Alignment getCatAlign() {
        return catAlign;
    }

    public void setCatAlign(Alignment catAlign) {
        this.catAlign = catAlign;
    }

    public boolean isMirrorHorizontal() {
        return mirrorHorizontal;
    }

    public void setMirrorHorizontal(boolean mirrorHorizontal) {
        this.mirrorHorizontal = mirrorHorizontal;
    }

    public boolean isMirrorVertical() {
        return mirrorVertical;
    }

    public void setMirrorVertical(boolean mirrorVertical) {
        this.mirrorVertical = mirrorVertical;
    }

    public int getIdleFrame() {
        return idleFrame;
    }

    public void setIdleFrame(int idleFrame) {
        this.idleFrame = idleFrame;
    }

    public int getKeypressDurationMs() {
        return keypressDurationMs;
    }

    public void setKeypressDurationMs(int keypressDurationMs) {
        this.keypressDurationMs = keypressDurationMs;
    }

    public int getFps() {
        return fps;
    }

    public void setFps(int fps) {
        this.fps = fps;
    }

    public boolean isEnableAntialiasing() {
        return enableAntialiasing;
    }

    public void setEnableAntialiasing(boolean enableAntialiasing) {
        this.enableAntialiasing = enableAntialiasing;
    }

    public List<String> getKeyboardDevices() {
        return keyboardDevices;
    }

    public void setKeyboardDevices(List<String> keyboardDevices) {
        this.keyboardDevices = keyboardDevices;
    }

    public int getTestAnimationDurationMs() {
        return testAnimationDurationMs;
    }

    public void setTestAnimationDurationMs(int testAnimationDurationMs) {
        this.testAnimationDurationMs = testAnimationDurationMs;
    }

    public int getTestAnimationIntervalMs() {
        return testAnimationIntervalMs;
    }

    public void setTestAnimationIntervalMs(int testAnimationIntervalMs) {
        this.testAnimationIntervalMs = testAnimationIntervalMs;
    }

    public boolean isEnableScheduledSleep() {
        return enableScheduledSleep;
    }

    public void setEnableScheduledSleep(boolean enableScheduledSleep) {
        this.enableScheduledSleep = enableScheduledSleep;
    }

    public Time getSleepBegin() {
        return sleepBegin;
    }

    public void setSleepBegin(Time sleepBegin) {
        this.sleepBegin = sleepBegin;
    }

    public Time getSleepEnd() {
        return sleepEnd;
    }

    public void setSleepEnd(Time sleepEnd) {
        this.sleepEnd = sleepEnd;
    }

    public int getIdleSleepTimeoutSec() {
        return idleSleepTimeoutSec;
    }

    public void setIdleSleepTimeoutSec(int idleSleepTimeoutSec) {
        this.idleSleepTimeoutSec = idleSleepTimeoutSec;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public List<Path> getAssetPaths() {
        return assetPaths;
    }

    public void setAssetPaths(List<Path> assetPaths) {
        this.assetPaths = assetPaths;
    }

    // Enums

    public enum LayerType {
        @JsonProperty("top")
        TOP,
        @JsonProperty("overlay")
        OVERLAY
    }

    public enum Alignment {
        @JsonProperty("left")
        LEFT(-1),
        @JsonProperty("center")
        CENTER(0),
        @JsonProperty("right")
        RIGHT(1);

        private final int value;

        Alignment(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    @Override
    public String toString() {
        return "Configuration{" +
                "monitorName='" + monitorName + '\'' +
                ", overlayHeight=" + overlayHeight +
                ", overlayPosition=" + overlayPosition +
                ", catHeight=" + catHeight +
                ", fps=" + fps +
                ", keyboardDevices=" + keyboardDevices +
                '}';
    }
}
