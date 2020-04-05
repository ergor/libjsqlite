package no.netb.libjsqlite;

import java.sql.SQLException;

public class DbSuccess {

    private DbSuccess() {}

    public static DbSuccess create() {
        return new DbSuccess();
    }

    public void commit() throws SQLException {
        Jsqlite.getConnection().commit();
    }
}
