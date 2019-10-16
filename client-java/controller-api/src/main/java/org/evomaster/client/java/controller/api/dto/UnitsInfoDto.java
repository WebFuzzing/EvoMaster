package org.evomaster.client.java.controller.api.dto;

import java.util.HashSet;
import java.util.Set;

/**
 * Information about the "units" in the SUT.
 * In case of OO languages like Java and Kotlin, those will be "classes"
 *
 * Created by arcuri82 on 27-Sep-19.
 */
public class UnitsInfoDto {

    /**
     * Then name of all the units (eg classes) in the SUT
     */
    public Set<String> unitNames = new HashSet<>();

    /**
     * The total number of lines/statements/instructions in all
     * units of the whole SUT
     */
    public int numberOfLines;


    /**
     * The total number of branches in all
     * units of the whole SUT
     */
    public int numberOfBranches;

    /**
     * Number of replaced method testability transformations.
     * But only for SUT units.
     */
    public int numberOfReplacedMethodsInSut;

    /**
     * Number of replaced method testability transformations.
     * But only for third-party library units (ie all units not in the SUT).
     */
    public int numberOfReplacedMethodsInThirdParty;


    /**
     * Number of tracked methods. Those are special methods for which
     * we explicitly keep track of how they are called (eg their inputs).
     */
    public int numberOfTrackedMethods;
}
