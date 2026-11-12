package CoReMatch.esp.api.util;

import CoReMatch.esp.api.provider.EventSink;
import CoReMatch.event.Event;

import java.io.PrintStream;

/**
 * A sink printing each event to the given {@link PrintStream}
 */
public class PrintingSink implements EventSink {

    /**
     * The stream to print to
     */
    private final PrintStream stream;

    /**
     * Creates a new instance using {@link System#out} as stream
     */
    public PrintingSink() {
        this(System.out);
    }

    /**
     * Creates a new instance
     *
     * @param stream the stream to print events to
     */
    public PrintingSink(PrintStream stream) {
        this.stream = stream;
    }

    @Override
    public void receiveEvent(Event event) {
        stream.println(event.toString());
    }

}
