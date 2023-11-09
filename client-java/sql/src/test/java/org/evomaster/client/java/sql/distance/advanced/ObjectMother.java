package org.evomaster.client.java.sql.distance.advanced;

import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;

public class ObjectMother {

    public static Column createColumn(String name) {
        return new Column().withColumnName(name);
    }

    public static Column createColumn(String name, Table table) {
        return createColumn(name).withTable(table);
    }
}
