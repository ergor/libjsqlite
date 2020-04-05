package no.netb.libjsqlite.resulttypes.queryresult;

public class QueryErr<V> extends QueryResult<V> {
    private QueryErr() {
        // hide
    }

    public QueryErr(Exception exception) {
        this.errVal = exception;
    }
}
