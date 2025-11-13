package rocks.blackblock.bongocat.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Represents a time value (hour:minute) for sleep scheduling.
 */
public class Time {
    private final int hour;
    private final int minute;

    public Time(int hour, int minute) {
        if (hour < 0 || hour > 23) {
            throw new IllegalArgumentException("Hour must be between 0 and 23, got: " + hour);
        }
        if (minute < 0 || minute > 59) {
            throw new IllegalArgumentException("Minute must be between 0 and 59, got: " + minute);
        }
        this.hour = hour;
        this.minute = minute;
    }

    @JsonCreator
    public static Time parse(String time) {
        if (time == null || time.trim().isEmpty()) {
            return new Time(0, 0);
        }

        String[] parts = time.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Time must be in format HH:MM, got: " + time);
        }

        try {
            int hour = Integer.parseInt(parts[0].trim());
            int minute = Integer.parseInt(parts[1].trim());
            return new Time(hour, minute);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid time format: " + time, e);
        }
    }

    public int getHour() {
        return hour;
    }

    public int getMinute() {
        return minute;
    }

    /**
     * Convert to total minutes since midnight for easy comparison
     */
    public int toMinutes() {
        return hour * 60 + minute;
    }

    @JsonValue
    @Override
    public String toString() {
        return String.format("%02d:%02d", hour, minute);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Time time = (Time) o;
        return hour == time.hour && minute == time.minute;
    }

    @Override
    public int hashCode() {
        return 31 * hour + minute;
    }
}
