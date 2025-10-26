package org.evomaster.testoldlibraries.heuristic.validator;

import cz.jirutka.validator.collection.constraints.EachPattern;

import java.util.ArrayList;
import java.util.List;

public class PatternBean {

    @EachPattern(regexp="a+")
    public List<String> list = new ArrayList<>();
}
