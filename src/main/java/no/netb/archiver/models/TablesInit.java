package no.netb.archiver.models;

import no.netb.archiver.annotations.Db;
import no.netb.archiver.annotations.Fk;
import no.netb.archiver.annotations.Models;
import no.netb.archiver.annotations.Pk;
import no.netb.archiver.common.ReflectionUtil;
import no.netb.archiver.repository.Repository;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * The {@link no.netb.archiver.models} package must be loaded for {@link #createTables()} to work.
 * That means the init function must be inside this package for it to work.
 */
public class TablesInit {

    public static void createTables() throws SQLException {
        Package[] packages = Package.getPackages();
        System.out.println("Table initialization:");
        for (Package p : packages) {
            Models annotation = p.getAnnotation(Models.class);
            if (annotation != null) {
                Class<? extends ModelBase>[] models = annotation.models();
                for (Class<? extends ModelBase> modelClass : models) {
                    createTable(modelClass);
                }
            }
        }
    }

    private static void createTable(Class<? extends ModelBase> modelClass) throws SQLException {

        List<Field> idFirstColumns = new ArrayList<>();
        {
            Set<Field> columns = ReflectionUtil.getAllDbFields(modelClass);
            Field primaryKey = columns.stream()
                    .filter(f -> f.isAnnotationPresent(Pk.class))
                    .findAny()
                    .orElseThrow(() -> new RuntimeException("No primary key found for model: " + modelClass.getName()));
            columns.remove(primaryKey);
            idFirstColumns.add(primaryKey);
            idFirstColumns.addAll(columns);
        }

        Optional<String> foreignKeys = makeForeignKeys(idFirstColumns);

        String statement = String.format("CREATE TABLE IF NOT EXISTS %s (%s %s %s)",
                modelClass.getSimpleName(),
                idFirstColumns.stream()
                        .map(TablesInit::makeField)
                        .collect(Collectors.joining(", ")),
                foreignKeys.isPresent() ? "," : "",
                foreignKeys.orElse(""));

        System.out.println(statement);

        Connection conn = Repository.getConnection();
        conn.createStatement().execute(statement);
        conn.commit();
    }

    private static Optional<String> makeForeignKeys(List<Field> fields) {
        Predicate<Field> isFk = f -> f.isAnnotationPresent(Fk.class);

        if (fields.stream().noneMatch(isFk)) {
            return Optional.empty();
        }
        return Optional.of(fields.stream()
                .filter(isFk)
                .map(f -> String.format("FOREIGN KEY (\"%s\") REFERENCES \"%s\"",
                        f.getName(),
                        f.getAnnotation(Fk.class).value().getSimpleName()))
                .collect(Collectors.joining(", ")));
    }

    private static String makeField(Field field) {
        return String.format("\"%s\" %s %s %s",
                field.getName(),
                mapType(field),
                field.getAnnotation(Db.class).nullable() ? "" : "NOT NULL", // TODO: default values
                field.isAnnotationPresent(Pk.class) ? "PRIMARY KEY" : "");
    }

    private static String mapType(Field field) {
        Class<?> type = field.getType();
        if (type == boolean.class
                || type == Timestamp.class
                || type == byte.class
                || type == short.class
                || type == int.class
                || type == long.class) {
            return "INTEGER";
        }
        if (type == String.class) {
            return "TEXT";
        }

        throw new IllegalStateException("TableInit: field types exhausted. No mapping for: " + type.getName());
    }
}
