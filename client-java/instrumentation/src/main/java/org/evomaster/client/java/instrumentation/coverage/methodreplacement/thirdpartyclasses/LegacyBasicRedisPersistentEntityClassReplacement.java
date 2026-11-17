package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyCast;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;

import java.util.Arrays;

/**
 * The replacement of the constructor of {@code BasicRedisPersistentEntity} for the older versions of Spring
 * Data (2.x and 3.x), in which its {@code TypeInformation} is in {@code org.springframework.data.util}.
 * For the latest versions, see {@link BasicRedisPersistentEntityClassReplacement}.
 *
 * @see AbstractBasicRedisPersistentEntityClassReplacement
 */
public class LegacyBasicRedisPersistentEntityClassReplacement extends AbstractBasicRedisPersistentEntityClassReplacement {

    private static final String TYPE_INFORMATION_CLASS_NAME = "org.springframework.data.util.TypeInformation";

    @Override
    protected String getNameOfTypeInformationClass() {
        return TYPE_INFORMATION_CLASS_NAME;
    }

    public static Object consumeInstance() {
        return takeInstance();
    }

    @Replacement(
            replacingConstructor = true,
            type = ReplacementType.TRACKER,
            category = ReplacementCategory.REDIS,
            id = "constructorEntityLegacy",
            castTo = ENTITY_CLASS_NAME
    )
    public static void BasicRedisPersistentEntity(
            @ThirdPartyCast(actualType = TYPE_INFORMATION_CLASS_NAME) Object information,
            @ThirdPartyCast(actualType = KEY_SPACE_RESOLVER_CLASS_NAME) Object fallbackKeySpaceResolver,
            @ThirdPartyCast(actualType = TIME_TO_LIVE_ACCESSOR_CLASS_NAME) Object timeToLiveAccessor) {
        handleBasicRedisPersistentEntityConstructor(
                Arrays.asList(information, fallbackKeySpaceResolver, timeToLiveAccessor), TYPE_INFORMATION_CLASS_NAME);
    }
}
