package CoReMatch.pattern.cost.selection;

import CoReMatch.pattern.cost.transform.SubPatternCondition;
import CoReMatch.db.util.TimeInterval;

import java.util.Iterator;
import java.util.List;

public interface PatternStats {

    List<MatchInterval> getMatchIntervals();

    List<ConditionStats> getConditionStats();

    interface MatchInterval {
        TimeInterval getInterval();

        long getEventCount();

        long getExtendEnd();

        long getExtendBegin();
    }

    interface ConditionStats {
        SubPatternCondition.ConditionId getConditionId();

        long getTotalEventCount();

        long getTotalDuration();

        int getNumIntervals();

        double getSelectedEventCount();

        Iterator<TimeInterval> intervals();

        double getGlobalSelectivity();

        double getLocalSelectivity();
    }

}
