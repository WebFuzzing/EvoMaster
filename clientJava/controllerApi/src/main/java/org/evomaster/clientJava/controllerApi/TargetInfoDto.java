package org.evomaster.clientJava.controllerApi;

public class TargetInfoDto {

    /**
     * The id of the target
     */
    public Integer id;

    /**
     * A unique id for the target that is also descriptive for it.
     * Note: this string will usually be much longer than the numeric id.
     *
     * This field is optional: usually sent only the first time the target
     * has been encountered, and will be mainly used for debugging reasons
     */
    public String descriptiveId;

    /**
     * The fitness value for this target, in [0,1], where 1 means covered
     */
    public Double value;
}
