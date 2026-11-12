package CoReMatch.pattern.replay;

import CoReMatch.db.core.primaryindex.PrimaryIndex;
import CoReMatch.pattern.util.NativeOperatorWrapper;
import CoReMatch.esp.api.bridge.EventChannel;
import CoReMatch.esp.api.epa.EPA;
import CoReMatch.esp.bridges.nat.epa.NativeOperator;

public abstract class NativeReplayOperator<T extends EPA> extends ReplayOperator<NativeOperatorWrapper> {

    private final T def;
    private final NativeOperatorFactory<T> factory;

    protected NativeReplayOperator(PrimaryIndex tree, T def, NativeOperatorFactory<T> f) {
        super(tree);
        this.def = def;
        this.factory = f;
    }

    @Override
    protected NativeOperatorWrapper createEPA() {
        return new NativeOperatorWrapper(new NativeOperatorWrapper.OperatorFactory() {
            @Override
            public EPA getDefinition() {
                return def;
            }
            @Override
            public NativeOperator create(EventChannel... inputChannels) {
                return factory.create(def, inputChannels);
            }
        });
    }

    public static interface NativeOperatorFactory<T extends EPA> {
        NativeOperator create(T def, EventChannel... inputChannels);
    }
}
