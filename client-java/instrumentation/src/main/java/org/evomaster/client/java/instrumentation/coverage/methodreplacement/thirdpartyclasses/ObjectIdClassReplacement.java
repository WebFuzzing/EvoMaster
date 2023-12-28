package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyMethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.UsageFilter;
import org.evomaster.client.java.instrumentation.shared.*;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class ObjectIdClassReplacement extends ThirdPartyMethodReplacementClass {

    private static final ThreadLocal<Object> instance = new ThreadLocal<>();

    private static final ObjectIdClassReplacement singleton = new ObjectIdClassReplacement();

    private static final String OBJECT_ID_REGEX = "^[0-9a-fA-F]{24}$";

    @Override
    protected String getNameOfThirdPartyTargetClass() {
        return "org.bson.types.ObjectId";
    }

    @Replacement(replacingStatic = false,
            type = ReplacementType.TRACKER,
            id = "objectid_constructor",
            usageFilter = UsageFilter.ANY,
            category = ReplacementCategory.MONGO,
            replacingConstructor = true,
            castTo = "org.bson.types.ObjectId"
    )
    public static void ObjectId(final String hexString) throws Exception {

        if (ExecutionTracer.isTaintInput(hexString)) {
            ExecutionTracer.addStringSpecialization(hexString,
                    new StringSpecializationInfo(StringSpecialization.REGEX_WHOLE, OBJECT_ID_REGEX));
        }

        Constructor<?> originalConstructor = getOriginalConstructor(singleton, "objectid_constructor");

        try {
            Object client = originalConstructor.newInstance(hexString);
            addInstance(client);
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw (RuntimeException) e.getCause();
        }
    }

    /**
     * This method must be implemented to be called by the
     * method replacement infraestructure whenever a
     * constructor is replaced.
     *
     * @return the freshly created instance
     */
    public static Object consumeInstance() {
        Object client = instance.get();
        if (client == null) {
            throw new IllegalStateException("No instance to consume");
        }
        instance.set(null);
        return client;
    }

    private static void addInstance(Object x) {
        Object client = instance.get();
        if (client != null) {
            throw new IllegalStateException("Previous instance was not consumed");
        }
        instance.set(x);
    }


}
