package no.netb.archiver.models;

import no.netb.archiver.annotations.Db;
import no.netb.archiver.annotations.Fk;

/**
 * To manually connect multiple index runs and mark them as logically 1 index run.
 *
 * Use case: you index localhost, remote A and remote B in separate runs,
 * and between the runs you are sure you didn't modify the dirs under indexing.
 * Then you can "connect" the runs together as 1 logical run.
 */
public class IndexRunConnector extends ModelBase {

    @Db
    private String uuid; // this will be the logical run id.

    @Db
    @Fk(IndexRun.class)
    private long indexRunId;
}
