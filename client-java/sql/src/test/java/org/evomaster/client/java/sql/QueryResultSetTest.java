package org.evomaster.client.java.sql;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class QueryResultSetTest {

    @Test
    void testAddNamedTableQueryResult() {
        QueryResult queryResult = new QueryResult(Arrays.asList("UserId","UserName"),"Users");
        QueryResultSet queryResultSet = new QueryResultSet();
        queryResultSet.addQueryResult(queryResult);

        final QueryResult actualQueryResult = queryResultSet.getQueryResultForNamedTable("Users");
        assertNotNull(actualQueryResult);
        assertEquals(queryResult, actualQueryResult);
    }



    @Test
    void testDuplicateNamedTableThrowsException() {
        QueryResult queryResult1 =new QueryResult(Arrays.asList("UserId","UserName"),"Users");
        QueryResult queryResult2 = new QueryResult(Arrays.asList("UserId","UserName"),"Users");

        QueryResultSet queryResultSet = new QueryResultSet();
        queryResultSet.addQueryResult(queryResult1);
        assertThrows(IllegalArgumentException.class, () -> queryResultSet.addQueryResult(queryResult2));
    }

    @Test
    void testDuplicatedTableThrowsException() {
        QueryResult queryResult1 =  new QueryResult(Arrays.asList("UserId","UserName"),"Users");
        QueryResult queryResult2 =  new QueryResult(Arrays.asList("userId","userName"),"users");
        QueryResultSet queryResultSet = new QueryResultSet();
        queryResultSet.addQueryResult(queryResult1);
        assertThrows(IllegalArgumentException.class, () -> queryResultSet.addQueryResult(queryResult2));
    }

    @Test
    void testCaseInsensitiveTableNames() {
        QueryResultSet caseInsensitiveSet = new QueryResultSet();
        QueryResult queryResult = new QueryResult(Arrays.asList("UserId","UserName"),"Users");

        caseInsensitiveSet.addQueryResult(queryResult);
        assertNotNull(caseInsensitiveSet.getQueryResultForNamedTable("users"));
    }

}
