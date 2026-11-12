package CoReMatch.pattern.util;

import java.util.concurrent.atomic.AtomicLong;

public final class PhaseTimers {

    private static final AtomicLong phase1Nanos = new AtomicLong();
    private static final AtomicLong phase2Nanos = new AtomicLong();
    private static final AtomicLong phase3CombineNanos = new AtomicLong();
    private static final AtomicLong phase3MaterializeNanos = new AtomicLong();
    private static final ThreadLocal<Frame> frame = ThreadLocal.withInitial(Frame::new);
    private static volatile boolean enabled;

    private PhaseTimers() {
    }

    public static void beginQuery() {
        enabled = false;
        phase1Nanos.set(0);
        phase2Nanos.set(0);
        phase3CombineNanos.set(0);
        phase3MaterializeNanos.set(0);
        frame.get().reset();
        enabled = true;
    }

    public static void addPhase1(long nanos) {
        if (enabled && nanos > 0) {
            phase1Nanos.addAndGet(nanos);
        }
    }

    public static void enterPhase2() {
        enter(true);
    }

    public static void exitPhase2() {
        exit(true);
    }

    public static void enterPhase3() {
        enter(false);
    }

    public static void exitPhase3() {
        exit(false);
    }

    public static void enterPhase3Materialize() {
        if (!enabled) {
            return;
        }
        Frame f = frame.get();
        if (f.materializeDepth++ == 0) {
            f.materializeStart = System.nanoTime();
        }
    }

    public static void exitPhase3Materialize() {
        if (!enabled) {
            return;
        }
        Frame f = frame.get();
        if (f.materializeDepth == 0) {
            return;
        }
        if (--f.materializeDepth == 0) {
            phase3MaterializeNanos.addAndGet(System.nanoTime() - f.materializeStart);
        }
    }

    public static void report() {
        printReport(null);
        endQuery();
    }

    public static double reportAndContinue(String scope) {
        double totalMs = printReport(scope);
        phase1Nanos.set(0);
        phase2Nanos.set(0);
        phase3CombineNanos.set(0);
        phase3MaterializeNanos.set(0);
        frame.get().reset();
        return totalMs;
    }

    public static void endQuery() {
        enabled = false;
        frame.get().reset();
    }

    private static double printReport(String scope) {
        double p1 = phase1Nanos.get() / 1_000_000.0;
        double p2 = phase2Nanos.get() / 1_000_000.0;
        double p3 = phase3CombineNanos.get() / 1_000_000.0;
        double p3m = phase3MaterializeNanos.get() / 1_000_000.0;
        return p1 + p2 + p3 + p3m;
    }

    private static void enter(boolean phase2) {
        if (!enabled) {
            return;
        }
        Frame f = frame.get();
        if (phase2) {
            if (f.phase2Depth++ == 0) {
                f.phase2Start = System.nanoTime();
            }
        } else if (f.phase3Depth++ == 0) {
            f.phase3Start = System.nanoTime();
        }
    }

    private static void exit(boolean phase2) {
        if (!enabled) {
            return;
        }
        Frame f = frame.get();
        if (phase2) {
            if (f.phase2Depth == 0) {
                return;
            }
            if (--f.phase2Depth == 0) {
                phase2Nanos.addAndGet(System.nanoTime() - f.phase2Start);
            }
        } else {
            if (f.phase3Depth == 0) {
                return;
            }
            if (--f.phase3Depth == 0) {
                phase3CombineNanos.addAndGet(System.nanoTime() - f.phase3Start);
            }
        }
    }

    private static final class Frame {
        int phase2Depth;
        int phase3Depth;
        int materializeDepth;
        long phase2Start;
        long phase3Start;
        long materializeStart;

        void reset() {
            phase2Depth = 0;
            phase3Depth = 0;
            materializeDepth = 0;
        }
    }
}
