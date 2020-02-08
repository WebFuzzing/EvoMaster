/**
 * This represents the same data as in TargetInfoDto.
 * Here is replicated to have a clear distinction on how
 * such data is used
 */
export default class TargetInfo {

    public static notReached(theID: number): TargetInfo {
        return new TargetInfo(theID, null, 0.0, -1);
    }

    public readonly mappedId: number;

    public readonly descriptiveId: string;

    /**
     * heuristic [0,1], where 1 means covered
     */
    public readonly value: number;

    /**
     * Can be negative if target was never reached.
     * But this means that {@code value} must be 0
     */
    public readonly actionIndex: number;

    constructor(mappedId: number, descriptiveId: string, value: number, actionIndex: number) {
        this.mappedId = mappedId;
        this.descriptiveId = descriptiveId;
        this.value = value;
        this.actionIndex = actionIndex;
    }

    public withMappedId(theID: number): TargetInfo {

        if (this.mappedId) {
            throw new Error("Id already existing");
        }
        return new TargetInfo(theID, this.descriptiveId, this.value, this.actionIndex);
    }

    public withNoDescriptiveId(): TargetInfo {
        return new TargetInfo(this.mappedId, null, this.value, this.actionIndex);
    }
}
