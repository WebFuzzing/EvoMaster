package org.evomaster.client.java.sql.internal;


import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.statement.update.Update;
import org.evomaster.client.java.controller.api.dto.database.schema.DbSchemaDto;

import java.util.*;

/**
 * Given a column, we need to determinate to which table it
 * belongs to. This is not always simple, as SQL queries can use "aliases".
 * <p>
 * This problem is further exacerbated by:
 * 1) a SELECT can have many sub-SELECTs inside it, each one defining their own
 * independent aliases
 * 2) a SQL command might not have all the necessary info to infer the right table
 * for a column. In those (valid) cases of ambiguity, we must refer to the schema.
 *
 * WARNING: we lowercase all names of tables and columns, as SQL is (should be?) case insensitive
 */
public class SqlNameContext {

    /**
     * WARNING: in general we shouldn't use mutable DTO as internal data structures.
     * But, here, what we need is very simple (just checking for names).
     */
    private  DbSchemaDto schema;


    /**
     * Key -> table alias,
     * Value -> table name
     */
    private final Map<String, String> tableAliases = new HashMap<>();

    private final Statement statement;

    //TODO will need refactoring when supporting nested SELECTs
    public static final String UNNAMED_TABLE = "___unnamed_table___";


    /**
     * WARNING: should only be used in tests, to avoid each time having
     * to provide a schema for the test data
     *
     * @param statement to create context for
     */
    public SqlNameContext(Statement statement) {
        schema = null;
        this.statement = Objects.requireNonNull(statement);
        computeAliases();
    }

    public void setSchema(DbSchemaDto schema) {
        this.schema = Objects.requireNonNull(schema);
    }

    /**
     * Check if table contains a column with the given name.
     * This is based on the DB schema.
     *
     * If no schema is defined, this method returns false.
     */
    public boolean hasColumn(String tableName, String columnName){
        Objects.requireNonNull(tableName);
        Objects.requireNonNull(columnName);

        if(schema == null){
            return false;
        }

        return this.schema.tables.stream()
                .filter(t -> t.name.equalsIgnoreCase(tableName))
                .flatMap(t -> t.columns.stream())
                .filter(c -> c.name.equalsIgnoreCase(columnName))
                .count() > 0;
    }

  /*
        TODO
        code here is not supporting nested SELECTs, for the moment
     */

    /**
     * @param column a column object
     * @return the name of the table that this column belongs to
     */
    public String getTableName(Column column) {

        Table table = column.getTable();

        if (table != null) {
            return tableAliases.getOrDefault(table.getName().toLowerCase(), table.getName().toLowerCase());
        }

        if(statement instanceof Select) {
            List<String> candidates = getTableNamesInFrom();

            assert !candidates.isEmpty();

            if (candidates.size() == 1) {
                return candidates.get(0);
            } else {
                //TODO case of possible ambiguity... need to check the schema
                throw new IllegalArgumentException("TODO ambiguity");
            }
        } else if(statement instanceof Delete){
            Delete delete = (Delete) statement;
            return delete.getTable().getName().toLowerCase();
        } else if(statement instanceof Update){
            Update update = (Update) statement;
            return update.getTable().getName().toLowerCase();
        }else {
            throw new IllegalArgumentException("Cannot handle table name for: " + statement);
        }
    }


    private List<String> getTableNamesInFrom() {

        FromItem fromItem = getFromItem();

        List<String> names = new ArrayList<>();

        FromItemVisitorAdapter visitor = new FromItemVisitorAdapter(){
            @Override
            public void visit(Table table) {
                names.add(table.getName().toLowerCase());
            }
        };

        fromItem.accept(visitor);

        return names;
    }

    private FromItem getFromItem() {

        FromItem fromItem = null;

        if(statement instanceof Select) {
            SelectBody selectBody = ((Select) statement).getSelectBody();

            if (selectBody instanceof PlainSelect) {
                PlainSelect plainSelect = (PlainSelect) selectBody;

                fromItem =  plainSelect.getFromItem();
            } else {
                throw new IllegalArgumentException("Currently only handling Plain SELECTs");
            }
        }

        if(fromItem == null)
            throw new IllegalArgumentException("Cannot handle FromItem for: " + statement);

        return fromItem;
    }


    private void computeAliases() {

        if (statement instanceof Select) {
            FromItem fromItem = getFromItem();
            fromItem.accept(new AliasVisitor(tableAliases));

            SelectBody selectBody = ((Select) statement).getSelectBody();
            PlainSelect plainSelect = (PlainSelect) selectBody;

            List<Join> joins = plainSelect.getJoins();
            if (joins != null) {
                joins.forEach(j -> j.getRightItem().accept(new AliasVisitor(tableAliases)));
            }
        } else if(statement instanceof Delete){
            //no alias required?
            return;
        } else if(statement instanceof Update){
            /*
                TODO can update have aliases?
                https://www.h2database.com/html/commands.html#update
             */
            return;
        }
    }


    private static class AliasVisitor extends FromItemVisitorAdapter {

        private final Map<String, String> aliases;

        private AliasVisitor(Map<String, String> aliases) {
            this.aliases = aliases;
        }

        @Override
        public void visit(Table table) {
            handleAlias(aliases, table);
        }

        @Override
        public void visit(SubSelect subSelect) {
            handleAlias(aliases, subSelect);
        }

    }


    private static void handleAlias(Map<String, String> aliases, SubSelect subSelect) {
        Alias alias = subSelect.getAlias();
        if (alias != null) {
            String aliasName = alias.getName();
            if (aliasName != null) {
                /*
                    FIXME: need to generalize,
                    ie for when there can be several un-named sub-selects referring
                    to columns with same names
                 */
                String tableName = UNNAMED_TABLE;
                aliases.put(aliasName.trim().toLowerCase(), tableName.trim().toLowerCase());
            }
        }
    }


    private static void handleAlias(Map<String, String> aliases, Table table) {
        Alias alias = table.getAlias();
        if (alias != null) {
            String aliasName = alias.getName();
            if (aliasName != null) {
                String tableName = table.getName().toLowerCase();
                aliases.put(aliasName.trim().toLowerCase(), tableName.trim().toLowerCase());
            }
        }
    }
}
