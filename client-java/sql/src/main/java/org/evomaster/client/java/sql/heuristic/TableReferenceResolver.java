package org.evomaster.client.java.sql.heuristic;

import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.update.Update;

import java.util.*;

/**
 * This class is responsible for resolving table aliases in SQL statements.
 * Every time a new SQL alias context (e.g. subselect) is entered, the
 * method enterAliasContext should be called. Every time the context is exited,
 * the method exitAliasContext should be called.
 */
public class TableReferenceResolver {

    private final Deque<Map<String, SqlTableReference>> stackOfTableAliases = new ArrayDeque<>();

    /**
     * This method is called when entering a new alias context.
     * It processes the given SQL statement to collect table aliases.
     *
     * @param statement the SQL statement to process
     */
    public void enterAliasContext(Statement statement) {
        Objects.requireNonNull(statement, "statement cannot be null");

        createNewAliasContext();
        if (statement instanceof Update) {
            processUpdate((Update) statement);
        } else if (statement instanceof Delete) {
            processDelete((Delete) statement);
        } else if (statement instanceof Select) {
            processSelect((Select) statement);
        } else {
            throw new IllegalArgumentException("Unsupported SQL statement type: " + statement.getClass().getName());
        }
    }

    public TableReferenceResolver() {
        super();
    }

    private void processJoins(List<Join> joins) {
        Objects.requireNonNull(joins, "joins cannot be null");

        for (Join join : joins) {
            processFromItem(join.getRightItem());
        }
    }

    private void processUpdate(Update update) {
        Objects.requireNonNull(update, "update cannot be null");

        if (update.getWithItemsList() != null) {
            processWithItemsList(update.getWithItemsList());
        }
        processFromItem(update.getTable());
        if (update.getStartJoins() != null) {
            processJoins(update.getStartJoins());
        }
        if (update.getFromItem() != null) {
            processFromItem(update.getFromItem());
        }
        if (update.getJoins() != null) {
            processJoins(update.getJoins());
        }
    }

    private void processDelete(Delete delete) {
        Objects.requireNonNull(delete, "delete cannot be null");

        if (delete.getWithItemsList() != null) {
            processWithItemsList(delete.getWithItemsList());
        }
        processFromItem(delete.getTable());
        if (delete.getJoins() != null) {
            processJoins(delete.getJoins());
        }
    }

    private void processSelect(Select select) {
        Objects.requireNonNull(select, "select cannot be null");

        if (select.getWithItemsList() != null) {
            processWithItemsList(select.getWithItemsList());
        }

        if (select instanceof PlainSelect) {
            processPlainSelect((PlainSelect) select);
        } else if (select instanceof ParenthesedSelect) {
            ParenthesedSelect parenthesedSelect = (ParenthesedSelect) select;
            if (parenthesedSelect.getAlias() != null) {
                stackOfTableAliases.peek().put(parenthesedSelect.getAlias().getName(), new SqlDerivedTableReference(parenthesedSelect));
            }
            Select innerSelect = parenthesedSelect.getSelect();
            processSelect(innerSelect);
        }

    }

    private void createNewAliasContext() {
        stackOfTableAliases.push(new HashMap<>());
    }

    private void processWithItemsList(List<WithItem> withItemsList) {
        for (WithItem withItem : withItemsList) {
            if (withItem.getAlias() != null) {

                final String aliasName = withItem.getAlias().getName();
                final Select subquery = withItem.getSelect();
                final SqlTableReference derivedSqlTableReference = new SqlDerivedTableReference(subquery);
                stackOfTableAliases.peek().put(aliasName, derivedSqlTableReference);
            }
        }
    }


    private void processPlainSelect(PlainSelect select) {
        Objects.requireNonNull(select, "select cannot be null");

        PlainSelect plainSelect = select;
        if (plainSelect.getFromItem()!=null) {
            processFromItem(plainSelect.getFromItem());
        }
        if (plainSelect.getJoins() != null) {
            for (Join join : plainSelect.getJoins()) {
                processFromItem(join.getRightItem());
            }
        }
    }

    private void processFromItem(FromItem fromItem) {
        Objects.requireNonNull(fromItem, "fromItem cannot be null");

        if (fromItem instanceof Table) {
            Table table = (Table) fromItem;
            if (table.getAlias() != null) {
                stackOfTableAliases.peek().put(table.getAlias().getName(), new SqlBaseTableReference(table.getFullyQualifiedName()));
            }
        } else if (fromItem instanceof ParenthesedSelect) {
            ParenthesedSelect subSelect = (ParenthesedSelect) fromItem;
            if (subSelect.getAlias() != null) {
                stackOfTableAliases.peek().put(subSelect.getAlias().getName(), new SqlDerivedTableReference(subSelect));
            }
        }
    }

    /**
     * Returns the TableReference for the given alias from the closest context
     * where it was defined
     *
     * @param alias the alias to resolve
     * @return a TableReference object with the table or the derived table (e.g. view)
     */
    public SqlTableReference resolveTableReference(String alias) {
        if (!isAliasDeclaredInAnyContext(alias)) {
            throw new IllegalArgumentException("Alias not found in any context: " + alias);
        }

        // The Deque's iterator traverses the stack from top (latest push) to bottom (first push)
        for (Map<String, SqlTableReference> context : stackOfTableAliases) {
            if (context.containsKey(alias)) {
                return context.get(alias);
            }
        }
        throw new IllegalArgumentException("Alias not found in any context: " + alias);
    }

    /**
     * Removes all the aliases declared in the current alias context.
     */
    public void exitAliasContext() {
        stackOfTableAliases.pop();
    }

    /**
     * Returns the current depth of the alias context stack.
     *
     * @return The current depth of the alias context stack.
     */
    public int getContextDepth() {
        return stackOfTableAliases.size();
    }

    /**
     * Checks if the alias is declared in the current context.
     *
     * @param alias The alias to check.
     * @return true if the alias is declared in the current context, false otherwise.
     */
    public boolean isAliasDeclaredInCurrentContext(String alias) {
        if (stackOfTableAliases.isEmpty()) {
            throw new IllegalArgumentException("Alias stack is empty. Cannot resolve alias: " + alias);
        }
        return stackOfTableAliases.peek().containsKey(alias);
    }

    /**
     * Checks if the alias is declared in any context.
     *
     * @param alias
     * @return
     */
    public boolean isAliasDeclaredInAnyContext(String alias) {
        for (Map<String, SqlTableReference> context : stackOfTableAliases) {
            if (context.containsKey(alias)) {
                return true;
            }
        }
        return false;
    }
}
