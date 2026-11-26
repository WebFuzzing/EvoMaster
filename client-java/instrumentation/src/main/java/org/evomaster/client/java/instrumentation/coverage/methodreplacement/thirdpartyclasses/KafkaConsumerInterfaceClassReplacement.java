package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyMethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.UsageFilter;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;

import java.time.Duration;

/**
 * Tracks the records the SUT receives with Consumer.poll(Duration), the interface Spring Kafka's
 * listener containers poll through. See {@link KafkaConsumerClassReplacement} for why there is a
 * replacement for the interface and one for the class.
 */
public class KafkaConsumerInterfaceClassReplacement extends ThirdPartyMethodReplacementClass {

    private static final KafkaConsumerInterfaceClassReplacement singleton = new KafkaConsumerInterfaceClassReplacement();

    @Override
    protected String getNameOfThirdPartyTargetClass() {
        return KafkaReplacementHelper.CONSUMER;
    }

    @Replacement(replacingStatic = false,
            type = ReplacementType.TRACKER,
            id = KafkaReplacementHelper.POLL,
            usageFilter = UsageFilter.ANY,
            category = ReplacementCategory.KAFKA,
            castTo = KafkaReplacementHelper.CONSUMER_RECORDS)
    public static Object poll(Object caller, Duration timeout) {
        return KafkaReplacementHelper.poll(singleton, caller, KafkaReplacementHelper.waits(timeout), timeout);
    }
}
