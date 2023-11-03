package org.evomaster.client.java.sql.internal;

import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import org.evomaster.client.java.sql.internal.ParserUtils;
import org.evomaster.client.java.sql.internal.SqlHandler;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SqlHandlerTest {

    @Test
    public void testPatioIssue() throws Exception {

        String select = "SELECT v.* FROM voting v, groups g WHERE v.expired = false AND '2021-04-28T16:02:27.426+0200' >= v.created_at + g.voting_duration * INTERVAL '1 hour' AND v.group_id = g.id";

        Statement stmt = CCJSqlParserUtil.parse(select);

        Map<String, Set<String>> columns = new SqlHandler(null).extractColumnsInvolvedInWhere(stmt);
        assertTrue(columns.values().stream().flatMap(s -> s.stream()).noneMatch(c -> c.equals("false")));

        //TODO add more check on returned columns
    }

    @Test
    public void testBooleans() throws Exception {

        String select = "SELECT f.* FROM Foo WHERE f.a = TRUE AND f.b = On AND f.c = false AND f.d = f";

        Statement stmt = CCJSqlParserUtil.parse(select);

        /*
            TODO in the future, when handle boolean constants in parser, this ll need to be updated
         */
        Map<String, Set<String>> columns = new SqlHandler(null).extractColumnsInvolvedInWhere(stmt);
        assertTrue(columns.isEmpty());
    }

    @Test
    public void testCreateCachedLocalTemporaryTable() {
        String createSql = "create cached local temporary table if not exists HT_feature_constraint (id bigint not null) on commit drop transactional";
        boolean canParseSqlStatement = ParserUtils.canParseSqlStatement(createSql);
        assertFalse(canParseSqlStatement);
    }





}