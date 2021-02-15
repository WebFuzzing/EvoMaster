package org.evomaster.client.java.controller.db;

public class DbUnsupportedException extends IllegalStateException{

    DbUnsupportedException(SupportedDatabaseType type){
        this(type.toString());
    }

    DbUnsupportedException(String type){
        super("Database type " + type + " is not supported");
    }

}