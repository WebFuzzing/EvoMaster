package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyMethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.UsageFilter;
import org.evomaster.client.java.instrumentation.heuristic.Truthness;
import org.evomaster.client.java.instrumentation.heuristic.ValidatorHeuristics;
import org.evomaster.client.java.instrumentation.shared.ObjectiveNaming;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ValidatorClassReplacement extends ThirdPartyMethodReplacementClass {

    private static final ValidatorClassReplacement singleton = new ValidatorClassReplacement();

    @Override
    protected String getNameOfThirdPartyTargetClass() {
        return "javax.validation.Validator";
    }


    @Replacement(replacingStatic = false,
            type = ReplacementType.TRACKER,
            id = "validate",
            usageFilter = UsageFilter.ANY,
            category = ReplacementCategory.EXT_0,
            castTo = "java.util.Set"
    )
    //<T> Set<ConstraintViolation<T>> validate(T object, Class<?>... groups);
    public static  Object validate(Object caller, Object object, Class<?>... groups ) throws Exception {

        if(caller == null){
            throw new NullPointerException();
        }

        Method original = getOriginal(singleton, "validate", caller);

        Object result = null;

        try {
            result = original.invoke(caller, object, groups);
        } catch (IllegalAccessException e){
            throw new RuntimeException(e);// ah, the beauty of Java...
        } catch (InvocationTargetException e){
            throw (Exception) e.getCause();
        }

        if(object != null){
            Class<?> validatorClass = caller.getClass();
            Class<?> objectClass = object.getClass();
            Object beanDescriptor = validatorClass.getMethod("getConstraintsForClass", Class.class)
                    .invoke(caller, objectClass);
            boolean isConstrained = (Boolean) beanDescriptor.getClass().getMethod("isBeanConstrained")
                    .invoke(beanDescriptor);

            if(isConstrained){
                //compute branch distance on the object to validate
                String actionName = ExecutionTracer.getActionName(); //SHOULD NOT BE NULL
                String lastLine = ExecutionTracer.getLastExecutedStatement(); //can be null
                String idTemplate = ObjectiveNaming.METHOD_REPLACEMENT
                        + "__" + objectClass.getName()+"__" + actionName + "__" + lastLine;

                /*
                    bit special... this is a TRACKER method, but internally we handle it
                    as a BOOLEAN one with special idTemplate
                 */
                ReplacementType type = ReplacementType.BOOLEAN;
                Truthness t = ValidatorHeuristics.computeTruthness(caller, object);

                ExecutionTracer.executedReplacedMethod(idTemplate,type, t);
            }
        }

        return result;
    }
}
