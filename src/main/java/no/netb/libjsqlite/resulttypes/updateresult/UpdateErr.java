package no.netb.libjsqlite.resulttypes.updateresult;

public class UpdateErr extends UpdateResult {
    private UpdateErr() {
        // hide
    }

    public UpdateErr(Exception exception) {
        this.errVal = exception;
    }
}
