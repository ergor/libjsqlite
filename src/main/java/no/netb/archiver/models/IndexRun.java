package no.netb.archiver.models;

import no.netb.archiver.annotations.Db;
import no.netb.archiver.annotations.Fk;

import java.util.Date;


public class IndexRun extends ModelBase {

    @Db
    private Date date;
    @Db
    @Fk(Host.class)
    private long hostId;
}
