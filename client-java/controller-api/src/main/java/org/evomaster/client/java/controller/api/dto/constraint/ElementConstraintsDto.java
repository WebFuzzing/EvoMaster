package org.evomaster.client.java.controller.api.dto.constraint;


import java.util.List;

/**
 * Set of possible constraints applicable to a single element (ie, no intra-dependency constraints among
 * different elements).
 *
 * Note: the type and id of the element is undefined here.
 * Some constraint might be interpreted differently based on the type of the element.
 * For example, a min value could be an integer or a double based on the element.
 * This also implies that numeric values are passed as string (this also helps with JSON representation issues)
 */
public class ElementConstraintsDto {

    public Boolean isNullable = null;

    public Boolean isOptional = null;

    public String minValue = null;

    public String maxValue = null;

    public List<String> enumValuesAsStrings;

    //TODO much more can be added here
}
