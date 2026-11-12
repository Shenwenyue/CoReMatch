package CoReMatch.event;

import com.vividsolutions.jts.geom.Geometry;
import CoReMatch.common.IncompatibleTypeException;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Attribute implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;

    private DataType type;

    private Map<String, String> properties;

     public Attribute(String name, DataType type) {

        if (!name.matches("[a-zA-Z]+[a-zA-Z0-9_]*"))
            throw new SchemaException("Illegal attribute name: " + name + ". Must be of the form [a-zA-Z][a-zA-Z0-9_]*");

        this.name = name.toUpperCase();
        this.type = type;
        this.properties = new HashMap<>();
        if (type == DataType.STRING || type == DataType.GEOMETRY)
            setMaxStringSize(200);
    }

    public Attribute(Attribute a) {
        this.name = a.name;
        this.type = a.type;
        this.properties = new HashMap<>(a.properties);
    }

    private Attribute() {
        this.properties = new HashMap<>();
    }

    public static Attribute read(DataInput dataInput) throws IOException {
        Attribute result = new Attribute();
        result.name = dataInput.readUTF();
        result.type = DataType.valueOf(dataInput.readUTF().toUpperCase());
        int propertyCount = dataInput.readInt();
        for (int i = 0; i < propertyCount; i++) {
            result.properties.put(dataInput.readUTF(), dataInput.readUTF());
        }
        return result;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (!name.matches("[a-zA-Z]+[a-zA-Z0-9_]*"))
            throw new SchemaException("Illegal attribute name: " + name + ". Must be of the form [a-zA-Z][a-zA-Z0-9_]*");
        this.name = name.toUpperCase();
    }

    public String getQualifiedName() {
        String qualifier = properties.get(SchemaProperty.QUALIFIER.key);
        return qualifier != null ? qualifier + "." + name : name;
    }

    public String getQualifier() {
        return properties.get(SchemaProperty.QUALIFIER.key);
    }

    public void setQualifier(String name) {
        properties.put(SchemaProperty.QUALIFIER.key, name == null ? null : name.toUpperCase());
    }

    public Set<String> getProperties() {
        return properties.keySet();
    }

    public DataType getType() {
        return type;
    }

    public void setType(DataType type) {
        this.type = type;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.type == null) ? 0 : this.type.hashCode());
        result = prime * result + ((this.name == null) ? 0 : this.name.hashCode());
        result = prime * result + ((this.properties == null) ? 0 : this.properties.hashCode());
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
        Attribute other = (Attribute) obj;
        if (this.type != other.type)
            return false;
        if (this.name == null) {
            if (other.name != null)
                return false;
        } else if (!this.name.equals(other.name))
            return false;
        if (this.properties == null) {
            if (other.properties != null)
                return false;
        } else if (!this.properties.equals(other.properties))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return name + ":" + type;
    }

    public void write(DataOutput dataOutput) throws IOException {
        dataOutput.writeUTF(name);
        dataOutput.writeUTF(type.toString());
        dataOutput.writeInt(properties.size());
        for (String s : properties.keySet()) {
            dataOutput.writeUTF(s);
            dataOutput.writeUTF(properties.get(s));
        }
    }

    public int getMaxStringSize() {
        if (type == DataType.STRING || type == DataType.GEOMETRY)
            return Integer.valueOf(getProperty(SchemaProperty.MAX_STRING_SIZE.key)) / 4;
        else
            return 0;
    }

    public Attribute setMaxStringSize(int maxStringSize) {
        if (type == DataType.STRING || type == DataType.GEOMETRY)
            setProperty(SchemaProperty.MAX_STRING_SIZE.key, ((Integer) (maxStringSize * 4)).toString());
        return this;
    }

    public Attribute copy() {
        return new Attribute(this);
    }

    public Attribute setIsKey() {
        setProperty(SchemaProperty.IS_KEY.key, Boolean.toString(true));
        return this;
    }

    public Attribute unsetIsKey() {
        removeProperty(SchemaProperty.IS_KEY.key);
        return this;
    }

    public boolean isKey() {
        return Boolean.valueOf(getProperty(SchemaProperty.IS_KEY.key));
    }

    public boolean isNullable() {
        return Boolean.valueOf(getProperty(SchemaProperty.NULLABLE.key));
    }

    public void setNullable(boolean value) {
        setProperty(SchemaProperty.NULLABLE.key, Boolean.toString(value));
    }

    public String getProperty(String property) {
        return properties.get(property);
    }

    public Attribute setProperty(String property, String value) {
        properties.put(property, value);
        return this;
    }

    public Attribute removeProperty(String property) {
        if (hasProperty(property))
            properties.remove(property);
        return this;
    }

    public boolean hasProperty(String property) {
        return properties.containsKey(property);
    }

    public enum DataType {

        BYTE(Byte.class, true, true, false),

        SHORT(Short.class, true, true, false),

        INTEGER(Integer.class, true, true, false),

        LONG(Long.class, true, true, false),

        FLOAT(Float.class, true, false, true),

        DOUBLE(Double.class, true, false, true),

        STRING(String.class, false, false, false),

        GEOMETRY(Geometry.class, false, false, false),

        SET(ArrayList.class, false, false, false);

        private final Class<?> javaType;
        private final boolean numeric;
        private final boolean integral;
        private final boolean decimal;

        private DataType(Class<?> javaType, boolean numeric, boolean integral, boolean decimal) {
            this.javaType = javaType;
            this.numeric = numeric;
            this.integral = integral;
            this.decimal = decimal;
        }

        public static DataType getGCT(DataType t1, DataType t2) throws IncompatibleTypeException {
            if (t1 == t2)
                return t1;
            else if (!(t1.isNumeric() && t2.isNumeric()))
                throw new IncompatibleTypeException(t1 + " and " + t2 + " have no common super-type");

            return values()[Math.max(t1.ordinal(), t2.ordinal())];
        }

        public static DataType fromValue(Object value) {
            return fromClass(value.getClass());
        }

        public static DataType fromClass(Class<?> clazz) {
            for (DataType t : values())
                if (t.javaType.isAssignableFrom(clazz))
                    return t;
            throw new IllegalArgumentException("No datatype for class: " + clazz.getName());
        }

        public Class<?> getJavaType() {
            return javaType;
        }

        public boolean isNumeric() {
            return numeric;
        }

        public boolean isIntegral() {
            return integral;
        }

        public boolean isDecimal() {
            return decimal;
        }
    }

    public static enum SchemaProperty {

        IS_KEY("IsKey"),
        MAX_STRING_SIZE("MaxStringSize"),
        QUALIFIER("q_id"),
        NULLABLE("nullable");

        public final String key;

        private SchemaProperty(String key) {
            this.key = key;
        }
    }
}
