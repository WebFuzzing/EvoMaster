package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyMethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.UsageFilter;
import org.evomaster.client.java.distance.heuristics.Truthness;
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
    public static  Object validate(Object caller, Object object, Class<?>... groups ) throws Throwable {

        if(caller == null){
            throw new NullPointerException();
        }

        Method original = getOriginal(singleton, "validate", caller);


        /*
            Looking at last line is too problematic.
            If this is done on @Valid check of REST controller, then no code SUT is executed yet, apart
            from the init and setters of the DTOs.
            This messes up everything.
            Looking at Thread.currentThread().getStackTrace() does not help much either, as still would be
            difficult to distinguish when @Valid is on method calls inside the SUTs...
            so, might be best to simply ignore this
         */
        String lastLine = "";//ExecutionTracer.getLastExecutedStatement(); //can be null

        Object result;
        try {
            result = original.invoke(caller, object, groups);
        } catch (IllegalAccessException e){
            throw new RuntimeException(e);// ah, the beauty of Java...
        } catch (InvocationTargetException e){
            throw e.getCause(); // this could be a java.lang.AssertionError
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
                String idTemplate = ObjectiveNaming.METHOD_REPLACEMENT
                        + "__" + "VALIDATE"
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
