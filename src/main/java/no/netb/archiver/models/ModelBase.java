package no.netb.archiver.models;

import no.netb.archiver.annotations.Db;

public abstract class ModelBase {

    @Db
    private long id;

    public long getId() {
        return id;
    }

    public void createTable() {

    }
}
