package org.evomaster.clientJava.instrumentation;

/**
 * This represents the same data as in TargetInfoDto.
 * Here is replicated to have a clear distinction on how
 * such data is used
 */
public class TargetInfo {

    public final Integer id;

    public final String descriptiveId;

    public final Double value;

    public TargetInfo(Integer id, String descriptiveId, Double value) {
        this.id = id;
        this.descriptiveId = descriptiveId;
        this.value = value;
    }
}
