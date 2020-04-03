package no.netb.archiver;


import de.perschon.resultflow.Result;
import no.netb.archiver.models.FsNode;
import no.netb.archiver.repository.Repository;

public class Main {

    public static void main( String args[] ) {
        Result<FsNode[], Exception> selectResult = Repository.selectN(FsNode.class, "WHERE f.isFile = ?", 0);
        if (selectResult.isErr()) {
            throw new RuntimeException("select failed:", selectResult.getError().get());
        }
        FsNode[] fsNodes = selectResult.unwrap();
        System.out.println("ok");
    }
}
