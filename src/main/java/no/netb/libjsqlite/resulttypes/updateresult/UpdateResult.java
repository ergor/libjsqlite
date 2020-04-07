package no.netb.libjsqlite.resulttypes.updateresult;

import no.netb.libjcommon.result.Result;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

public abstract class UpdateResult extends Result<UpdateOkAction, Exception> {

    public static UpdateOk ok(Connection connection) {
        return new UpdateOk(() -> {
            try {
                connection.commit();
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
