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
    private final String aliasColumnName;

    /**
     * Can be null
     */
    private final String tableName;

    /*
     * If no alias, this value will be equal to table name
     */
    private final String aliasTableName;

    public VariableDescriptor(String columnName) {
        this(columnName, columnName, null, null);
    }

    public VariableDescriptor(String columnName, String aliasColumnName, String tableName) {
        this(columnName, aliasColumnName, tableName, tableName);
    }

    public VariableDescriptor(String columnName, String aliasColumnName, String tableName, String aliasTableName) {
        this.columnName = (columnName==null || columnName.trim().isEmpty() ?
                null : columnName.trim().toLowerCase());
        this.aliasColumnName = (aliasColumnName == null || aliasColumnName.trim().isEmpty() ?
                this.columnName :
                aliasColumnName.trim().toLowerCase());
        this.tableName = (tableName == null || tableName.trim().isEmpty() ?
                null : tableName.trim().toLowerCase());
        this.aliasTableName = (aliasTableName == null || aliasTableName.trim().isEmpty() ?
                null : aliasTableName.trim().toLowerCase());
    }

    public String getColumnName() {
        return columnName;
    }

    public String getAliasColumnName() {
        return aliasColumnName;
    }

    public String getTableName() {
        return tableName;
    }


    @Override
    public String toString() {
        String table = "";
        if(tableName != null){
            table = tableName + "/" + aliasTableName +".";
        }

        return table + columnName + "/" + aliasColumnName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        VariableDescriptor that = (VariableDescriptor) o;

        if (columnName != null ? !columnName.equals(that.columnName) : that.columnName != null) return false;
        if (aliasColumnName != null ? !aliasColumnName.equals(that.aliasColumnName) : that.aliasColumnName != null) return false;
        if (tableName != null ? !tableName.equals(that.tableName) : that.tableName != null) return false;
        return aliasTableName != null ? aliasTableName.equals(that.aliasTableName) : that.aliasTableName == null;
    }

    @Override
    public int hashCode() {
        int result = columnName != null ? columnName.hashCode() : 0;
        result = 31 * result + (aliasColumnName != null ? aliasColumnName.hashCode() : 0);
        result = 31 * result + (tableName != null ? tableName.hashCode() : 0);
        result = 31 * result + (aliasTableName != null ? aliasTableName.hashCode() : 0);
        return result;
    }

    public String getAliasTableName() {
        return aliasTableName;
    }
}
