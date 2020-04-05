package no.netb.libjsqlite.resulttypes.updateresult;

import java.sql.SQLException;
import java.util.Optional;

@FunctionalInterface
public interface UpdateOkAction {
    Optional<SQLException> commit();
}
