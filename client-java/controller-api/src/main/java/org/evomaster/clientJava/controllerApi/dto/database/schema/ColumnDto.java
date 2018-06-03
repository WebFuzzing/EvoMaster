package org.evomaster.clientJava.controllerApi.dto.database.schema;

public class ColumnDto {

    public String table;

    public String name;

    public String type;

    public int size;

    public boolean primaryKey;

    public boolean nullable;

    public boolean unique;

    public boolean autoIncrement;

//    public boolean identity;

    //TODO something for other constraints
}
