package CoReMatch.esp.api.bridge;

import CoReMatch.esp.api.EPProvider;
import CoReMatch.event.Event;

/**
 * Callback used by EPA-instances to report results
 */
public interface EPACallback {

    /**
     * Reports results to the running {@link EPProvider}
     *
     * @param event the result event
     */
    void receive(Event event);

}
