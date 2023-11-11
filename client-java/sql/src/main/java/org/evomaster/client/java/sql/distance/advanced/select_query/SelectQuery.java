package org.evomaster.client.java.sql.distance.advanced.select_query;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.*;

import java.util.LinkedList;
import java.util.List;

import static java.lang.String.format;
import static java.util.Objects.nonNull;
import static org.evomaster.client.java.sql.distance.advanced.helpers.SqlParserHelper.parseStatement;

/**
 * Abstraction over JSQL parser SELECT statement. In order to apply the heuristic, we
 * need to know if the SELECT has a WHERE clause and be able to remove that clause. See
 * {@link #isRestricted } and {@link #unrestrict } methods for those functionalities.
 */
public class SelectQuery {

    private PlainSelect select;

    private SelectQuery(PlainSelect select) {
        this.select = select;
    }

    public static SelectQuery createSelectQuery(String queryString) {
        Statement statement = parseStatement(queryString);
        if(statement instanceof Select) {
            return createSelectQuery((Select) statement);
        } else {
            throw new UnsupportedOperationException(format("Unsupported query: %s", statement.toString()));
        }
    }

    public static SelectQuery createSelectQuery(Select select) {
        if(select instanceof ParenthesedSelect) {
            return createSelectQuery(((ParenthesedSelect) select).getSelect());
        }
        return new SelectQuery(select.getPlainSelect());
    }

    private FromItem getFromItem() {
        return select.getFromItem();
    }

    /**
     * Determines if query is restricted, this is, if a WHERE clause is present.
     *
     * @return true if query is restricted, false otherwise.
     */
    public Boolean isRestricted() {
        return nonNull(getWhere());
    }

    public Expression getWhere() {
        return select.getWhere();
    }

    private List<Join> getJoins() {
        return select.getJoins();
    }

    /**
     * Removes query restrictions, this is, removes the WHERE clause. It also removes
     * the selected columns and replaces them with an *. Maintains the JOINs, if any.
     *
     * @return a new select query with the mentioned modifications
     */
    public SelectQuery unrestrict() {
        List<SelectItem<?>> selectItems = new LinkedList<>();
        selectItems.add(new SelectItem<>(new AllColumns()));
        return new SelectQuery(new PlainSelect()
            .withSelectItems(selectItems)
            .withFromItem(getFromItem())
            .withJoins(getJoins()));
    }

    @Override
    public String toString() {
        return select.toString();
    }
}
