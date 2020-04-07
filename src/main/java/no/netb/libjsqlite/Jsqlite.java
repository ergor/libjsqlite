package no.netb.libjsqlite;

import no.netb.libjcommon.result.Result;
import no.netb.libjcommon.tuples.Pair;
import no.netb.libjsqlite.annotations.Db;
import no.netb.libjsqlite.resulttypes.updateresult.UpdateResult;

import java.lang.reflect.Field;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Jsqlite {

    private static final Logger LOG = Logger.getLogger(Jsqlite.class.getName());

    private static Connection dbconn;
    static {
        System.setProperty(
                "java.util.logging.config.file",
                Jsqlite.class.getClassLoader().getResource("log.properties").getFile()
        );
        try {
            dbconn = DriverManager.getConnection("jdbc:sqlite:test.db");
            dbconn.setAutoCommit(false);
            LOG.info("Opened database successfully");
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to open database", e);
            System.exit(1);
        }
    }

    public Jsqlite(String database) {

    }

    public static Connection getConnection() {
        return dbconn;
    }

    /* EXISTS:
     */


    public static<T extends BaseModel> UpdateResult save(T model) {
        if (model.isNew()) {
            return insert(model);
        }
        return update(model);
    }

    public static<T extends BaseModel> UpdateResult update(T model) {
        try {
            String tableName = getTableName(model.getClass()).getA();

            Set<Column> columns = getAllColumnFields(model.getClass());
            columns.forEach(c -> c.setFieldAccesible(true));

            String sql = String.format("UPDATE \"%s\" SET %s WHERE id = ?",
                    tableName,
                    columns
                            .stream()
                            .filter(c -> !c.isPrimaryKey())
                            .map(c -> String.format("%s = %s", c.getColumnNameForQuery(), c.getValueForQuery(model)))
                            .collect(Collectors.joining(", ")));

            LOG.fine(sql);
            PreparedStatement preparedStatement = dbconn.prepareStatement(sql);
            preparedStatement.setObject(1, model.getId());
            preparedStatement.execute();
            return UpdateResult.ok();
        } catch (Exception e) {
            return UpdateResult.err(e);
        }
    }

    public static<T extends BaseModel> UpdateResult insert(T model) {
        try {
            String tableName = getTableName(model.getClass()).getA();

            Set<Column> columns = getAllColumnFields(model.getClass());

            for (Column column : columns) {
                column.setFieldAccesible(true);
                if (column.isPrimaryKey()) {
                    Result<Integer, Exception> maxIdResult = getMaxId(model, column);
                    if (maxIdResult.isErr()) {
                        return UpdateResult.err(maxIdResult.unwrapErr());
                    }
                    long nextId = maxIdResult.unwrap() + 1;
                    column.setField(model, nextId);
                }
            }

            String sql = String.format("INSERT INTO \"%s\" (%s) VALUES (%s)",
                    tableName,
                    columns.stream().map(Column::getColumnNameForQuery).collect(Collectors.joining(", ")),
                    columns.stream().map(c -> c.getValueForQuery(model)).collect(Collectors.joining(", ")));

            LOG.fine(sql);

            PreparedStatement preparedStatement = dbconn.prepareStatement(sql);
            preparedStatement.execute();
            return UpdateResult.ok();
        } catch (Exception e) {
            return UpdateResult.err(e);
        }
    }

    private static<T extends BaseModel> Result<Integer, Exception> getMaxId(T model, Column idColumn) {
        try {
            String tableName = getTableName(model.getClass()).getA();
            String sql = String.format("SELECT MAX(%s) AS max_id FROM \"%s\"", idColumn.getColumnNameForQuery(), tableName);
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
            LOG.fine(query);
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

    public static Set<Column> getAllColumnFields(Class<?> modelClass) {
        return getAllColumnFields(modelClass, new HashSet<>());
    }

    private static Set<Column> getAllColumnFields(Class<?> modelClass, Set<Column> collection) {
        if (modelClass == null) {
            return collection;
        }
        Field[] fields = modelClass.getDeclaredFields();
        collection.addAll(
                Arrays.stream(fields)
                        .filter(f -> f.isAnnotationPresent(Db.class))
                        .map(Column::new)
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
        Set<Column> columns = getAllColumnFields(modelClass);

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
