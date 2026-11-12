package CoReMatch.esp.api.epa;

import CoReMatch.common.EPException;
import CoReMatch.common.IncompatibleTypeException;
import CoReMatch.esp.api.epa.pattern.Output;
import CoReMatch.esp.api.epa.pattern.Partition;
import CoReMatch.esp.api.epa.pattern.regex.Pattern;
import CoReMatch.esp.api.epa.pattern.symbol.Binding;
import CoReMatch.esp.api.epa.pattern.symbol.Bindings;
import CoReMatch.esp.api.epa.pattern.symbol.Symbol;
import CoReMatch.esp.api.epa.pattern.symbol.Symbols;
import CoReMatch.event.Attribute;
import CoReMatch.event.EventSchema;
import CoReMatch.event.SchemaException;

import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;

public class PatternMatcher implements EPA {

    private static final long serialVersionUID = 1L;

    private final long within;

    private final Symbols symbols;

    private final Pattern pattern;

    private final Output output;

    private final Partition partitionBy;

    private EPA input;

    public PatternMatcher(EPA input, long within, Symbols symbols, Pattern pattern, Output output) {
        this(input, within, symbols, pattern, output, new Partition());
    }

    public PatternMatcher(EPA input, long within, Symbols symbols, Pattern pattern, Output output, Partition partitionBy) {
        super();
        this.input = input;
        this.within = within;
        this.symbols = symbols;
        this.pattern = pattern;
        this.output = output;
        this.partitionBy = partitionBy;
    }

    @Override
    public EPA[] getInputEPAs() {
        return new EPA[]{input};
    }

    @Override
    public void setInput(int idx, EPA input) {
        if (idx == 0)
            this.input = input;
        else
            throw new IllegalArgumentException("Invalid input index: " + idx);
    }

    public long getWithin() {
        return within;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public Symbols getSymbols() {
        return symbols;
    }

    public Output getOutput() {
        return output;
    }

    public Partition getPartitionBy() {
        return partitionBy;
    }

    @Override
    public EventSchema computeOutputSchema(EventSchema... inputSchemas) throws SchemaException {

        try {
            List<Attribute> attributes = new ArrayList<>();
            Bindings allBindings = symbols.getAllBindings();
            for (String var : output) {

                Binding b = symbols.findBinding(var);

                attributes.add(new Attribute(var, b.getValue().getDataType(inputSchemas[0], allBindings)));
            }

            return new EventSchema(attributes.toArray(new Attribute[attributes.size()]));
        } catch (EPException epex) {

            throw new SchemaException("Could not compute output schema", epex);
        }
    }

    @Override
    public String toString() {
        return "PatternMatcher [input=" + input + ", within=" + within + ", symbols=" + symbols + ", pattern=" + pattern + ", output=" + output
                + ", partitionBy=" + partitionBy + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((input == null) ? 0 : input.hashCode());
        result = prime * result + ((output == null) ? 0 : output.hashCode());
        result = prime * result + ((partitionBy == null) ? 0 : partitionBy.hashCode());
        result = prime * result + ((pattern == null) ? 0 : pattern.hashCode());
        result = prime * result + ((symbols == null) ? 0 : symbols.hashCode());
        result = prime * result + (int) (within ^ (within >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        PatternMatcher other = (PatternMatcher) obj;
        if (input == null) {
            if (other.input != null)
                return false;
        } else if (!input.equals(other.input))
            return false;
        if (output == null) {
            if (other.output != null)
                return false;
        } else if (!output.equals(other.output))
            return false;
        if (partitionBy == null) {
            if (other.partitionBy != null)
                return false;
        } else if (!partitionBy.equals(other.partitionBy))
            return false;
        if (pattern == null) {
            if (other.pattern != null)
                return false;
        } else if (!pattern.equals(other.pattern))
            return false;
        if (symbols == null) {
            if (other.symbols != null)
                return false;
        } else if (!symbols.equals(other.symbols))
            return false;
        if (within != other.within)
            return false;
        return true;
    }

}
