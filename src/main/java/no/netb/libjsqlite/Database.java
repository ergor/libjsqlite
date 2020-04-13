package no.netb.libjsqlite;

import no.netb.libjcommon.result.Result;
import no.netb.libjcommon.tuples.Pair;
import no.netb.libjsqlite.resulttypes.updateresult.UpdateResult;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Database {

    private static final Logger LOG = Logger.getLogger(Database.class.getName());

    private Connection conn;

    Database(Connection connection) {
        this.conn = connection;
    }

    /*
        TODO: implement EXISTS
     */

    public void commit() throws SQLException {
        conn.commit();
    }

    public <T extends BaseModel> UpdateResult save(T model) {
        if (model.isNew()) {
            return insert(model);
        }
        return update(model);
    }

    public <T extends BaseModel> UpdateResult update(T model) {
        try {
            String tableName = Jsqlite.getTableName(model.getClass()).getA();

            Set<Column> columns = Jsqlite.getAllColumnFields(model.getClass());
            columns.forEach(c -> c.setFieldAccesible(true));

            String sql = String.format("UPDATE \"%s\" SET %s WHERE id = ?",
                    tableName,
                    columns
                            .stream()
                            .filter(c -> !c.isPrimaryKey())
                            .map(c -> String.format("%s = %s", c.getColumnNameForQuery(), c.getValueForQuery(model)))
                            .collect(Collectors.joining(", ")));

            LOG.fine(sql);
            PreparedStatement preparedStatement = conn.prepareStatement(sql);
            preparedStatement.setObject(1, model.getId());
            preparedStatement.execute();
            return UpdateResult.ok(conn);
        } catch (Exception e) {
            return UpdateResult.err(e);
        }
    }

    public <T extends BaseModel> UpdateResult insert(T model) {
        try {
            String tableName = Jsqlite.getTableName(model.getClass()).getA();

            Set<Column> columns = Jsqlite.getAllColumnFields(model.getClass());

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
            PreparedStatement preparedStatement = conn.prepareStatement(sql);
            preparedStatement.execute();
            return UpdateResult.ok(conn);
        } catch (Exception e) {
            return UpdateResult.err(e);
        }
    }

    public <T extends BaseModel> Result<Integer, Exception> getMaxId(T model, Column idColumn) {
        try {
            String tableName = Jsqlite.getTableName(model.getClass()).getA();
            String sql = String.format("SELECT MAX(%s) AS max_id FROM \"%s\"", idColumn.getColumnNameForQuery(), tableName);
            PreparedStatement preparedStatement = conn.prepareStatement(sql);
            Integer maxId = preparedStatement.executeQuery().getInt("max_id");
            return Result.ok(maxId);
        } catch (SQLException e) {
            return Result.err(e);
        }
    }

    public <T extends BaseModel> Result<List<T>, Exception> selectN(Class<T> modelClass, String where, Object... args) {
        Pair<String, String> names = Jsqlite.getTableName(modelClass);
        String tableName = names.getA();
        String tableVar = names.getB();

        String query = String.format("SELECT %s.* FROM %s %s %s",
                tableVar,
                tableName,
                tableVar,
                where);

        return executeN(modelClass, query, args);
    }

    private <T extends BaseModel> Result<List<T>, Exception> executeN(Class<T> modelClass, String query, Object... args) {
        try {
            LOG.fine(query);
            PreparedStatement preparedStatement = conn.prepareStatement(query);
            if (args != null) {
                for (int i = 0; i < args.length; i++) {
                    preparedStatement.setObject(i + 1, args[i]);
                }
            }
            ResultSet resultSet = preparedStatement.executeQuery();
            return TypeMapping.mapToJavaModel(modelClass, resultSet);
        } catch (Exception e) {
            return Result.err(e);
        }
    }

    public UpdateResult createTablesIfNotExists(List<Class<? extends BaseModel>> models) {
        for (Class<? extends BaseModel> modelClass : models) {
            try {
                createTableIfNotExists(modelClass);
            } catch (SQLException e) {
                return UpdateResult.err(e);
            }
        }
        return UpdateResult.ok(conn);
    }

    private <T extends BaseModel> void createTableIfNotExists(Class<T> modelClass) throws SQLException {

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

        Optional<String> foreignKeys = makeForeignKeyDefs(idFirstColumns);

        String statement = String.format("CREATE TABLE IF NOT EXISTS %s (%s %s %s)",
                modelClass.getSimpleName(),
                idFirstColumns.stream()
                        .map(this::makeColumnDef)
                        .collect(Collectors.joining(", ")),
                foreignKeys.isPresent() ? "," : "",
                foreignKeys.orElse(""));

        LOG.fine(statement);

        conn.createStatement().execute(statement);
    }

    private Optional<String> makeForeignKeyDefs(List<Column> fields) {
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

    private String makeColumnDef(Column column) {
        boolean nullable = column.isNullable();
        boolean primaryKey = column.isPrimaryKey();
        TypeMapping.AbstractType abstractType = column.getAbstractTypeOrFail();

        String columnDef = String.format("%s %s", column.getColumnNameForQuery(), abstractType.getTypeNameForQuery()); // "columnName" TYPE
        String constraint1 = nullable ? "DEFAULT NULL" : String.format("NOT NULL DEFAULT %s", abstractType.getDefaultValueForQuery()); // DEFAULT NULL | NOT NULL DEFAULT x
        String constraint2 = primaryKey ? "PRIMARY KEY ASC" : ""; //

        return String.join(" ", columnDef, constraint1, constraint2);
    }
}
