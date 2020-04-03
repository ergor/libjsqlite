package no.netb.archiver.models;

import no.netb.archiver.annotations.Db;
import no.netb.archiver.annotations.Pk;

public abstract class ModelBase {

    @Db
    @Pk
    private long id;

    public long getId() {
        return id;
    }
}
