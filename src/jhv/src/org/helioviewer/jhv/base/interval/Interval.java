package org.helioviewer.jhv.base.interval;

import java.util.ArrayList;

import org.helioviewer.jhv.base.time.TimeUtils;

public class Interval implements Comparable<Interval> {

    public final long start;
    public final long end;

    public Interval(long _start, long _end) {
        start = _start;
        end = _end;
    }

    public boolean containsInclusive(Interval other) {
        return containsPointInclusive(other.start) && containsPointInclusive(other.end);
    }

    public boolean containsPointInclusive(long time) {
        return time >= start && time <= end;
    }

    public boolean overlaps(Interval otherInterval) {
        return !(start > otherInterval.end || end < otherInterval.start);
    }

    public boolean overlapsInclusive(Interval other) {
        return containsPointInclusive(other.start) || containsPointInclusive(other.end) || other.containsPointInclusive(start) || other.containsPointInclusive(end);
    }

    /**
     * Returns the value that is as close as possible to the given value, but
     * still contained in the interval. If the given value is outside the
     * interval, the the interval's closest 'edge' is returned.
     */
    public long squeeze(long value) {
        if (containsPointInclusive(value)) {
            return value;
        } else if (value <= start) {
            return start;
        }
        return end;
    }

    @Override
    public String toString() {
        return "[" + start + "," + end + ")";
    }

    @Override
    public int compareTo(Interval other) {
        return (int) ((start - other.start) % 1);

    }

    public static ArrayList<Interval> splitInterval(Interval interval, int days) {
        ArrayList<Interval> intervals = new ArrayList<Interval>();
        long startDate = interval.start;

        while (true) {
            long newStartDate = startDate + TimeUtils.DAY_IN_MILLIS * days;
            if (interval.containsPointInclusive(newStartDate)) {
                intervals.add(new Interval(startDate, newStartDate));
                startDate = newStartDate;
            } else {
                intervals.add(new Interval(startDate, interval.end));
                break;
            }
        }

        return intervals;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Interval) {
            Interval s = (Interval) other;
            return start == s.start && end == s.end;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = (int) (start ^ (start >>> 32));
        return 31 * result + (int) (end ^ (end >>> 32));
    }

}
