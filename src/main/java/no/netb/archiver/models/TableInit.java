package no.netb.archiver.models;

import no.netb.archiver.annotations.Fk;
import no.netb.archiver.annotations.Models;
import no.netb.archiver.annotations.Pk;
import no.netb.archiver.common.ReflectionUtil;

import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * The {@link no.netb.archiver.models} package must be loaded for {@link #createTables()} to work.
 * That means the init function must be inside this package for it to work.
 */
public class TableInit {

    public static void createTables() {
        Package[] packages = Package.getPackages();
        System.out.println("Registered table classes:");
        for (Package p : packages) {
            Models annotation = p.getAnnotation(Models.class);
            if (annotation != null) {
                Class<? extends ModelBase>[] models = annotation.models();
                for (Class<? extends ModelBase> modelClass : models) {
                    System.out.println("\t" + modelClass.getSimpleName());
                    createTable(modelClass);
                }
            }
        }
    }

    private static void createTable(Class<? extends ModelBase> modelClass) {
        Set<Field> columns = ReflectionUtil.getAllDbFields(modelClass);
        Optional<String> foreignKeys = makeForeignKeys(columns);

        String statement = String.format("CREATE TABLE IF NOT EXISTS %s (%s %s %s)",
                modelClass.getSimpleName(),
                columns.stream()
                        .map(TableInit::makeField)
                        .collect(Collectors.joining(", ")),
                foreignKeys.isPresent() ? "," : "",
                foreignKeys.orElse(""));

        System.out.println(statement);
    }

    private static Optional<String> makeForeignKeys(Set<Field> fields) {
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
        return String.format("\"%s\" %s%s",
                field.getName(),
                mapType(field),
                field.isAnnotationPresent(Pk.class) ? " PRIMARY KEY" : "");
    }

    private static String mapType(Field field) {
        Object type = field.getType();
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

        throw new IllegalStateException("TableInit: field types exhausted. No mapping for: " + type);
    }
}
