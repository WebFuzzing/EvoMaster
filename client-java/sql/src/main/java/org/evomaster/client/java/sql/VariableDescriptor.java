package org.evomaster.client.java.sql;

/**
 * A descriptor representing a column in a SQL table.
 *
 * Immutable. Names are case-insensitive.
 * At least column name, which might have alias
 */
public class VariableDescriptor {

    private final String columnName;

    /**
     * If no alias, this value will be equal to the column name
     */
    private final String alias;

    /**
     * Can be null
     */
    private final String tableName;


    public VariableDescriptor(String columnName) {
        this(columnName, columnName, null);
    }

    public VariableDescriptor(String columnName, String alias, String tableName) {
        this.columnName = columnName.trim().toLowerCase();
        this.alias = (alias == null || alias.trim().isEmpty() ?
                this.columnName :
                alias.trim().toLowerCase());
        this.tableName = (tableName == null || tableName.trim().isEmpty() ?
                null : tableName.trim().toLowerCase());
    }

    public String getColumnName() {
        return columnName;
    }

    public String getAlias() {
        return alias;
    }

    public String getTableName() {
        return tableName;
    }


    @Override
    public String toString() {
        String table = "";
        if(tableName != null){
            table = tableName +".";
        }

        return table + columnName + "/" + alias;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        VariableDescriptor that = (VariableDescriptor) o;

        if (columnName != null ? !columnName.equals(that.columnName) : that.columnName != null) return false;
        if (alias != null ? !alias.equals(that.alias) : that.alias != null) return false;
        return tableName != null ? tableName.equals(that.tableName) : that.tableName == null;
    }

    @Override
    public int hashCode() {
        int result = columnName != null ? columnName.hashCode() : 0;
        result = 31 * result + (alias != null ? alias.hashCode() : 0);
        result = 31 * result + (tableName != null ? tableName.hashCode() : 0);
        return result;
    }
}
