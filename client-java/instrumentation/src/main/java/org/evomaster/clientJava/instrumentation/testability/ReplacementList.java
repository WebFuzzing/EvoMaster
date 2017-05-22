package org.evomaster.clientJava.instrumentation.testability;

import java.util.Arrays;
import java.util.List;

public class ReplacementList {

    public static List<BooleanMethodTransformer> getBooleanMethodTransformers(){
        return Arrays.asList(
                new StringTransformer()
        );
    }
}
