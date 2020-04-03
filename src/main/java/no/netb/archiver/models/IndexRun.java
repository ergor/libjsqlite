package no.netb.archiver.models;

import no.netb.archiver.annotations.Db;
import no.netb.archiver.annotations.Fk;

import java.sql.Timestamp;

public class IndexRun extends ModelBase {

    @Db
    private Timestamp timestamp;

    @Db
    @Fk(Host.class)
    private long hostId;

    public IndexRun(Host host) {
        this.timestamp = new Timestamp(System.currentTimeMillis());
        this.hostId = host.getId();
    }
}
