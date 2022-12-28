package org.evomaster.client.java.instrumentation.heuristic.validator.javax;


import org.evomaster.client.java.instrumentation.heuristic.validator.javax.custom.ClassConstraintXY;
import org.evomaster.client.java.instrumentation.heuristic.validator.javax.custom.ClassConstraintXZ;

import javax.validation.constraints.Positive;

@ClassConstraintXY
@ClassConstraintXZ
public class ClassConstraintsBean {

    @Positive
    public int x;

    public int y;

    public int z;
}
