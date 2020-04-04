package models;

import no.netb.libjsqlite.BaseModel;
import no.netb.libjsqlite.annotations.Db;
import no.netb.libjsqlite.annotations.Fk;

public class ReferencingSomeTable extends BaseModel {

    @Db
    private String otherText;

    @Db
    @Fk(SomeTable.class)
    private long someTableId;
}
