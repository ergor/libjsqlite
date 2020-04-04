package no.netb.libjsqlite;

import no.netb.libjsqlite.annotations.Db;
import no.netb.libjsqlite.annotations.Pk;

public abstract class BaseModel {

    @Db
    @Pk
    private long id;

    public long getId() {
        return id;
    }
}
