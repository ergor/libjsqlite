package no.netb.archiver.models;

import no.netb.archiver.annotations.Db;
import no.netb.archiver.annotations.Table;

@Table
public class Host extends ModelBase {

    @Db
    private String name;
}
