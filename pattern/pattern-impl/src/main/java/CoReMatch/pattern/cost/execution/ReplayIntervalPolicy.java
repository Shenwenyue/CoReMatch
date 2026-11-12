package CoReMatch.pattern.cost.execution;

import CoReMatch.pattern.cost.transform.TransformedPattern;

/**
 * Controls replay-interval shaping and output path in {@link SecondaryStrategy}.
 *
 * <p>System property {@code pattern.replayInterval}:
 * <ul>
 *   <li>{@code EXTEND} (default) — {@link CoReMatch.pattern.util.WindowExtendCursor} then
 *       {@link CoReMatch.pattern.util.IntervalIntersectionCursor}, primary-index interval replay, NFA</li>
 *   <li>{@code RAW} or {@code RAW_CANDIDATE} — use pre-extension {@code [t1,t2]}; skip WindowExtend and
 *       lightweight intersect; then merge, interval replay, NFA</li>
 *   <li>{@code ANCHOR} or {@code ANCHOR_ONLY} — anchor-only output: skip merge, interval replay, and NFA;
 *       load full anchor events via TID point lookup on the primary index</li>
 *   <li>{@code AUTO} — {@code RAW_CANDIDATE} when config {@link TransformedPattern.ExecutableConfiguration#isExact()}, else {@code EXTEND}</li>
 * </ul>
 *
 * <p>Anchor-only mode (paper): after secondary-index candidate conjunction, partial order, and time-window
 * checks, results are anchor events loaded by TID from the primary index—no merged replay intervals,
 * no interval scan, no NFA.
 */
public enum ReplayIntervalPolicy {

    /** Apply {@link CoReMatch.pattern.util.WindowExtendCursor} / {@code getTScope} (default). */
    EXTEND,

    /** Pass pre-extension {@code [t1,t2]} through merge only (no extend, no lightweight intersect). */
    RAW_CANDIDATE,

    /**
     * Anchor-only output: {@link CoReMatch.pattern.cost.cursor.PatternAnchorCursor} + primary TID lookup.
     */
    ANCHOR_ONLY,

    /**
     * {@link RAW_CANDIDATE} when {@link TransformedPattern.ExecutableConfiguration#isExact()},
     * otherwise {@link EXTEND}.
     */
    AUTO;

    private static final String PROPERTY = "pattern.replayInterval";

    public static ReplayIntervalPolicy fromSystemProperty() {
        String raw = System.getProperty(PROPERTY);
        if (raw == null || raw.trim().isEmpty()) {
            return EXTEND;
        }
        String value = raw.trim().toUpperCase();
        if ("RAW".equals(value) || "RAW_CANDIDATE".equals(value)) {
            return RAW_CANDIDATE;
        }
        if ("ANCHOR".equals(value) || "ANCHOR_ONLY".equals(value)) {
            return ANCHOR_ONLY;
        }
        if ("AUTO".equals(value)) {
            return AUTO;
        }
        if ("EXTEND".equals(value)) {
            return EXTEND;
        }
        System.err.printf(
            "[ReplayIntervalPolicy] Unknown %s=%s, using EXTEND%n", PROPERTY, raw);
        return EXTEND;
    }

    public ReplayIntervalPolicy resolve(TransformedPattern.ExecutableConfiguration config) {
        if (this != AUTO) {
            return this;
        }
        return config.isExact() ? RAW_CANDIDATE : EXTEND;
    }
}
