package org.evomaster.client.java.sql;

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType;

public class DbUnsupportedException extends IllegalStateException{

    DbUnsupportedException(DatabaseType type){
        this(type.toString());
    }

    DbUnsupportedException(String type){
        super("Database type " + type + " is not supported");
    }

}