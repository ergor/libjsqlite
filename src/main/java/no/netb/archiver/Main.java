package no.netb.archiver;


import no.netb.archiver.models.FsNode;
import no.netb.archiver.repository.Repository;

public class Main {

    public static void main( String args[] ) {
        FsNode[] nodes = Repository.selectN(FsNode.class, "WHERE f.isFile = ?", 0);
        System.out.println("ok");
    }
}
