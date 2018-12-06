package org.evomaster.client.java.instrumentation.example.queryparam;

import com.foo.somedifferentpackage.examples.queryparam.UsingWebRequestImp;
import org.evomaster.client.java.instrumentation.AdditionalInfo;
import org.evomaster.client.java.instrumentation.InstrumentingClassLoader;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.context.request.WebRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UsingWebRequestTest {

    private UsingWebRequest uwr;


    private WebRequest mockWR(){

        WebRequest x = mock(WebRequest.class);
        when(x.getParameter("p0")).thenReturn("0");
        when(x.getParameterValues("p1")).thenReturn(new String[]{"1"});
        when(x.getHeader("h0")).thenReturn("2");
        when(x.getHeaderValues("h1")).thenReturn(new String[]{"3"});

        return x;
    }

    @BeforeEach
    public void reset() throws Exception {
        ExecutionTracer.reset();

        InstrumentingClassLoader cl = new InstrumentingClassLoader("com.foo");

        uwr = (UsingWebRequest) cl.loadClass(UsingWebRequestImp.class.getName()).newInstance();
    }


    @Test
    public void testHeadersAndParams(){

        AdditionalInfo info = ExecutionTracer.exposeAdditionalInfoList().get(0);
        assertFalse(info.getQueryParametersView().contains("p0"));
        assertFalse(info.getQueryParametersView().contains("p1"));
        assertFalse(info.getHeadersView().contains("h0"));
        assertFalse(info.getHeadersView().contains("h1"));

        WebRequest x = mockWR();
        String res = uwr.compute(x);
        assertEquals("0123", res);

        assertTrue(info.getQueryParametersView().contains("p0"));
        assertTrue(info.getQueryParametersView().contains("p1"));
        assertTrue(info.getHeadersView().contains("h0"));
        assertTrue(info.getHeadersView().contains("h1"));
    }

}
