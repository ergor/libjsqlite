package no.netb.libjsqlite;

public class TableInfo {

    private String name;
    private String alias;

    public TableInfo(String name, String alias) {
        this.name = name;
        this.alias = alias;
    }

    public String getName() {
        return name;
    }

    public String getAlias() {
        return alias;
    }

    static<T extends BaseModel> TableInfo fromClass(Class<T> modelClass) {
        String name = modelClass.getSimpleName();
        return new TableInfo(name, String.valueOf(name.toLowerCase().charAt(0)));
    }
}
