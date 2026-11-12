package CoReMatch.esp.ql.impl.spatial.factory;

import org.kohsuke.MetaInfServices;
import CoReMatch.common.IncompatibleTypeException;
import CoReMatch.esp.api.epa.pattern.symbol.Bindings;
import CoReMatch.esp.api.epa.pattern.symbol.NoSuchVariableException;
import CoReMatch.esp.api.expression.spatial.factory.PointCreator;
import CoReMatch.esp.expressions.spatial.factory.EVPointCreator;
import CoReMatch.esp.ql.ExpressionTranslator;
import CoReMatch.esp.ql.Translator;
import CoReMatch.esp.ql.TranslatorException;
import CoReMatch.event.EventSchema;
import CoReMatch.event.SchemaException;

@MetaInfServices
public class PointCreatorTranslator extends Translator<PointCreator, EVPointCreator> {

    public PointCreatorTranslator() {
        super(PointCreator.class, EVPointCreator.class);
    }

    @Override
    protected EVPointCreator process(PointCreator in, ExpressionTranslator translator, EventSchema schema, Bindings bindings) throws TranslatorException, IncompatibleTypeException, NoSuchVariableException, SchemaException {
        return new EVPointCreator(
                translator.translateNumericExpression(in.getInput(0), schema, bindings),
                translator.translateNumericExpression(in.getInput(1), schema, bindings));
    }

}
