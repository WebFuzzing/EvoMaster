package org.evomaster.client.java.sql.advanced.select_query;

import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.schema.Table;

import static java.util.Objects.nonNull;
import static org.evomaster.client.java.sql.advanced.query_calculator.where_calculator.WhereCalculator.EMPTY_STRING;

public class QueryTable {

    public static final QueryTable DEFAULT_TABLE = createQueryTable(EMPTY_STRING);

    private String name;
    private String alias;

    private QueryTable(String name, String alias) {
        this.name = name;
        this.alias = alias;
    }

    public static QueryTable createQueryTable(String name, String alias) {
        return new QueryTable(name.toLowerCase(), nonNull(alias) ? alias.toLowerCase() : null);
    }

    public static QueryTable createQueryTable(Table table, Alias alias) {
        return createQueryTable(table.getName(), nonNull(alias) ? alias.getName() : null);
    }

    public static QueryTable createQueryTable(Table table) {
        return createQueryTable(table, table.getAlias());
    }

    public static QueryTable createQueryTable(String name) {
        return createQueryTable(name, null);
    }

    public String getName() {
        return name;
    }

    public Boolean hasAlias() {
        return nonNull(alias);
    }

    public String getAlias() {
        return alias;
    }

    @Override
    public String toString(){
        return hasAlias() ? name + " " + alias : name;
    }
}
