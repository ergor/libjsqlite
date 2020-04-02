package no.netb.archiver;

import no.netb.archiver.models.FsNode;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class Indexer {

    private File directory;

    private List<FsNode> nodes;

    public Indexer(File directory) throws InstantiationException {
        if (!directory.isDirectory()) {
            throw new InstantiationException(String.format("Indexer: %s is not a directory (expected directory).", directory.getAbsolutePath()));
        }
        this.directory = directory;
    }

    public void index() throws IOException {
        File[] files = directory.listFiles();

        if (files == null) {
            throw new IOException(String.format("Indexer: could not list files in %s", directory.getAbsolutePath()));
        }

        for (File file : files) {
            
        }
    }
}
