package org.evomaster.testoldlibraries.heuristic.validator;

import cz.jirutka.validator.collection.constraints.EachRange;

import java.util.ArrayList;
import java.util.List;

public class RangeBean {

    @EachRange(min = 1, max = 10)
    public List<Integer> list = new ArrayList<>();
}
