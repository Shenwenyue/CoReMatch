package CoReMatch.esp.api.epa;

import CoReMatch.common.IncompatibleTypeException;
import CoReMatch.event.EventSchema;
import CoReMatch.event.SchemaException;

import java.io.Serializable;

public interface EPA extends Serializable {

    public static final EPA[] EMPTY_EPA_ARRAY = {};

    EPA[] getInputEPAs();

    void setInput(int idx, EPA input);

    default int getDepth() {
        int childDepth = 0;
        for (EPA c : getInputEPAs())
            childDepth = Math.max(childDepth, c.getDepth());
        return 1 + childDepth;
    }

    EventSchema computeOutputSchema(EventSchema... inputSchemas) throws SchemaException, IncompatibleTypeException;

}
