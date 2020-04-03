package no.netb.archiver.repository;

import de.perschon.resultflow.Result;
import no.netb.archiver.annotations.Db;
import no.netb.archiver.common.Pair;
import no.netb.archiver.models.*;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
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
            System.exit(1);
        }
    }

    public static List<Class<? extends ModelBase>> modelClasses;
    static {

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        assert classLoader != null;

        List<Class<? extends ModelBase>> list = new ArrayList<>();

        try {
            Enumeration<URL> resources = classLoader.getResources("no/netb/archiver/models");
            List<File> dirs = new ArrayList<>();
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                dirs.add(new File(resource.getFile()));
            }
            for (File dir : dirs) {
                list.addAll()
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }



        list.add(FsNode.class);
        list.add(Host.class);
        list.add(HostIdentifier.class);
        list.add(IndexRun.class);
        list.add(IndexRunConnector.class);

        modelClasses = Collections.unmodifiableList(list);
    }

    /**
     * Recursive method used to find all classes in a given directory and subdirs.
     *
     * @param directory   The base directory
     * @param packageName The package name for classes found inside the base directory
     * @return The classes
     * @throws ClassNotFoundException
     */
    private static List<Class> findClasses(File directory, String packageName) throws ClassNotFoundException {
        List<Class> classes = new ArrayList<>();
        if (!directory.exists()) {
            return classes;
        }
        File[] files = directory.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                assert !file.getName().contains(".");
                classes.addAll(findClasses(file, packageName + "." + file.getName()));
            } else if (file.getName().endsWith(".class")) {
                classes.add(Class.forName(packageName + '.' + file.getName().substring(0, file.getName().length() - 6)));
            }
        }
        return classes;
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

    private static<T extends ModelBase> Pair<String, String> getTableName(Class<T> modelClass) {
        String name = modelClass.getSimpleName();
        return new Pair<>(name, String.valueOf(name.toLowerCase().charAt(0)));
    }

    private static<T> Set<Field> collectColumnFields(Class<T> modelClass, Set<Field> collection) {
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


    private static <T extends ModelBase> Result<T[], Exception> executeN(Class<T> modelClass, String query, Object... args) {
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

    public static <T extends ModelBase> Result<T[], Exception> selectN(Class<T> modelClass, String where, Object... args) {
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

    //public static <T extends ModelBase> T getById(Class<T> modelClass, long id) {
    //    return selectN(modelClass, "WHERE id = ?", id)[0];
    //}
}
