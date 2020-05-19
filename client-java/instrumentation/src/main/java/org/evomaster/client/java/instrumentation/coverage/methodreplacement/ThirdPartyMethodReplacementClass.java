package org.evomaster.client.java.instrumentation.coverage.methodreplacement;

import org.evomaster.client.java.instrumentation.shared.ReplacementType;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Third-party libraries might or might not be on the classpath.
 * Furthermore, they MUST NOT be part of EvoMaster.
 * So we have to use reflection to access them at runtime.
 * <br>
 * There is a problem though :(
 * The replaced methods might have inputs or outputs from the third-party library.
 * To write replacements, we need to create the right method signatures, and those must
 * be available at compilation time.
 * A solution here is to include those dependencies, but with "provided" scope (so
 * they will not be included in the uber jar).
 * But we should think if there is any better approach to deal with this issue.
 * Still, we need to have mechanism in place to avoid crashing EM at runtime if
 * such libraries are missing. This should be automatically handled here
 * by checking {@link #isAvailable()}.
 */
public abstract class ThirdPartyMethodReplacementClass implements MethodReplacementClass{

    private Class<?> targetClass;

    private boolean triedToLoad = false;

    /**
     * Key -> id defined in @Replacement
     * Value -> original target method that was replaced
     */
    private final Map<String, Method> methods = new HashMap<>();

    protected ThirdPartyMethodReplacementClass(){

        if(! isAvailable()){
            //nothing to initialize
            return;
        }

        /*
            Use reflection to load all methods that were replaced.
            This is essential to simplify the writing of the replacement, as those
            must still call the original, but only via reflection (as original third-party
            library must not included in EvoMaster)
         */
        Class<? extends ThirdPartyMethodReplacementClass> subclass = this.getClass();

        for (Method m : subclass.getDeclaredMethods()) {

            Replacement r = m.getAnnotation(Replacement.class);
            if (r == null || r.id().isEmpty()) {
                continue;
            }

            Class[] inputs = m.getParameterTypes();

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

            Method targetMethod;
            try {
                targetMethod = targetClass.getMethod(m.getName(), reducedInputs);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException("BUG in EvoMaster: " + e);
            }

            String id = r.id();

            if(methods.containsKey(id)){
                throw new IllegalStateException("Non-unique id: " + id);
            }

            methods.put(id, targetMethod);
        }
    }

    protected abstract String getNameOfThirdPartyTargetClass();

    /**
     *
     * @param singleton a reference to an instance of the subclass. As reflection is expensive,
     *                  we suggest to create it only once, and save it in final static field
     * @param id    of a replacement method
     * @return  original method that was replaced
     */
    public static Method getOriginal(ThirdPartyMethodReplacementClass singleton, String id){
        if(id == null || id.isEmpty()){
            throw new IllegalArgumentException("Invalid empty id");
        }
        Method original = singleton.methods.get(id);
        if(original == null){
            throw new IllegalArgumentException("No method exists with id: " + id);
        }
        return original;
    }

    @Override
    public Class<?> getTargetClass() {

        if(targetClass != null){
            return targetClass;
        }

        /*
            If not present, try to load it via reflection based on the class name.
            But try only once
         */
        if(!triedToLoad){
            triedToLoad = true;

            try{
                targetClass = Class.forName(getTargetClassName());
            }catch (Exception e){
                //this can happen if the third-party library is missing.
                //it is not a bug/error
            }
        }

        return targetClass;
    }

    @Override
    public final String getTargetClassName() {
        return getNameOfThirdPartyTargetClass();
    }
}
