package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyCast;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyMethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.UsageFilter;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;

import java.util.concurrent.Future;

/**
 * Tracks the records the SUT emits with Producer.send, the interface Spring Kafka's KafkaTemplate
 * sends through. A record that goes through a producer wrapping another one, as Spring's
 * CloseSafeProducer does, is recorded once, see {@link KafkaReplacementHelper}.
 */
public class KafkaProducerInterfaceClassReplacement extends ThirdPartyMethodReplacementClass {

    private static final KafkaProducerInterfaceClassReplacement singleton = new KafkaProducerInterfaceClassReplacement();

    @Override
    protected String getNameOfThirdPartyTargetClass() {
        return KafkaReplacementHelper.PRODUCER;
    }

    @Replacement(replacingStatic = false,
            type = ReplacementType.TRACKER,
            id = KafkaReplacementHelper.SEND,
            usageFilter = UsageFilter.ANY,
            category = ReplacementCategory.KAFKA)
    public static Future<?> send(Object caller,
                                 @ThirdPartyCast(actualType = KafkaReplacementHelper.PRODUCER_RECORD) Object record) {
        return KafkaReplacementHelper.send(singleton, KafkaReplacementHelper.SEND, caller, record);
    }

    @Replacement(replacingStatic = false,
            type = ReplacementType.TRACKER,
            id = KafkaReplacementHelper.SEND_WITH_CALLBACK,
            usageFilter = UsageFilter.ANY,
            category = ReplacementCategory.KAFKA)
    public static Future<?> send(Object caller,
                                 @ThirdPartyCast(actualType = KafkaReplacementHelper.PRODUCER_RECORD) Object record,
                                 @ThirdPartyCast(actualType = KafkaReplacementHelper.CALLBACK) Object callback) {
        return KafkaReplacementHelper.send(singleton, KafkaReplacementHelper.SEND_WITH_CALLBACK, caller, record, callback);
    }
}
