package no.netb.archiver.repository;

import no.netb.archiver.annotations.Db;
import no.netb.archiver.common.Pair;
import no.netb.archiver.models.ModelBase;

import java.lang.reflect.Field;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class Repository {

    private static Connection dbconn;
    static {
        try {
            dbconn = DriverManager.getConnection("jdbc:sqlite:test.db");
            dbconn.setAutoCommit(false);
            System.out.println("Opened database successfully");
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(0);
        }
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

    /* GET:
     */

    private static Pair<String, String> getTableName(Class modelClass) {
        String name = modelClass.getSimpleName();
        return new Pair<>(name, String.valueOf(name.toLowerCase().charAt(0)));
    }

    private static Set<Field> collectColumnFields(Class modelClass, Set<Field> collection) {
        if (modelClass == null) {
            return collection;
        }
        Field[] fields = modelClass.getDeclaredFields();
        collection.addAll(
                Arrays.stream(fields)
                        .filter(f -> f.isAnnotationPresent(Db.class))
                        .collect(Collectors.toSet())
        );
        return collectColumnFields(modelClass.getSuperclass(), collection);
    }

    private static Object mapField(ResultSet resultSet, Field field) throws SQLException {
        String name = field.getName();
        Class fieldType = field.getType();

        if (fieldType == long.class) {
            return resultSet.getLong(name);
        }
        if (fieldType == boolean.class) {
            return resultSet.getInt(name) != 0;
        }
        else if (fieldType == Timestamp.class) {
            return new Timestamp(resultSet.getLong(name));
        }

        throw new IllegalStateException(String.format("Exhausted all field mapping types: no mapping for type \"%s\"", fieldType.getName()));
    }

    private static <T extends ModelBase> T[] mapToModel(Class<T> modelClass, ResultSet resultSet) throws SQLException, IllegalAccessException, InstantiationException {

        List<T> rows = new ArrayList<>();
        Set<Field> columnFields = collectColumnFields(modelClass, new HashSet<>());

        while (resultSet.next()) {
            T obj = modelClass.newInstance();
            for (Field field : columnFields) {
                field.setAccessible(true);

                Object value = mapField(resultSet, field);

                field.set(obj, value);
                field.setAccessible(false);
            }
            rows.add(obj);
        }

        return (T[]) rows.toArray();
    }


    private static <T extends ModelBase> T[] executeN(Class<T> modelClass, String query, Object... args) {
        try {
            PreparedStatement preparedStatement = dbconn.prepareStatement(query);
            if (args != null) {
                for (int i = 0; i < args.length; i++) {
                    preparedStatement.setObject(i + 1, args[i]);
                }
            }
            ResultSet resultSet = preparedStatement.executeQuery();
            return mapToModel(modelClass, resultSet);
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }
        return (T[]) new Object[0];
    }

    public static <T extends ModelBase> T[] selectN(Class<T> modelClass, String where, Object... args) {
        Pair<String, String> names = getTableName(modelClass);
        String tableName = names.getFirst();
        String tableVar = names.getSecond();

        String query = String.format("SELECT %s.* FROM %s %s %s",
                tableVar,
                tableName,
                tableVar,
                where);

        return executeN(modelClass, query, args);
    }

    public static <T extends ModelBase> T getById(Class<T> modelClass, long id) {
        return selectN(modelClass, "WHERE id = ?", id)[0];
    }
}
