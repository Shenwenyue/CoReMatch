package CoReMatch.pattern.cost.execution;

/**
 * Cross-variable combination policy over per-symbol candidate lists.
 *
 * <p>System property {@code pattern.matchPolicy}:
 * <ul>
 *   <li>{@code STNM} or {@code SKIP_TILL_NEXT_MATCH} (default) — earliest-successor merge</li>
 *   <li>{@code STAM} or {@code SKIP_TILL_ANY_MATCH} — enumerate all order- and window-feasible successors</li>
 * </ul>
 *
 * <p>When {@code pattern.matchPolicy=STAM}, {@code pattern.stamMode} selects the successor search:
 * <ul>
 *   <li>{@code DFS} (default) — precompute SeqId/timestamp bounds, then enumerate the range</li>
 *   <li>{@code SCAN} — the earlier STAM: for each partial match, scan the next candidate list element by element</li>
 * </ul>
 */
public enum MatchPolicy {

    STNM,
    STAM;

    private static final String PROPERTY = "pattern.matchPolicy";
    private static final String STAM_MODE_PROPERTY = "pattern.stamMode";

    public static MatchPolicy fromSystemProperty() {
        String raw = System.getProperty(PROPERTY);
        if (raw == null || raw.trim().isEmpty()) {
            return STNM;
        }
        String value = raw.trim().toUpperCase();
        if ("STAM".equals(value) || "SKIP_TILL_ANY_MATCH".equals(value) || "ANY".equals(value)) {
            return STAM;
        }
        if ("STNM".equals(value) || "SKIP_TILL_NEXT_MATCH".equals(value) || "NEXT".equals(value)) {
            return STNM;
        }
        System.err.printf("[MatchPolicy] Unknown %s=%s, using STNM%n", PROPERTY, raw);
        return STNM;
    }

    /**
     * @return {@code true} when STAM should precompute successor and window bounds.
     *         {@code false} restores the full-list scan used before that optimization.
     */
    public static boolean stamUsesPrecomputedBounds() {
        String raw = System.getProperty(STAM_MODE_PROPERTY);
        if (raw == null || raw.trim().isEmpty()) {
            return true;
        }
        String value = raw.trim().toUpperCase();
        if ("SCAN".equals(value) || "NAIVE".equals(value) || "LINEAR".equals(value)) {
            return false;
        }
        if ("DFS".equals(value) || "BOUND".equals(value) || "RANGE".equals(value)) {
            return true;
        }
        System.err.printf("[MatchPolicy] Unknown %s=%s, using DFS%n", STAM_MODE_PROPERTY, raw);
        return true;
    }
}
