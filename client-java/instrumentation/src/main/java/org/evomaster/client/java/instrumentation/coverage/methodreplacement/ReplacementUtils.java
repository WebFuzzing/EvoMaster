package org.evomaster.client.java.instrumentation.coverage.methodreplacement;

import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.objectweb.asm.Type;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class ReplacementUtils {

    public static String getDescriptor(Method m, int skipFirsts, int skipLast) {
        Class<?>[] parameters = m.getParameterTypes();
        StringBuilder buf = new StringBuilder();
        buf.append('(');

        //skipping first parameter(s)
        int start = skipFirsts;
        int end = parameters.length - skipLast;

        /*
            we might skip the first (if replacing non-static), and
            skipping the last (id template)
         */
        for (int i = start; i < end; ++i) {
            buf.append(Type.getDescriptor(parameters[i]));
        }
        buf.append(')');
        buf.append(Type.getDescriptor(m.getReturnType()));

        return buf.toString();
    }

    public static Optional<Method> chooseMethodFromCandidateReplacement(
            boolean isInSUT, String name, String desc, List<MethodReplacementClass> candidateClasses, boolean requirePure) {
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
                    catch (Throwable t){return false;}
                })
                .flatMap(i -> Stream.of(i.getClass().getDeclaredMethods()))
                .filter(m -> m.getDeclaredAnnotation(Replacement.class) != null)
                .filter(m -> m.getName().equals(name))
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

                    int skipFirst = br.replacingStatic() ? 0 : 1;
                    int skipLast = br.type() == ReplacementType.TRACKER ? 0 : 1;
                    return desc.equals(getDescriptor(m, skipFirst, skipLast));
                })
                .findAny();
        return r;
    }
}
