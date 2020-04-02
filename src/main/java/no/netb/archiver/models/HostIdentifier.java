package no.netb.archiver.models;

import no.netb.archiver.annotations.Db;
import no.netb.archiver.annotations.Fk;

public class HostIdentifier {

    @Db
    @Fk(Host.class)
    private long hostId;
}
