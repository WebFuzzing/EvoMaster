package org.evomaster.client.java.instrumentation;


import org.junit.jupiter.api.Test;

public class InstrumentingIssuesTest {

    @Test
    public void testIssueWithMySQLIO() throws Exception {

        InstrumentingClassLoader cl = new InstrumentingClassLoader("does.not.matter");

         cl.loadClass("com.mysql.jdbc.MysqlIO").newInstance();
    }
}
