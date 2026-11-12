package CoReMatch.db.event;

import CoReMatch.event.Event;

public class PersistentEvent extends Persistent<Event> implements Event {

    public PersistentEvent(TID id, Event item) {
        super(id, item);
    }

    @Override
    public Object get(int index) {
        return item.get(index);
    }

    @Override
    public <T> T get(int index, Class<T> clazz) {
        return item.get(index, clazz);
    }

    @Override
    public long getT1() {
        return item.getT1();
    }

    @Override
    public long getT2() {
        return item.getT2();
    }

    @Override
    public int getNumberOfAttributes() {
        return item.getNumberOfAttributes();
    }

    @Override
    public PersistentEvent window(long t1, long t2) {
        return new PersistentEvent(id, item.window(t1, t2));
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("{");
        for (int i = 0; i < getNumberOfAttributes(); i++) {
            builder.append(get(i)).append(", ");
        }
        builder.append(getT1());
        builder.append("}");
        return builder.toString();
    }
}
