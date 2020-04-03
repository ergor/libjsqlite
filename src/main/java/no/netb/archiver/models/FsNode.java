package no.netb.archiver.models;

import no.netb.archiver.annotations.Db;
import no.netb.archiver.annotations.Fk;
import no.netb.archiver.annotations.Table;

import java.sql.Timestamp;

@Table
public class FsNode extends ModelBase {

    @Db
    private boolean isFile;

    @Db
    private String name;

    @Db
    private String hash;

    @Db
    private Timestamp creationDate;

    @Db
    private Timestamp modifiedDate;

    @Db(nullable = true)
    @Fk(FsNode.class)
    private long parentId;

    @Db
    @Fk(IndexRun.class)
    private long indexRunId;
}
