package org.evomaster.client.java.instrumentation.coverage.methodreplacement;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes.WebRequestClassReplacement;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReplacementListTest {

    @Test
    public void testIntegerReplacement(){

        List<MethodReplacementClass> list = ReplacementList.getReplacements("java/lang/Integer");
        assertTrue(list.size() > 0);
    }

    @Test
    public void testReplacementMethods() {

        for (MethodReplacementClass mrc : ReplacementList.getList()) {

            //make sure that during testing all third-party libraries are available
            assertTrue(mrc.isAvailable(), "Not available: " + mrc.getClass().getName());

            for (Method m : mrc.getClass().getDeclaredMethods()) {

                Replacement r = m.getAnnotation(Replacement.class);
                if (r == null) {
                    continue;
                }

                assertTrue(Modifier.isStatic(m.getModifiers()), "Replacement methods must be static");

                if (r.type() == ReplacementType.BOOLEAN) {
                    assertSame(m.getReturnType(), Boolean.TYPE,
                            "Non-boolean return " + m.getReturnType() + " type for " +
                                    mrc.getClass().getName() + "#" + m.getName());
                }

                Class[] inputs = m.getParameterTypes();
                Class<?> targetClass = mrc.getTargetClass();
                assertNotNull(targetClass);

                if(r.type() != ReplacementType.TRACKER){
                    assertTrue(inputs.length>0, "Should always be at least 1 parameter, eg the idTemplate");
                    assertEquals(String.class, inputs[inputs.length-1], "Last parameter should always be the idTemplate");
                }

                if(! r.replacingStatic()){
                    //if not replacing a static method, then caller must be passed as first input
                    assertTrue(inputs.length >= 1);// caller

                    if(mrc instanceof ThirdPartyMethodReplacementClass) {
                        //must always be Object when dealing with third-party library replacements
                        assertEquals(Object.class, inputs[0]);
                    } else {
                        assertEquals(targetClass, inputs[0]);
                    }
                }

                int start = 0;
                if(!r.replacingStatic()){
                    start = 1;
                }

                int end = inputs.length-1;
                if(r.type() == ReplacementType.TRACKER){
                    //no idTemplate at the end
                    end = inputs.length;
                }

                Class[] reducedInputs = Arrays.copyOfRange(inputs, start, end);

                Method targetMethod = null;
                try {
                    targetMethod = targetClass.getMethod(m.getName(), reducedInputs);
                } catch (NoSuchMethodException e) {
                    fail("No target method '"+m.getName()+" in class "+targetClass.getName()+" with the right input parameters");
                }

                assertEquals(r.replacingStatic(), Modifier.isStatic(targetMethod.getModifiers()));
            }

        }

    }
}