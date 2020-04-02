package no.netb.archiver.models;

import no.netb.archiver.annotations.Db;
import no.netb.archiver.annotations.Fk;

import java.util.Date;

public abstract class FsNode extends ModelBase {

    @Db
    private boolean isFile;
    @Db
    private String name;
    @Db
    private String hash;
    @Db
    private Date creationDate;
    @Db
    private Date modifiedDate;
    @Db
    @Fk(FsNode.class)
    private long parentId;
    @Db
    @Fk(IndexRun.class)
    private long indexRunId;
}
