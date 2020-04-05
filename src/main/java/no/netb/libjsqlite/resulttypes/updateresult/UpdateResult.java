package no.netb.libjsqlite.resulttypes.updateresult;

import no.netb.libjcommon.result.Result;
import no.netb.libjsqlite.Jsqlite;

import java.sql.SQLException;
import java.util.Optional;

public abstract class UpdateResult extends Result<UpdateOkAction, Exception> {

    public static UpdateOk ok() {
        return new UpdateOk(() -> {
            try {
                Jsqlite.getConnection().commit();
                return Optional.empty();
            } catch (SQLException e) {
                return Optional.of(e);
            }
        });
    }

    public static UpdateErr err(Exception e) {
        return new UpdateErr(e);
    }
}
