package org.evomaster.client.java.controller.api.dto.database.schema;

public enum DatabaseType {

    H2,

    DERBY,

    MYSQL,

    POSTGRES,

    MARIADB,

    MS_SQL_SERVER,


    /**
     * In case used database is not listed in this enum, can
     * still try to build SQL queries, although cannot guarantee
     * that it would be correct (ie, wrong dialect).
     *
     * there are some codes for MS SQL Server 2000.
     * but since the technique is retired, we do not support it now.
     */
    OTHER
}
