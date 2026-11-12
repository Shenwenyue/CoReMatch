package CoReMatch.esp.api.epa.aggregates.spatial;

import CoReMatch.esp.api.epa.aggregates.Aggregate;

//TODO: Refactor: This should be ST_LineMerge and temporal should be detected automatically in the bridge.
public class TemporalLineStringMerge extends Aggregate {

    /** The serialVersionUID */
    private static final long serialVersionUID = 1L;

    public TemporalLineStringMerge(String attributeIn, String attributeOut) {
        super(attributeIn, attributeOut);
    }
}
