package org.evomaster.client.java.sql.heuristic;

import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.update.Update;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TableAliasResolver {
    private final Map<String, TableReference> aliasMap = new HashMap<>();

    public TableAliasResolver(Statement statement) {
        if (statement instanceof Select) {
            processSelect((Select)statement);
        } else if (statement instanceof Delete) {
            //TODO implement Delete aliases
        } else if (statement instanceof Update) {
            //TODO implement UPDATE aliases
        }
    }

    private void processSelect(Select select) {
        if (select.getWithItemsList()!=null) {
            final List<WithItem> withItemsList = select.getWithItemsList();
            processWithItemsList(withItemsList);
        }

        if (select instanceof PlainSelect) {
            processPlainSelect((PlainSelect) select);
        } else if (select instanceof SetOperationList) {
            processSetOperationList((SetOperationList) select);
        } else if (select instanceof WithItem) {
            processWithItem((WithItem) select);
        } else if (select instanceof ParenthesedSelect) {
            processParenthesedSelect((ParenthesedSelect) select);
        }
    }

    private void processWithItemsList(List<WithItem> withItemsList) {
        for (WithItem withItem : withItemsList) {
            if (withItem.getAlias() != null) {
                aliasMap.put(withItem.getAlias().getName(), TableReference.createDerivedTableReference(withItem));
            }
            processSelect(withItem);
        }
    }

    private void processParenthesedSelect(ParenthesedSelect select) {
        ParenthesedSelect parenthesedSelect = select;
        processSelect(parenthesedSelect.getSelect());
    }

    private void processWithItem(WithItem select) {
        WithItem withItem = select;
        processSelect(withItem.getSelect());
    }

    private void processSetOperationList(SetOperationList select) {
        SetOperationList setOperationList = select;
        for (Select body : setOperationList.getSelects()) {
            processSelect(body);
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
                aliasMap.put(table.getAlias().getName(), TableReference.createBaseTableReference(table.getName()));
            }
        } else if (fromItem instanceof ParenthesedSelect) {
            ParenthesedSelect subSelect = (ParenthesedSelect) fromItem;
            if (subSelect.getAlias() != null) {
                aliasMap.put(subSelect.getAlias().getName(), TableReference.createDerivedTableReference(subSelect));
            }
            processSelect(subSelect.getPlainSelect());
        }
    }

    public Map<String, TableReference> getAliasMap() {
        return aliasMap;
    }

    public TableReference getTableReference(String alias) {
        return aliasMap.get(alias);
    }

}
