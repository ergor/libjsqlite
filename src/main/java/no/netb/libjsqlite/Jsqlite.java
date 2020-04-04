package no.netb.libjsqlite;

import no.netb.libjcommon.result.Result;
import no.netb.libjcommon.tuples.Pair;
import no.netb.libjsqlite.annotations.Db;

import java.lang.reflect.Field;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class Jsqlite {

    private static Connection dbconn;
    static {
        try {
            dbconn = DriverManager.getConnection("jdbc:sqlite:test.db");
            dbconn.setAutoCommit(false);
            System.out.println("Opened database successfully");
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static Connection getConnection() {
        return dbconn;
    }

    public static Set<Field> getAllDbFields(Class<?> modelClass) {
        return getAllDbFields(modelClass, new HashSet<>());
    }

    /* CREATE:
     * INSERT INTO <class name> VALUES <field name,value pairs>
     */

    /* SAVE:
     * if not exists: CREATE(all values)
     * else: UPDATE <class name> SET <field name,value pairs> WHERE id = <this.id>
     */

    /* EXISTS:
     */

    public static <T extends BaseModel> Result<List<T>, Exception> selectN(Class<T> modelClass, String where, Object... args) {
        Pair<String, String> names = getTableName(modelClass);
        String tableName = names.getA();
        String tableVar = names.getB();

        String query = String.format("SELECT %s.* FROM %s %s %s",
                tableVar,
                tableName,
                tableVar,
                where);

        return executeN(modelClass, query, args);
    }

    private static <T extends BaseModel> Result<List<T>, Exception> executeN(Class<T> modelClass, String query, Object... args) {
        try {
            PreparedStatement preparedStatement = dbconn.prepareStatement(query);
            if (args != null) {
                for (int i = 0; i < args.length; i++) {
                    preparedStatement.setObject(i + 1, args[i]);
                }
            }
            ResultSet resultSet = preparedStatement.executeQuery();
            return Result.ok(mapToModel(modelClass, resultSet));
        } catch (Exception e) {
            return Result.err(e);
        }
    }

    private static Set<Field> getAllDbFields(Class<?> modelClass, Set<Field> collection) {
        if (modelClass == null) {
            return collection;
        }
        Field[] fields = modelClass.getDeclaredFields();
        collection.addAll(
                Arrays.stream(fields)
                        .filter(f -> f.isAnnotationPresent(Db.class))
                        .collect(Collectors.toSet())
        );
        return getAllDbFields(modelClass.getSuperclass(), collection);
    }

    private static<T extends BaseModel> Pair<String, String> getTableName(Class<T> modelClass) {
        String name = modelClass.getSimpleName();
        return new Pair<>(name, String.valueOf(name.toLowerCase().charAt(0)));
    }

    private static <T extends BaseModel> List<T> mapToModel(Class<T> modelClass, ResultSet resultSet) throws SQLException, IllegalAccessException, InstantiationException {

        List<T> rows = new ArrayList<>();
        Set<Field> columnFields = getAllDbFields(modelClass);

        while (resultSet.next()) {
            T obj = modelClass.newInstance();
            for (Field field : columnFields) {
                field.setAccessible(true);

                Object value = mapToField(resultSet, field);

                field.set(obj, value);
                field.setAccessible(false);
            }
            rows.add(obj);
        }

        return rows;
    }

    private static Object mapToField(ResultSet resultSet, Field field) throws SQLException {
        String name = field.getName();
        Class<?> fieldType = field.getType();

        if (fieldType == long.class) {
            return resultSet.getLong(name);
        }
        if (fieldType == boolean.class) {
            return resultSet.getInt(name) != 0;
        }
        if (fieldType == Timestamp.class) {
            return new Timestamp(resultSet.getLong(name));
        }
        if (fieldType == String.class) {
            return resultSet.getString(name);
        }

        throw new IllegalStateException(String.format("Exhausted all field mapping types: no mapping for type \"%s\"", fieldType.getName()));
    }
}
