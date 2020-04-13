package no.netb.libjsqlite;

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

    public void rollback() throws SQLException {
        conn.rollback();
    }

    public <T extends BaseModel> void save(T model) throws SQLException {
        if (model.isNew()) {
            insert(model);
        }
        update(model);
    }

    public <T extends BaseModel> void update(T model) throws SQLException {
        TableInfo tableInfo = TableInfo.fromClass(model.getClass());

        Set<Column> columns = Jsqlite.getAllColumnFields(model.getClass());
        columns.forEach(c -> c.setFieldAccesible(true));

        // TODO: SET %s ought to be prepared as well
        String sql = String.format("UPDATE \"%s\" SET %s WHERE id = ?",
                tableInfo.getName(),
                columns
                        .stream()
                        .filter(c -> !c.isPrimaryKey())
                        .map(c -> String.format("%s = %s", c.getColumnNameForQuery(), c.getValueForQuery(model)))
                        .collect(Collectors.joining(", ")));

        LOG.fine(sql);
        PreparedStatement preparedStatement = conn.prepareStatement(sql);
        preparedStatement.setObject(1, model.getId());
        preparedStatement.execute();
    }

    public <T extends BaseModel> void insert(T model) throws SQLException {
        TableInfo tableInfo = TableInfo.fromClass(model.getClass());
        Set<Column> columns = Jsqlite.getAllColumnFields(model.getClass());

        for (Column column : columns) {
            if (column.isPrimaryKey()) {
                long nextId = getMaxId(model, column) + 1;
                try (ReflectionAccess access = ReflectionAccess.grant(column.getField())) {
                    access.setFieldValue(model, nextId);
                }
                break;
            }
        }

        // TODO: VALUES (%s) ought to be prepared
        String sql = String.format("INSERT INTO \"%s\" (%s) VALUES (%s)",
                tableInfo.getName(),
                columns.stream().map(Column::getColumnNameForQuery).collect(Collectors.joining(", ")),
                columns.stream().map(c -> c.getValueForQuery(model)).collect(Collectors.joining(", ")));

        LOG.fine(sql);
        PreparedStatement preparedStatement = conn.prepareStatement(sql);
        preparedStatement.execute();
    }

    public <T extends BaseModel> long getMaxId(T model, Column idColumn) throws SQLException {
        TableInfo tableInfo = TableInfo.fromClass(model.getClass());
        String sql = String.format("SELECT MAX(%s) AS max_id FROM \"%s\"", idColumn.getColumnNameForQuery(), tableInfo.getName());
        PreparedStatement preparedStatement = conn.prepareStatement(sql);
        return preparedStatement.executeQuery().getLong("max_id");
    }

    /**
     * @param modelClass The model type you want the query result mapped to.
     * @param where Used in the query as such: SELECT %s.* FROM %s %s [[where]]
     * @param args The arguments to use in the prepared statement.
     * @param <T> The model type you want the query result mapped to.
     * @return The query result as a list of given type.
     * @throws SQLException If a problem occurs in JDBC.
     * @throws InstantiationException If the given model type doesn't have the default constructor.
     * @throws IllegalAccessException If the given model type hides the default constructor.
     */
    public <T extends BaseModel> List<T> selectN(Class<T> modelClass, String where, Object... args) throws SQLException, InstantiationException, IllegalAccessException {
        TableInfo tableInfo = TableInfo.fromClass(modelClass);

        String query = String.format("SELECT %s.* FROM %s %s %s",
                tableInfo.getAlias(),
                tableInfo.getName(),
                tableInfo.getAlias(),
                where);

        LOG.fine(query);
        PreparedStatement preparedStatement = conn.prepareStatement(query);
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                preparedStatement.setObject(i + 1, args[i]);
            }
        }
        ResultSet resultSet = preparedStatement.executeQuery();
        return TypeMapping.mapToJavaModel(modelClass, resultSet);
    }

    public void createTablesIfNotExists(List<Class<? extends BaseModel>> models) throws SQLException, JsqliteException {
        for (Class<? extends BaseModel> modelClass : models) {
            createTableIfNotExists(modelClass);
        }
    }

    private <T extends BaseModel> void createTableIfNotExists(Class<T> modelClass) throws SQLException, JsqliteException {

        List<Column> idFirstColumns = new ArrayList<>();
        {
            Set<Column> columns = Jsqlite.getAllColumnFields(modelClass);
            Column primaryKey = columns.stream()
                    .filter(Column::isPrimaryKey)
                    .findAny()
                    .orElseThrow(() -> new JsqliteException("no primary key found", modelClass));
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
