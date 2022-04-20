package org.evomaster.client.java.controller.api.dto.problem.rpc;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * a dto to collect info of param of endpoints to be tested
 * that is used by both core (for identifying genes) and driver (for endpoint invocation) sides
 */
public class ParamDto {

    /**
     * param name
     */
    public String name;

    /**
     * type of the param
     */
    public TypeDto type;

    /**
     * value with string format
     */
    public String stringValue;

    /**
     * inner content
     */
    public List<ParamDto> innerContent;

    /**
     * whether the param could be null
     */
    public boolean isNullable = true;


    /**
     * a min size of the param, eg, String, List
     */
    public Long minSize;

    /**
     * a max size of the param, eg, String, List
     */
    public Long maxSize;


    /**
     * a min value of the param, eg, Numeric
     */
    public String minValue;

    /**
     * a max value of the param, eg, Numeric
     */
    public String maxValue;

    /**
     * precision
     * applicable to bigdecimal, double, float
     */
    public Integer precision;

    /**
     * scale
     * applicable to bigdecimal, double, float
     */
    public Integer scale;

    /**
     * pattern specified with regex exp
     */
    public String pattern;

    /**
     * a list of candidates for the param customized by user
     */
    public List<ParamDto> candidates;

    /**
     * a list of references to candidates which are used for
     * dependent candidates which have same reference
     */
    public List<String> candidateReferences;

    /**
     * @return a copy of the paramDto
     */
    public ParamDto copy(){
        ParamDto copy = new ParamDto();
        copy.name = name;
        copy.type = type;
        copy.isNullable = isNullable;
        if (innerContent != null)
            copy.innerContent = innerContent.stream().map(ParamDto::copy).collect(Collectors.toList());
        copy.stringValue = stringValue;
        copy.maxSize = maxSize;
        copy.minSize = minSize;
        copy.minValue = minValue;
        copy.maxValue = maxValue;
        copy.pattern = pattern;

        if (candidates != null)
            copy.candidates = candidates.stream().map(ParamDto::copy).collect(Collectors.toList());

        if (candidateReferences != null)
            copy.candidateReferences = new ArrayList<>(candidateReferences);
        return copy;
    }

}
