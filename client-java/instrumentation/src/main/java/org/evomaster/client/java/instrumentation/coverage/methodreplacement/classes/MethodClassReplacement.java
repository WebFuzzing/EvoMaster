package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.*;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

public class MethodClassReplacement implements MethodReplacementClass {

    /**
     * Reflection is used everywhere, especially in Spring. so, we have
     * to be careful about performance here
     */
    private final static Set<String> toSkipCache = new CopyOnWriteArraySet<>();

    /**
     * key -> unique identifier for method
     * value -> replacement method. can be null
     */
    private final static Map<String, Method> methodCache = Collections.synchronizedMap(new HashMap<>());
    //cannot use ConcurrentHashMap, as it does not accept null values
    //new ConcurrentHashMap<>();

    @Override
    public Class<?> getTargetClass() {
        return Method.class;
    }

    @Replacement(type = ReplacementType.TRACKER,
            category = ReplacementCategory.EXT_0,
            //unfortunately, ANY kills performance (50%-70%!!!), at least on OCVN
            //debugged with profiler, seems like reflection is treated very specially inside the JVM
            //FIXME: but needed otherwise with ONLY_SUT we miss most of SQL handling :( need alternative solution.
            //an example is org.apache.tomcat.jdbc.pool.StatementFacade$StatementProxy
            //trying to exclude packages that are unlikely to be useful to handle
            usageFilter = UsageFilter.ONLY_SUT,
            packagesToSkip = {
                    ".fasterxml.jackson.", //written like this, without "com", to avoid bug in shade plugin
                    "org.springframework.",
                    "ch.qos.logback.",
                    "org.apache.tomcat.util.",
                    "org.jboss.logging",
                    "net.sf.ehcache.config"
            },
            extraPackagesToConsider = {"org.apache.tomcat.jdbc."}
    )
    public static Object invoke(Method caller, Object obj, Object... args) throws InvocationTargetException, IllegalAccessException {

        /*
            This is tricky... what if reflection is done on a method for which we have a replacement?
            This actually happens for example in
            org.apache.tomcat.jdbc.pool.StatementFacade$StatementProxy
         */
        if (caller == null) {
            //sure NPE
            return caller.invoke(obj, args);
        }

        /*
            unfortunately, invoke() is  @CallerSensitive :(
            so, we cannot guarantee to preserve the semantic, and must set it
            accessible
            TODO in theory it should not have any impact whatsoever, but you never know...
            actually, it can! eg, if caller would fail in its context...
            FIXME to do it right, should manually check contextClassName, and put it accessible to
            true only temporarily for this internal call.
            could check: sun.reflect.Reflection.getCallerClass();
         */
        caller.setAccessible(true);

        String targetClassName = caller.getDeclaringClass().getName();

        if (toSkipCache.contains(targetClassName)) {
            return caller.invoke(obj, args);
        }

        //due to performance reasons, here we do strict check
        List<MethodReplacementClass> candidateClasses = ReplacementList.getReplacements(targetClassName, true);
        if (candidateClasses.isEmpty()) {
            toSkipCache.add(targetClassName);
            return caller.invoke(obj, args);
        }

        /*
            Problem here is that isInSut is not available... we would need explicitly pass down as input
            to the function (and we would need to push such boolean on the stack)...
            plus there is the issue with the handling of new target templates...
            But, even if we do not want to deal with them, we still want to do taint analysis...

            TODO fix these issues, but likely they are very, very rare
         */

        String name = caller.getName();
        String desc = ReplacementUtils.getDescriptor(caller, 0, 0);
        String id = targetClassName + "." + name + desc;

        Method replacement = null;

        if (methodCache.containsKey(id)) {
            replacement = methodCache.get(id);
        } else {
            boolean isInSUT = false; //FIXME
            String contextClassName = null; //FIXME same as above

            Optional<Method> r = ReplacementUtils.chooseMethodFromCandidateReplacement(
                    isInSUT, name, desc, candidateClasses, false, contextClassName);
            replacement = r.orElse(null);
            methodCache.put(id, replacement);
        }

        if (replacement == null) {
            return caller.invoke(obj, args);
        }

        Replacement br = replacement.getAnnotation(Replacement.class);

        List<Object> tmp = new LinkedList<>();
        if (args != null) {
            tmp.addAll(Arrays.asList(args));
        }
        if (!br.replacingStatic()) {
            tmp.add(0, obj);
        }
        if (br.type() != ReplacementType.TRACKER) {
            tmp.add(null); // null template at the end
        }

        return replacement.invoke(null, tmp.toArray());

        /*
         //SEE discussion above on why this code cannot be used :(

        if(! br.isPure()){
            // we can call it only once... and so cannot use the original :(
            // in theory, this break semantics if there were some access control
            return replacement.invoke(null, tmp.toArray());
        }

        try {
            replacement.invoke(null, tmp.toArray());
        } catch (Exception e){
            SimpleLogger.warn("Failed reflection call in replacement", e);
        }

        // still going to call the original, needed to deal with not breaking semantics (eg access rules on
        // reflection should still be applied)
        return caller.invoke(obj, args);
         */
    }
}
