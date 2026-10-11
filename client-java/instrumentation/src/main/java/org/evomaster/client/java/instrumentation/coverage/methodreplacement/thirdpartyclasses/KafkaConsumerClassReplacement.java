package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyMethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.UsageFilter;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;

import java.time.Duration;

/**
 * Tracks the records the SUT receives with KafkaConsumer.poll(Duration), see {@link KafkaReplacementHelper}.
 *
 * Replacements match the class a call is compiled against, so this one applies where the consumer is
 * declared as a KafkaConsumer, and {@link KafkaConsumerInterfaceClassReplacement} where it is declared as a
 * Consumer, as in Spring Kafka.
 */
public class KafkaConsumerClassReplacement extends ThirdPartyMethodReplacementClass {

    private static final KafkaConsumerClassReplacement singleton = new KafkaConsumerClassReplacement();

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
    public static Object poll(Object caller, Duration timeout) {
        return KafkaReplacementHelper.poll(singleton, caller, KafkaReplacementHelper.waits(timeout), timeout);
    }
}
