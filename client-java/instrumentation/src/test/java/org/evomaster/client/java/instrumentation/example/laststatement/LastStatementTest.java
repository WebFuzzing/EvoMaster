package org.evomaster.client.java.instrumentation.example.laststatement;

import com.foo.somedifferentpackage.examples.laststatement.LastStatementImp;
import com.foo.somedifferentpackage.examples.queryparam.UsingWebRequestImp;
import org.evomaster.client.java.instrumentation.AdditionalInfo;
import org.evomaster.client.java.instrumentation.InstrumentingClassLoader;
import org.evomaster.client.java.instrumentation.example.queryparam.UsingWebRequest;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class LastStatementTest {

    private LastStatement ls;

    @BeforeEach
    public void reset() throws Exception {

        ExecutionTracer.reset();

        InstrumentingClassLoader cl = new InstrumentingClassLoader("com.foo");

        ls = (LastStatement) cl.loadClass(LastStatementImp.class.getName()).newInstance();
    }

    @Test
    public void testBase(){

        //as constructor was called in @Before, let's force a reset
        ExecutionTracer.reset();

        AdditionalInfo info = ExecutionTracer.exposeAdditionalInfoList().get(0);
        String last = info.getLastExecutedStatement();
        assertNull(last);

        ls.base();

        info = ExecutionTracer.exposeAdditionalInfoList().get(0);
        last = info.getLastExecutedStatement();
        assertNotNull(last);
        assertTrue(last.contains("base"));
    }

    @Test
    public void testBaseNoReset(){

        //even if no reset, a new call on empty stack should get rid off the previous last
        ls.base();

        AdditionalInfo info = ExecutionTracer.exposeAdditionalInfoList().get(0);
        String last = info.getLastExecutedStatement();
        assertNotNull(last);
        assertTrue(last.contains("base"));
    }

    @Test
    public void testExceptionInMiddle(){

        AdditionalInfo info;

        ls.exceptionInMiddle(false);
        info = ExecutionTracer.exposeAdditionalInfoList().get(0);
        String a = info.getLastExecutedStatement();
        assertNotNull(a);
        assertTrue(a.contains("exceptionInMiddle"));

        try{ls.exceptionInMiddle(true);} catch (Exception e){}

        info = ExecutionTracer.exposeAdditionalInfoList().get(0);
        String b = info.getLastExecutedStatement();
        assertNotNull(b);
        assertTrue(b.contains("exceptionInMiddle"));

        //as line number is higher, because exception was in middle
        assertTrue(a.compareTo(b) > 0);
    }


    @Test
    public void exceptionInMethodInput(){

        AdditionalInfo info;
        String last;

        try{ls.exceptionInMethodInput(false);}catch (Exception e){}
        info = ExecutionTracer.exposeAdditionalInfoList().get(0);
        last = info.getLastExecutedStatement();
        assertNotNull(last);
        assertFalse(last.contains("exceptionInMiddle"), last);
        assertTrue(last.contains("exceptionInMethodInput"), last);

        ExecutionTracer.reset();

        try{ls.exceptionInMethodInput(true);}catch (Exception e){}
        info = ExecutionTracer.exposeAdditionalInfoList().get(0);
        last = info.getLastExecutedStatement();
        assertNotNull(last);
        //now it is inverted
        assertTrue(last.contains("exceptionInMiddle"), last);
        assertFalse(last.contains("exceptionInMethodInput"), last);
    }
}