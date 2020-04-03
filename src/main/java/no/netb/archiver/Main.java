package no.netb.archiver;


import de.perschon.resultflow.Result;
import no.netb.archiver.models.FsNode;
import no.netb.archiver.models.TablesInit;
import no.netb.archiver.repository.Repository;

import java.sql.SQLException;
import java.util.List;

public class Main {

    public static void main(String args[]) {

        try {
            TablesInit.createTables();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        Result<List<FsNode>, Exception> selectResult = Repository.selectN(FsNode.class, "WHERE f.isFile = ?", 0);
        if (selectResult.isErr()) {
            throw new RuntimeException("select failed:", selectResult.getError().get());
        }
        List<FsNode> fsNodes = selectResult.unwrap();
        System.out.println("ok");
    }
}
