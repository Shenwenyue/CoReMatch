package CoReMatch.esp.ql.impl.spatial.geometric;

import org.kohsuke.MetaInfServices;
import CoReMatch.common.IncompatibleTypeException;
import CoReMatch.esp.api.epa.pattern.symbol.Bindings;
import CoReMatch.esp.api.expression.spatial.geometric.Buffer;
import CoReMatch.esp.expressions.spatial.geometric.EVSpatialBuffer;
import CoReMatch.esp.ql.ExpressionTranslator;
import CoReMatch.esp.ql.Translator;
import CoReMatch.esp.ql.TranslatorException;
import CoReMatch.event.EventSchema;
import CoReMatch.event.SchemaException;

@MetaInfServices
public class BufferTranslator extends Translator<Buffer, EVSpatialBuffer> {

    public BufferTranslator() {
        super(Buffer.class, EVSpatialBuffer.class);
    }

    @Override
    protected EVSpatialBuffer process(Buffer in, ExpressionTranslator translator, EventSchema schema, Bindings bindings)
            throws TranslatorException, IncompatibleTypeException, SchemaException {
        return new EVSpatialBuffer(translator.translateSpatialExpression(in.getInput(0), schema, bindings), in.getDistance(), in.getCapStyle());
    }

}

