package org.evomaster.client.java.controller.api.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by arcuri82 on 16-Sep-19.
 */
public class ActionDto {

    /**
     * The index of this action in the test.
     * Eg, in a test with 10 indices, the index would be
     * between 0 and 9
     */
    public Integer index = null;

    /**
     * A list (possibly empty) of String values used in the action.
     * This info can be used for different kinds of taint analysis, eg
     * to check how such values are used in the SUT
     */
    public List<String> inputVariables = new ArrayList<>();
}
