package org.evomaster.client.java.instrumentation.coverage.methodreplacement;

import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.evomaster.client.java.instrumentation.staticstate.UnitsInfoRecorder;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Third-party libraries might or might not be on the classpath.
 * Furthermore, they MUST NOT be part of EvoMaster.
 * So we have to use reflection to access them at runtime.
 * <p>
 * There is a problem though :(
 * The replaced methods might have inputs or outputs from the third-party library.
 * To write replacements, we need to create the right method signatures, and those must
 * be available at compilation time.
 * A previous attempt to solve this issue was to include those dependencies, but with "provided" scope (so
 * they will not be included in the uber jar).
 * Unfortunately, this does NOT work, as the classloader that loads the instrumentation might be different from
 * the one used for the SUT. This is the case for example for Spring applications when using External Driver.
 * <p>
 * The current solution is to use reflection, and have such 3rd-party library NOT on the classpath.
 * They can be in "test" scope when running tests (eg to check validity of string constants), though.
 * <p>
 * Still, this leaves issue with method signatures.
 * For return types using 3rd-party objects, must put Object as return type, with actual type specified
 * in "castTo". A forced casting is automatically then done at instrumentation time.
 * For input parameters, will need to use the ThirdPartyCast annotation.
 * <p>
 * There is still the issue of which classloader to use for reflection.
 * For MR of non-static methods, can use classloader of the original caller.
 * For the other cases (eg, static methods and constructors), need to retrieve appropriate classloader from
 * UnitInfoRecorder.
 */
public abstract class ThirdPartyMethodReplacementClass implements MethodReplacementClass {

    private static class StateInfo {

        public Class<?> targetClass;

        /**
         * Key -> id defined in @Replacement
         * Value -> original target method that was replaced
         */
        public final Map<String, Method> methods = new HashMap<>();

        /**
         * Key -> id defined in @Replacement
         * Value -> original target method that was replaced
         */
        public final Map<String, Constructor> constructors = new HashMap<>();
    }

    private final IdentityHashMap<ClassLoader, StateInfo> classInfoPerClassLoader = new IdentityHashMap<>();


    protected ThirdPartyMethodReplacementClass() {
    }

    private void initMethods(ClassLoader loader, StateInfo info) {
    /*
        Use reflection to load all methods that were replaced.
        This is essential to simplify the writing of the replacement, as those
        must still call the original, but only via reflection (as original third-party
        library must not be included in EvoMaster)
     */
        Class<? extends ThirdPartyMethodReplacementClass> subclass = this.getClass();

        for (Method m : subclass.getDeclaredMethods()) {

            Replacement r = m.getAnnotation(Replacement.class);
            if (r == null || r.id().isEmpty()) {
                continue;
            }
            if (r.replacingConstructor())
                continue;

            Class[] inputs = m.getParameterTypes();
            Annotation[][] annotations = m.getParameterAnnotations();

            int start = 0;
            if (!r.replacingStatic()) {
                start = 1;
            }

            int end = inputs.length - 1;
            if (r.type() == ReplacementType.TRACKER) {
                //no idTemplate at the end
                end = inputs.length;
            }

            Class[] reducedInputs = Arrays.copyOfRange(inputs, start, end);

            for (int i = start; i < end; i++) {
                if (annotations[i].length > 0) {
                    Class<?> klazz = ReplacementUtils.getCastedToThirdParty(loader,annotations[i]);
                    if (klazz != null)
                        reducedInputs[i - start] = klazz;
                }
            }
            for(int i=0; i<reducedInputs.length; i++){
                try {
                    //TODO might no longer be needed after change to getCastedToThirdParty
                    reducedInputs[i] = loader.loadClass(reducedInputs[i].getName());
                } catch (ClassNotFoundException e) {
                    //shouldn't really happen...
                }
            }


            Class<?> targetClass = getTargetClass(loader);
            Method targetMethod;
            String replacementMethodName = ReplacementUtils.getPossiblyModifiedName(m);
            try {
                //this will not return private methods
                targetMethod = targetClass.getMethod(replacementMethodName, reducedInputs);
            } catch (NoSuchMethodException e) {
                try {
                    //this would return private methods, but not public in superclasses
                    targetMethod = targetClass.getDeclaredMethod(replacementMethodName, reducedInputs);
                } catch (NoSuchMethodException noSuchMethodException) {
                    throw new RuntimeException("BUG in EvoMaster: " + e);
                }
            }

            String id = r.id();

            if (info.methods.containsKey(id)) {
                throw new IllegalStateException("Non-unique id: " + id);
            }

            info.methods.put(id, targetMethod);
        }
    }

    private void initConstructors(ClassLoader loader, StateInfo info) {

        Class<? extends ThirdPartyMethodReplacementClass> subclass = this.getClass();

        for (Method m : subclass.getDeclaredMethods()) {

            Replacement r = m.getAnnotation(Replacement.class);

            if (r == null || r.id().isEmpty()) {
                continue;
            }

            if (!r.replacingConstructor())
                continue;

            Class[] inputs = m.getParameterTypes();

            int start = 0;

            int end = inputs.length - 1;
            if (r.type() == ReplacementType.TRACKER) {
                //no idTemplate at the end
                end = inputs.length;
            }

            Class[] reducedInputs = Arrays.copyOfRange(inputs, start, end);
            Annotation[][] annotations = m.getParameterAnnotations();

            for (int i = start; i < end; i++) {
                if (annotations[i].length > 0) {
                    Class<?> klazz = ReplacementUtils.getCastedToThirdParty(loader,annotations[i]);
                    if (klazz != null)
                        reducedInputs[i - start] = klazz;
                }
            }

            Constructor targetConstructor = null;
            try {
                targetConstructor = getTargetClass(loader).getConstructor(reducedInputs);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException("BUG in EvoMaster: " + e);
            }

            String id = r.id();

            if (info.constructors.containsKey(id)) {
                throw new IllegalStateException("Non-unique id: " + id);
            }

            info.constructors.put(id, targetConstructor);

        }
    }

    protected abstract String getNameOfThirdPartyTargetClass();

    /**
     * @param singleton a reference to an instance of the subclass. As reflection is expensive,
     *                  we suggest to create it only once, and save it in final static field
     * @param id        of a replacement method
     * @return original method that was replaced
     */
    public static Method getOriginal(ThirdPartyMethodReplacementClass singleton, String id, Object obj) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Invalid empty id");
        }

        Objects.requireNonNull(obj);
        ClassLoader loader = obj.getClass().getClassLoader();
        StateInfo info = singleton.classInfoPerClassLoader.get(loader);

        if (info == null) {
            info = initializeClassInfo(singleton, loader);
        }

        Method original = info.methods.get(id);
        if (original == null) {
            throw new IllegalArgumentException("No method exists with id: " + id);
        }
        return original;
    }


    private static StateInfo initializeClassInfo(ThirdPartyMethodReplacementClass singleton, ClassLoader loader) {
        Class<?> target = singleton.tryLoadingClass(loader);
        StateInfo info = new StateInfo();
        info.targetClass = target;
        singleton.initMethods(loader, info);
        singleton.initConstructors(loader, info);
        singleton.classInfoPerClassLoader.put(loader, info);
        return info;
    }

    /**
     * @param singleton a reference to an instance of the subclass. As reflection is expensive,
     *                  we suggest to create it only once, and save it in final static field
     * @param id        of a replacement method
     * @return original constructor that was replaced
     */
    public static Constructor getOriginalConstructor(ThirdPartyMethodReplacementClass singleton, String id) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Invalid empty id");
        }

            /*
                we do not have access to the caller directly here, so we need to use what registered
                in ExecutionTracer
             */

        String callerName = ExecutionTracer.getLastCallerClass();
        if (callerName == null) {
            //this would be clearly a bug...
            throw new IllegalStateException("No access to last caller class");
        }

        /*
            TODO what if more than 1 classloader available ???

            This is tricky... originally, we went directly for classloader of caller class, to avoid possible issues
            of class not been initialized yet.
            however, that didn't work in some cases (eg reservations-api).
            so, we go directly to the original class, and, if no info for it, we fallback on caller class
         */
        ClassLoader loader = UnitsInfoRecorder.getInstance().getFirstClassLoader(singleton.getTargetClassName());
        if(loader == null) {
            //might have to store last one in use for caller class
            loader = UnitsInfoRecorder.getInstance().getClassLoaders(callerName).get(0);
        }

        StateInfo info = singleton.classInfoPerClassLoader.get(loader);

        if (info == null) {
            info = initializeClassInfo(singleton, loader);
        }

        Constructor original = info.constructors.get(id);
        if (original == null) {
            throw new IllegalArgumentException("No constructor exists with id: " + id);
        }
        return original;
    }

    private Class<?> tryLoadingClass(ClassLoader classLoader) {
        try {
            return classLoader.loadClass(getTargetClassName());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("ISSUE IN EVOMASTER: classloader problems when dealing with: " + getTargetClassName());
        }
    }

    @Override
    public final Class<?> getTargetClass() {
        throw new IllegalStateException("This method should never be called on a third-party replacement");
    }

    @Override
    public Class<?> getTargetClass(ClassLoader loader) {

        try {
            return loader.loadClass(getTargetClassName());
        } catch (ClassNotFoundException e) {
            //this can happen if the third-party library is missing.
            //it is not a bug/error
            return null;
        }
    }

    @Override
    public final String getTargetClassName() {
        return getNameOfThirdPartyTargetClass();
    }

    protected static Object getField(Object object, String fieldName) {
        try {
            Field field = object.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(object);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
