import java.util.*;

public class RowToSolve {

    private String tableName;

    // Key: column name, Value: column type: Int, Real or String
    private Map<String, RowValueType> columns = new HashMap<>();

    public RowToSolve(String tableName, Map<String, RowValueType> columns) {
        this.tableName = tableName;
        this.columns = columns;
    }

    public String getTableName() {
        return tableName;
    }

    public Map<String, RowValueType> getColumns() {
        return columns;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RowToSolve that = (RowToSolve) o;
        return Objects.equals(tableName, that.tableName) && Objects.equals(columns, that.columns);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tableName, columns);
    }
}
