package no.netb.libjsqlite;

import no.netb.libjcommon.result.Result;
import no.netb.libjcommon.tuples.Pair;
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

    static {
        System.setProperty(
                "java.util.logging.config.file",
                Jsqlite.class.getClassLoader().getResource("log.properties").getFile()
        );
    }

    public static Result<Database, SQLException> connect(String path, boolean autoCommit) {
        try {
            Connection connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            connection.setAutoCommit(autoCommit);
            Database database = new Database(connection);
            LOG.info("jsqlite: successfully opened database " + path);
            return Result.ok(database);
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to open database", e);
            return Result.err(e);
        }
    }

    /**
     * @return Pair: (table name, table var)
     */
    static<T extends BaseModel> Pair<String, String> getTableName(Class<T> modelClass) {
        String name = modelClass.getSimpleName();
        return new Pair<>(name, String.valueOf(name.toLowerCase().charAt(0)));
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
