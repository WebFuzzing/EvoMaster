package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.*;

public class StatementClassReplacementTest {

    private static boolean previosIsExecutingInitSql;

    @BeforeAll
    public static void setUpExecutionTracer() {
        previosIsExecutingInitSql = ExecutionTracer.isExecutingInitSql();
        ExecutionTracer.reset();
        ExecutionTracer.setExecutingInitSql(false);
    }

    @AfterAll
    public static void restoreExecutionTracer() {
        ExecutionTracer.setExecutingInitSql(previosIsExecutingInitSql);
    }

    @Test
    public void testHandleSqlForEmptySqlString() {
        assertEquals(1, ExecutionTracer.exposeAdditionalInfoList().size());
        assertTrue(ExecutionTracer.exposeAdditionalInfoList().get(0).getSqlInfoData().isEmpty());

        StatementClassReplacement.handleSql("", false, 2);

        assertEquals(1, ExecutionTracer.exposeAdditionalInfoList().size());
        assertEquals(1, ExecutionTracer.exposeAdditionalInfoList().get(0).getSqlInfoData().size());
        assertEquals("", ExecutionTracer.exposeAdditionalInfoList().get(0).getSqlInfoData().iterator().next().getSqlCommand());
    }
}
