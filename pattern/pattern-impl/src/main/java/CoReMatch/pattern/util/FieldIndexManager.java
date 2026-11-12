package CoReMatch.pattern.util;

import CoReMatch.db.core.primaryindex.PrimaryIndex;
import CoReMatch.event.EventSchema;
import CoReMatch.event.SchemaException;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

public class FieldIndexManager {

    private static PrimaryIndex primaryIndex;
    private static Map<String, Integer> attributeIndexMap = new HashMap<>();

    public static void setPrimaryIndex(PrimaryIndex index) {
        primaryIndex = index;
        attributeIndexMap.clear();

        if (index != null) {
            EventSchema schema = index.getSchema();

            for (int i = 0; i < schema.getNumAttributes(); i++) {
                String attributeName = schema.getAttribute(i).getName();
                attributeIndexMap.put(attributeName, i);
            }
        }
    }

    public static void setPrimaryIndex(PrimaryIndex index, Set<String> fieldNames) {
        primaryIndex = index;
        attributeIndexMap.clear();

        if (index != null) {
            EventSchema schema = index.getSchema();
            for (String fieldName : fieldNames) {
                try {
                    int idx = schema.getAttributeIndex(fieldName);
                    attributeIndexMap.put(fieldName, idx);
                } catch (SchemaException ignored) {
                }
            }
        }
    }

    public static int getAttributeIndex(String attributeName) {
        return attributeIndexMap.getOrDefault(attributeName, -1);
    }

    public static boolean hasAttribute(String attributeName) {
        return attributeIndexMap.containsKey(attributeName);
    }

    public static PrimaryIndex getPrimaryIndex() {
        return primaryIndex;
    }

    public static Set<String> getAllFieldNames() {
        return new HashSet<>(attributeIndexMap.keySet());
    }
}
