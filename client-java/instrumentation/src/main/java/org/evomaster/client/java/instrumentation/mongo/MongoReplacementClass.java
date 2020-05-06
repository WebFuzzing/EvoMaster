package org.evomaster.client.java.instrumentation.mongo;

import com.mongodb.client.ClientSession;
import com.mongodb.client.FindIterable;
import org.bson.conversions.Bson;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyMethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.UsageFilter;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;

public class MongoReplacementClass extends ThirdPartyMethodReplacementClass {

    private static final MongoReplacementClass singleton = new MongoReplacementClass();

    private static boolean invokeHasNext(Object findIterable) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Method iteratorMethod = findIterable.getClass().getMethod("iterator");
        Iterator iterator = (Iterator) iteratorMethod.invoke(findIterable);
        return iterator.hasNext();
    }

    @Override
    protected String getNameOfThirdPartyTargetClass() {
        return "com.mongodb.client.MongoCollection";
    }

    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = "find", usageFilter = UsageFilter.ANY)
    public static FindIterable<?> find(Object mongoCollection, Bson bson, Class documentClass) {
        try {
            Method findMethod = getOriginal(singleton, "find");
            Object findIterable = findMethod.invoke(mongoCollection, bson, documentClass);

            boolean operationReturnedDocuments = invokeHasNext(findIterable);
            MongoLogger.getInstance().logFind(mongoCollection, bson, operationReturnedDocuments);
            return (FindIterable<?>) findIterable;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }


    @Replacement(replacingStatic = false, type = ReplacementType.TRACKER, id = "findWithClientSession", usageFilter = UsageFilter.ANY)
    public static FindIterable<?> find(Object mongoCollection, ClientSession clientSession, Bson bson, Class documentClass) {

        try {
            Method findMethod = mongoCollection.getClass().getMethod("find", ClientSession.class, Bson.class, Class.class);
            Object findIterableInstance = findMethod.invoke(mongoCollection, clientSession, bson, documentClass);
            boolean operationReturnedDocuments = invokeHasNext(findIterableInstance);
            MongoLogger.getInstance().logFind(mongoCollection, bson, operationReturnedDocuments);
            return (FindIterable<?>) findIterableInstance;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
