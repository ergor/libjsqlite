package no.netb.archiver;

import de.perschon.resultflow.Result;
import no.netb.archiver.models.Host;
import no.netb.archiver.models.IndexRun;

import java.io.File;

public class Indexer {

    public static Result<Object, String> index(Host host, File directory) {

        if (!directory.isDirectory()) {
            return Result.err(String.format("Indexer: %s is not a directory (expected directory).", directory.getAbsolutePath()));
        }

        File[] files = directory.listFiles();

        if (files == null) {
            return Result.err(String.format("Indexer: could not list files in %s", directory.getAbsolutePath()));
        }

        IndexRun indexRun = new IndexRun(host);

        for (File file : files) {

        }

        return Result.ok(new Object());
    }
}
