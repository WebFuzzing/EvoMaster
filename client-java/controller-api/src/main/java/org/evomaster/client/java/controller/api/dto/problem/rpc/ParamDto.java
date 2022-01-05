package org.evomaster.client.java.controller.api.dto.problem.rpc;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * created by manzhang on 2021/11/27
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
    public Long minValue;

    /**
     * a max value of the param, eg, Numeric
     */
    public Long maxValue;

    public List<ParamDto> candidates;

    public List<String> candidateReferences;

    /**
     * create a copy
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

        if (candidates != null)
            copy.candidates = candidates.stream().map(ParamDto::copy).collect(Collectors.toList());

        if (candidateReferences != null)
            copy.candidateReferences = new ArrayList<>(candidateReferences);
        return copy;
    }

}
