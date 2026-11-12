package CoReMatch.pattern.cost.execution;

import CoReMatch.db.core.IOMeasuring;

import java.util.List;
import java.util.stream.Collectors;

public class Estimates implements Comparable<Estimates> {

    private static final double LN_2 = Math.log(2);
    private final long primaryLeafIO;
    private final long primaryNavigationIO;

    private final List<SecondaryIndexEstimate> secondary;
    private final long replayIntervals;
    private final long unmergedIntervals;
    private final double temporalCoverage;

    public Estimates(long primaryIO, long primaryNavigationIO, List<SecondaryIndexEstimate> secondaryIO, long replayIntervals, long unmergedIntervals, double temporalCoverage) {
        this.primaryLeafIO = primaryIO;
        this.primaryNavigationIO = primaryNavigationIO;
        this.secondary = secondaryIO;
        this.replayIntervals = replayIntervals;
        this.unmergedIntervals = unmergedIntervals;
        this.temporalCoverage = temporalCoverage;
    }

    long getTotalIO() {
        return primaryLeafIO + primaryNavigationIO + getSecondaryIO();
    }

    public long getScore() {
        long secondaryIO = getSecondaryIO();
        long secondarySort = secondary.stream().collect(Collectors.summarizingLong(
                x -> (long) (x.matchCount * (Math.log(x.matchCount) / LN_2))
        )).getSum();

        double wPIO = 1.0;
        double wSIO = 2.536732308682677;
        double wSort = 12.731394655606488;

        long secondaryTotal = (long) Math.ceil(wSIO * secondaryIO + wSort * secondarySort);
        return primaryNavigationIO + (long) Math.ceil(wPIO * primaryLeafIO) + secondaryTotal;

    }

    @Override
    public int compareTo(Estimates o) {
        return Long.compare(getScore(), o.getScore());

    }

    boolean dominates(Estimates o) {
        return getPrimaryLeafIO() < o.getPrimaryLeafIO() && getSecondaryIO() < o.getSecondaryIO();

    }

    public long getPrimaryLeafIO() {
        return primaryLeafIO;
    }

    public long getPrimaryNavigationIO() {
        return primaryNavigationIO;
    }

    public long getSecondaryIO() {
        return secondary.stream().collect(Collectors.summarizingLong(SecondaryIndexEstimate::getIo)).getSum();
    }

    public long getReplayIntervals() {
        return replayIntervals;
    }

    public long getUnmergedIntervals() {
        return unmergedIntervals;
    }

    public double getTemporalCoverage() {
        return temporalCoverage;
    }

    public List<SecondaryIndexEstimate> getSecondary() {
        return secondary;
    }

    @Override
    public String toString() {
        return "Estimates[" +
                "primaryLeafIO=" + IOMeasuring.formatMiB(primaryLeafIO) + " MiB" +
                ", primaryNavigationIO=" + IOMeasuring.formatMiB(primaryNavigationIO) + " MiB" +
                ", secondaryIO=" + IOMeasuring.formatMiB(getSecondaryIO()) + " MiB" +
                ", replayIntervals=" + replayIntervals +
                ", unmergedIntervals=" + unmergedIntervals +
                ", temporalCoverage=" + temporalCoverage +
                ", score=" + getScore() +
                ']';
    }

    public static class SecondaryIndexEstimate {
        private final long io;
        private final long matchCount;

        public SecondaryIndexEstimate(long io, long matchCount) {
            this.io = io;
            this.matchCount = matchCount;
        }

        public long getIo() {
            return io;
        }

        public long getMatchCount() {
            return matchCount;
        }

        @Override
        public String toString() {
            return "SecondaryIndexEstimate{" +
                    "io=" + io +
                    ", matchCount=" + matchCount +
                    '}';
        }
    }
}
