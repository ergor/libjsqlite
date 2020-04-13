package no.netb.libjsqlite;

import no.netb.libjsqlite.annotations.Db;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Jsqlite {

    private static final Logger LOG = Logger.getLogger(Jsqlite.class.getName());

    public static Database connect(String path, boolean autoCommit) throws SQLException {
        try {
            Connection connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            connection.setAutoCommit(autoCommit);
            Database database = new Database(connection);
            LOG.info("jsqlite: successfully opened database: " + path);
            return database;
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "jsqlite: failed to open database: " + path, e);
            throw e;
        }
    }

    static Set<Column> getAllColumnFields(Class<?> modelClass) {
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
}
