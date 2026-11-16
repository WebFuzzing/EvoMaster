package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.*;
import org.evomaster.client.java.instrumentation.dynamodb.DynamoDbSchemaRegistry;
import org.evomaster.client.java.instrumentation.shared.*;

/** Captures enhanced table converters independently of table contents. */
public class DynamoDbEnhancedClientClassReplacement {
    /** Observes table creation on the sync enhanced client. */
    public static class Sync extends ThirdPartyMethodReplacementClass {
        private static final Sync singleton = new Sync();

        /** {@inheritDoc} */
        @Override
        protected String getNameOfThirdPartyTargetClass() {
            return "software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient";
        }

        /** Creates the original table and records supported converter formats. */
        @Replacement(type = ReplacementType.TRACKER, id = "table", usageFilter = UsageFilter.ANY,
                category = ReplacementCategory.DYNAMODB, castTo = "software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable")
        public static Object table(Object client, String name,
                @ThirdPartyCast(actualType = "software.amazon.awssdk.enhanced.dynamodb.TableSchema") Object schema) {
            try {
                Object table = getOriginal(singleton, "table", client).invoke(client, name, schema);
                DynamoDbSchemaRegistry.register(name, schema);
                return table;
            } catch (java.lang.reflect.InvocationTargetException e) {
                Throwable cause = e.getCause();
                if (cause instanceof RuntimeException) throw (RuntimeException) cause;
                if (cause instanceof Error) throw (Error) cause;
                throw new RuntimeException(cause);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }
    /** Observes table creation on the async enhanced client. */
    public static class Async extends ThirdPartyMethodReplacementClass {
        private static final Async singleton = new Async();

        /** {@inheritDoc} */
        @Override
        protected String getNameOfThirdPartyTargetClass() {
            return "software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient";
        }

        /** Creates the original table and records supported converter formats. */
        @Replacement(type = ReplacementType.TRACKER, id = "table", usageFilter = UsageFilter.ANY,
                category = ReplacementCategory.DYNAMODB, castTo = "software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable")
        public static Object table(Object client, String name,
                @ThirdPartyCast(actualType = "software.amazon.awssdk.enhanced.dynamodb.TableSchema") Object schema) {
            try {
                Object table = getOriginal(singleton, "table", client).invoke(client, name, schema);
                DynamoDbSchemaRegistry.register(name, schema);
                return table;
            } catch (java.lang.reflect.InvocationTargetException e) {
                Throwable cause = e.getCause();
                if (cause instanceof RuntimeException) throw (RuntimeException) cause;
                if (cause instanceof Error) throw (Error) cause;
                throw new RuntimeException(cause);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
