package models;

import no.netb.libjsqlite.BaseModel;
import no.netb.libjsqlite.EnumColumn;
import no.netb.libjsqlite.annotations.Db;

import java.sql.Timestamp;

public class SomeTable extends BaseModel {

    public enum SomeEnum implements EnumColumn {
        MEMBER_A(0),
        MEMBER_B(1);

        private int value;

        SomeEnum(int value) {
            this.value = value;
        }

        @Override
        public int toInt() {
            return value;
        }
    }

    @Db
    private long x;

    @Db
    private String text;

    @Db
    private Timestamp timestamp;

    @Db
    private boolean bool;

    @Db
    private SomeEnum someEnum;

    public SomeTable() {

    }

    public SomeTable(long x, String text, Timestamp timestamp, boolean bool, SomeEnum someEnum) {
        this.x = x;
        this.text = text;
        this.timestamp = timestamp;
        this.bool = bool;
        this.someEnum = someEnum;
    }

    public long getX() {
        return x;
    }

    public void setX(long x) {
        this.x = x;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isBool() {
        return bool;
    }

    public void setBool(boolean bool) {
        this.bool = bool;
    }

    public SomeEnum getSomeEnum() {
        return someEnum;
    }

    public void setSomeEnum(SomeEnum someEnum) {
        this.someEnum = someEnum;
    }
}
