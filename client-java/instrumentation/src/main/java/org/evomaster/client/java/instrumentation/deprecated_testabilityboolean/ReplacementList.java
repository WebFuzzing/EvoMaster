package org.evomaster.client.java.instrumentation.deprecated_testabilityboolean;

import java.util.Arrays;
import java.util.List;

@Deprecated
public class ReplacementList {

    public static List<BooleanMethodTransformer> getBooleanMethodTransformers(){
        return Arrays.asList(
                new StringTransformer()
        );
    }
}
