package CoReMatch.esp.ql.impl.predicate;

import org.kohsuke.MetaInfServices;
import CoReMatch.common.IncompatibleTypeException;
import CoReMatch.esp.api.epa.pattern.symbol.Bindings;
import CoReMatch.esp.api.expression.ArithmeticExpression;
import CoReMatch.esp.api.expression.predicate.set.Subset;
import CoReMatch.esp.expressions.predicate.set.EVSubset;
import CoReMatch.esp.ql.ExpressionTranslator;
import CoReMatch.esp.ql.Translator;
import CoReMatch.esp.ql.TranslatorException;
import CoReMatch.event.Attribute.DataType;
import CoReMatch.event.EventSchema;
import CoReMatch.event.SchemaException;

@MetaInfServices
public class SubsetTranslator extends Translator<Subset, EVSubset> {

    public SubsetTranslator() {
        super(Subset.class, EVSubset.class);
    }

    @Override
    protected EVSubset process(Subset in, ExpressionTranslator translator, EventSchema schema, Bindings bindings)
            throws TranslatorException, IncompatibleTypeException, SchemaException {
        ArithmeticExpression l = in.getInput(0);
        ArithmeticExpression r = in.getInput(1);

        DataType lt = l.getDataType(schema, bindings);
        DataType rt = r.getDataType(schema, bindings);

        if (lt == DataType.SET && rt == DataType.SET) {
            return new EVSubset(translator.translateSetExpression(l, schema, bindings), translator.translateSetExpression(r, schema, bindings));
        } else
            throw new IncompatibleTypeException("No implementation for Subset with input-types: " + lt + " and " + rt);
    }
}
