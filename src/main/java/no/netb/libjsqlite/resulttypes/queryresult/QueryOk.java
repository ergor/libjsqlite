package no.netb.libjsqlite.resulttypes.queryresult;

public class QueryOk<V> extends QueryResult<V> {
    private QueryOk() {
        // hide
    }

    public QueryOk(V okVal) {
        this.okVal = okVal;
    }
}
