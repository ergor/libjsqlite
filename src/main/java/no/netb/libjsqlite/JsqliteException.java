package no.netb.libjsqlite;

public class JsqliteException extends Exception {
    public JsqliteException(String s, Class<? extends BaseModel> modelClass) {
        super("jsqlite: problem with model class " + modelClass.getName() + ": " + s);
    }
}
