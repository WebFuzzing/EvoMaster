package org.evomaster.client.java.instrumentation.coverage.methodreplacement;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class ReplacementListTest {


    @Test
    public void testReplacementMethods() {

        for (MethodReplacementClass mrc : ReplacementList.getList()) {

            for (Method m : mrc.getClass().getDeclaredMethods()) {

                Replacement r = m.getAnnotation(Replacement.class);
                if (r == null) {
                    continue;
                }

                if (r.type() == Replacement.TYPE.BOOLEAN) {
                    assertSame(m.getReturnType(), Boolean.TYPE,
                            "Non-boolean return " + m.getReturnType() + " type for " +
                                    mrc.getClass().getName() + "#" + m.getName());
                }

                Class[] inputs = m.getParameterTypes();
                assertTrue(inputs.length>0, "Should always be at least 1 parameter, eg the idTemplate");
                assertEquals(String.class, inputs[inputs.length-1], "Last parameter should always be the idTemplate");

                Class<?> targetClass = mrc.getTargetClass();
                assertNotNull(targetClass);

                Class[] reducedInputs;
                if(r.replacingStatic()){
                    reducedInputs = Arrays.copyOfRange(inputs, 0, inputs.length-1);
                } else {
                    reducedInputs = Arrays.copyOfRange(inputs, 1, inputs.length-1);
                }

                try {
                    targetClass.getMethod(m.getName(), reducedInputs);
                } catch (NoSuchMethodException e) {
                    fail("No target method '"+m.getName()+" in class "+targetClass.getName()+" with the right input parameters");
                }
            }

        }

    }
}