import java.util.HashMap;
import java.util.Map;

public class RowWithValues {


    private String tableName;

    private Map<String, SolvedValue> columns = new HashMap<>();

    public RowWithValues(String tableName) {
        this.tableName = tableName;
    }

    public String getTableName() {
        return tableName;
    }

    public RowWithValues withColumn(String columnName, SolvedValue val) {
        columns.put(columnName, val);
        return this;
    }
    public SolvedValue getColumn(String columnName) {
        return columns.get(columnName);
    }
}
