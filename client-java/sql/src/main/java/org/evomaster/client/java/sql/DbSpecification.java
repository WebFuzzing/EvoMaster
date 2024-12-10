package org.evomaster.client.java.sql;

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType;

import java.sql.Connection;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class DbSpecification {

    /**
     * specify a type of the database
     */
    public final DatabaseType dbType;

    /**
     * sql connection
     */
    public final Connection connection;

    /**
     * schema name
     * TODO might remove this later if we could get such info with the connection
     */
    public final List<String> schemaNames;

    /**
     * a script to initialize the data in database
     */
    public final String initSqlScript;

    /**
     * a resource path where the init sql script is
     * Note that this parameter is specific to resource path,
     * not a path to the file.
     */
    public final String initSqlOnResourcePath;

    /**
     * specify whether to employ the smart db clean by cleaning all
     * data in table which have been accessed after every test
     * Default is True
     */
    public final boolean employSmartDbClean;


    private DbSpecification(DatabaseType dbType, Connection connection, List<String> schemaNames, String initSqlScript, String initSqlOnResourcePath, boolean employSmartDbClean) {
        this.dbType = Objects.requireNonNull(dbType);
        this.connection = Objects.requireNonNull(connection);
        this.schemaNames = schemaNames;
        this.initSqlScript = initSqlScript;
        this.initSqlOnResourcePath = initSqlOnResourcePath;
        this.employSmartDbClean = employSmartDbClean;
    }

    public DbSpecification(DatabaseType dbType, Connection connection) {
        this(dbType, connection, null, null, null, true);
    }

    public DbSpecification withSchemas(String... schemas){

        for(String s : schemas){
            if(s == null || s.isEmpty() || s.trim().isEmpty()){
                throw new IllegalArgumentException("Empty schema name");
            }
        }
        if(schemas.length == 0){
            throw new IllegalArgumentException("No schema name provided");
        }

        return new DbSpecification(
                this.dbType,
                this.connection,
                Arrays.asList(schemas),
                this.initSqlScript,
                this.initSqlOnResourcePath,
                this.employSmartDbClean
        );
    }

    public DbSpecification withDisabledSmartClean(){
        return new DbSpecification(
                this.dbType,
                this.connection,
                this.schemaNames,
                this.initSqlScript,
                this.initSqlOnResourcePath,
                false
        );
    }


    public DbSpecification withInitSqlScript(String script){

        if(script==null || script.isEmpty() || script.trim().isEmpty()){
            throw new IllegalArgumentException("Missing script");
        }

        return new DbSpecification(
                this.dbType,
                this.connection,
                this.schemaNames,
                script,
                this.initSqlOnResourcePath,
                this.employSmartDbClean
        );
    }

    public DbSpecification withInitSqlOnResourcePath(String path){

        if(path==null || path.isEmpty() || path.trim().isEmpty()){
            throw new IllegalArgumentException("Missing script path");
        }

        return new DbSpecification(
                this.dbType,
                this.connection,
                this.schemaNames,
                this.initSqlScript,
                path,
                this.employSmartDbClean
        );
    }
}
