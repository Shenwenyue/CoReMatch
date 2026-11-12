package CoReMatch.pattern.cost.estimation;

import CoReMatch.db.core.primaryindex.HistogramAccess;
import CoReMatch.db.core.primaryindex.queries.range.AttributeRange;
import CoReMatch.db.core.primaryindex.queries.range.DoubleAttributeRange;
import CoReMatch.db.core.primaryindex.queries.range.StringAttributeRange;
import CoReMatch.pattern.cost.transform.TransformedPattern.ExecutableCondition;
import CoReMatch.db.util.TimeInterval;
import CoReMatch.event.EventSchema;

import java.util.ArrayList;
import java.util.List;

public class ConditionRunner {

    private final ExecutableCondition condition;

    private long totalEvents = 0L;
    private long hitEvents = 0L;
    private double selectedEvents = 0.0;

    private List<TimeInterval> coverage = new ArrayList<>();

    private TimeInterval lastSeen = null;

    private TimeInterval current = null;

    public ConditionRunner(EventSchema schema, ExecutableCondition c) {
        this.condition = c;
    }

    static double calcLocalSelectivity(HistogramAccess t, AttributeRange<? extends Number> range) {
        var agg = t.getAggregates(range.getName());
        var nodeRange = new DoubleAttributeRange(range.getName(), agg.getValues()[1].doubleValue(),
                agg.getValues()[2].doubleValue(), true, true);
        return computeOverlapPercent(nodeRange, range);
    }

    private static double computeOverlapPercent(DoubleAttributeRange r1, AttributeRange<? extends Number> r2) {
        if (r2.getUpper().equals(r2.getLower()) && r1.contains(r2.getUpper().doubleValue()))
            return 0.01;

        double overlap =
                Math.max(0, Math.min(r1.getUpper(), r2.getUpper().doubleValue()) -
                        Math.max(r1.getLower(), r2.getLower().doubleValue()));
        return overlap / (r1.getUpper() - r1.getLower());
    }

    public ExecutableCondition getCondition() {
        return condition;
    }

    public double getGlobalSelectivity() {
        return selectedEvents / totalEvents;
    }

    public double getLocalSelectivity(int symbolIdx) {
        return selectedEvents / hitEvents;
    }

    public double getCoveragePercent() {
        return (double) hitEvents / totalEvents;
    }

    public long getTotalEvents() {
        return totalEvents;
    }

    public ConditionInfo update(HistogramAccess t) {
        totalEvents += t.getEventCount();
        lastSeen = t.getCoveredTimeInterval();

        double selectivity = 1.0;
        if (!(((AttributeRange) condition.getRange()) instanceof StringAttributeRange)) {
            selectivity = calcLocalSelectivity(t, (AttributeRange<? extends Number>) condition.getRange());
        }

        // Update intersectAll
        if (selectivity > 0) {
            selectedEvents += (t.getEventCount() * selectivity);
            hitEvents += t.getEventCount();
            // Create interval
            if (current == null)
                current = lastSeen;
        } else if (current != null) {
            coverage.add(new TimeInterval(current.getT1(), lastSeen.getT1()));
            current = null;
        }
        return new ConditionInfo(condition, selectivity);
    }

    public void close() {
        if (current != null) {
            coverage.add(new TimeInterval(current.getT1(), lastSeen.getT2()));
            current = null;
        }
    }
}
