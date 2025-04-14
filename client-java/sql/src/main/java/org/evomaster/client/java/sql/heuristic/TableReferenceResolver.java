package org.evomaster.client.java.sql.heuristic;

import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.schema.Table;

import java.util.*;

/**
 * This class is responsible for resolving table aliases in SQL statements.
 * Every time a new SQL alias context (e.g. subselect) is entered, the
 * method enterAliasContext should be called. Every time the context is exited,
 * the method exitAliasContext should be called.
 */
public class TableReferenceResolver {

    private final Deque<Map<String, TableReference>> stackOfTableAliases = new ArrayDeque<>();

    /**
     * This method is called when entering a new alias context.
     * It processes the given Select statement to collect table aliases.
     *
     * @param statement the Select statement to process
     */
    public void enterAliasContext(Select statement) {
        processSelect(statement);
    }

    public TableReferenceResolver() {
        super();
    }

    private void processSelect(Select select) {
        createNewAliasContext();

        if (select.getWithItemsList() != null) {
            final List<WithItem> withItemsList = select.getWithItemsList();
            processWithItemsList(withItemsList);
        }

        if (select instanceof PlainSelect) {
            processPlainSelect((PlainSelect) select);
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
                final TableReference derivedTableReference = TableReference.createDerivedTableReference(subquery);
                stackOfTableAliases.peek().put(aliasName, derivedTableReference);
            }
        }
    }


    private void processPlainSelect(PlainSelect select) {
        PlainSelect plainSelect = select;
        processFromItem(plainSelect.getFromItem());
        if (plainSelect.getJoins() != null) {
            for (Join join : plainSelect.getJoins()) {
                processFromItem(join.getRightItem());
            }
        }
    }

    private void processFromItem(FromItem fromItem) {
        if (fromItem instanceof Table) {
            Table table = (Table) fromItem;
            if (table.getAlias() != null) {
                stackOfTableAliases.peek().put(table.getAlias().getName(), TableReference.createBaseTableReference(table));
            }
        } else if (fromItem instanceof ParenthesedSelect) {
            ParenthesedSelect subSelect = (ParenthesedSelect) fromItem;
            if (subSelect.getAlias() != null) {
                stackOfTableAliases.peek().put(subSelect.getAlias().getName(), TableReference.createDerivedTableReference(subSelect));
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
    public TableReference resolveTableReference(String alias) {
        if (!isAliasDeclaredInAnyContext(alias)) {
            throw new IllegalArgumentException("Alias not found in any context: " + alias);
        }

        // The Deque's iterator traverses the stack from top (latest push) to bottom (first push)
        for (Map<String, TableReference> context : stackOfTableAliases) {
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
        for (Map<String, TableReference> context : stackOfTableAliases) {
            if (context.containsKey(alias)) {
                return true;
            }
        }
        return false;
    }
}
