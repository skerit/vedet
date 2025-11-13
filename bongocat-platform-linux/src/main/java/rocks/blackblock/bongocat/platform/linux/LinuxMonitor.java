package rocks.blackblock.bongocat.platform.linux;

import com.sun.jna.Pointer;
import rocks.blackblock.bongocat.platform.Monitor;

/**
 * Linux implementation of Monitor interface (represents a Wayland output).
 */
public class LinuxMonitor implements Monitor {
    private final Pointer outputPointer;
    private String name;
    private int x;
    private int y;
    private int width;
    private int height;
    private int physicalWidth;
    private int physicalHeight;
    private double scale = 1.0;
    private Transform transform = Transform.NORMAL;
    private boolean primary = false;

    public LinuxMonitor(Pointer outputPointer) {
        this.outputPointer = outputPointer;
    }

    @Override
    public String getName() {
        return name != null ? name : "unknown";
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public int getY() {
        return y;
    }

    @Override
    public double getScale() {
        return scale;
    }

    @Override
    public Transform getTransform() {
        return transform;
    }

    @Override
    public boolean isPrimary() {
        return primary;
    }

    // Package-private setters for LinuxDisplayManager

    void setName(String name) {
        this.name = name;
    }

    void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    void setPhysicalSize(int physicalWidth, int physicalHeight) {
        this.physicalWidth = physicalWidth;
        this.physicalHeight = physicalHeight;
    }

    void setScale(double scale) {
        this.scale = scale;
    }

    void setTransform(Transform transform) {
        this.transform = transform;
    }

    void setPrimary(boolean primary) {
        this.primary = primary;
    }

    public Pointer getOutputPointer() {
        return outputPointer;
    }

    @Override
    public String toString() {
        return "LinuxMonitor{" +
                "name='" + name + '\'' +
                ", x=" + x +
                ", y=" + y +
                ", width=" + width +
                ", height=" + height +
                ", scale=" + scale +
                ", transform=" + transform +
                ", primary=" + primary +
                '}';
    }
}
