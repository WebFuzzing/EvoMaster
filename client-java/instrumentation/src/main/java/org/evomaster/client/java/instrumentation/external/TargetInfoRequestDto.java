package org.evomaster.client.java.instrumentation.external;

import java.io.Serializable;
import java.util.Collection;

public class TargetInfoRequestDto implements Serializable {

    public Collection<Integer> ids;
    public boolean fullyCovered;
    public boolean descriptiveIds;
}
