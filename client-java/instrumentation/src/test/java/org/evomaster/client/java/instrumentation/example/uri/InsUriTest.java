package org.evomaster.client.java.instrumentation.example.uri;

import com.foo.somedifferentpackage.examples.uri.InsUriImpl;
import org.evomaster.client.java.instrumentation.InputProperties;
import org.evomaster.client.java.instrumentation.InstrumentingClassLoader;
import org.evomaster.client.java.instrumentation.shared.StringSpecialization;
import org.evomaster.client.java.instrumentation.shared.StringSpecializationInfo;
import org.evomaster.client.java.instrumentation.shared.TaintInputName;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

public class InsUriTest {

    private InsUri x;


    @BeforeEach
    public void reset() throws Exception {
        ExecutionTracer.reset();

        System.setProperty(InputProperties.REPLACEMENT_CATEGORIES, "BASE,SQL,EXT_0");

        InstrumentingClassLoader cl = new InstrumentingClassLoader("com.foo");

        x = (InsUri) cl.loadClass(InsUriImpl.class.getName()).newInstance();
    }


    // protocol, path

    @Test
    public void testProtocol(){
        String res = x.getProtocol("http://foo");
        assertEquals("http", res);
    }

    @Test
    public void testPath(){
        String res = x.getPath("https://foo.com/hello");
        assertEquals("/hello", res);
    }


    @Test
    public void testUri0(){
        Executable fail = null;//() -> x.uri0("foo"); //don't manage to get an invalid URI...
        Supplier<String> ok = () -> x.uri0("http://hello.com/url/0");
        final String taintValue = TaintInputName.getTaintName(0);
        Executable taint = () ->  x.uri0(taintValue);

        checkUrl(fail, ok, taint, taintValue, StringSpecialization.URI);
    }

    @Test
    public void testUri1(){
        Executable fail =  null;//() -> x.uri1("foo");
        Supplier<String> ok = () -> x.uri1("http://hello.com/url/0");
        final String taintValue = TaintInputName.getTaintName(0);
        Executable taint = () ->  x.uri1(taintValue);

        checkUrl(fail, ok, taint, taintValue, StringSpecialization.URI);
    }

    @Test
    public void testUri2(){
        Executable fail =  null;//() -> x.uri2("foo");
        Supplier<String> ok = () -> x.uri2("http://hello.com/url/0");
        final String taintValue = TaintInputName.getTaintName(0);
        Executable taint = () ->  x.uri2(taintValue);

        checkUrl(fail, ok, taint, taintValue, StringSpecialization.URI);
    }

    @Test
    public void testUrl0(){
        Executable fail = () -> x.url0("foo");
        Supplier<String> ok = () -> x.url0("http://hello.com/url/0");
        final String taintValue = TaintInputName.getTaintName(0);
        Executable taint = () ->  x.url0(taintValue);

        checkUrl(fail, ok, taint, taintValue, StringSpecialization.URL);
    }

    @Test
    public void testUrl1(){
        Executable fail = () -> x.url1("foo:foo");
        Supplier<String> ok = () -> x.url1("http://hello.com/url/0");
        final String taintValue = TaintInputName.getTaintName(0);
        Executable taint = () ->  x.url1(taintValue);

        checkUrl(fail, ok, taint, taintValue, StringSpecialization.URL);
    }

    @Test
    public void testUrl2(){
        Executable fail = () -> x.url2("foo");
        Supplier<String> ok = () -> x.url2("http://hello.com/url/0");
        final String taintValue = TaintInputName.getTaintName(0);
        Executable taint = () ->  x.url2(taintValue);

        checkUrl(fail, ok, taint, taintValue, StringSpecialization.URL);
    }

    private void checkUrl(Executable fail, Supplier<String> ok, Executable taint, String taintValue, StringSpecialization specialization){


        if(fail != null) {
            assertThrows(IllegalArgumentException.class, fail);
        }

        String res = ok.get();
        assertEquals("OK",res);

        Map<String, Set<StringSpecializationInfo>> specs = ExecutionTracer.exposeAdditionalInfoList().get(0).getStringSpecializationsView();
        assertTrue(specs.isEmpty());

        if(fail == null){
            try {
                taint.execute();
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        } else {
            assertThrows(IllegalArgumentException.class, taint);
        }

        specs = ExecutionTracer.exposeAdditionalInfoList().get(0).getStringSpecializationsView();
        assertEquals(1, specs.size());
        Set<StringSpecializationInfo> set = specs.get(taintValue);
        assertTrue(set.stream().anyMatch(t -> t.getStringSpecialization() == specialization));
    }
}
