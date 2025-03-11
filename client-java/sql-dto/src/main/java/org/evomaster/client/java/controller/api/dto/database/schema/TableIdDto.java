package org.evomaster.client.java.controller.api.dto.database.schema;

public class TableIdDto {

    /**
     * The schema this table belongs to.
     *  Note that databases like MySQL make no distinction between catalog and schema.
     */
    public String schema;


    /**
     * A physical database can have several independent catalogs.
     * For Postgres, can only access the catalog specified in the connection, ie., one per connection.
     * MySQL can access all catalogs even with a single connection (they work more like schemas...)
     */
    public String catalog;

    /**
     * The name of the table
     */
    public String name;
}
