package no.netb.libjsqlite;

import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.util.*;
import java.util.function.Function;

public class TypeMapping {

    public enum AbstractType {
        INTEGER(SqliteType.INTEGER, 0, TypeMapping::mapIntegerValueForQuery),
        TIMESTAMP(SqliteType.INTEGER, new Timestamp(0), TypeMapping::mapTimestampValueForQuery),
        BOOLEAN(SqliteType.INTEGER, false, TypeMapping::mapBooleanValueForQuery),
        STRING(SqliteType.TEXT, "", TypeMapping::mapStringValueForQuery);

        private SqliteType sqliteType;
        private Function<Object, String> valueMapper;
        private Object defaultValue;

        AbstractType(SqliteType sqliteType, Object defaultValue, Function<Object, String> valueMapper) {
            this.sqliteType = sqliteType;
            this.valueMapper = valueMapper;
            this.defaultValue = defaultValue;
        }

        public SqliteType getSqliteType() {
            return sqliteType;
        }

        public String getTypeNameForQuery() {
            return sqliteType.getName();
        }

        public String mapValueForQuery(Object value) {
            return valueMapper.apply(value);
        }

        public Object getDefaultValueForQuery() {
            return valueMapper.apply(this.defaultValue);
        }
    }

    private static String mapIntegerValueForQuery(Object integer) {
        return integer.toString();
    }

    private static String mapTimestampValueForQuery(Object timestamp) {
        return Long.toString(((Timestamp) timestamp).getTime());
    }

    private static String mapBooleanValueForQuery(Object bool) {
        return (boolean) bool ? "1" : "0";
    }

    private static String mapStringValueForQuery(Object string) {
        return String.format("\"%s\"", string);
    }

    private static final Map<Class<?>, AbstractType> javaToAbstractMap;
    static {
        Map<Class<?>, AbstractType> map = new HashMap<>();

        Arrays.asList(
                byte.class,
                short.class,
                int.class,
                long.class,
                Byte.class,
                Short.class,
                Integer.class,
                Long.class
        ).forEach(t -> map.put(t, AbstractType.INTEGER));

        Arrays.asList(
                boolean.class,
                Boolean.class
        ).forEach(t -> map.put(t, AbstractType.BOOLEAN));

        map.put(Timestamp.class, AbstractType.TIMESTAMP);
        map.put(String.class, AbstractType.STRING);

        javaToAbstractMap = Collections.unmodifiableMap(map);
    }

    public static Optional<AbstractType> toAbstractType(Field javaField) {
        return Optional.ofNullable(javaToAbstractMap.get(javaField.getType()));
    }
}
