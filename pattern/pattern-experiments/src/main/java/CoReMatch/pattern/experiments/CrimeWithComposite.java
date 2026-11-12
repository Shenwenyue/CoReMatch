package CoReMatch.pattern.experiments;

import CoReMatch.common.IncompatibleTypeException;
import CoReMatch.db.DBException;
import CoReMatch.db.core.primaryindex.impl.PrimaryWithSecondaryIndexImpl;
import CoReMatch.db.core.primaryindex.queries.range.DoubleAttributeRange;
import CoReMatch.db.core.secondaryindex.CompositeKey;
import CoReMatch.db.core.secondaryindex.EventID;
import CoReMatch.db.core.secondaryindex.LSMCompositeTimeIndex;
import CoReMatch.db.core.secondaryindex.SecondaryTimeIndex;
import CoReMatch.db.event.TID;
import CoReMatch.esp.api.epa.PatternMatcher;
import CoReMatch.esp.api.epa.Stream;
import CoReMatch.esp.api.epa.pattern.Output;
import CoReMatch.esp.api.epa.pattern.regex.Atom;
import CoReMatch.esp.api.epa.pattern.regex.KleeneStar;
import CoReMatch.esp.api.epa.pattern.regex.Pattern;
import CoReMatch.esp.api.epa.pattern.regex.Sequence;
import CoReMatch.esp.api.epa.pattern.symbol.Binding;
import CoReMatch.esp.api.epa.pattern.symbol.Bindings;
import CoReMatch.esp.api.epa.pattern.symbol.Symbol;
import CoReMatch.esp.api.epa.pattern.symbol.Symbols;
import CoReMatch.esp.api.expression.BooleanExpression;
import CoReMatch.esp.api.expression.arithmetic.atomic.DoubleConstant;
import CoReMatch.esp.api.expression.arithmetic.atomic.ShortConstant;
import CoReMatch.esp.api.expression.arithmetic.atomic.StringConstant;
import CoReMatch.esp.api.expression.arithmetic.atomic.Variable;
import CoReMatch.esp.api.expression.logical.And;
import CoReMatch.esp.api.expression.logical.Or;
import CoReMatch.esp.api.expression.logical.True;
import CoReMatch.esp.api.expression.predicate.Between;
import CoReMatch.esp.api.expression.predicate.Equal;
import CoReMatch.esp.ql.TranslatorException;
import CoReMatch.event.Attribute;
import CoReMatch.event.Attribute.DataType;
import CoReMatch.event.Event;
import CoReMatch.event.EventSchema;
import CoReMatch.event.TimeRepresentation;
import CoReMatch.event.impl.SimpleEvent;
import CoReMatch.pattern.cost.execution.*;
import CoReMatch.pattern.cost.selection.PatternStats;
import CoReMatch.pattern.cost.selection.impl.NaiveStrategySelector;
import CoReMatch.pattern.cost.transform.TransformedPattern;
import CoReMatch.pattern.experiments.util.*;
import CoReMatch.pattern.util.CachedCursor;
import CoReMatch.pattern.util.PhaseTimers;
import CoReMatch.pattern.util.TIDCursor;
import CoReMatch.pattern.util.Util;
import xxl.core.cursors.Cursor;
import xxl.core.util.Pair;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class CrimeWithComposite {

    private static final String JSON_CONFIG = "range_queries_composite.json";

    private static EventSchema getSchema() {

        EventSchema schema = new EventSchema(
                new Attribute("ID", DataType.LONG),
                new Attribute("PRIMARY", DataType.STRING),
                new Attribute("BEAT", DataType.SHORT),
                new Attribute("LATITUDE", DataType.DOUBLE),
                new Attribute("LONGITUDE", DataType.DOUBLE));

        schema.byName("PRIMARY").setMaxStringSize(32);

        schema.byName("PRIMARY").setProperty("index", "true").setProperty("secondaryIndex", "true");

        schema.byName("PRIMARY").setProperty("compositeIndex_LATITUDE", "true");
        schema.byName("LATITUDE").setProperty("compositeIndexPrimary", "true");

        schema.byName("PRIMARY").setProperty("compositeIndex_LONGITUDE", "true");
        schema.byName("LONGITUDE").setProperty("compositeIndexPrimary", "true");

        schema.byName("PRIMARY").setProperty("compositeIndex_BEAT", "true");
        schema.byName("BEAT").setProperty("compositeIndexPrimary", "true");
        return schema;
    }

    private static PrimaryWithSecondaryIndexImpl buildCrimeStoreWithCompositeIndexes(Path input, Path directory, ExperimentUtil.ExperimentConfig cfg)
            throws DBException, NumberFormatException, IOException {

        Files.createDirectories(directory);
        System.out.println(directory);

        EventSchema schema = getSchema();

        PrimaryWithSecondaryIndexImpl result = new PrimaryWithSecondaryIndexImpl(directory, schema,
                TimeRepresentation.POINT, cfg.iParams, cfg.mParams);

        System.out.println("Creating composite indexes...");

        LSMCompositeTimeIndex<String, Double> primaryLatitudeIndex = new LSMCompositeTimeIndex<>(directory, schema, "PRIMARY", "LATITUDE", result.getTree().BLOCK_SIZE);

        LSMCompositeTimeIndex<String, Double> primaryLongitudeIndex = new LSMCompositeTimeIndex<>(directory, schema, "PRIMARY", "LONGITUDE", result.getTree().BLOCK_SIZE);

        LSMCompositeTimeIndex<String, Short> primaryBeatIndex = new LSMCompositeTimeIndex<>(directory, schema, "PRIMARY", "BEAT", result.getTree().BLOCK_SIZE);

        System.out.println("Created 3 composite indexes: (PRIMARY,LATITUDE), (PRIMARY,LONGITUDE), (PRIMARY,BEAT)");

        System.out.println("Building dictionary for primary category.");

        Map<String, Integer> primaryDictionary = buildPrimaryDictionary(input);
        System.out.println("Finished building dictionary for primary category:");
        primaryDictionary.entrySet().stream()
                .sorted(Comparator.comparingInt(Map.Entry::getValue))
                .forEach(x -> System.out.println("  " + x.getValue() + ": " + x.getKey()));

        try (var r = Files.newBufferedReader(input)) {
            r.readLine();

            String line = null;
            long sequenceId = 0;
            while ((line = r.readLine()) != null) {

                String[] values = line.split(";");

                Object[] payload = new Object[values.length-1];

                payload[0] = Long.parseLong(values[0]);
                payload[1] = values[1];
                payload[2] = Short.parseShort(values[2]);
                payload[3] = Double.parseDouble(values[3]);
                payload[4] = Double.parseDouble(values[4]);

                long timestamp = Long.parseLong(values[5]);
                SimpleEvent event = new SimpleEvent(payload, timestamp);

                TID tid;
                try {
                    tid = result.insert(event).get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Insert interrupted", e);
                } catch (java.util.concurrent.ExecutionException e) {
                    throw new RuntimeException("Insert failed", e.getCause());
                }

                EventID eventId = new EventID(tid.getBlockId(), tid.getOffset(), timestamp, sequenceId);

                CompositeKey<String, Double> keyLat = new CompositeKey<>((String) payload[1], (Double) payload[3]);
                primaryLatitudeIndex.getLSMTree().insert(new Pair<>(keyLat, eventId));

                CompositeKey<String, Double> keyLon = new CompositeKey<>((String) payload[1], (Double) payload[4]);
                primaryLongitudeIndex.getLSMTree().insert(new Pair<>(keyLon, eventId));

                CompositeKey<String, Short> keyBeat = new CompositeKey<>((String) payload[1], (Short) payload[2]);
                primaryBeatIndex.getLSMTree().insert(new Pair<>(keyBeat, eventId));

                 sequenceId++;

            }
        }

        primaryLatitudeIndex.close();
        primaryLongitudeIndex.close();
        primaryBeatIndex.close();

        System.out.println("Finished inserting data into composite indexes.");

        return result;
    }

    private static Map<String, Integer> buildPrimaryDictionary(Path input) throws IOException {
        Map<String, Integer> primaryDictionary = new HashMap<>();
        try (var r = Files.newBufferedReader(input)) {
            NavigableSet<String> primary = new TreeSet<>();
            r.readLine();

            String line = null;
            int lineNumber = 1;
            while ((line = r.readLine()) != null) {
                lineNumber++;

                if (line.trim().isEmpty()) {
                    System.out.println("Skipped empty line " + lineNumber);
                    continue;
                }

                String[] parts = line.split(";");

                if (parts.length >= 6) {
                    primary.add(parts[1].trim().toUpperCase());
                } else {
                    System.err.println("Line " + lineNumber + " has too few fields, expected at least 6, actual " + parts.length + ": " + line);

                    continue;
                }
            }

            int id = 0;
            for (String p : primary) {
                primaryDictionary.put(p, id++);

            }
        }
        return primaryDictionary;
    }

    public static Pair<AccessCountingPrimaryIndex, List<SecondaryTimeIndex<?>>> loadCrimeCompositeStore(ExperimentUtil.ExperimentConfig cfg) throws NumberFormatException, DBException, IOException {

        Path dest = cfg.basePath.resolve("db").resolve("crime");

        if (!Files.exists(dest)) {
            System.out.println("Creating tree");
            Path src = cfg.basePath.resolve("src").resolve("CrimesExample.csv");

            buildCrimeStoreWithCompositeIndexes(src, dest, cfg).close();
            System.out.println("Finished creating tree");
        }

        System.out.println("Loading tree and secondary indexes");

        AccessCountingPrimaryIndex primary = new AccessCountingPrimaryIndex(dest, cfg.mParams);

        List<SecondaryTimeIndex<?>> allIndexes = new ArrayList<>();

        for (Attribute a : primary.getSchema()) {
            if (a.getProperty("secondaryIndex") != null && a.getProperty("secondaryIndex").equalsIgnoreCase("true")) {
                AccessCountingLSMTimeIndex<?> independentIndex = new AccessCountingLSMTimeIndex<>(dest, primary.getSchema(), a.getName(),
                        primary.getTree().BLOCK_SIZE,
                        cfg.mParams.isUseDirectIO());
                allIndexes.add(independentIndex);
                System.out.println("Loaded independent index for: " + a.getName());
            }
        }

        AccessCountingLSMCompositeTimeIndex<String, Double> primaryLatitudeIndex =
                new AccessCountingLSMCompositeTimeIndex<>(dest, primary.getSchema(),
                        "PRIMARY", "LATITUDE", primary.getTree().BLOCK_SIZE, cfg.mParams.isUseDirectIO());
        allIndexes.add(primaryLatitudeIndex);

        AccessCountingLSMCompositeTimeIndex<String, Double> primaryLongitudeIndex =
                new AccessCountingLSMCompositeTimeIndex<>(dest, primary.getSchema(),
                        "PRIMARY", "LONGITUDE", primary.getTree().BLOCK_SIZE, cfg.mParams.isUseDirectIO());
        allIndexes.add(primaryLongitudeIndex);

        AccessCountingLSMCompositeTimeIndex<String, Short> primaryBeatIndex =
                new AccessCountingLSMCompositeTimeIndex<>(dest, primary.getSchema(),
                        "PRIMARY", "BEAT", primary.getTree().BLOCK_SIZE, cfg.mParams.isUseDirectIO());
        allIndexes.add(primaryBeatIndex);

        System.out.println("Loaded 3 composite indexes: (PRIMARY,LATITUDE), (PRIMARY,LONGITUDE), (PRIMARY,BEAT)");
        System.out.println("Total indexes: 1 independent (PRIMARY) + 3 composite = 4 indexes");

        System.out.println("******************** loadCrimeCompositeStore completed ********************");

        return new Pair<>(primary, allIndexes);
    }

    private static Symbol createCrimeSymbol(char name, String primary, int beat) {

        BooleanExpression be = new Equal(new Variable("PRIMARY"), new StringConstant(primary));

        if (beat >= 0)
            be = new And(be, new Equal(new Variable("BEAT"), new ShortConstant(beat)));

        Bindings bindings = new Bindings(
                new Binding(name + "_ID", new Variable("ID")),
                new Binding(name + "_PRIMARY", new Variable("PRIMARY")),
                new Binding(name + "_BEAT", new Variable("BEAT")),
                new Binding(name + "_LAT", new Variable("LATITUDE")),
                new Binding(name + "_LON", new Variable("LONGITUDE")));

        return new Symbol(name, be, bindings);

    }

    private static Symbol createCrimeSymbolWithBeatRange(char name, String primary, int beatMin, int beatMax) {

        BooleanExpression be = new Equal(new Variable("PRIMARY"), new StringConstant(primary));

        BooleanExpression beatBe = new Between(new Variable("BEAT"), new ShortConstant(beatMin), new ShortConstant(beatMax));
        be = new And(be, beatBe);

        Bindings bindings = new Bindings(
                new Binding(name + "_ID", new Variable("ID")),
                new Binding(name + "_PRIMARY", new Variable("PRIMARY")),
                new Binding(name + "_BEAT", new Variable("BEAT")),
                new Binding(name + "_LAT", new Variable("LATITUDE")),
                new Binding(name + "_LON", new Variable("LONGITUDE")));

        return new Symbol(name, be, bindings);

    }

    private static PatternMatcher getCrimeSequenceQueryWithOrPrimary(
            List<List<String>> primaryTypesList, long window) {
        List<Symbol> ss = new ArrayList<>();
        ss.add(new Symbol('X', new True()));

        char id = 'A';
        for (List<String> primaryTypes : primaryTypesList) {
            ss.add(createCrimeSymbolWithOrPrimary(id++, primaryTypes));
        }

        List<String> out = new ArrayList<>();
        List<Pattern> seq = new ArrayList<>();
        for (int i = 0; i < primaryTypesList.size(); i++) {
            char n = (char) (i + 'A');
            if (i > 0)
                seq.add(new KleeneStar(new Atom('X')));
            seq.add(new Atom(n));

            out.add(n + "_ID");
            out.add(n + "_PRIMARY");
            out.add(n + "_LAT");
            out.add(n + "_LON");
            out.add(n + "_BEAT");
        }

        Pattern p = new Sequence(seq);
        Output o = new Output(out.toArray(new String[0]));

        System.out.println(p);
        System.out.println(o);

        return new PatternMatcher(new Stream("crimes"), window, new Symbols(ss), p, o);
    }

    private static Symbol createCrimeSymbolWithOrPrimary(char name, List<String> primaryTypes) {
        BooleanExpression be;

        if (primaryTypes.size() == 1) {

            be = new Equal(new Variable("PRIMARY"), new StringConstant(primaryTypes.get(0)));
        } else {

            BooleanExpression firstBranch = new Equal(new Variable("PRIMARY"), new StringConstant(primaryTypes.get(0)));

            be = firstBranch;
            for (int i = 1; i < primaryTypes.size(); i++) {
                BooleanExpression branch = new Equal(new Variable("PRIMARY"), new StringConstant(primaryTypes.get(i)));
                be = new Or(be, branch);
            }
            System.out.println("[OR] symbol " + name + " has " + primaryTypes.size() + " PRIMARY values: " + primaryTypes);
        }

        Bindings bindings = new Bindings(
                new Binding(name + "_ID", new Variable("ID")),
                new Binding(name + "_PRIMARY", new Variable("PRIMARY")),
                new Binding(name + "_BEAT", new Variable("BEAT")),
                new Binding(name + "_LAT", new Variable("LATITUDE")),
                new Binding(name + "_LON", new Variable("LONGITUDE")));

        return new Symbol(name, be, bindings);
    }

    private static PatternMatcher getCrimeSequenceQueryWithOrPrimaryLat(
            List<List<String>> primaryTypesList, DoubleAttributeRange lat, long window) {
        List<Symbol> ss = new ArrayList<>();
        ss.add(new Symbol('X', new True()));

        char id = 'A';
        for (List<String> primaryTypes : primaryTypesList) {
            ss.add(createCrimeSymbolWithOrPrimaryLat(id++, primaryTypes, lat));
        }

        List<String> out = new ArrayList<>();
        List<Pattern> seq = new ArrayList<>();
        for (int i = 0; i < primaryTypesList.size(); i++) {
            char n = (char) (i + 'A');
            if (i > 0)
                seq.add(new KleeneStar(new Atom('X')));
            seq.add(new Atom(n));

            out.add(n + "_ID");
            out.add(n + "_PRIMARY");
            out.add(n + "_LAT");
            out.add(n + "_LON");
            out.add(n + "_BEAT");
        }

        Pattern p = new Sequence(seq);
        Output o = new Output(out.toArray(new String[0]));

        System.out.println(p);
        System.out.println(o);

        return new PatternMatcher(new Stream("crimes"), window, new Symbols(ss), p, o);
    }

    private static PatternMatcher getCrimeSequenceQueryWithOrPrimaryLon(
            List<List<String>> primaryTypesList, DoubleAttributeRange lon, long window) {
        List<Symbol> ss = new ArrayList<>();
        ss.add(new Symbol('X', new True()));

        char id = 'A';
        for (List<String> primaryTypes : primaryTypesList) {
            ss.add(createCrimeSymbolWithOrPrimaryLon(id++, primaryTypes, lon));
        }

        List<String> out = new ArrayList<>();
        List<Pattern> seq = new ArrayList<>();
        for (int i = 0; i < primaryTypesList.size(); i++) {
            char n = (char) (i + 'A');
            if (i > 0)
                seq.add(new KleeneStar(new Atom('X')));
            seq.add(new Atom(n));

            out.add(n + "_ID");
            out.add(n + "_PRIMARY");
            out.add(n + "_LAT");
            out.add(n + "_LON");
            out.add(n + "_BEAT");
        }

        Pattern p = new Sequence(seq);
        Output o = new Output(out.toArray(new String[0]));

        System.out.println(p);
        System.out.println(o);

        return new PatternMatcher(new Stream("crimes"), window, new Symbols(ss), p, o);
    }

    private static Symbol createCrimeSymbolWithOrPrimaryLon(char name, List<String> primaryTypes, DoubleAttributeRange lon) {
        BooleanExpression be = primaryDisjunction(primaryTypes);
        if (primaryTypes.size() > 1) {
            System.out.println("[OR] symbol " + name + " has " + primaryTypes.size() + " PRIMARY values: " + primaryTypes);
        }
        be = new And(be, new Between(new Variable("LONGITUDE"), new DoubleConstant(lon.getLower()), new DoubleConstant(lon.getUpper())));

        Bindings bindings = new Bindings(
                new Binding(name + "_ID", new Variable("ID")),
                new Binding(name + "_PRIMARY", new Variable("PRIMARY")),
                new Binding(name + "_BEAT", new Variable("BEAT")),
                new Binding(name + "_LAT", new Variable("LATITUDE")),
                new Binding(name + "_LON", new Variable("LONGITUDE")));

        return new Symbol(name, be, bindings);
    }

    private static Symbol createCrimeSymbolWithOrPrimaryLat(char name, List<String> primaryTypes, DoubleAttributeRange lat) {
        BooleanExpression be = primaryDisjunction(primaryTypes);
        if (primaryTypes.size() > 1) {
            System.out.println("[OR] symbol " + name + " has " + primaryTypes.size() + " PRIMARY values: " + primaryTypes);
        }
        be = new And(be, new Between(new Variable("LATITUDE"), new DoubleConstant(lat.getLower()), new DoubleConstant(lat.getUpper())));

        Bindings bindings = new Bindings(
                new Binding(name + "_ID", new Variable("ID")),
                new Binding(name + "_PRIMARY", new Variable("PRIMARY")),
                new Binding(name + "_BEAT", new Variable("BEAT")),
                new Binding(name + "_LAT", new Variable("LATITUDE")),
                new Binding(name + "_LON", new Variable("LONGITUDE")));

        return new Symbol(name, be, bindings);
    }

    private static Symbol createCrimeSymbolWithOrPrimaryBeat(char name, List<String> primaryTypes, int beatMin, int beatMax) {
        BooleanExpression be = primaryDisjunction(primaryTypes);
        if (primaryTypes.size() > 1) {
            System.out.println("[OR] symbol " + name + " has " + primaryTypes.size() + " PRIMARY values: " + primaryTypes);
        }
        be = new And(be, new Between(new Variable("BEAT"), new ShortConstant(beatMin), new ShortConstant(beatMax)));

        Bindings bindings = new Bindings(
                new Binding(name + "_ID", new Variable("ID")),
                new Binding(name + "_PRIMARY", new Variable("PRIMARY")),
                new Binding(name + "_BEAT", new Variable("BEAT")),
                new Binding(name + "_LAT", new Variable("LATITUDE")),
                new Binding(name + "_LON", new Variable("LONGITUDE")));

        return new Symbol(name, be, bindings);
    }

    private static BooleanExpression primaryDisjunction(List<String> primaryTypes) {
        BooleanExpression be = new Equal(new Variable("PRIMARY"), new StringConstant(primaryTypes.get(0)));
        for (int i = 1; i < primaryTypes.size(); i++) {
            be = new Or(be, new Equal(new Variable("PRIMARY"), new StringConstant(primaryTypes.get(i))));
        }
        return be;
    }

    private static PatternMatcher getCrimeSequenceQueryWithOrPrimaryLatLon(
            List<List<String>> primaryTypesList, DoubleAttributeRange lat, DoubleAttributeRange lon, long window) {
        List<Symbol> ss = new ArrayList<>();
        ss.add(new Symbol('X', new True()));

        char id = 'A';
        for (List<String> primaryTypes : primaryTypesList) {
            ss.add(createCrimeSymbolWithOrPrimaryLatLon(id++, primaryTypes, lat, lon));
        }

        List<String> out = new ArrayList<>();
        List<Pattern> seq = new ArrayList<>();
        for (int i = 0; i < primaryTypesList.size(); i++) {
            char n = (char) (i + 'A');
            if (i > 0)
                seq.add(new KleeneStar(new Atom('X')));
            seq.add(new Atom(n));

            out.add(n + "_ID");
            out.add(n + "_PRIMARY");
            out.add(n + "_LAT");
            out.add(n + "_LON");
            out.add(n + "_BEAT");
        }

        Pattern p = new Sequence(seq);
        Output o = new Output(out.toArray(new String[0]));

        System.out.println(p);
        System.out.println(o);

        return new PatternMatcher(new Stream("crimes"), window, new Symbols(ss), p, o);
    }

    private static Symbol createCrimeSymbolWithOrPrimaryLatLon(char name, List<String> primaryTypes, DoubleAttributeRange lat, DoubleAttributeRange lon) {
        BooleanExpression be = primaryDisjunction(primaryTypes);
        if (primaryTypes.size() > 1) {
            System.out.println("[OR] symbol " + name + " has " + primaryTypes.size() + " PRIMARY values: " + primaryTypes);
        }
        be = new And(be, new Between(new Variable("LATITUDE"), new DoubleConstant(lat.getLower()), new DoubleConstant(lat.getUpper())));
        be = new And(be, new Between(new Variable("LONGITUDE"), new DoubleConstant(lon.getLower()), new DoubleConstant(lon.getUpper())));

        Bindings bindings = new Bindings(
                new Binding(name + "_ID", new Variable("ID")),
                new Binding(name + "_PRIMARY", new Variable("PRIMARY")),
                new Binding(name + "_BEAT", new Variable("BEAT")),
                new Binding(name + "_LAT", new Variable("LATITUDE")),
                new Binding(name + "_LON", new Variable("LONGITUDE")));

        return new Symbol(name, be, bindings);
    }

    private static Symbol createCrimeSymbolLatLon(char name, String primary, DoubleAttributeRange lat, DoubleAttributeRange lon) {

        BooleanExpression primBe = new Equal(new Variable("PRIMARY"), new StringConstant(primary));

        BooleanExpression latBe = new Between(new Variable("LATITUDE"), new DoubleConstant(lat.getLower()), new DoubleConstant(lat.getUpper()));

        BooleanExpression lonBe = new Between(new Variable("LONGITUDE"), new DoubleConstant(lon.getLower()), new DoubleConstant(lon.getUpper()));

        BooleanExpression be = new And(new And(latBe, lonBe), primBe);

        Bindings bindings = new Bindings(
                new Binding(name + "_ID", new Variable("ID")),
                new Binding(name + "_PRIMARY", new Variable("PRIMARY")),
                new Binding(name + "_BEAT", new Variable("BEAT")),
                new Binding(name + "_LAT", new Variable("LATITUDE")),
                new Binding(name + "_LON", new Variable("LONGITUDE")));

        return new Symbol(name, be, bindings);
    }

    private static Symbol createCrimeSymbolWithLat(char name, String primary, DoubleAttributeRange lat) {
        BooleanExpression primBe = new Equal(new Variable("PRIMARY"), new StringConstant(primary));
        BooleanExpression latBe = new Between(new Variable("LATITUDE"), new DoubleConstant(lat.getLower()), new DoubleConstant(lat.getUpper()));
        BooleanExpression be = new And(primBe, latBe);

        Bindings bindings = new Bindings(
                new Binding(name + "_ID", new Variable("ID")),
                new Binding(name + "_PRIMARY", new Variable("PRIMARY")),
                new Binding(name + "_BEAT", new Variable("BEAT")),
                new Binding(name + "_LAT", new Variable("LATITUDE")),
                new Binding(name + "_LON", new Variable("LONGITUDE")));

        return new Symbol(name, be, bindings);
    }

    private static Symbol createCrimeSymbolWithLon(char name, String primary, DoubleAttributeRange lon) {
        BooleanExpression primBe = new Equal(new Variable("PRIMARY"), new StringConstant(primary));
        BooleanExpression lonBe = new Between(new Variable("LONGITUDE"), new DoubleConstant(lon.getLower()), new DoubleConstant(lon.getUpper()));
        BooleanExpression be = new And(primBe, lonBe);

        Bindings bindings = new Bindings(
                new Binding(name + "_ID", new Variable("ID")),
                new Binding(name + "_PRIMARY", new Variable("PRIMARY")),
                new Binding(name + "_BEAT", new Variable("BEAT")),
                new Binding(name + "_LAT", new Variable("LATITUDE")),
                new Binding(name + "_LON", new Variable("LONGITUDE")));

        return new Symbol(name, be, bindings);
    }

    private static PatternMatcher getCrimeSequenceQuery(List<String> primary, int beat, long window) {

        List<Symbol> ss = new ArrayList<>();
        ss.add(new Symbol('X', new True()));

        char id = 'A';
        for (String p : primary) {
            ss.add(createCrimeSymbol(id++, p, beat));

        }

        List<String> out = new ArrayList<>();
        List<Pattern> seq = new ArrayList<>();
        for (int i = 0; i < primary.size(); i++) {
            char n = (char) (i + 'A');
            if (i > 0)
                seq.add(new KleeneStar(new Atom('X')));

            seq.add(new Atom(n));

            out.add(n + "_ID");
            out.add(n + "_PRIMARY");
            out.add(n + "_LAT");
            out.add(n + "_LON");
            out.add(n + "_BEAT");

        }

        Pattern p = new Sequence(seq);

        Output o = new Output(out.toArray(new String[0]));

        System.out.println(p);
        System.out.println(o);

        return new PatternMatcher(new Stream("crimes"), window, new Symbols(ss), p, o);

    }

    private static PatternMatcher getCrimeSequenceQueryWithBeatRange(List<String> primary, int beatMin, int beatMax, long window) {
        List<Symbol> ss = new ArrayList<>();
        ss.add(new Symbol('X', new True()));

        char id = 'A';
        for (String p : primary) {
            ss.add(createCrimeSymbolWithBeatRange(id++, p, beatMin, beatMax));

        }

        List<String> out = new ArrayList<>();
        List<Pattern> seq = new ArrayList<>();
        for (int i = 0; i < primary.size(); i++) {
            char n = (char) (i + 'A');
            if (i > 0)
                seq.add(new KleeneStar(new Atom('X')));

            seq.add(new Atom(n));

            out.add(n + "_ID");
            out.add(n + "_PRIMARY");
            out.add(n + "_LAT");
            out.add(n + "_LON");
            out.add(n + "_BEAT");

        }

        Pattern p = new Sequence(seq);

        Output o = new Output(out.toArray(new String[0]));

        System.out.println(p);
        System.out.println(o);

        return new PatternMatcher(new Stream("crimes"), window, new Symbols(ss), p, o);

    }

    private static PatternMatcher buildCrossEventPattern(List<String> primaryTypes, List<int[]> beatRanges,
                                                         List<double[]> latitudeRanges, List<double[]> longitudeRanges,
                                                         long window) {
        List<Symbol> ss = new ArrayList<>();
        ss.add(new Symbol('X', new True()));
        for (int i = 0; i < primaryTypes.size(); i++) {
            char name = (char) ('A' + i);
            ss.add(createCrossEventSymbol(name, primaryTypes.get(i), beatRanges.get(i),
                    latitudeRanges.get(i), longitudeRanges.get(i)));
        }

        List<String> out = new ArrayList<>();
        List<Pattern> seq = new ArrayList<>();
        for (int i = 0; i < primaryTypes.size(); i++) {
            char n = (char) ('A' + i);
            if (i > 0) {
                seq.add(new KleeneStar(new Atom('X')));
            }
            seq.add(new Atom(n));
            out.add(n + "_ID");
            out.add(n + "_PRIMARY");
            out.add(n + "_LAT");
            out.add(n + "_LON");
            out.add(n + "_BEAT");
        }
        return new PatternMatcher(new Stream("crimes"), window, new Symbols(ss), new Sequence(seq),
                new Output(out.toArray(new String[0])));
    }

    private static Symbol createCrossEventSymbol(char name, String primary, int[] beat,
                                                double[] latitude, double[] longitude) {
        BooleanExpression be = new Equal(new Variable("PRIMARY"), new StringConstant(primary));
        if (beat != null) {
            be = new And(be, new Between(new Variable("BEAT"), new ShortConstant(beat[0]), new ShortConstant(beat[1])));
        }
        if (latitude != null) {
            be = new And(be, new Between(new Variable("LATITUDE"),
                    new DoubleConstant(latitude[0]), new DoubleConstant(latitude[1])));
        }
        if (longitude != null) {
            be = new And(be, new Between(new Variable("LONGITUDE"),
                    new DoubleConstant(longitude[0]), new DoubleConstant(longitude[1])));
        }
        Bindings bindings = new Bindings(
                new Binding(name + "_ID", new Variable("ID")),
                new Binding(name + "_PRIMARY", new Variable("PRIMARY")),
                new Binding(name + "_BEAT", new Variable("BEAT")),
                new Binding(name + "_LAT", new Variable("LATITUDE")),
                new Binding(name + "_LON", new Variable("LONGITUDE")));
        return new Symbol(name, be, bindings);
    }

    private static int[] readOptionalBeat(JsonObject query, char symbol) {
        String prefix = String.valueOf(Character.toLowerCase(symbol));
        String minKey = prefix + "BeatMin";
        String maxKey = prefix + "BeatMax";
        String pointKey = prefix + "Beat";
        boolean hasMin = query.containsKey(minKey);
        boolean hasMax = query.containsKey(maxKey);
        if (hasMin || hasMax) {
            if (!hasMin || !hasMax) {
                throw new IllegalArgumentException(symbol + " requires both " + minKey + " and " + maxKey);
            }
            return new int[]{query.getInt(minKey), query.getInt(maxKey)};
        }
        if (query.containsKey(pointKey)) {
            int beat = query.getInt(pointKey);
            return new int[]{beat, beat};
        }
        return null;
    }

    private static double[] readOptionalDoubleRange(JsonObject query, char symbol, String attribute) {
        String prefix = Character.toLowerCase(symbol) + attribute;
        String minKey = prefix + "Min";
        String maxKey = prefix + "Max";
        boolean hasMin = query.containsKey(minKey);
        boolean hasMax = query.containsKey(maxKey);
        if (hasMin || hasMax) {
            if (!hasMin || !hasMax) {
                throw new IllegalArgumentException(symbol + " requires both " + minKey + " and " + maxKey);
            }
            return new double[]{query.getJsonNumber(minKey).doubleValue(), query.getJsonNumber(maxKey).doubleValue()};
        }
        return null;
    }

    private static PatternMatcher getCrimeSequenceQueryWithIndividualBeat(
            List<String> primaryTypes, List<Integer> beats, long window) {
        List<Symbol> ss = new ArrayList<>();
        ss.add(new Symbol('X', new True()));

        char id = 'A';
        for (int i = 0; i < primaryTypes.size(); i++) {
            ss.add(createCrimeSymbol(id++, primaryTypes.get(i), beats.get(i)));
        }

        List<String> out = new ArrayList<>();
        List<Pattern> seq = new ArrayList<>();
        for (int i = 0; i < primaryTypes.size(); i++) {
            char n = (char) (i + 'A');
            if (i > 0)
                seq.add(new KleeneStar(new Atom('X')));
            seq.add(new Atom(n));

            out.add(n + "_ID");
            out.add(n + "_PRIMARY");
            out.add(n + "_LAT");
            out.add(n + "_LON");
            out.add(n + "_BEAT");
        }

        Pattern p = new Sequence(seq);
        Output o = new Output(out.toArray(new String[0]));

        System.out.println(p);
        System.out.println(o);

        return new PatternMatcher(new Stream("crimes"), window, new Symbols(ss), p, o);
    }

    private static PatternMatcher getCrimeSequenceQueryWithIndividualBeatRange(
            List<String> primaryTypes, List<int[]> beatRanges, long window) {
        List<Symbol> ss = new ArrayList<>();
        ss.add(new Symbol('X', new True()));

        char id = 'A';
        for (int i = 0; i < primaryTypes.size(); i++) {
            int[] range = beatRanges.get(i);
            ss.add(createCrimeSymbolWithBeatRange(id++, primaryTypes.get(i), range[0], range[1]));
        }

        List<String> out = new ArrayList<>();
        List<Pattern> seq = new ArrayList<>();
        for (int i = 0; i < primaryTypes.size(); i++) {
            char n = (char) (i + 'A');
            if (i > 0)
                seq.add(new KleeneStar(new Atom('X')));
            seq.add(new Atom(n));

            out.add(n + "_ID");
            out.add(n + "_PRIMARY");
            out.add(n + "_LAT");
            out.add(n + "_LON");
            out.add(n + "_BEAT");
        }

        Pattern p = new Sequence(seq);
        Output o = new Output(out.toArray(new String[0]));

        System.out.println(p);
        System.out.println(o);

        return new PatternMatcher(new Stream("crimes"), window, new Symbols(ss), p, o);
    }

    private static PatternMatcher getCrimeSequenceQueryWithOrPrimaryBeat(
            List<List<String>> primaryTypesList, int beatMin, int beatMax, long window) {
        List<Symbol> ss = new ArrayList<>();
        ss.add(new Symbol('X', new True()));

        char id = 'A';
        for (List<String> primaryTypes : primaryTypesList) {
            ss.add(createCrimeSymbolWithOrPrimaryBeat(id++, primaryTypes, beatMin, beatMax));
        }

        List<String> out = new ArrayList<>();
        List<Pattern> seq = new ArrayList<>();
        for (int i = 0; i < primaryTypesList.size(); i++) {
            char n = (char) (i + 'A');
            if (i > 0)
                seq.add(new KleeneStar(new Atom('X')));
            seq.add(new Atom(n));

            out.add(n + "_ID");
            out.add(n + "_PRIMARY");
            out.add(n + "_LAT");
            out.add(n + "_LON");
            out.add(n + "_BEAT");
        }

        Pattern p = new Sequence(seq);
        Output o = new Output(out.toArray(new String[0]));

        System.out.println(p);
        System.out.println(o);

        return new PatternMatcher(new Stream("crimes"), window, new Symbols(ss), p, o);
    }

    private static PatternMatcher buildSharedLatLonPattern(List<String> primary, long window, DoubleAttributeRange lat, DoubleAttributeRange lon) {

        List<Symbol> ss = new ArrayList<>();
        ss.add(new Symbol('X', new True()));

        char id = 'A';
        for (String p : primary) {
            ss.add(createCrimeSymbolLatLon(id++, p, lat, lon));

        }

        List<String> out = new ArrayList<>();
        List<Pattern> seq = new ArrayList<>();

        for (int i = 0; i < primary.size(); i++) {
            char n = (char) (i + 'A');
            if (i > 0)
                seq.add(new KleeneStar(new Atom('X')));
            seq.add(new Atom(n));

            out.add(n + "_ID");
            out.add(n + "_PRIMARY");
            out.add(n + "_BEAT");
            out.add(n + "_LAT");
            out.add(n + "_LON");
        }

        Pattern p = new Sequence(seq);
        Output o = new Output(out.toArray(new String[0]));
        return new PatternMatcher(new Stream("crimes"), window, new Symbols(ss), p, o);
    }

    private static PatternMatcher getCrimeSequenceQueryWithLatRange(List<String> primary, long window, DoubleAttributeRange lat) {
        List<Symbol> ss = new ArrayList<>();
        ss.add(new Symbol('X', new True()));

        char id = 'A';
        for (String p : primary) {
            ss.add(createCrimeSymbolWithLat(id++, p, lat));
        }

        List<String> out = new ArrayList<>();
        List<Pattern> seq = new ArrayList<>();
        for (int i = 0; i < primary.size(); i++) {
            char n = (char) (i + 'A');
            if (i > 0)
                seq.add(new KleeneStar(new Atom('X')));
            seq.add(new Atom(n));

            out.add(n + "_ID");
            out.add(n + "_PRIMARY");
            out.add(n + "_BEAT");
            out.add(n + "_LAT");
            out.add(n + "_LON");
        }

        Pattern p = new Sequence(seq);
        Output o = new Output(out.toArray(new String[0]));

        System.out.println(p);
        System.out.println(o);

        return new PatternMatcher(new Stream("crimes"), window, new Symbols(ss), p, o);
    }

    private static PatternMatcher getCrimeSequenceQueryWithLonRange(List<String> primary, long window, DoubleAttributeRange lon) {
        List<Symbol> ss = new ArrayList<>();
        ss.add(new Symbol('X', new True()));

        char id = 'A';
        for (String p : primary) {
            ss.add(createCrimeSymbolWithLon(id++, p, lon));
        }

        List<String> out = new ArrayList<>();
        List<Pattern> seq = new ArrayList<>();
        for (int i = 0; i < primary.size(); i++) {
            char n = (char) (i + 'A');
            if (i > 0)
                seq.add(new KleeneStar(new Atom('X')));
            seq.add(new Atom(n));

            out.add(n + "_ID");
            out.add(n + "_PRIMARY");
            out.add(n + "_BEAT");
            out.add(n + "_LAT");
            out.add(n + "_LON");
        }

        Pattern p = new Sequence(seq);
        Output o = new Output(out.toArray(new String[0]));

        System.out.println(p);
        System.out.println(o);

        return new PatternMatcher(new Stream("crimes"), window, new Symbols(ss), p, o);
    }

    private static PatternRun runIndexedPattern(PatternMatcher def, AccessCountingPrimaryIndex primary,
                                       List<SecondaryTimeIndex<?>> secondaries,
                                       Map<String, CachedCursor> sharedQueryResultCache)
            throws TranslatorException, IncompatibleTypeException {
        return runIndexedPattern(def, primary, secondaries, sharedQueryResultCache, List.of());
    }

    private static PatternRun runIndexedPattern(PatternMatcher def, AccessCountingPrimaryIndex primary,
                                       List<SecondaryTimeIndex<?>> secondaries,
                                       Map<String, CachedCursor> sharedQueryResultCache,
                                       List<AttributeEquality> attributeEqualities)
            throws TranslatorException, IncompatibleTypeException {

        PhaseTimers.beginQuery();
        long phase1Start = System.nanoTime();
        TransformedPattern pattern = Util.transformPattern(def, primary.getSchema());
        pattern.setAttributeEqualities(attributeEqualities);
        if (!attributeEqualities.isEmpty()) {
            System.out.println("Residual equalities: " + attributeEqualities.stream()
                    .map(eq -> eq.leftOutputName() + "=" + eq.rightOutputName())
                    .collect(Collectors.joining(", ")));
        }
        PhaseTimers.addPhase1(System.nanoTime() - phase1Start);

        System.out.println("Computing pattern stats");

        PatternStats stats = ExperimentsBasics.computePatternStats(primary, secondaries, pattern, sharedQueryResultCache);

        phase1Start = System.nanoTime();
        NaiveStrategySelector naive = new NaiveStrategySelector(primary, secondaries, pattern, stats, sharedQueryResultCache);
        naive.selectIndexes();

        ExperimentsBasics.IOMeasures iom = new ExperimentsBasics.IOMeasures();
        List<StrategyRunResult> strategyRuns = new ArrayList<>();

        var strategies = naive.getAllStrategies().stream().sorted().collect(Collectors.toList());

        ExecutionStrategy trueLightweightStrategy = null;
        ExecutionStrategy fullConditionStrategy = null;
        int maxConditions = 0;

        for (ExecutionStrategy es : strategies) {

            if (es.getConfig().getConditions().isEmpty()) {
                trueLightweightStrategy = es;
            }

            int conditionCount = es.getConfig().getConditions().size();
            if (conditionCount > maxConditions) {
                maxConditions = conditionCount;
                fullConditionStrategy = es;
            }
        }

        if (trueLightweightStrategy == null) {
            trueLightweightStrategy = strategies.get(0);
            System.out.println("Warning: no zero-condition strategy found; using the first strategy after sorting");
        }
        if (fullConditionStrategy == null) {
            fullConditionStrategy = strategies.get(strategies.size() - 1);
        }
        PhaseTimers.addPhase1(System.nanoTime() - phase1Start);

        double phaseTotalMs = PhaseTimers.reportAndContinue("query preparation");

        System.out.println("Warming Up");
        {

            String executionMode = "TWO_EXTREMES";

            switch (executionMode) {

                case "TWO_EXTREMES":

                    {
                        if (fullConditionStrategy instanceof SecondaryStrategy) {
                            SecondaryStrategy strategy = (SecondaryStrategy) fullConditionStrategy;
                            System.out.printf("* Secondary [%d sub-patterns, %d conditions]",
                                    strategy.getConfig().getSubPatterns().size(),
                                    strategy.getNumberOfIndexes());
                        } else {
                            System.out.println("* " + fullConditionStrategy);
                        }
                        strategyRuns.add(executeStrategy(primary, secondaries, fullConditionStrategy, iom));
                        phaseTotalMs += PhaseTimers.reportAndContinue("full-condition strategy");

                        System.out.println();
                    }
                    break;


                default:
                    System.out.println("Unknown execution mode: " + executionMode);
            }
        }

        long matchCount = strategyRuns.isEmpty()
                ? 0
                : strategyRuns.get(strategyRuns.size() - 1).matchCount;
        PhaseTimers.endQuery();
        return new PatternRun(matchCount, phaseTotalMs);

    }

    private static final class PatternRun {
        final long matchCount;
        final double phaseTotalMs;

        PatternRun(long matchCount, double phaseTotalMs) {
            this.matchCount = matchCount;
            this.phaseTotalMs = phaseTotalMs;
        }
    }

    private static boolean printMatchDetails() {
        return "detail".equalsIgnoreCase(System.getProperty("corematch.matchOutput", "detail"));
    }

    private static void printAnchorMatchGroup(PrintStream out, EventSchema schema, AnchorMatchResult group, long matchIndex) {
        out.println("\n *********** match " + matchIndex + " ***********");
        int count = 0;
        for (Event e : group.getAnchors()) {
            count++;
            StringBuilder strb = new StringBuilder();
            strb.append("event ").append(count).append(": ");
            int numAttributes = e.getNumberOfAttributes();
            for (int k = 0; k < numAttributes; k++) {
                if (k < schema.getNumAttributes()) {
                    strb.append(schema.getAttribute(k).getName()).append('=');
                }
                strb.append(e.get(k)).append('\t');
            }
            strb.append("TIMESTAMP=").append(e.getT1());
            out.println(strb);
        }
        out.println();
    }

    private static StrategyRunResult executeStrategy(AccessCountingPrimaryIndex primary,
                                                     List<? extends SecondaryTimeIndex<?>> secondaries,
                                                     ExecutionStrategy es, ExperimentsBasics.IOMeasures iom) throws IncompatibleTypeException, TranslatorException {
        iom.resetMeasures(primary, secondaries);

        long consumeExecuteStartTime = System.nanoTime();
        long matchCount = 0;
        double consumeExecuteDurationMs;

        PrintStream previousOut = System.out;
        System.setOut(new PrintStream(OutputStream.nullOutputStream()));
        try {
            if (es instanceof SecondaryStrategy
                    && ((SecondaryStrategy) es).getReplayIntervalPolicy() == ReplayIntervalPolicy.ANCHOR_ONLY) {
                Cursor<AnchorMatchResult> anchorCursor = ((SecondaryStrategy) es).executeAnchorMatches();
                anchorCursor.open();
                try {
                    while (anchorCursor.hasNext()) {
                        AnchorMatchResult group = anchorCursor.next();
                        matchCount++;
                        if (printMatchDetails()) {
                            printAnchorMatchGroup(previousOut, primary.getSchema(), group, matchCount);
                        }
                    }
                } finally {
                    anchorCursor.close();
                }
            } else {
                matchCount = ExperimentsBasics.consume(es.execute());
            }
        } finally {
            consumeExecuteDurationMs = (System.nanoTime() - consumeExecuteStartTime) / 1_000_000.0;
            System.setOut(previousOut);
        }

        iom.updateAndReset(es, primary, secondaries);
        return new StrategyRunResult(matchCount, consumeExecuteDurationMs);
    }

    private static final class StrategyRunResult {
        final long matchCount;
        final double timeMs;

        StrategyRunResult(long matchCount, double timeMs) {
            this.matchCount = matchCount;
            this.timeMs = timeMs;
        }
    }

    private static List<AttributeEquality> readAttributeEqualities(JsonObject query) {
        if (!query.containsKey("attributeEqualities")) {
            return List.of();
        }
        JsonArray array = query.getJsonArray("attributeEqualities");
        List<AttributeEquality> equalities = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            equalities.add(parseAttributeEquality(array.getString(i)));
        }
        return equalities;
    }

    private static AttributeEquality parseAttributeEquality(String text) {
        String[] sides = text.split("=");
        if (sides.length != 2) {
            throw new IllegalArgumentException("Expected Symbol.ATTR=Symbol.ATTR, got: " + text);
        }
        String[] left = sides[0].trim().split("\\.");
        String[] right = sides[1].trim().split("\\.");
        if (left.length != 2 || right.length != 2 || left[0].length() != 1 || right[0].length() != 1) {
            throw new IllegalArgumentException("Expected Symbol.ATTR=Symbol.ATTR, got: " + text);
        }
        return new AttributeEquality(left[0].charAt(0), left[1], right[0].charAt(0), right[1]);
    }

    private static JsonArray loadQueriesFromJSON() {
        try (InputStream inputStream = CrimeWithComposite.class.getClassLoader().getResourceAsStream(JSON_CONFIG);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
             JsonReader jsonReader = Json.createReader(reader)) {

            return jsonReader.readArray();
        } catch (Exception e) {
            System.err.println("Failed to read the JSON configuration: " + e.getMessage());
            e.printStackTrace();
            return Json.createArrayBuilder().build();
        }
    }

    private static void runCompositeQueries(AccessCountingPrimaryIndex primary,
                                              List<SecondaryTimeIndex<?>> secondaries,
                                              Map<String, Integer> primaryDictionary,
                                              PeakMemoryTracker mem)
            throws TranslatorException, IncompatibleTypeException {
        JsonArray queries = loadQueriesFromJSON();

        for (int i = 0; i < queries.size(); i++) {
            JsonObject query = queries.getJsonObject(i);
            String queryName = query.getString("name");
            String queryType = query.getString("type");

            Map<String, CachedCursor> sharedCache = new HashMap<>();

            mem.beginQuery(queryName);
            PatternRun run = null;
            List<AttributeEquality> equalities = readAttributeEqualities(query);
            try {

            if ("or_primary_beat".equals(queryType)) {
                System.out.println("Executing query with OR PRIMARY+BEAT from JSON config: " + queryName);
                long timeWindow = query.getJsonNumber("timeWindow").longValue();
                int beatMin = query.getInt("beatMin");
                int beatMax = query.getInt("beatMax");
                JsonArray primaryTypesListArray = query.getJsonArray("primaryTypesList");
                List<List<String>> primaryTypesList = new ArrayList<>();
                for (int j = 0; j < primaryTypesListArray.size(); j++) {
                    JsonArray innerArray = primaryTypesListArray.getJsonArray(j);
                    List<String> innerList = new ArrayList<>();
                    for (int k = 0; k < innerArray.size(); k++) {
                        innerList.add(innerArray.getString(k));
                    }
                    primaryTypesList.add(innerList);
                }
                System.out.println("[OR query] primaryTypesList: " + primaryTypesList + ", BEAT range: [" + beatMin + ", " + beatMax + "]");
                PatternMatcher defOrPrimaryBeat =
                    getCrimeSequenceQueryWithOrPrimaryBeat(primaryTypesList, beatMin, beatMax, timeWindow);
                run = runIndexedPattern(defOrPrimaryBeat, primary, secondaries, sharedCache, equalities);
                continue;
            }

            if ("or_primary".equals(queryType)) {
                System.out.println("Executing query with OR PRIMARY from JSON config: " + queryName);
                long timeWindow = query.getJsonNumber("timeWindow").longValue();
                JsonArray primaryTypesListArray = query.getJsonArray("primaryTypesList");
                List<List<String>> primaryTypesList = new ArrayList<>();
                for (int j = 0; j < primaryTypesListArray.size(); j++) {
                    JsonArray innerArray = primaryTypesListArray.getJsonArray(j);
                    List<String> innerList = new ArrayList<>();
                    for (int k = 0; k < innerArray.size(); k++) {
                        innerList.add(innerArray.getString(k));
                    }
                    primaryTypesList.add(innerList);
                }
                System.out.println("[OR query] primaryTypesList: " + primaryTypesList);
                PatternMatcher defOrPrimary =
                    getCrimeSequenceQueryWithOrPrimary(primaryTypesList, timeWindow);
                run = runIndexedPattern(defOrPrimary, primary, secondaries, sharedCache, equalities);
                continue;
            }

            if ("or_primary_lat".equals(queryType)) {
                System.out.println("Executing query with OR PRIMARY+LAT from JSON config: " + queryName);
                long timeWindow = query.getJsonNumber("timeWindow").longValue();
                JsonArray primaryTypesListArray = query.getJsonArray("primaryTypesList");
                List<List<String>> primaryTypesList = new ArrayList<>();
                for (int j = 0; j < primaryTypesListArray.size(); j++) {
                    JsonArray innerArray = primaryTypesListArray.getJsonArray(j);
                    List<String> innerList = new ArrayList<>();
                    for (int k = 0; k < innerArray.size(); k++) {
                        innerList.add(innerArray.getString(k));
                    }
                    primaryTypesList.add(innerList);
                }
                JsonObject latObj = query.getJsonObject("latitude");
                double latMin = latObj.getJsonNumber("min").doubleValue();
                double latMax = latObj.getJsonNumber("max").doubleValue();
                DoubleAttributeRange latRange = new DoubleAttributeRange("LATITUDE", latMin, latMax, true, true);
                System.out.println("[OR query] primaryTypesList: " + primaryTypesList + ", LATITUDE range: [" + latMin + ", " + latMax + "]");
                PatternMatcher defOrPrimaryLat =
                    getCrimeSequenceQueryWithOrPrimaryLat(primaryTypesList, latRange, timeWindow);
                run = runIndexedPattern(defOrPrimaryLat, primary, secondaries, sharedCache, equalities);
                continue;
            }

            if ("or_primary_lon".equals(queryType)) {
                System.out.println("Executing query with OR PRIMARY+LON from JSON config: " + queryName);
                long timeWindow = query.getJsonNumber("timeWindow").longValue();
                JsonArray primaryTypesListArray = query.getJsonArray("primaryTypesList");
                List<List<String>> primaryTypesList = new ArrayList<>();
                for (int j = 0; j < primaryTypesListArray.size(); j++) {
                    JsonArray innerArray = primaryTypesListArray.getJsonArray(j);
                    List<String> innerList = new ArrayList<>();
                    for (int k = 0; k < innerArray.size(); k++) {
                        innerList.add(innerArray.getString(k));
                    }
                    primaryTypesList.add(innerList);
                }
                JsonObject lonObj = query.getJsonObject("longitude");
                double lonMin = lonObj.getJsonNumber("min").doubleValue();
                double lonMax = lonObj.getJsonNumber("max").doubleValue();
                DoubleAttributeRange lonRange = new DoubleAttributeRange("LONGITUDE", lonMin, lonMax, true, true);
                System.out.println("[OR query] primaryTypesList: " + primaryTypesList + ", LONGITUDE range: [" + lonMin + ", " + lonMax + "]");
                PatternMatcher defOrPrimaryLon =
                    getCrimeSequenceQueryWithOrPrimaryLon(primaryTypesList, lonRange, timeWindow);
                run = runIndexedPattern(defOrPrimaryLon, primary, secondaries, sharedCache, equalities);
                continue;
            }

            if ("or_primary_lat_lon".equals(queryType)) {
                System.out.println("Executing query with OR PRIMARY+LAT+LON from JSON config: " + queryName);
                long timeWindow = query.getJsonNumber("timeWindow").longValue();
                JsonArray primaryTypesListArray = query.getJsonArray("primaryTypesList");
                List<List<String>> primaryTypesList = new ArrayList<>();
                for (int j = 0; j < primaryTypesListArray.size(); j++) {
                    JsonArray innerArray = primaryTypesListArray.getJsonArray(j);
                    List<String> innerList = new ArrayList<>();
                    for (int k = 0; k < innerArray.size(); k++) {
                        innerList.add(innerArray.getString(k));
                    }
                    primaryTypesList.add(innerList);
                }
                JsonObject latObj = query.getJsonObject("latitude");
                JsonObject lonObj = query.getJsonObject("longitude");
                double latMin = latObj.getJsonNumber("min").doubleValue();
                double latMax = latObj.getJsonNumber("max").doubleValue();
                double lonMin = lonObj.getJsonNumber("min").doubleValue();
                double lonMax = lonObj.getJsonNumber("max").doubleValue();
                DoubleAttributeRange latRange = new DoubleAttributeRange("LATITUDE", latMin, latMax, true, true);
                DoubleAttributeRange lonRange = new DoubleAttributeRange("LONGITUDE", lonMin, lonMax, true, true);
                System.out.println("[OR query] primaryTypesList: " + primaryTypesList + ", LATITUDE range: [" + latMin + ", " + latMax + "], LONGITUDE range: [" + lonMin + ", " + lonMax + "]");
                PatternMatcher defOrPrimaryLatLon =
                    getCrimeSequenceQueryWithOrPrimaryLatLon(primaryTypesList, latRange, lonRange, timeWindow);
                run = runIndexedPattern(defOrPrimaryLatLon, primary, secondaries, sharedCache, equalities);
                continue;
            }

            if ("individual_beat".equals(queryType)) {
                System.out.println("Executing query with individual BEAT from JSON config: " + queryName);
                long timeWindow = query.getJsonNumber("timeWindow").longValue();
                JsonArray primaryTypesArray = query.getJsonArray("primaryTypes");
                List<String> primaryTypes = new ArrayList<>();
                for (int j = 0; j < primaryTypesArray.size(); j++) {
                    primaryTypes.add(primaryTypesArray.getString(j));
                }
                JsonArray beatsArray = query.getJsonArray("beats");
                List<Integer> beats = new ArrayList<>();
                for (int j = 0; j < beatsArray.size(); j++) {
                    beats.add(beatsArray.getInt(j));
                }
                System.out.println("[Individual BEAT] primaryTypes: " + primaryTypes + ", beats: " + beats);
                PatternMatcher defIndividualBeat =
                    getCrimeSequenceQueryWithIndividualBeat(primaryTypes, beats, timeWindow);
                run = runIndexedPattern(defIndividualBeat, primary, secondaries, sharedCache, equalities);
                continue;
            }

            if ("individual_beat_range".equals(queryType)) {
                System.out.println("Executing query with individual BEAT range from JSON config: " + queryName);
                long timeWindow = query.getJsonNumber("timeWindow").longValue();
                JsonArray primaryTypesArray = query.getJsonArray("primaryTypes");
                List<String> primaryTypes = new ArrayList<>();
                for (int j = 0; j < primaryTypesArray.size(); j++) {
                    primaryTypes.add(primaryTypesArray.getString(j));
                }
                JsonArray beatRangesArray = query.getJsonArray("beatRanges");
                List<int[]> beatRanges = new ArrayList<>();
                for (int j = 0; j < beatRangesArray.size(); j++) {
                    JsonObject rangeObj = beatRangesArray.getJsonObject(j);
                    beatRanges.add(new int[]{rangeObj.getInt("beatMin"), rangeObj.getInt("beatMax")});
                }
                System.out.println("[Individual BEAT range] primaryTypes: " + primaryTypes + ", beatRanges: " + beatRanges.stream().map(r -> "[" + r[0] + "," + r[1] + "]").collect(Collectors.joining(",")));
                PatternMatcher defIndividualBeatRange =
                    getCrimeSequenceQueryWithIndividualBeatRange(primaryTypes, beatRanges, timeWindow);
                run = runIndexedPattern(defIndividualBeatRange, primary, secondaries, sharedCache, equalities);
                continue;
            }

            JsonArray primaryTypesArray = query.getJsonArray("primaryTypes");
            List<String> primaryTypes = new ArrayList<>();
            for (int j = 0; j < primaryTypesArray.size(); j++) {
                String typeName = primaryTypesArray.getString(j);

                primaryTypes.add(typeName);
            }
            long timeWindow = query.getJsonNumber("timeWindow").longValue();

            if ("cross_event_beat".equals(queryType) || "cross_event".equals(queryType)) {
                List<int[]> beatRanges = new ArrayList<>();
                List<double[]> latitudeRanges = new ArrayList<>();
                List<double[]> longitudeRanges = new ArrayList<>();
                StringBuilder constraintText = new StringBuilder();
                for (int s = 0; s < primaryTypes.size(); s++) {
                    char symbol = (char) ('A' + s);
                    int[] beat = readOptionalBeat(query, symbol);
                    double[] latitude = readOptionalDoubleRange(query, symbol, "Latitude");
                    double[] longitude = readOptionalDoubleRange(query, symbol, "Longitude");
                    beatRanges.add(beat);
                    latitudeRanges.add(latitude);
                    longitudeRanges.add(longitude);
                    if (constraintText.length() > 0) {
                        constraintText.append(", ");
                    }
                    constraintText.append(symbol);
                    if (beat == null && latitude == null && longitude == null) {
                        constraintText.append("=type");
                    } else {
                        if (beat != null) {
                            constraintText.append(" beat[").append(beat[0]).append(',').append(beat[1]).append(']');
                        }
                        if (latitude != null) {
                            constraintText.append(" lat[").append(latitude[0]).append(',').append(latitude[1]).append(']');
                        }
                        if (longitude != null) {
                            constraintText.append(" lon[").append(longitude[0]).append(',').append(longitude[1]).append(']');
                        }
                    }
                }
                System.out.println("Executing cross-event query from JSON config: " + queryName + " " + constraintText);
                PatternMatcher defCrossEvent = buildCrossEventPattern(
                        primaryTypes, beatRanges, latitudeRanges, longitudeRanges, timeWindow);
                run = runIndexedPattern(defCrossEvent, primary, secondaries, sharedCache, equalities);
            } else if ("beat_range".equals(queryType)) {

                System.out.println("Executing query with BEAT range from JSON config: " + queryName);
                int beatMin = query.getInt("beatMin");
                int beatMax = query.getInt("beatMax");
                PatternMatcher defBeatRange =
                        getCrimeSequenceQueryWithBeatRange(primaryTypes, beatMin, beatMax, timeWindow);
                run = runIndexedPattern(defBeatRange, primary, secondaries, sharedCache, equalities);
            } else if ("lat_lon_range".equals(queryType)) {
                System.out.println("Executing query with LAT/LON from JSON config: " + queryName);
                JsonObject latObj = query.getJsonObject("latitude");
                JsonObject lonObj = query.getJsonObject("longitude");
                double latMin = latObj.getJsonNumber("min").doubleValue();
                double latMax = latObj.getJsonNumber("max").doubleValue();
                double lonMin = lonObj.getJsonNumber("min").doubleValue();
                double lonMax = lonObj.getJsonNumber("max").doubleValue();

                DoubleAttributeRange latRange = new DoubleAttributeRange("LATITUDE", latMin, latMax, true, true);
                DoubleAttributeRange lonRange = new DoubleAttributeRange("LONGITUDE", lonMin, lonMax, true, true);

                PatternMatcher defLatLon =
                    buildSharedLatLonPattern(primaryTypes, timeWindow, latRange, lonRange);
                run = runIndexedPattern(defLatLon, primary, secondaries, sharedCache, equalities);
            } else if ("lat_range".equals(queryType)) {
                System.out.println("Executing query with LAT range only from JSON config: " + queryName);
                JsonObject latObj = query.getJsonObject("latitude");
                double latMin = latObj.getJsonNumber("min").doubleValue();
                double latMax = latObj.getJsonNumber("max").doubleValue();

                DoubleAttributeRange latRange = new DoubleAttributeRange("LATITUDE", latMin, latMax, true, true);

                PatternMatcher defLat =
                    getCrimeSequenceQueryWithLatRange(primaryTypes, timeWindow, latRange);
                run = runIndexedPattern(defLat, primary, secondaries, sharedCache, equalities);
            } else if ("lon_range".equals(queryType)) {
                System.out.println("Executing query with LON range only from JSON config: " + queryName);
                JsonObject lonObj = query.getJsonObject("longitude");
                double lonMin = lonObj.getJsonNumber("min").doubleValue();
                double lonMax = lonObj.getJsonNumber("max").doubleValue();

                DoubleAttributeRange lonRange = new DoubleAttributeRange("LONGITUDE", lonMin, lonMax, true, true);

                PatternMatcher defLon =
                    getCrimeSequenceQueryWithLonRange(primaryTypes, timeWindow, lonRange);
                run = runIndexedPattern(defLon, primary, secondaries, sharedCache, equalities);
            } else if ("primary_type".equals(queryType)) {
                System.out.println("Executing query with PRIMARY TYPE from JSON config: " + queryName);
                PatternMatcher defPrimaryType =
                    ChicagoCrimeHelper.getCrimeSequenceQueryWithPrimaryTypes(primaryTypes, timeWindow);
                run = runIndexedPattern(defPrimaryType, primary, secondaries, sharedCache, equalities);
            }
            } finally {
                double peakMb = mem.endQuery(queryName);
                if (run != null) {
                    System.out.printf("results[%d, %.3f, %.1f]%n", run.matchCount, run.phaseTotalMs, peakMb);
                }

            }
        }
    }

    private static final class PeakMemoryTracker {
        private final MemoryMXBean mxBean = ManagementFactory.getMemoryMXBean();
        private final AtomicLong peakUsedBytes = new AtomicLong(0);
        private final AtomicLong queryPeakBytes = new AtomicLong(0);
        private final AtomicBoolean running = new AtomicBoolean(false);
        private final AtomicBoolean inQuery = new AtomicBoolean(false);
        private volatile long queryStartBytes = 0;
        private Thread sampler;

        long usedBytes() {
            return mxBean.getHeapMemoryUsage().getUsed();
        }

        private void observe(long used) {
            peakUsedBytes.accumulateAndGet(used, Math::max);
            if (inQuery.get()) {
                queryPeakBytes.accumulateAndGet(used, Math::max);
            }
        }

        void sample(String label) {
            long used = usedBytes();
            observe(used);
            System.out.printf("[Mem] %-28s used=%.1f MB  peak=%.1f MB%n",
                    label, used / (1024.0 * 1024.0), peakUsedBytes.get() / (1024.0 * 1024.0));
        }

        void beginQuery(String queryName) {
            long used = usedBytes();
            queryStartBytes = used;
            queryPeakBytes.set(used);
            inQuery.set(true);
            observe(used);
        }

        double endQuery(String queryName) {
            long used = usedBytes();
            observe(used);
            inQuery.set(false);
            return queryPeakBytes.get() / (1024.0 * 1024.0);
        }

        void start(long intervalMs) {
            if (!running.compareAndSet(false, true)) {
                return;
            }
            sample("start");
            sampler = new Thread(() -> {
                while (running.get()) {
                    observe(usedBytes());
                    try {
                        Thread.sleep(intervalMs);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }, "peak-mem-sampler");
            sampler.setDaemon(true);
            sampler.start();
        }

        void stopAndReport() {
            running.set(false);
            if (sampler != null) {
                try {
                    sampler.join(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    public static void main(String[] args) throws DBException, NumberFormatException, IOException,
            TranslatorException, IncompatibleTypeException {

        System.setProperty("pattern.replayInterval", "ANCHOR_ONLY");
        System.setProperty("pattern.matchPolicy", "STAM");
        System.setProperty("pattern.stamMode", "DFS");

        PeakMemoryTracker mem = new PeakMemoryTracker();
        mem.start(200);

        var config = ExperimentUtil.getConfig();
        Pair<AccessCountingPrimaryIndex, List<SecondaryTimeIndex<?>>> p = null;

        try {

            p = loadCrimeCompositeStore(config);
            mem.sample("after loadCrimeCompositeStore");

            var primary = p.getElement1();

            TIDCursor.setPrimaryIndex(primary);
            System.out.println("Index height is  : " + primary.getHeight());
            System.out.println("Compression ratio: " + primary.getCompressionRatio());

            System.out.print("Warming up page cache...");
            System.out.println(" Done! (" + ExperimentsBasics.warmUpPageCache(primary) + ")");
            mem.sample("after warmUp");

            Path src = config.basePath.resolve("src").resolve("CrimesExample.csv");
            Map<String, Integer> primaryDictionary = buildPrimaryDictionary(src);

            runCompositeQueries(primary, p.getElement2(), primaryDictionary, mem);
        } finally {
            if (p != null) {

                System.out.println("Primary index:");
                System.out.println(p.getElement1().toString());

                System.out.println("Secondary indexes:");
                p.getElement2().forEach(index ->{
                    System.out.println(index.toString());
                });

                p.getElement1().close();

                p.getElement2().forEach(SecondaryTimeIndex::close);
            }
            mem.stopAndReport();
        }
    }

}
