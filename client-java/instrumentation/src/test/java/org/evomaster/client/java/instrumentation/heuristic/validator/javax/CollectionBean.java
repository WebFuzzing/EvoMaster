package org.evomaster.client.java.instrumentation.heuristic.validator.javax;

import javax.validation.constraints.Positive;
import java.util.ArrayList;
import java.util.List;

public class CollectionBean {

    public List<@Positive Integer> list = new ArrayList<>();
}
