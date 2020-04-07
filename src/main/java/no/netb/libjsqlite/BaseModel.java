package no.netb.libjsqlite;

import no.netb.libjsqlite.annotations.Db;
import no.netb.libjsqlite.annotations.Pk;
import no.netb.libjsqlite.resulttypes.updateresult.UpdateResult;

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

    public void saveOrFail(Database database) {
        UpdateResult saveResult = database.save(this);
        saveResult.getErr().ifPresent(e -> {throw new RuntimeException(e);});
    }
}
