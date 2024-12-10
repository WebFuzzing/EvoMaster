package org.evomaster.client.java.instrumentation.example.cast;

import com.foo.somedifferentpackage.examples.branches.BranchesImp;
import com.foo.somedifferentpackage.examples.cast.CastImp;
import com.squareup.okhttp.OkHttpClient;
import org.evomaster.client.java.instrumentation.InputProperties;
import org.evomaster.client.java.instrumentation.InstrumentingClassLoader;
import org.evomaster.client.java.instrumentation.example.branches.Branches;
import org.evomaster.client.java.instrumentation.staticstate.UnitsInfoRecorder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class CastInstrumentedTest {

    protected Cast getInstance() throws Exception {

        InstrumentingClassLoader cl = new InstrumentingClassLoader("com.foo");

        return (Cast) cl.loadClass(CastImp.class.getName()).newInstance();
    }

    private static String defaultReplacement;

    @BeforeAll
    public static void initClass(){
        defaultReplacement = System.getProperty(InputProperties.REPLACEMENT_CATEGORIES);
        if(defaultReplacement != null) {
            System.setProperty(InputProperties.REPLACEMENT_CATEGORIES, defaultReplacement + ",NET");
        } else {
            System.setProperty(InputProperties.REPLACEMENT_CATEGORIES, "BASE,SQL,EXT_0,NET");
        }
    }

    @AfterAll
    public static void tearDown(){
        if(defaultReplacement != null) {
            System.setProperty(InputProperties.REPLACEMENT_CATEGORIES, defaultReplacement);
        }
    }

    @BeforeEach
    public void initTest(){
        UnitsInfoRecorder.reset();
    }

    @Test
    public void testCast() throws Exception{

        assertEquals(0, UnitsInfoRecorder.getInstance().getNumberOfTrackedMethods());

        OkHttpClient okHttpClient = getInstance().get();
        assertNotNull(okHttpClient);

        assertEquals(1, UnitsInfoRecorder.getInstance().getNumberOfTrackedMethods());
    }
}
