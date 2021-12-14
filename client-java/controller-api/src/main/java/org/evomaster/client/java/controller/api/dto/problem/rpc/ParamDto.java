package org.evomaster.client.java.controller.api.dto.problem.rpc;

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
     * value with json format
     */
    public String jsonValue;

    /**
     * inner content
     */
    public List<ParamDto> innerContent;

    /**
     * whether the param could be null
     * TODO handle this with javax.validate, but now it is all true
     */
    public boolean isNullable = true;


    public Long minSize;

    public Long maxSize;

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
        copy.jsonValue = jsonValue;
        copy.maxSize = maxSize;
        copy.minSize = minSize;
        return copy;
    }

}
