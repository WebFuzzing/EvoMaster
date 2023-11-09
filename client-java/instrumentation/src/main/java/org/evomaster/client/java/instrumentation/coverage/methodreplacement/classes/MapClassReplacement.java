package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.distance.heuristics.DistanceHelper;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.*;
import org.evomaster.client.java.distance.heuristics.Truthness;
import org.evomaster.client.java.distance.heuristics.TruthnessUtils;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MapClassReplacement implements MethodReplacementClass {

    private static final Method linearCostContainsKey;

    private static final Map<Class<? extends Map>, Boolean> cacheIsLinearCost = new ConcurrentHashMap<>();

    static {
        try {
            linearCostContainsKey = AbstractMap.class.getMethod("containsKey", Object.class);
        } catch (NoSuchMethodException e) {
            //should never happen...
            throw new RuntimeException(e);
        }
    }

    /*
        Note, if for any reason in a method X we need to call another method Y (eg for isEmpty() we call size()) to
        compute any heuristics, then we MUST do so in a try/catch...
        There are abominations like

        org.thymeleaf.spring6.expression.SPELContextMapWrapper

        where implements some methods, and throw exception on all others...
     */


    private static boolean hasLinearCost(Class<? extends Map> klass){
        Boolean isLinear = cacheIsLinearCost.get(klass);
        if(isLinear == null){
            try {
                Method m = klass.getMethod("containsKey",Object.class);
                isLinear = m.equals(linearCostContainsKey);
            } catch (NoSuchMethodException e) {
                isLinear = false;
            }
            cacheIsLinearCost.put(klass, isLinear);
            return isLinear;
        }
        return isLinear;
    }

    @Override
    public Class<?> getTargetClass() {
        return Map.class;
    }

    /*
        This is same code as in Collection.
        However, Map does not extend Collection...
        so, in theory, could have a Map that is not a Collection...
     */
    @Replacement(type = ReplacementType.BOOLEAN, category = ReplacementCategory.EXT_0)
    public static boolean isEmpty(Map caller, String idTemplate) {
        Objects.requireNonNull(caller);

        boolean result = caller.isEmpty();
        if (idTemplate == null) {
            return result;
        }

        int len;
        try {
            len = caller.size();
        } catch (Exception e){
            return result;
        }
        Truthness t = TruthnessUtils.getTruthnessToEmpty(len);

        ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.BOOLEAN, t);
        return result;
    }


    @Replacement(type = ReplacementType.BOOLEAN, category = ReplacementCategory.BASE)
    public static boolean containsKey(Map c, Object o, String idTemplate) {
        Objects.requireNonNull(c);

        if (c instanceof IdentityHashMap) {
            /*
                IdentityHashMap does not use .equals() for the comparisons
             */
            return c.containsKey(o);
        }

        /*
            keySet() returns a set instance that indirectly calls
            to containsKey() when doing a contains().
            In order to avoid a stack overflow in classes sub-classing JDK
            collections, we compute a fresh collection.
            An example is StringHashMap from RestAssured.

            The problem though is this can be come quickly very, very
            expensive...

            NOTE: due to changes in ReplacementList, this does not seem a problem
            anymore, due to how we handle subclasses. See comments in that class.
         */
        //Collection keyCollection = new HashSet(c.keySet());
        Collection keyCollection;
        try {
            keyCollection = c.keySet();
        } catch (Exception e){
            return c.containsKey(o);
        }

        CollectionsDistanceUtils.evaluateTaint(keyCollection, o);

        /*
            Even if we skip instrumenting subclass calls, we still have issues.
            For example, in Guava Maps, contains() is implemented as
            map().containsKey()
            which still leads to infinite recursion, due to map() returning
            a reference to Map, and also due to the keyset be a non-JDK subclass as well.
            So, we still need to look at the actual concrete class, and not just the ref.

            Skipping it is not so great, but is there any alternative?
            However, if still can evaluate taint, then should not be a big problem
         */
        if(!keyCollection.getClass().getName().startsWith("java.util")){
            return c.containsKey(o);
        }

        boolean result;
        try {
            result = keyCollection.contains(o);
        } catch (Exception e){
            return c.containsKey(o);
        }

        if (idTemplate == null) {
            return result;
        }

        Truthness t;
        if (result) {
            t = new Truthness(1d, DistanceHelper.H_NOT_NULL);
        } else {
            double h = CollectionsDistanceUtils.getHeuristicToContains(keyCollection, o);
            t = new Truthness(h, 1d);
        }
        ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.BOOLEAN, t);
        return result;
    }

    @Replacement(type = ReplacementType.OBJECT, category = ReplacementCategory.EXT_0)
    public static Object get(Map map, Object key, String idTemplate){
        Objects.requireNonNull(map);

        if(! (map instanceof IdentityHashMap)) {
            try {
                //check Map.containsKey is not from AbstractMap, which is O(n). Case for Kotlin's ZipEntryMap
                if(! hasLinearCost(map.getClass())) {
                    containsKey(map, key, idTemplate);
                }
            } catch (Exception e) {
                //do nothing
                //this does actually happen in Kotlin for ConcurrentRefValueHashMap throwing a IncorrectOperationException
            }
        }
        return map.get(key);
    }

    @Replacement(type = ReplacementType.OBJECT, category = ReplacementCategory.EXT_0)
    public static Object getOrDefault(Map map, Object key, Object defaultValue, String idTemplate) {
        get(map,key,idTemplate);//compute taint + heuristics
        return map.getOrDefault(key,defaultValue);
    }

    @Replacement(type = ReplacementType.BOOLEAN, category = ReplacementCategory.EXT_0)
    public static boolean containsValue(Map c, Object o, String idTemplate) {
        Objects.requireNonNull(c);

        if (idTemplate == null || c instanceof IdentityHashMap) {
            /*
                IdentityHashMap does not use .equals() for the comparisons
             */
            return c.containsValue(o);
        }

        Collection data;
        try {
            data = c.values();
        }catch (Exception e){
            return c.containsValue(o);
        }

        CollectionsDistanceUtils.evaluateTaint(data, o);

        boolean result;
        try {
            result = data.contains(o);
        } catch (Exception e){
            return c.containsValue(o);
        }

        if (idTemplate == null) {
            return result;
        }

        Truthness t;
        if (result) {
            t = new Truthness(1d, DistanceHelper.H_NOT_NULL);
        } else {
            double h = CollectionsDistanceUtils.getHeuristicToContains(data, o);
            t = new Truthness(h, 1d);
        }
        ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.BOOLEAN, t);
        return result;
    }



    @Replacement(type = ReplacementType.BOOLEAN, category = ReplacementCategory.EXT_0, isPure = false)
    public static boolean remove(Map map, Object key, Object value, String idTemplate) {
        Objects.requireNonNull(map);

        /*
        Object curValue = get(key);
        if (!Objects.equals(curValue, value) ||
            (curValue == null && !containsKey(key))) {
            return false;
        }
        remove(key);
        return true;
         */

        try{
            map.keySet();
        }catch (Exception e){
            return map.remove(key, value);
        }

        CollectionsDistanceUtils.evaluateTaint(map.keySet(), key);
        Object curValue;
        try {
                curValue =map.get(key);
        } catch (Exception e){
            return map.remove(key, value);
        }
        if(curValue != null) {
            CollectionsDistanceUtils.evaluateTaint(Arrays.asList(curValue), value);
        }

        boolean result = map.remove(key, value);

        if (idTemplate == null) {
            return result;
        }

        Truthness t;
        if (result) {
            t = new Truthness(1d, DistanceHelper.H_NOT_NULL);
        } else {
            double hb = CollectionsDistanceUtils.getHeuristicToContains(map.keySet(), key) / 2d;
            double dv = DistanceHelper.getDistance(value, curValue);
            double hv = DistanceHelper.heuristicFromScaledDistanceWithBase(DistanceHelper.H_NOT_NULL, dv) / 2d;
            double h = hb + hv;
            assert h >= DistanceHelper.H_NOT_NULL && h <= 1;
            t = new Truthness(h, 1d);
        }
        ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.BOOLEAN, t);
        return result;
    }


    @Replacement(type = ReplacementType.BOOLEAN, category = ReplacementCategory.EXT_0, isPure = false)
    public static boolean replace(Map map, Object key, Object oldValue, Object newValue, String idTemplate) {
        Objects.requireNonNull(map);

        /*
         Object curValue = get(key);
        if (!Objects.equals(curValue, oldValue) ||
                (curValue == null && !containsKey(key))) {
            return false;
        }
        put(key, newValue);
        return true;
         */

        try {
            boolean removed = remove(map, key, oldValue, idTemplate);
            if (removed) {
                map.put(key, newValue);
            }
            return removed;
        } catch (Exception e){
            return map.replace(key,oldValue, newValue);
        }
    }

}
