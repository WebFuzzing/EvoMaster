package org.evomaster.client.java.instrumentation;


import org.junit.jupiter.api.Test;

public class InstrumentingIssuesTest {

    @Test
    public void testIssueWithMySQLIO() throws ClassNotFoundException {

        InstrumentingClassLoader cl = new InstrumentingClassLoader("does.not.matter");
        cl.setCrashWhenFailedInstrumentation(true);

        //this should not crash
        cl.loadClass("com.mysql.jdbc.MysqlIO");

        /*
            The bug here was that we were skipping JSR inlining in the <clinit>
         */
    }
}
