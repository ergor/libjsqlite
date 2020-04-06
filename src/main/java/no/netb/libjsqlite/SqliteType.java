package no.netb.libjsqlite;

public enum SqliteType {
    INTEGER("INTEGER"),
    TEXT("TEXT");

    private String name;

    SqliteType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
