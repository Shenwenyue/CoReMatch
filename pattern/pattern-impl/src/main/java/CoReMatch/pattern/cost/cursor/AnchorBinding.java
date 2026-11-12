package CoReMatch.pattern.cost.cursor;

import CoReMatch.db.util.TimeInterval;

/**
 * A validated anchor combination from {@link PatternCandidateBindingsIterator}:
 * one {@link SubPatternCandidate} per executable sub-pattern plus the time envelope.
 */
public final class AnchorBinding {

    private final SubPatternCandidate[] anchors;
    private final TimeInterval interval;

    public AnchorBinding(SubPatternCandidate[] anchors, TimeInterval interval) {
        this.anchors = anchors;
        this.interval = interval;
    }

    public SubPatternCandidate[] getAnchors() {
        return anchors;
    }

    public TimeInterval getInterval() {
        return interval;
    }
}
