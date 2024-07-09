package org.evomaster.client.java.sql.advanced.select_query;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.statement.select.SetOperationList.SetOperationType;
import net.sf.jsqlparser.statement.update.Update;
import org.evomaster.client.java.sql.advanced.function_finder.FunctionFinder;
import org.evomaster.client.java.sql.advanced.helpers.SqlParserHelper;
import org.evomaster.client.java.sql.advanced.query_calculator.where_calculator.WhereCalculator;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static java.util.Objects.nonNull;
import static net.sf.jsqlparser.statement.select.SetOperationList.SetOperationType.UNION;
import static org.evomaster.client.java.sql.advanced.select_query.QueryTable.createQueryTable;

public class SelectQuery {

    public static final String ALL = "ALL";

    private Select select;

    private SelectQuery(Select select){
        this.select = select;
    }

    public static SelectQuery createSelectQuery(String queryString){
        Statement statement = SqlParserHelper.parseStatement(queryString);
        if(statement instanceof Select){
            return createSelectQuery((Select) statement);
        } else if(statement instanceof Delete) {
            return createSelectQuery(createSelectStatement((Delete) statement));
        } else if(statement instanceof Update) {
            return createSelectQuery(createSelectStatement((Update) statement));
        } else {
            throw new UnsupportedOperationException(format("Unsupported query: %s", statement.toString()));
        }
    }

    public static SelectQuery createSelectQuery(Select select){
        if(select instanceof ParenthesedSelect){
            return createSelectQuery(((ParenthesedSelect) select).getSelect());
        }
        return new SelectQuery(select);
    }

    private static Select createSelectStatement(Delete delete) {
        return new PlainSelect()
            .withSelectItems(selectAllItems())
            .withFromItem(delete.getTable())
            .withJoins(delete.getJoins())
            .withWhere(delete.getWhere());
    }

    private static Select createSelectStatement(Update update) {
        return new PlainSelect()
            .withSelectItems(selectAllItems())
            .withFromItem(update.getTable())
            .withJoins(update.getJoins())
            .withWhere(update.getWhere());
    }

    public Boolean isPlainSelect(){
        return select instanceof PlainSelect;
    }

    private PlainSelect getPlainSelect(){
        return select.getPlainSelect();
    }

    public Boolean hasFrom(){
        return nonNull(getFrom());
    }

    private FromItem getFrom(){
        return getPlainSelect().getFromItem();
    }

    public List<QueryTable> getFromTables(){
        List<QueryTable> tables = new LinkedList<>();
        if(hasFrom()){
            tables.add(table(getFrom()));
            if(hasJoins()){
                getJoins().forEach(join -> tables.add(table(join.getFromItem())));
            }
        }
        return tables;
    }

    private QueryTable table(FromItem fromItem){
        if(isTable(fromItem)){
            return createQueryTable((Table) fromItem);
        } else {
            SelectQuery subquery = subquery(fromItem);
            FromItem subqueryFromItem = subquery.getFrom();
            if(isTable(subqueryFromItem)){
                return createQueryTable((Table) subqueryFromItem, fromItem.getAlias());
            } else {
                throw new UnsupportedOperationException(format("Unsupported subquery in query: %s", select.toString()));
            }
        }
    }

    private static Boolean isTable(FromItem fromItem){
        return fromItem instanceof Table;
    }

    private static SelectQuery subquery(FromItem fromItem){
        return createSelectQuery((Select) fromItem);
    }

    public Boolean hasFromSubquery(){
        return hasFrom() && !isTable(getFrom());
    }

    public SelectQuery getFromSubquery(){
        return subquery(getFrom());
    }

    public Boolean hasJoins(){
        return nonNull(getJoins());
    }

    private List<Join> getJoins(){
        return getPlainSelect().getJoins();
    }

    public Boolean isInnerJoin(){
        return getJoin().isInnerJoin();
    }

    public Boolean isFullJoin(){
        return getJoin().isFull();
    }

    public Boolean isLeftJoin(){
        return getJoin().isLeft();
    }

    public Boolean isRightJoin(){
        return getJoin().isRight();
    }

    public Boolean isCrossJoin(){
        return getJoin().isCross() || (isInnerJoin() && getJoin().getOnExpressions().isEmpty());
    }

    private Join getJoin(){
        return getJoins().stream()
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Joins must not be empty"));
    }

    public List<SelectQuery> getJoinSelects(){
        return getAllFromItems().stream()
            .map(this::fromItemToSelect)
            .collect(Collectors.toList());
    }

    private List<FromItem> getAllFromItems(){
        return Stream.concat(Stream.of(getFrom()),
            hasJoins() ? getJoins().stream().map(Join::getFromItem) : Stream.of()).collect(Collectors.toList());
    }

    private SelectQuery fromItemToSelect(FromItem fromItem){
        if(isTable(fromItem)){
            return createSelectQuery(new PlainSelect().withSelectItems(selectAllItems()).withFromItem(fromItem));
        } else {
            return createSelectQuery((Select) fromItem);
        }
    }

    public SelectQuery convertToCrossJoin(){
        Join innerJoin = getJoin();
        Join crossJoin = new Join()
            .withCross(true)
            .setFromItem(innerJoin.getFromItem());
        PlainSelect plainSelect = new PlainSelect()
            .withSelectItems(selectAllItemsAndFunctions())
            .withFromItem(getFrom())
            .withJoins(singletonList(crossJoin));
        if(!innerJoin.getOnExpressions().isEmpty()){
            plainSelect.withWhere(innerJoin.getOnExpressions().stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Expressions must not be empty")));
        }
        return new SelectQuery(plainSelect);
    }

    private List<SelectItem<?>> selectAllItemsAndFunctions(){
        List<SelectItem<?>> selectItems = selectAllItems();
        FunctionFinder functionFinder = new FunctionFinder(select);
        List<Function> functions = functionFinder.getFunctions();
        selectItems.addAll(functions.stream().map(SelectItem::new).collect(Collectors.toList()));
        return selectItems;
    }

    private static List<SelectItem<?>> selectAllItems(){
        List<SelectItem<?>> selectItems = new LinkedList<>();
        selectItems.add(new SelectItem<>(new AllColumns()));
        return selectItems;
    }

    public Boolean isSetOperationList(){
        return select instanceof SetOperationList;
    }

    public List<SelectQuery> getSetOperationSelects(){
        return select.getSetOperationList().getSelects().stream()
            .map(SelectQuery::createSelectQuery).collect(Collectors.toList());
    }

    private SetOperationList getSetOperationList(){
        return select.getSetOperationList();
    }

    public Boolean isUnion(){
        return isSetOperationList() && hasUnionOperators();
    }

    private Boolean hasUnionOperators(){
        return getSetOperationList().getOperations().stream()
            .map(SetOperation::toString)
            .map(SelectQuery::removeAllModifier)
            .map(SetOperationType::from)
            .allMatch(setOperationType -> setOperationType.equals(UNION));
    }

    private static String removeAllModifier(String setOperation){
        return setOperation.contains(ALL) ? setOperation.replaceAll(ALL, WhereCalculator.EMPTY_STRING).trim() : setOperation;
    }

    public Boolean hasWhere(){
        return nonNull(getWhere());
    }

    public Expression getWhere(){
        return getPlainSelect().getWhere();
    }

    public SelectQuery removeWhere(){
        return new SelectQuery(new PlainSelect()
            .withSelectItems(selectAllItemsAndFunctions())
            .withFromItem(getFrom())
            .withJoins(getJoins()));
    }

    @Override
    public String toString(){
        return select.toString();
    }
}