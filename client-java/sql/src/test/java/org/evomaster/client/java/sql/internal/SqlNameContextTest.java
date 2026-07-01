package org.evomaster.client.java.sql.internal;

import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SqlNameContextTest {

    @Test
    public void handlesAliasOnParenthesizedUnionSubquery() {
        String sql = "SELECT derived.id\n" +
                "FROM (\n" +
                "    SELECT id FROM constraint_requires\n" +
                "    UNION ALL\n" +
                "    SELECT id FROM constraint_excludes\n" +
                ") derived\n" +
                "WHERE derived.id = 2";

        Statement statement = SqlParserUtils.parseSqlCommand(sql);
        SqlNameContext context = assertDoesNotThrow(() -> new SqlNameContext(statement));

        assertEquals(
                SqlNameContext.UNNAMED_TABLE,
                context.getFullyQualifiedTableName(new Column(new Table("derived"), "id"))
        );
    }
}
