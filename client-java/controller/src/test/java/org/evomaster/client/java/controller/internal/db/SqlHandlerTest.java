package org.evomaster.client.java.controller.internal.db;

import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class SqlHandlerTest {

    @Test
    public void testPatioIssue() throws Exception{

        String select = "SELECT v.* FROM voting v, groups g WHERE v.expired = false AND '2021-04-28T16:02:27.426+0200' >= v.created_at + g.voting_duration * INTERVAL '1 hour' AND v.group_id = g.id";

        Statement stmt = CCJSqlParserUtil.parse(select);

        Map<String, Set<String>> columns = SqlHandler.extractColumnsInvolvedInWhere(stmt);
        assertTrue(columns.values().stream().flatMap(s -> s.stream()).noneMatch(c -> c.equals("false")));

        //TODO add more check on returned columns
    }
}
