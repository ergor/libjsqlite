package no.netb.libjsqlite;

import java.sql.Timestamp;
import java.util.*;
import java.util.function.Function;

public enum SqliteType {
    INTEGER("INTEGER", 0, Object::toString),
    TEXT("TEXT", "", value -> String.format("\"%s\"", value));

    private String name;
    private Object defaultValue;
    private Function<Object, String> valueForQueryMapper;

    SqliteType(String name, Object defaultValue, Function<Object, String> valueForQueryMapper) {
        this.name = name;
        this.defaultValue = defaultValue;
        this.valueForQueryMapper = valueForQueryMapper;
    }

    public String getName() {
        return name;
    }

    public Object getDefaultValueForQuery() {
        return mapToValueForQuery(defaultValue);
    }

    public String mapToValueForQuery(Object value) {
        return this.valueForQueryMapper.apply(value);
    }

    public static Optional<SqliteType> mapFromJavaType(Class<?> javaType) {
        return Optional.ofNullable(javaTypeMap.get(javaType));
    }

    private static final Map<Class<?>, SqliteType> javaTypeMap;
    static {
        Map<Class<?>, SqliteType> map = new HashMap<>();

        Arrays.asList(
                Boolean.class,
                Byte.class,
                Short.class,
                Integer.class,
                Long.class,
                Timestamp.class,
                boolean.class,
                byte.class,
                short.class,
                int.class,
                long.class
        ).forEach(t -> map.put(t, INTEGER));

        map.put(String.class, TEXT);

        javaTypeMap = Collections.unmodifiableMap(map);
    }
}
