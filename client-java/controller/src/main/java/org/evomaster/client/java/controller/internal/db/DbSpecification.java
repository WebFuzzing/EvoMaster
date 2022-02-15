package org.evomaster.client.java.controller.internal.db;

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType;

import java.sql.Connection;
import java.util.List;

public class DbSpecification {

    /**
     * specify a type of the database
     */
    public DatabaseType dbType;

    /**
     * sql connection
     */
    public List<Connection> connections;

    /**
     * schema name
     * TODO might remove this later if we could get such info with the connection
     */
    public String schemaName;

    /**
     * a script to initialize the data in database
     */
    public String initSqlScript;

    /**
     * a resource path where the init sql script is
     * Note that this parameter is specific to resource path,
     * not a path to the file.
     */
    public String initSqlOnResourcePath;

    /**
     * specify whether to employ the smart db clean by cleaning all
     * data in table which have been accessed after every test
     * Default is True
     */
    public boolean employSmartDbClean = true;

}
