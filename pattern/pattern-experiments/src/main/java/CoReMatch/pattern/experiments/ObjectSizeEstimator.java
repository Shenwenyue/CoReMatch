package CoReMatch.pattern.experiments;
import java.lang.instrument.Instrumentation;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

public class ObjectSizeEstimator {

    private static final int OBJECT_HEADER_SIZE = 16;

    private static final int REFERENCE_SIZE = 8;

    public static long estimateObjectSize(Object obj) {
        if (obj == null) return 0;

        Set<Object> visited = new HashSet<>();
        Deque<Object> toVisit = new LinkedList<>();
        toVisit.add(obj);
        visited.add(obj);

        long size = 0;

        while (!toVisit.isEmpty()) {
            Object current = toVisit.poll();
            Class<?> clazz = current.getClass();

            if (clazz.isPrimitive() || clazz.isEnum()) {
                size += getPrimitiveSize(clazz);
                continue;
            }

            size += OBJECT_HEADER_SIZE;

            if (clazz.isArray()) {
                int length = Array.getLength(current);
                Class<?> componentType = clazz.getComponentType();

                if (componentType.isPrimitive()) {
                    size += length * getPrimitiveSize(componentType);
                } else {
                    size += length * REFERENCE_SIZE;
                    for (int i = 0; i < length; i++) {
                        Object element = Array.get(current, i);
                        if (element != null && !visited.contains(element)) {
                            toVisit.add(element);
                            visited.add(element);
                        }
                    }
                }
                continue;
            }

            for (Field field : clazz.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;

                field.setAccessible(true);
                Class<?> fieldType = field.getType();

                try {
                    if (fieldType.isPrimitive()) {
                        size += getPrimitiveSize(fieldType);
                    } else {
                        size += REFERENCE_SIZE;
                        Object fieldValue = field.get(current);
                        if (fieldValue != null && !visited.contains(fieldValue)) {
                            toVisit.add(fieldValue);
                            visited.add(fieldValue);
                        }
                    }
                } catch (IllegalAccessException e) {

                }
            }
        }

        return size;
    }

    private static int getPrimitiveSize(Class<?> clazz) {
        if (clazz == boolean.class || clazz == byte.class) return 1;
        if (clazz == char.class || clazz == short.class) return 2;
        if (clazz == int.class || clazz == float.class) return 4;
        if (clazz == long.class || clazz == double.class) return 8;
        return 0;
    }
}
