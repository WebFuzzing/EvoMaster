package org.evomaster.client.java.instrumentation.coverage.methodreplacement;

import org.evomaster.client.java.instrumentation.Constants;
import org.evomaster.client.java.instrumentation.InputProperties;
import org.evomaster.client.java.instrumentation.shared.ClassName;
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

        String returnType = Type.getDescriptor(m.getReturnType());
        if(applyThirdPartyCast){
            Replacement r = m.getAnnotation(Replacement.class);
            if(!r.castTo().isEmpty() && !r.replacingConstructor()){
                //Issue with classloaders here, so we compute manually
//                Class<?> casted = loadClass(r.castTo());
//                if(casted != null){
//                    returnType = Type.getDescriptor(casted);
//                }
                returnType = getDescriptorForClassName(r.castTo());
            }
        }

        return getDescriptor(m,skipFirsts,skipLast,returnType,applyThirdPartyCast);
    }

    private static String getDescriptorForClassName(String className){
        return "L"+className.replace(".","/")+";";
    }


    private static String getDescriptor(Method m, int skipFirsts, int skipLast, String returnType, boolean applyThirdPartyCast) {
        //List<Class<?>> types = getParameterTypes(m,skipFirsts,skipLast,applyThirdPartyCast);
        StringBuilder buf = new StringBuilder();

        buf.append('(');

//        types.stream().forEach( t ->
//                buf.append(Type.getDescriptor(t))
//        );

        Class<?>[] parameters = m.getParameterTypes();
        Annotation[][] annotations = m.getParameterAnnotations();

        //skipping first parameter(s)
        int start = skipFirsts;
        int end = parameters.length - skipLast;


        for (int i = start; i < end; i++) {
            Class<?> t = parameters[i];
            ThirdPartyCast tpc = getThirdPartyCast(annotations[i]);
            if(applyThirdPartyCast && tpc != null){
                buf.append(getDescriptorForClassName(tpc.actualType().trim()));
            } else {
                buf.append(Type.getDescriptor(t));
            }
        }

        buf.append(')');
        buf.append(returnType);

        return buf.toString();
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
                ThirdPartyCast tpc = getThirdPartyCast(annotations[i]);
                //WARN this only work for tests
                Class<?> casted = getCastedToThirdParty(ReplacementUtils.class.getClassLoader(), annotations[i]);
                if(casted != null){
                    t = casted;
                } else if(tpc != null){
                    throw new IllegalStateException("BUG: this code should not be called outside tests, as would not work");
                }
            }
            types.add(t);
        }

        return types;
    }


    public static ThirdPartyCast getThirdPartyCast(Annotation[] annotations){
        return (ThirdPartyCast) Arrays.stream(annotations).filter(a -> a instanceof ThirdPartyCast)
                .findFirst().orElse(null);
    }

    public static Class<?> getCastedToThirdParty(ClassLoader loader, Annotation[] annotations) {
        ThirdPartyCast thirdPartyCast = getThirdPartyCast(annotations);
        if(thirdPartyCast != null){
            //we trim to avoid possible issues with Shader plugin
            return loadClass(loader, thirdPartyCast.actualType().trim());
        }
        return null;
    }

    private static Class<?> loadClass(ClassLoader loader, String className){
        try {
            return loader.loadClass(className);
        } catch (ClassNotFoundException e) {
            SimpleLogger.error("Cannot load third-party cast class: " + className,e);
            return null;
        }
    }

    /**
     * Given a method of a replacement class, return actually matching name for the target.
     * At times, there is no 1-to-1 mapping, and might have to use suffixes such as _EM_x.
     *
     * @see ThirdPartyCast
     */
    public static String getPossiblyModifiedName(Method m){

        String replacementName= m.getName();
        if(ThirdPartyCast.NAME_REGEX.matcher(replacementName).matches()){

            if(!ThirdPartyMethodReplacementClass.class.isAssignableFrom(m.getDeclaringClass())){
                throw  new IllegalArgumentException("Modified names are only used for ThirdPartyMethodReplacementClass subclasses");
            }

            replacementName = replacementName.split(ThirdPartyCast.SEPARATOR)[0];
        }
        return replacementName;
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
             * Recall there can be several methods in class with the same name.
             */
            String desc,
            /**
             *  Possible classes that might contain replacement for this target method
             */
            List<MethodReplacementClass> candidateClasses,
            /**
             * Force selection of only pure function replacements (if any available)
             */
            boolean requirePure,
            /**
             * The name of class in which the method call replacement is going to be applied on.
             * There are cases in which we do not want to apply replacements on specific libraries, but
             * just for some specific methods, not all
             */
            String contextClassName
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
                        String ctx = ClassName.get(contextClassName).getFullNameWithDots();
                        if(br.extraPackagesToConsider().length == 0
                                || Arrays.stream(br.packagesToSkip()).noneMatch(it -> ctx.startsWith(it))) {
                            return false;
                        }
                    }
                    if(requirePure && !br.isPure()){
                        return false;
                    }
                    String ctg = br.category().toString();
                    String categories = System.getProperty(InputProperties.REPLACEMENT_CATEGORIES);

                    if(categories == null || !Arrays.stream(categories.split(",")).anyMatch(c -> c.equals(ctg))){
                        return false;
                    }

                    if(br.packagesToSkip().length > 0 && contextClassName != null) {
                        String ctx = ClassName.get(contextClassName).getFullNameWithDots();
                        if (Arrays.stream(br.packagesToSkip()).anyMatch(
                                it -> ctx.startsWith(it) || (it.startsWith(".") && ctx.contains(it)))){
                            return false;
                        }
                    }

                    boolean isConstructor = name.equals(Constants.INIT_METHOD);

                    if(isConstructor){
                        int skipFirst = 0;
                        int skipLast = br.type() == ReplacementType.TRACKER ? 0 : 1;
                        return desc.equals(getDescriptor(m, skipFirst, skipLast, true));

                    } else {

                        //TODO might need something similar for constructors
                        String replacementName = getPossiblyModifiedName(m);

                        if(! replacementName.equals(name)){
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
