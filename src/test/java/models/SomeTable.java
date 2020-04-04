package models;

import no.netb.libjsqlite.BaseModel;
import no.netb.libjsqlite.annotations.Db;

public class SomeTable extends BaseModel {
    @Db
    private long x;

    @Db
    private String text;
}
