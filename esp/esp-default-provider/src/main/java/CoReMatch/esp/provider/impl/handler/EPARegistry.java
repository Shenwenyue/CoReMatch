package CoReMatch.esp.provider.impl.handler;

import CoReMatch.esp.api.epa.EPA;
import CoReMatch.esp.api.provider.AlreadyRegisteredException;
import CoReMatch.esp.api.provider.NoSuchQueryException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class EPARegistry implements Iterable<EPAHandler> {

    private final Map<EPA, EPAHandler> epas = new HashMap<>();

    public EPARegistry() {
    }

    public void addEPA(EPAHandler epa) throws AlreadyRegisteredException {
        if (epas.containsKey(epa.getDefinition()))
            throw new AlreadyRegisteredException("This EPA is already registered!");

        epas.put(epa.getDefinition(), epa);
    }

    public EPAHandler getEPA(EPA epa) throws NoSuchQueryException {
        EPAHandler result = epas.get(epa);
        if (result == null)
            throw new NoSuchQueryException("No EPA found for definition: " + epa);
        return result;
    }

    public EPAHandler removeEPA(EPA epa) throws NoSuchQueryException {
        EPAHandler result = epas.remove(epa);
        if (result == null)
            throw new NoSuchQueryException("No EPA found for definition: " + epa);
        return result;
    }

    public boolean hasEPA(EPA epa) {
        return epas.containsKey(epa);
    }

    @Override
    public Iterator<EPAHandler> iterator() {
        return epas.values().iterator();
    }

}
