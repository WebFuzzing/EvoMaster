package org.evomaster.client.java.controller.api.dto.problem.rpc.schema.dto;

import java.util.List;
import java.util.Map;

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

}
