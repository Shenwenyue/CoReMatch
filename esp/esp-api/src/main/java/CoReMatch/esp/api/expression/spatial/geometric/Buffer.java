package CoReMatch.esp.api.expression.spatial.geometric;

import com.vividsolutions.jts.operation.buffer.BufferParameters;
import CoReMatch.common.IncompatibleTypeException;
import CoReMatch.esp.api.epa.pattern.symbol.Bindings;
import CoReMatch.esp.api.expression.ArithmeticExpression;
import CoReMatch.esp.api.expression.base.AbstractExpression;
import CoReMatch.event.Attribute.DataType;
import CoReMatch.event.EventSchema;

/**
 * Calculates a buffer around the input geometry.
 *
 *
 */
public class Buffer extends AbstractExpression<ArithmeticExpression> implements ArithmeticExpression {

    /** The CapStyle */
    private final CapStyle capStyle;

    /** The buffer's distance */
    private final double distance;

    /**
     * @param x the input
     * @param distance the distance value
     * @param capStyle the capStyle value
     */
    public Buffer(ArithmeticExpression x, double distance, CapStyle capStyle) {
        super(x);
        this.distance = distance;
        this.capStyle = capStyle;
    }

    @Override
    public DataType getDataType(EventSchema schema, Bindings bindings) throws IncompatibleTypeException {
        return DataType.GEOMETRY;
    }

    public double getDistance() {
        return distance;
    }

    public CapStyle getCapStyle() {
        return capStyle;
    }

    /**
     * Describes the ending style of a buffer applied to a geometry.
     *
     */
    public static enum CapStyle {
        ROUND(BufferParameters.CAP_ROUND), FLAT(BufferParameters.CAP_FLAT), SQUARE(BufferParameters.CAP_SQUARE);

        /** The jts internal value for this cap-style */
        public final int jtsValue;

        /**
         * Constructs a new CapStyle instance
         *
         * @param jtsValue The jts internal value for this cap-style
         */
        private CapStyle(int jtsValue) {
            this.jtsValue = jtsValue;
        }
    }

}

