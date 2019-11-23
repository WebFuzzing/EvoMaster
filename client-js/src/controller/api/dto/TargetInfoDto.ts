
export default class TargetInfoDto {

    /**
     * The id of the target
     */
    public id: number;

    /**
     * A unique id for the target that is also descriptive for it.
     * Note: this string will usually be much longer than the numeric id.
     *
     * This field is optional: usually sent only the first time the target
     * has been encountered, and will be mainly used for debugging reasons
     */
    public descriptiveId: string;

    /**
     * The fitness value for this target, in [0,1], where 1 means covered
     */
    public value: number;

    /**
     * An id identify the action that led to this fitness score for this
     * testing target.
     * For example, it can be the index in the list representation of
     * the test case.
     * Can be negative if target was never reached.
     * But this means that {@code value} must be 0
     */
    public actionIndex: number;
}
