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

    public ReferencingSomeTable(String otherText, long someTableId) {
        this.otherText = otherText;
        this.someTableId = someTableId;
    }

    public String getOtherText() {
        return otherText;
    }

    public long getSomeTableId() {
        return someTableId;
    }
}
