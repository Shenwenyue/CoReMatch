package CoReMatch.pattern.replay;

import CoReMatch.db.DBRuntimeException;
import CoReMatch.db.core.primaryindex.PrimaryIndex;
import CoReMatch.db.event.PersistentEvent;
import CoReMatch.db.util.TimeInterval;
import CoReMatch.esp.api.epa.UserDefinedEPA;
import CoReMatch.event.Event;
import xxl.core.cursors.Cursor;

public abstract class ReplayOperator<T extends UserDefinedEPA> {

    protected final PrimaryIndex tree;

    public ReplayOperator(PrimaryIndex tree) {
        this.tree = tree;
    }

    protected abstract T createEPA();

    protected Cursor<PersistentEvent> getInput(PrimaryIndex tree, T epa, TimeInterval region) throws Exception {
        return tree.query(region.getT1(), region.getT2());
    }

    public final Cursor<Event> executeDirect(Cursor<PersistentEvent> input) {

        return new ESPReplayCursor<T>(tree.getSchema(), createEPA()) {

            @Override
            protected Cursor<PersistentEvent> getInput(T epa) {
                return input;
            }
        };
    }

    public final Cursor<Event> execute(TimeInterval queryInterval) {
        return new ESPReplayCursor<T>(tree.getSchema(), createEPA()) {

            @Override
            protected Cursor<PersistentEvent> getInput(T epa) {
                try {
                    return ReplayOperator.this.getInput(tree, epa, queryInterval);
                } catch (Exception e) {
                    throw new DBRuntimeException("Could not initialize input query.", e);
                }
            }
        };

    }

    public final Cursor<Event> execute() {
        return execute(new TimeInterval(Long.MIN_VALUE, Long.MAX_VALUE));

    }

}
