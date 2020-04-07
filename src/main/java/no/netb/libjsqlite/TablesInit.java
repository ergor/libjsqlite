package no.netb.libjsqlite;

import no.netb.libjsqlite.resulttypes.updateresult.UpdateResult;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class TablesInit {

    private static final Logger LOG = Logger.getLogger(TablesInit.class.getName());

    public static UpdateResult createTablesIfNotExists(List<Class<? extends BaseModel>> models) {
        LOG.fine("Table initialization:");

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

        List<Column> idFirstColumns = new ArrayList<>();
        {
            Set<Column> columns = Jsqlite.getAllColumnFields(modelClass);
            Column primaryKey = columns.stream()
                    .filter(Column::isPrimaryKey)
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

        LOG.fine(statement);

        Connection conn = Jsqlite.getConnection();
        conn.createStatement().execute(statement);
    }

    private static Optional<String> makeForeignKeys(List<Column> fields) {
        Predicate<Column> isFk = Column::isForeignKey;

        if (fields.stream().noneMatch(isFk)) {
            return Optional.empty();
        }
        return Optional.of(fields.stream()
                .filter(isFk)
                .map(c -> String.format("FOREIGN KEY (%s) REFERENCES %s",
                        c.getColumnNameForQuery(),
                        c.getFkNameForQuery()))
                .collect(Collectors.joining(", ")));
    }

    private static String makeField(Column column) {
        boolean nullable = column.isNullable();
        boolean primaryKey = column.isPrimaryKey();
        TypeMapping.AbstractType abstractType = column.getAbstractTypeOrFail();

        String columnDef = String.format("%s %s", column.getColumnNameForQuery(), abstractType.getTypeNameForQuery()); // "columnName" TYPE
        String constraint1 = nullable ? "DEFAULT NULL" : String.format("NOT NULL DEFAULT %s", abstractType.getDefaultValueForQuery()); // DEFAULT NULL | NOT NULL DEFAULT x
        String constraint2 = primaryKey ? "PRIMARY KEY ASC" : ""; //

        return String.join(" ", columnDef, constraint1, constraint2);
    }
}
