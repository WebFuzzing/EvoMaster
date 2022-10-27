package org.evomaster.client.java.controller.api.dto;

import org.evomaster.client.java.controller.api.dto.database.schema.ExtraConstraintsDto;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

    /**
     * Number of cases in which numeric comparisons needed customized
     * instrumentation (eg, needed on JVM for non-integer types)
     */
    public int numberOfInstrumentedNumberComparisons;

    /*
        Key -> DTO full name
        Value -> OpenAPI object schema
        TODO should consider if also adding info on type, eg JSON vs XML
     */
    public Map<String,String> parsedDtos;


    /**
     * Key is DTO full name
     * Value is OpenAPI object schema
     *
     * User might need to get schema of specific jvm dto classes
     * and such jvm classes might not be read with jackson or gson
     * this field is to collect a map of such specified jvm dto classes to their schema
     * */
    public Map<String, String> extractedSpecifiedDtos;

    /**
     * Extra information extracted for example from JPA entities
     */
    public List<ExtraConstraintsDto> extraDatabaseConstraintsDtos;
}
