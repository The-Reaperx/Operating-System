import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * A simple clock to manage simulation time, starting from a base time (e.g., 08:00).
 */
public class SimulationClock {
    private final long startTimeMillis; // Real system time when simulation started
    private final long simulationStartTimeOffsetMillis; // Offset for simulation start time (e.g., 8 hours for 08:00)
    private final SimpleDateFormat timeFormat;

    // Example: Start simulation conceptually at 08:00:00
    public SimulationClock() {
        this.startTimeMillis = System.currentTimeMillis();
        // Calculate offset for 8 AM UTC (adjust if timezone needed, but for formatting it's simpler)
        this.simulationStartTimeOffsetMillis = 8 * 60 * 60 * 1000L;
        this.timeFormat = new SimpleDateFormat("HH:mm:ss");
        this.timeFormat.setTimeZone(TimeZone.getTimeZone("UTC")); // Format relative to 00:00 base
    }

    /**
     * Gets the current simulation time in milliseconds since the simulation's conceptual epoch (00:00).
     * @return Current simulation time in milliseconds.
     */
    public long getTimeMillis() {
        long elapsedTime = System.currentTimeMillis() - startTimeMillis;
        return simulationStartTimeOffsetMillis + elapsedTime;
    }

    /**
     * Gets the current simulation time formatted as HH:mm:ss. [cite: 34]
     * @return Formatted time string.
     */
    public String getFormattedTime() {
        return timeFormat.format(new Date(getTimeMillis()));
    }

    /**
     * Gets the total elapsed real time since the simulation started.
     * @return Elapsed time in milliseconds.
     */
    public long getElapsedRealTimeMillis() {
        return System.currentTimeMillis() - startTimeMillis;
    }
}