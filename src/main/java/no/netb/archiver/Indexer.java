package no.netb.archiver;

import no.netb.archiver.models.Host;
import no.netb.archiver.models.IndexRun;

import java.io.File;
import java.io.IOException;

public class Indexer {

    public static void index(Host host, File directory) throws IOException {

        if (!directory.isDirectory()) {
            throw new IOException(String.format("Indexer: %s is not a directory (expected directory).", directory.getAbsolutePath()));
        }

        File[] files = directory.listFiles();

        if (files == null) {
            throw new IOException(String.format("Indexer: could not list files in %s", directory.getAbsolutePath()));
        }

        IndexRun indexRun = new IndexRun(host);

        for (File file : files) {

        }
    }
}
