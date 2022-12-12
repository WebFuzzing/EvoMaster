package org.evomaster.client.java.instrumentation.heuristic.validator;


import org.evomaster.client.java.instrumentation.heuristic.validator.custom.ClassConstraintXY;
import org.evomaster.client.java.instrumentation.heuristic.validator.custom.ClassConstraintXZ;

import javax.validation.constraints.Positive;

@ClassConstraintXY
@ClassConstraintXZ
public class ClassConstraintsBean {

    @Positive
    public int x;

    public int y;

    public int z;
}
