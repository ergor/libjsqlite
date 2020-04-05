package no.netb.libjsqlite;

import no.netb.libjcommon.result.Result;
import no.netb.libjcommon.tuples.Pair;
import no.netb.libjsqlite.annotations.Db;
import no.netb.libjsqlite.annotations.Pk;
import no.netb.libjsqlite.resulttypes.updateresult.UpdateResult;

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

    /* CREATE:
     * INSERT INTO <class name> VALUES <field name,value pairs>
     */

    /* SAVE:
     * if not exists: CREATE(all values)
     * else: UPDATE <class name> SET <field name,value pairs> WHERE id = <this.id>
     */

    /* EXISTS:
     */

    public static<T extends BaseModel> UpdateResult insert(T model) {
        try {
            String tableName = getTableName(model.getClass()).getA();

            List<String> columns = new ArrayList<>();
            List<Object> values = new ArrayList<>();

            for (Field field : getAllColumnFields(model.getClass())) {
                field.setAccessible(true);
                if (field.isAnnotationPresent(Pk.class)) {
                    Result<Integer, Exception> maxIdResult = getMaxId(model, field);
                    if (maxIdResult.isErr()) {
                        return UpdateResult.err(maxIdResult.unwrapErr());
                    }
                    long nextId = maxIdResult.unwrap() + 1;
                    values.add(nextId);
                    field.set(model, nextId);
                } else {
                    values.add(field.get(model));
                }
                columns.add(String.format("\"%s\"", field.getName()));
            }

            String sql = String.format("INSERT INTO \"%s\" (%s) VALUES (%s)",
                    tableName,
                    String.join(", ", columns),
                    values.stream()
                            .map(o -> new Pair<>(SqliteType.mapFromJavaType(o.getClass()).orElseThrow(() -> new RuntimeException("Failed to map type " + o.getClass().getName())), o))
                            .map(p -> p.getA().mapToValueForQuery(p.getB()))
                            .collect(Collectors.joining(", ")));

            System.out.println(sql);

            PreparedStatement preparedStatement = dbconn.prepareStatement(sql);
            preparedStatement.execute();
            return UpdateResult.ok();
        } catch (Exception e) {
            return UpdateResult.err(e);
        }
    }

    private static<T extends BaseModel> Result<Integer, Exception> getMaxId(T model, Field idField) {
        try {
            String tableName = getTableName(model.getClass()).getA();
            String sql = String.format("SELECT MAX(%s) AS max_id FROM \"%s\"", idField.getName(), tableName);
            PreparedStatement preparedStatement = dbconn.prepareStatement(sql);
            Integer maxId = preparedStatement.executeQuery().getInt("max_id");
            return Result.ok(maxId);
        } catch (SQLException e) {
            return Result.err(e);
        }
    }

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
            return Result.ok(mapToJavaModel(modelClass, resultSet));
        } catch (Exception e) {
            return Result.err(e);
        }
    }

    public static Set<Field> getAllColumnFields(Class<?> modelClass) {
        return getAllColumnFields(modelClass, new HashSet<>());
    }

    private static Set<Field> getAllColumnFields(Class<?> modelClass, Set<Field> collection) {
        if (modelClass == null) {
            return collection;
        }
        Field[] fields = modelClass.getDeclaredFields();
        collection.addAll(
                Arrays.stream(fields)
                        .filter(f -> f.isAnnotationPresent(Db.class))
                        .collect(Collectors.toSet())
        );
        return getAllColumnFields(modelClass.getSuperclass(), collection);
    }

    /**
     * @return Pair: (table name, table var)
     */
    private static<T extends BaseModel> Pair<String, String> getTableName(Class<T> modelClass) {
        String name = modelClass.getSimpleName();
        return new Pair<>(name, String.valueOf(name.toLowerCase().charAt(0)));
    }

    private static <T extends BaseModel> List<T> mapToJavaModel(Class<T> modelClass, ResultSet resultSet) throws SQLException, IllegalAccessException, InstantiationException {

        List<T> rows = new ArrayList<>();
        Set<Field> columnFields = getAllColumnFields(modelClass);

        while (resultSet.next()) {
            T obj = modelClass.newInstance();
            for (Field field : columnFields) {
                field.setAccessible(true);

                Object value = mapToJavaValue(resultSet, field);

                field.set(obj, value);
                field.setAccessible(false);
            }
            rows.add(obj);
        }

        return rows;
    }

    private static Object mapToJavaValue(ResultSet resultSet, Field field) throws SQLException {
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

        throw new IllegalStateException(String.format("Exhausted all java value mappings: no mapping for type \"%s\"", fieldType.getName()));
    }
}
