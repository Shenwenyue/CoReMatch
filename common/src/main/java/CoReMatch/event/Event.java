package CoReMatch.event;

public interface Event {

    Object get(int index);

    <T> T get(int index, Class<T> clazz);

    long getT1();

    long getT2();

    int getNumberOfAttributes();

    Event window(long t1, long t2);

}
