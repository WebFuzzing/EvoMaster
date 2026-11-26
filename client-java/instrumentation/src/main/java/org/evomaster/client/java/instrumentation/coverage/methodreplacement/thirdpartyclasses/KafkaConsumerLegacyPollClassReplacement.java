package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyMethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.UsageFilter;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;

/**
 * Tracks the records the SUT receives with KafkaConsumer.poll(long), deprecated since
 * kafka-clients 2.0 and removed in 4.0.
 *
 * In a class of its own because all replacements of a class are resolved together, and a missing
 * method fails all of them: next to poll(Duration), it would break every poll on kafka-clients 4.x.
 */
public class KafkaConsumerLegacyPollClassReplacement extends ThirdPartyMethodReplacementClass {

    private static final KafkaConsumerLegacyPollClassReplacement singleton = new KafkaConsumerLegacyPollClassReplacement();

    @Override
    protected String getNameOfThirdPartyTargetClass() {
        return KafkaReplacementHelper.KAFKA_CONSUMER;
    }

    @Replacement(replacingStatic = false,
            type = ReplacementType.TRACKER,
            id = KafkaReplacementHelper.POLL,
            usageFilter = UsageFilter.ANY,
            category = ReplacementCategory.KAFKA,
            castTo = KafkaReplacementHelper.CONSUMER_RECORDS)
    public static Object poll(Object caller, long timeoutMs) {
        return KafkaReplacementHelper.poll(singleton, caller, KafkaReplacementHelper.waits(timeoutMs), timeoutMs);
    }
}
