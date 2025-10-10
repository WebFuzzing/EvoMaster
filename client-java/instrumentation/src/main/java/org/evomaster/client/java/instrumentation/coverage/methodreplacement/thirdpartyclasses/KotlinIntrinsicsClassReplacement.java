package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyMethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes.ObjectsClassReplacement;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class KotlinIntrinsicsClassReplacement extends ThirdPartyMethodReplacementClass {

    //private static final  KotlinIntrinsicsClassReplacement singleton = new KotlinIntrinsicsClassReplacement();

    @Override
    protected String getNameOfThirdPartyTargetClass() {
        return "kotlin.jvm.internal.Intrinsics";
    }

    /*
        In theory, this should not be necessary, as internally it uses Object.equals.
        but possible issue in E2E if Kotlin classes are loaded before instrumentation
     */

    @Replacement(
            type = ReplacementType.BOOLEAN,
            replacingStatic = true,
            category = ReplacementCategory.BASE,
            id = "areEqual_object"
    )
    public static boolean areEqual(Object first, Object second, String idTemplate) {

      return ObjectsClassReplacement.equals(first, second, idTemplate);
    }
}
