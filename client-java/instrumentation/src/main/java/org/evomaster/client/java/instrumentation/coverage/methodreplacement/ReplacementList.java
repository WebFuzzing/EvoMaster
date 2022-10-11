package org.evomaster.client.java.instrumentation.coverage.methodreplacement;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes.*;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses.*;
import org.evomaster.client.java.instrumentation.shared.ClassName;

import java.util.*;
import java.util.stream.Collectors;

public class ReplacementList {

    private static List<MethodReplacementClass> listCache;

    /*
        FIXME why was this a list of instances and not a list of Class<? extends MethodReplacementClass> ???
     */

    /**
     * Return all the available method replacement classes.
     * Every time a new replacement class is implemented, it needs to
     * be manually added to this list.
     */
    public static List<MethodReplacementClass> getList() {

        if(listCache == null) {
            listCache = Arrays.asList(
                    new AbstractEndpointClassReplacement(),
                    new BooleanClassReplacement(),
                    new ByteClassReplacement(),
                    new CharacterClassReplacement(),
                    new CollectionClassReplacement(),
                    new DateClassReplacement(),
                    new DateFormatClassReplacement(),
                    new DoubleClassReplacement(),
                    new EnumClassReplacement(),
                    new FloatClassReplacement(),
                    new GsonClassReplacement(),
                    new Http11ProcessorReplacementClass(),
                    new HttpServletRequestClassReplacement(),
                    new IntegerClassReplacement(),
                    new JacksonObjectMapperClassReplacement(),
                    new LocalDateClassReplacement(),
                    new LocalDateTimeClassReplacement(),
                    new LocalTimeClassReplacement(),
                    new LongClassReplacement(),
                    new MapClassReplacement(),
                    new MatcherClassReplacement(),
                    new MethodClassReplacement(),
                    new ObjectClassReplacement(),
                    new ObjectsClassReplacement(),
                    new PatternClassReplacement(),
                    new PreparedStatementClassReplacement(),
                    new StatementClassReplacement(),
                    new StringClassReplacement(),
                    new ShortClassReplacement(),
                    new ServletRequestClassReplacement(),
                    new SocketClassReplacement(),
                    new WebRequestClassReplacement(),
                    new URIClassReplacement(),
                    new URLClassReplacement(),
                    new UUIDClassReplacement()
            );
        }

        return listCache;
    }


    public static List<MethodReplacementClass> getReplacements(String target) {
        return getReplacements(target, false);
    }


    public static List<MethodReplacementClass> getReplacements(String target, boolean strict) {
        Objects.requireNonNull(target);
        final String targetClassName = ClassName.get(target).getFullNameWithDots();

        List<MethodReplacementClass> list = getList().stream()
                //.filter(t -> t.isAvailable()) // bad idea to load 3rd classes at this point...
                .filter(t -> {

                    if(strict){
                        //exact match, no subclasses allowed
                        return t.getTargetClassName().equals(targetClassName);
                    }

                    /*
                        TODO: this is tricky, due to how "super" calls are
                        handled. For now, we just allow subclasses if they
                        are of standard JDK.
                        Furthermore, issues with classloading of non-JDK APIs

                        This gives major issues if class loads other non-JDK classes.
                        This for example happens with SQL stuff possibly loading drivers, eg H2.
                        So we cannot load JDK libraries indiscriminately here.
                    */

//                            boolean jdk = targetClassName.startsWith("java.");
                            //TODO based on actual packages used in the list
                            Set<String> prefixes = new HashSet<>();
                            prefixes.add("java.lang.");
                            prefixes.add("java.util.");
                            prefixes.add("java.time.");

                            prefixes.add("java.net.");

                            //we don't just use java.sql. as that seems to give issue (see previous comments)
                            prefixes.add("java.sql.Statement");
                            prefixes.add("java.sql.CallableStatement");
                            prefixes.add("java.sql.PreparedStatement");

                            boolean jdk = prefixes.stream().anyMatch(k -> targetClassName.startsWith(k)) &&
                                        prefixes.stream().anyMatch(k -> t.getTargetClassName().startsWith(k));

                            if (jdk) {
                                Class<?> klass;
                                try {
                                    klass = Class.forName(targetClassName);
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                                return t.getTargetClass().isAssignableFrom(klass);
                            }

                            return t.getTargetClassName().equals(targetClassName);
                        }
                )
                .collect(Collectors.toList());

        assert !strict || (list.size() <= 1); //if strict, at most 1 class can match

        return list;
    }
}
