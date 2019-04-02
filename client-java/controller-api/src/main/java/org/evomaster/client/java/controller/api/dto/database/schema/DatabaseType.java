package org.evomaster.client.java.controller.api.dto.database.schema;

public enum DatabaseType {

    H2,

    DERBY,

    POSTGRES,

    /**
     * In case used database is not listed in this enum, can
     * still try to build SQL queries, although cannot guarantee
     * that it would be correct (ie, wrong dialect).
     */
    OTHER
}
