package no.netb.archiver.models;

import no.netb.archiver.annotations.Db;

public abstract class ModelBase {

    @Db
    private long id;

    /* CREATE:
     * INSERT INTO <class name> VALUES <field name,value pairs>
     */

    /* SAVE:
     * if not exists: CREATE(all values)
     * else: UPDATE <class name> SET <field name,value pairs> WHERE id = <this.id>
     */

    /* EXISTS:
     */

    /* GET:
     */
}
