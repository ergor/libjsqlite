package no.netb.libjsqlite;

import no.netb.libjsqlite.annotations.Db;
import no.netb.libjsqlite.annotations.Pk;

import java.sql.SQLException;

/**
 * Every class inheriting this class <i>must</i> provide the default constructor,
 * otherwise exceptions might occur during instantiation of the subclasses in
 * certain methods, such as {@link Database#selectN(Class, String, Object...)}.
 */
public abstract class BaseModel {

    @Db
    @Pk
    private long id;

    public long getId() {
        return id;
    }

    public boolean isNew() {
        return id == 0;
    }

    public void save(Database database) throws SQLException {
        database.save(this);
    }
}
