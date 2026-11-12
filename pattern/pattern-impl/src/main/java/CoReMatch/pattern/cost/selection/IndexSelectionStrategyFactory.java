package CoReMatch.pattern.cost.selection;

import CoReMatch.db.core.secondaryindex.SecondaryTimeIndex;
import CoReMatch.db.core.primaryindex.impl.PrimaryIndexImpl;
import CoReMatch.pattern.cost.transform.TransformedPattern;

import java.util.List;

public interface IndexSelectionStrategyFactory {

    IndexSelectionStrategy create(PrimaryIndexImpl primary, List<? extends SecondaryTimeIndex<?>> secondaries,
                                  TransformedPattern pattern, PatternStats result);

}
