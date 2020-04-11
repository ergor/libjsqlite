package no.netb.libjsqlite;

import no.netb.libjcommon.result.Result;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
import java.util.function.Function;

public class TypeMapping {

    enum AbstractType {
        INTEGER(SqliteType.INTEGER, 0, AbstractType::mapIntegerValueForQuery),
        TIMESTAMP(SqliteType.INTEGER, new Timestamp(0), AbstractType::mapTimestampValueForQuery),
        BOOLEAN(SqliteType.INTEGER, false, AbstractType::mapBooleanValueForQuery),
        ENUM(SqliteType.INTEGER, null, AbstractType::mapEnumValueForQuery),
        STRING(SqliteType.TEXT, "", AbstractType::mapStringValueForQuery);

        private final SqliteType sqliteType;
        private final Function<Object, String> valueMapper;
        private final Object defaultValue;

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

        private static String mapIntegerValueForQuery(Object integer) {
            return integer.toString();
        }

        private static String mapTimestampValueForQuery(Object timestamp) {
            return Long.toString(((Timestamp) timestamp).getTime());
        }

        private static String mapBooleanValueForQuery(Object bool) {
            return (boolean) bool ? "1" : "0";
        }

        private static String mapEnumValueForQuery(Object enumMember) {
            return mapIntegerValueForQuery(enumMember == null ? 0 : ((EnumColumn) enumMember).toInt());
        }

        private static String mapStringValueForQuery(Object string) {
            return String.format("\"%s\"", string);
        }
    }

    private static final Map<Class<?>, AbstractType> JAVA_TO_ABSTRACT_TYPE_MAP;
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
        map.put(EnumColumn.class, AbstractType.ENUM);
        map.put(String.class, AbstractType.STRING);

        JAVA_TO_ABSTRACT_TYPE_MAP = Collections.unmodifiableMap(map);
    }

    static Optional<AbstractType> toAbstractType(Field javaField) {
        Class<?> type = javaField.getType();
        type = EnumColumn.class.isAssignableFrom(type) ? EnumColumn.class : type;
        return Optional.ofNullable(JAVA_TO_ABSTRACT_TYPE_MAP.get(type));
    }

    static <T extends BaseModel> Result<List<T>, Exception> mapToJavaModel(Class<T> modelClass, ResultSet resultSet) {
        try {
            List<T> rows = new ArrayList<>();
            Set<Column> columns = Jsqlite.getAllColumnFields(modelClass);

            while (resultSet.next()) {
                T obj = modelClass.newInstance();
                for (Column column : columns) {
                    column.setFieldAccesible(true);

                    Object value = mapToJavaValue(resultSet, column.getField());

                    column.setField(obj, value);
                    column.setFieldAccesible(false);
                }
                rows.add(obj);
            }
            return Result.ok(rows);
        }
        catch (Exception e) {
            return Result.err(e);
        }
    }

    private static Object mapToJavaValue(ResultSet resultSet, Field field) throws SQLException {
        String name = field.getName();
        Class<?> fieldType = field.getType();

        if (fieldType == byte.class) {
            return resultSet.getByte(name);
        }
        if (fieldType == short.class) {
            return resultSet.getShort(name);
        }
        if (fieldType == int.class) {
            return resultSet.getInt(name);
        }
        if (fieldType == long.class) {
            return resultSet.getLong(name);
        }
        if (fieldType == boolean.class) {
            return resultSet.getInt(name) != 0;
        }
        if (EnumColumn.class.isAssignableFrom(fieldType)) {
            return getEnumMapper(fieldType).get(resultSet.getInt(name));
        }
        if (fieldType == Timestamp.class) {
            return new Timestamp(resultSet.getLong(name));
        }
        if (fieldType == String.class) {
            return resultSet.getString(name);
        }

        throw new IllegalStateException(String.format("Exhausted all java value mappings: no mapping for type \"%s\"", fieldType.getName()));
    }

    private static final Map<Class<?>, Map<Integer, Enum<? extends EnumColumn>>> enumMappers = new HashMap<>();

    private static Map<Integer, Enum<? extends EnumColumn>> getEnumMapper(Class<?> paramType) {
        return enumMappers.computeIfAbsent(paramType, _fn -> {
            Map<Integer, Enum<? extends EnumColumn>> map = new HashMap<>();
            for (Object o : paramType.getEnumConstants()) {
                map.put(((EnumColumn) o).toInt(), (Enum<? extends EnumColumn>) o);
            }
            return map;
        });
    }
}
