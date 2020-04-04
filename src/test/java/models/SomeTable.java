package models;

import no.netb.libjsqlite.BaseModel;
import no.netb.libjsqlite.annotations.Db;

public class SomeTable extends BaseModel {
    @Db
    private long x;

    @Db
    private String text;

    public SomeTable(long x, String text) {
        this.x = x;
        this.text = text;
    }

    public long getX() {
        return x;
    }

    public String getText() {
        return text;
    }
}
