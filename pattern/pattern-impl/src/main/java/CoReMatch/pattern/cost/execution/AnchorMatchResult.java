package CoReMatch.pattern.cost.execution;

import CoReMatch.db.util.TimeInterval;
import CoReMatch.event.Event;

import java.util.List;

/**
 * One anchor match group: full {@link Event} records for each pattern anchor (A, B, C, …).
 */
public final class AnchorMatchResult {

    private final List<Event> anchors;
    private final TimeInterval interval;

    public AnchorMatchResult(List<Event> anchors, TimeInterval interval) {
        this.anchors = anchors;
        this.interval = interval;
    }

    public List<Event> getAnchors() {
        return anchors;
    }

    public Event getAnchor(int subPatternIndex) {
        return anchors.get(subPatternIndex);
    }

    public TimeInterval getInterval() {
        return interval;
    }

    public int getAnchorCount() {
        return anchors.size();
    }
}
