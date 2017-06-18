package org.evomaster.clientJava.instrumentation;

import java.io.Serializable;

/**
 * This represents the same data as in TargetInfoDto.
 * Here is replicated to have a clear distinction on how
 * such data is used
 */
public class TargetInfo implements Serializable{

    public final Integer mappedId;

    public final String descriptiveId;

    /**
     * heuristic [0,1], where 1 means covered
     */
    public final Double value;

    /**
     * Can be negative if target was never reached.
     * But this means that {@code value} must be 0
     */
    public final Integer actionIndex;

    public TargetInfo(Integer mappedId, String descriptiveId, Double value, Integer actionIndex) {
        this.mappedId = mappedId;
        this.descriptiveId = descriptiveId;
        this.value = value;
        this.actionIndex = actionIndex;
    }

    public static TargetInfo notReached(int theID){
        return new TargetInfo(theID, null, 0d, -1);
    }

    public TargetInfo withMappedId(int theID){
        if(mappedId != null){
            throw new IllegalArgumentException("Id already existing");
        }
        return new TargetInfo(theID, descriptiveId, value, actionIndex);
    }

    public TargetInfo withNoDescriptiveId(){
        return new TargetInfo(mappedId, null, value, actionIndex);
    }
}
