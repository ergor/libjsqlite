package no.netb.libjsqlite;

import java.sql.Timestamp;
import java.util.*;

public enum SqliteType {
    INTEGER("INTEGER", "0"),
    TEXT("TEXT", "\"\"");

    private String name;
    private String defaultValue;

    SqliteType(String name, String defaultValue) {
        this.name = name;
        this.defaultValue = defaultValue;
    }

    public String getName() {
        return name;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public static Optional<SqliteType> mapJavaType(Class<?> javaType) {
        return Optional.ofNullable(javaTypeMap.get(javaType));
    }

    private static final Map<Class<?>, SqliteType> javaTypeMap;
    static {
        Map<Class<?>, SqliteType> map = new HashMap<>();

        Arrays.asList(
                boolean.class,
                Timestamp.class,
                byte.class,
                short.class,
                int.class,
                long.class
        ).forEach(t -> map.put(t, INTEGER));

        map.put(String.class, TEXT);

        javaTypeMap = Collections.unmodifiableMap(map);
    }
}
