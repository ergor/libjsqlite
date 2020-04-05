package no.netb.libjsqlite;

import no.netb.libjsqlite.annotations.Db;
import no.netb.libjsqlite.annotations.Fk;
import no.netb.libjsqlite.annotations.Pk;
import no.netb.libjsqlite.resulttypes.updateresult.UpdateResult;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class TablesInit {

    public static UpdateResult createTables(List<Class<? extends BaseModel>> models) {
        System.out.println("Table initialization:");

        for (Class<? extends BaseModel> modelClass : models) {
            try {
                createTable(modelClass);
            } catch (SQLException e) {
                return UpdateResult.err(e);
            }
        }

        return UpdateResult.ok();
    }

    private static<T extends BaseModel> void createTable(Class<T> modelClass) throws SQLException {

        List<Field> idFirstColumns = new ArrayList<>();
        {
            Set<Field> columns = Jsqlite.getAllDbFields(modelClass);
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

        Connection conn = Jsqlite.getConnection();
        conn.createStatement().execute(statement);
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
        Class<?> fieldType = field.getType();
        SqliteType sqliteType = SqliteType.mapFromJavaType(fieldType)
                .orElseThrow(() -> new IllegalStateException("TablesInit: field types exhausted. No mapping for: " + fieldType.getName()));

        boolean nullable = field.getAnnotation(Db.class).nullable();
        boolean primaryKey = field.isAnnotationPresent(Pk.class);

        String columnDef = String.format("\"%s\" %s", field.getName(), sqliteType.getName()); // "columnName" TYPE
        String constraint1 = nullable ? "DEFAULT NULL" : String.format("NOT NULL DEFAULT %s", sqliteType.getDefaultValueForQuery()); // DEFAULT NULL | NOT NULL DEFAULT x
        String constraint2 = primaryKey ? "PRIMARY KEY ASC" : ""; //

        return String.join(" ", columnDef, constraint1, constraint2);
    }
}
