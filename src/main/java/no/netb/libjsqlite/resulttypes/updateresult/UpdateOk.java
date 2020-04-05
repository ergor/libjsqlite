package no.netb.libjsqlite.resulttypes.updateresult;


public class UpdateOk extends UpdateResult {

    private UpdateOk() {
        // hide
    }

    public UpdateOk(UpdateOkAction commitResultSupploer) {
        this.okVal = commitResultSupploer;
    }
}
