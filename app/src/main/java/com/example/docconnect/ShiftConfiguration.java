package com.example.docconnect;

import java.io.Serializable;

/**
 * Model class representing the time configuration for a doctor's shift.
 * This class stores the parameters required to calculate and generate individual
 * appointment slots, including break times and buffers.
 */
public class ShiftConfiguration implements Serializable {

    // Core shift timing fields
    public String startTime;        // e.g., "09:00"
    public String endTime;          // e.g., "17:00"

    // Break timing fields
    public String breakStartTime;   // e.g., "13:00"
    public String breakEndTime;     // e.g., "14:00"

    // Duration settings in minutes
    public int slotDurationMinutes;
    public int bufferDurationMinutes; // Extra time between slots if enabled

    // Toggle for buffer logic
    public boolean isBufferEnabled;

    /**
     * Primary constructor to initialize the shift settings.
     * * @param start           Shift start time
     * @param end             Shift end time
     * @param bStart          Break start time
     * @param bEnd            Break end time
     * @param duration        Length of each appointment slot (mins)
     * @param bufEnabled      Whether to add a buffer between slots
     * @param bufTime         Duration of the buffer (mins)
     */
    public ShiftConfiguration(String start, String end, String bStart, String bEnd,
                              int duration, boolean bufEnabled, int bufTime) {
        this.startTime = start;
        this.endTime = end;
        this.breakStartTime = bStart;
        this.breakEndTime = bEnd;
        this.slotDurationMinutes = duration;
        this.isBufferEnabled = bufEnabled;
        this.bufferDurationMinutes = bufTime;
    }

    /**
     * Default constructor - useful for specific serialization/deserialization
     * tools or manual object creation.
     */
    public ShiftConfiguration() {
    }
}