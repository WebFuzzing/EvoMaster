package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyMethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.UsageFilter;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;

/**
 * Tracks the records the SUT receives with Consumer.poll(long). In a class of its own for the same
 * reason as {@link KafkaConsumerLegacyPollClassReplacement}.
 */
public class KafkaConsumerInterfaceLegacyPollClassReplacement extends ThirdPartyMethodReplacementClass {

    private static final KafkaConsumerInterfaceLegacyPollClassReplacement singleton = new KafkaConsumerInterfaceLegacyPollClassReplacement();

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
    public static Object poll(Object caller, long timeoutMs) {
        return KafkaReplacementHelper.poll(singleton, caller, KafkaReplacementHelper.waits(timeoutMs), timeoutMs);
    }
}
