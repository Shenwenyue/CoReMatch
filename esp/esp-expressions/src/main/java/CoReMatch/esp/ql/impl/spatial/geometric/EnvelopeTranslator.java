package CoReMatch.esp.ql.impl.spatial.geometric;

import org.kohsuke.MetaInfServices;
import CoReMatch.common.IncompatibleTypeException;
import CoReMatch.esp.api.epa.pattern.symbol.Bindings;
import CoReMatch.esp.api.expression.spatial.geometric.Envelope;
import CoReMatch.esp.expressions.spatial.geometric.EVSpatialEnvelope;
import CoReMatch.esp.ql.ExpressionTranslator;
import CoReMatch.esp.ql.Translator;
import CoReMatch.esp.ql.TranslatorException;
import CoReMatch.event.EventSchema;
import CoReMatch.event.SchemaException;

@MetaInfServices
public class EnvelopeTranslator extends Translator<Envelope, EVSpatialEnvelope> {

    public EnvelopeTranslator() {
        super(Envelope.class, EVSpatialEnvelope.class);
    }

    @Override
    protected EVSpatialEnvelope process(Envelope in, ExpressionTranslator translator, EventSchema schema, Bindings bindings)
            throws TranslatorException, IncompatibleTypeException, SchemaException {
        return new EVSpatialEnvelope(translator.translateSpatialExpression(in.getInput(0), schema, bindings));
    }

}
