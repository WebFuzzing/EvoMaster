package org.evomaster.client.java.instrumentation.coverage.methodreplacement;

import org.evomaster.client.java.instrumentation.Constants;
import org.evomaster.client.java.instrumentation.InputProperties;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.utils.SimpleLogger;
import org.objectweb.asm.Type;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class ReplacementUtils {

    public static String getDescriptor(Method m, int skipFirsts, int skipLast) {
        return getDescriptor(m,skipFirsts,skipLast,Type.getDescriptor(m.getReturnType()),false);
    }

    public static String getDescriptor(Method m, int skipFirsts, int skipLast, boolean applyThirdPartyCast) {
        return getDescriptor(m,skipFirsts,skipLast,Type.getDescriptor(m.getReturnType()),applyThirdPartyCast);
    }

    public static List<Class<?>> getParameterTypes(Method m, int skipFirsts, int skipLast, boolean applyThirdPartyCast){
        Class<?>[] parameters = m.getParameterTypes();
        Annotation[][] annotations = m.getParameterAnnotations();

        //skipping first parameter(s)
        int start = skipFirsts;
        int end = parameters.length - skipLast;

        List<Class<?>> types = new ArrayList<>((end-start)+1);

        for (int i = start; i < end; i++) {
            Class<?> t = parameters[i];
            if(applyThirdPartyCast){
                Class<?> casted = getCastedToThirdParty(annotations[i]);
                if(casted != null){
                    t = casted;
                }
            }
            types.add(t);
        }

        return types;
    }

    public static Class<?> getCastedToThirdParty(Annotation[] annotations) {
        ThirdPartyCast thirdPartyCast = (ThirdPartyCast) Arrays.stream(annotations).filter(a -> a instanceof ThirdPartyCast)
                .findFirst().orElse(null);
        if(thirdPartyCast != null){
            try {
                return ReplacementUtils.class.getClassLoader().loadClass(thirdPartyCast.actualType());
            } catch (ClassNotFoundException e) {
                SimpleLogger.error("Cannot load third-party cast class: " + thirdPartyCast.actualType(),e);
            }
        }
        return null;
    }

    public static String getDescriptor(Method m, int skipFirsts, int skipLast, String returnType, boolean applyThirdPartyCast) {
        List<Class<?>> types = getParameterTypes(m,skipFirsts,skipLast,applyThirdPartyCast);
        StringBuilder buf = new StringBuilder();

        buf.append('(');
        types.stream().forEach( t ->
                buf.append(Type.getDescriptor(t))
        );
        buf.append(')');
        buf.append(returnType);

        return buf.toString();
    }

    public static Optional<Method> chooseMethodFromCandidateReplacement(
            /**
             * Whether the replacement is applied on SUT code (eg instead of third-party libraries)
             */
            boolean isInSUT,
            /**
             * The name of the method
             */
            String name,
            /**
             * The bytecode descriptor of the inputs/output.
             * Recall the can be several methods in class with the same name.
             */
            String desc,
            /**
             *  Possible classes that might contain replacement for this target method
             */
            List<MethodReplacementClass> candidateClasses,
            /**
             * Force selection of only pure function replacements (if any available)
             */
            boolean requirePure
    ) {
        Optional<Method> r = candidateClasses.stream()
                .filter(i -> {
                    /*
                        This is tricky. 3rd party replacements might have references to
                        classes that are not on the classpath (eg, as return, or as input),
                        and so reflection on methods will crash.
                        The weird thing is that, if that was the case, then the class should
                        not had been in the candidate list in the first place...
                        but this issue does happen in Proxyprint, but only for external driver,
                        due to custom CL in Spring.

                        TODO: should try to understand exactly what happens there...
                     */
                    try{i.getClass().getDeclaredMethods(); return true;}
                    catch (Throwable t){
                        String msg = "FAILED TO LOAD METHOD DECLARATIONS FOR: " + i.getClass().getName();
                        SimpleLogger.error(msg);
                        return false;
                    }
                })
                .flatMap(i -> Stream.of(i.getClass().getDeclaredMethods()))
                .filter(m -> m.getDeclaredAnnotation(Replacement.class) != null)
                .filter(m -> {
                    Replacement br = m.getAnnotation(Replacement.class);
                    if(isInSUT && br.usageFilter() == UsageFilter.ONLY_THIRD_PARTY){
                        return false;
                    }
                    if(!isInSUT && br.usageFilter() == UsageFilter.ONLY_SUT){
                        return false;
                    }
                    if(requirePure && !br.isPure()){
                        return false;
                    }
                    String ctg = br.category().toString();
                    String categories = System.getProperty(InputProperties.REPLACEMENT_CATEGORIES);

                    if(categories == null || !Arrays.stream(categories.split(",")).anyMatch(c -> c.equals(ctg))){
                        return false;
                    }

                    boolean isConstructor = name.equals(Constants.INIT_METHOD);

                    if(isConstructor){
                        int skipFirst = 0;
                        int skipLast = br.type() == ReplacementType.TRACKER ? 0 : 1;
                        return desc.equals(getDescriptor(m, skipFirst, skipLast, true));

                    } else {

                        if(! m.getName().equals(name)){
                            return false;
                        }

                        int skipFirst = br.replacingStatic() ? 0 : 1;
                        int skipLast = br.type() == ReplacementType.TRACKER ? 0 : 1;
                        return desc.equals(getDescriptor(m, skipFirst, skipLast, true));
                    }
                })
                .findAny();
        return r;
    }
}
