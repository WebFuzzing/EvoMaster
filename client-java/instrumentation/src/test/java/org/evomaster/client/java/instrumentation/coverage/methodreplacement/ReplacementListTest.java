package org.evomaster.client.java.instrumentation.coverage.methodreplacement;

import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

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

                assertTrue(!(r.replacingConstructor() && r.replacingStatic()), "We do not replace static constructors (would that even be possible?)");

                assertTrue(Modifier.isStatic(m.getModifiers()), "Issue with " + mrc.getClass() + ": Replacement methods must be static");

                if (r.type() == ReplacementType.BOOLEAN) {
                    assertSame(m.getReturnType(), Boolean.TYPE,
                            "Non-boolean return " + m.getReturnType() + " type for " +
                                    mrc.getClass().getName() + "#" + m.getName());
                }

                Class[] inputs = m.getParameterTypes();
                Class<?> targetClass = mrc.getTargetClass(this.getClass().getClassLoader());
                assertNotNull(targetClass);

                if (r.type() != ReplacementType.TRACKER) {
                    assertTrue(inputs.length > 0, "Should always be at least 1 parameter, eg the idTemplate");
                    assertEquals(String.class, inputs[inputs.length - 1], "Last parameter should always be the idTemplate");
                }

                if (!r.replacingStatic() && !r.replacingConstructor()) {
                    //if not replacing a static method, then caller must be passed as first input
                    assertTrue(inputs.length >= 1);// caller

                    if (mrc instanceof ThirdPartyMethodReplacementClass) {
                        //must always be Object when dealing with third-party library replacements
                        assertEquals(Object.class, inputs[0]);
                    } else {
                        assertEquals(targetClass, inputs[0]);
                    }
                }

                if (r.replacingConstructor()) {
                    assertEquals(Void.TYPE, m.getReturnType());
                }

                checkInputParameters(m);

                int start = 0;
                if (!r.replacingStatic()) {

                    if (r.replacingConstructor()) {
                        start = 0; // no skips
                    } else {
                        start = 1; // skip caller
                    }
                }

                int skipLast = 1;
                if (r.type() == ReplacementType.TRACKER) {
                    //no idTemplate at the end
                    skipLast = 0;
                }

                Class[] reducedInputs = ReplacementUtils.getParameterTypes(m, start, skipLast, true).toArray(new Class[0]);

                if (!r.replacingConstructor()) {

                    Method targetMethod = null;
                    String replacementMethodName = ReplacementUtils.getPossiblyModifiedName(m);
                    try {
                        targetMethod = targetClass.getMethod(replacementMethodName, reducedInputs);
                    } catch (NoSuchMethodException e) {
                        try {
                            targetMethod = targetClass.getDeclaredMethod(replacementMethodName, reducedInputs);
                        } catch (NoSuchMethodException noSuchMethodException) {
                            fail("No target method " + replacementMethodName + " in class " + targetClass.getName() + " with the right input parameters");
                        }
                    }
                    assertEquals(r.replacingStatic(), Modifier.isStatic(targetMethod.getModifiers()));
                    checkReturnType(r, targetMethod.getReturnType(), m);

                } else{
                    Constructor targetConstructor = null;
                    try {
                        targetConstructor = targetClass.getConstructor(reducedInputs);
                    } catch (NoSuchMethodException e) {
                        fail("No constructor in class " + targetClass.getName() + " with the right input parameters");
                    }
                    assertNotNull(targetConstructor);

                    Optional<Method> orc = Arrays.stream(mrc.getClass().getDeclaredMethods())
                            .filter(it -> it.getName().equals(MethodReplacementClass.CONSUME_INSTANCE_METHOD_NAME))
                            .findFirst();
                    if(!orc.isPresent()){
                        fail("No instance consume method: "+MethodReplacementClass.CONSUME_INSTANCE_METHOD_NAME+"()");
                    }
                    Method rconsume = orc.get();
                    if (rconsume.getAnnotation(Replacement.class) != null) {
                        fail("Consume method should not be marked with replacement annotation");
                    }
                    assertEquals(0, rconsume.getParameterCount());
                    checkReturnType(r, targetClass, rconsume);
                }

                if(r.extraPackagesToConsider().length > 0 && r.usageFilter() != UsageFilter.ONLY_SUT){
                    fail("Can apply 'extraPackagesToConsider' only to 'ONLY_SUT' filter");
                }
            }

        }

    }

    private void checkInputParameters(Method m){

        Class<?>[] parameters = m.getParameterTypes();
        Annotation[][] annotations = m.getParameterAnnotations();

        for(int i=0; i<parameters.length; i++){
            Class<?> p = parameters[i];
            checkBaseJdkType(m,p);

            Class<?> casted = ReplacementUtils.getCastedToThirdParty(ReplacementListTest.class.getClassLoader(),annotations[i]);
            if(casted != null) {
                assertEquals(Object.class, p); //all casts must use Object in signature
            }
        }
    }

    private void checkReturnType(
            Replacement r,
            Class<?> expectedReturnedType,
            Method emMethodReturningInstance) {
        Class returnType = emMethodReturningInstance.getReturnType();

        checkBaseJdkType(emMethodReturningInstance, returnType);

        if(!r.castTo().isEmpty()) {
            Class cast = null;
            try {
                cast = this.getClass().getClassLoader().loadClass(r.castTo());
            } catch (ClassNotFoundException e) {
                fail("Cannot find class " + r.castTo());
            }

            assertEquals(Object.class, emMethodReturningInstance.getReturnType(),"When using 'castTo', return type MUST be Object");
            returnType = cast;
        }

        assertEquals(expectedReturnedType, returnType);
    }

    private static void checkBaseJdkType(Method m, Class typeToCheck) {
        //TODO what about array of arrays?
        Class baseReturnType = typeToCheck.isArray() ? typeToCheck.getComponentType() : typeToCheck;

        assertTrue(baseReturnType.isPrimitive()
                        || baseReturnType.getName().startsWith("java."),
                "Types in signature (eg return and input parameters) must be basic from JDK API, ie java.*." +
                        " If not, use 'castTo' (for return) or 'ThirdPartyCast' (for inputs)." +
                        " This is a must to avoid issues in multi-classloader contexts," +
                        " eg, like in Spring applications." +
                        " Wrong type " + typeToCheck.getName() + " in " +
                        m.getDeclaringClass().getName() + "#" + m.getName()
        );
    }
}