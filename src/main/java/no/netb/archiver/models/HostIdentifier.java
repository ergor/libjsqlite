package no.netb.archiver.models;

import no.netb.archiver.annotations.Db;
import no.netb.archiver.annotations.Fk;

public class HostIdentifier extends ModelBase {

    @Db
    @Fk(Host.class)
    private long hostId;

    @Db
    private String fqdn;

    @Db
    private String ipAddress;
}
