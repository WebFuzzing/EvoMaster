import assert from "assert";

/**
 * see https://developer.mozilla.org/en-US/docs/Web/JavaScript/Equality_comparisons_and_sameness#abstract_equality_strict_equality_and_same_value_in_the_specification
 */
export enum EqualityAlgorithm{
    AbstractEquality, // ==
    StrictEquality, // ===
    SameValueZero, // Object.is()
    SameValue// same with Object.is(), but +0 and -0 are considered equal
}


export default class DistanceHelper {

    public static readonly H_REACHED_BUT_NULL = 0.05;

    public static readonly H_NOT_NULL = 0.1;

    public static readonly H_REACHED_BUT_EMPTY = DistanceHelper.H_REACHED_BUT_NULL;

    public static readonly H_NOT_EMPTY = DistanceHelper.H_NOT_NULL;


    //2^16=65536, max distance for a char
    public static readonly MAX_CHAR_DISTANCE = 65_536;

    public static getDistanceToEqualityNumber(a: number, b: number): number {
        if (!Number.isFinite(a) || !Number.isFinite(b)) {
            // one of the values is not finite
            return Number.MAX_VALUE;
        }

        let distance: number;
        if (a < b) {
            distance = b - a;
        } else {
            distance = a - b;
        }

        if (distance < 0 || !Number.isFinite(distance)) {
            // overflow has occurred
            return Number.MAX_VALUE;
        } else {
            return distance;
        }
    }

    public static getDistanceToEqualityString(a: string, b: string): number {
        return DistanceHelper.getLeftAlignmentDistance(a,b);
    }

    public static getLeftAlignmentDistance(a: string, b: string): number {

        const diff = Math.abs(a.length - b.length);
        let dist = diff * DistanceHelper.MAX_CHAR_DISTANCE;

        for (let i = 0; i < Math.min(a.length, b.length); i++) {
            dist += Math.abs(a.charCodeAt(i) - b.charCodeAt(i));
        }

        assert(dist >= 0);
        return dist;
    }

    /**
     * compute distance between left and right that is used in collection distance
     * @param left
     * @param right
     * @param equalityRule a rule to decide how to process equality comparison
     */
    public static getDistance(left: any, right: any, equalityRule : EqualityAlgorithm) : number{
        // TODO check null?

        const ltype = typeof left;
        const rtype = typeof right;

        let d : number;

        if (ltype == "number" && rtype == "number"){
            d = this.getDistanceToEqualityNumberWithAlgorithm(left, right, equalityRule);
        } else if (ltype == "string" && rtype == "string")
            d = this.getLeftAlignmentDistance(left, right);
        else {
            if (equalityRule === EqualityAlgorithm.AbstractEquality){
                if (ltype == "string" && rtype == "number"){
                    d = this.getLeftAlignmentDistance(left, right.toString());
                } else if (rtype == "string" && ltype == "number"){
                    d = this.getLeftAlignmentDistance(left.toString(), right);
                } else
                    d = Number.MAX_VALUE;
            }else{
                d = Number.MAX_VALUE;
            }
        }
        return d;
    }

    private static getDistanceToEqualityNumberWithAlgorithm(a: number, b: number, equlity: EqualityAlgorithm): number {
        // handling NaN
        if (isNaN(a) || isNaN(b)){
            if (equlity === EqualityAlgorithm.SameValue || equlity === EqualityAlgorithm.SameValueZero && (isNaN(a) && isNaN(b))) return 0;
            return Number.MAX_VALUE
        }
        // handling +0 and -0
        if (a === 0 && a === b){
            /*
                TODO need to check with Andrea
                currently, we employ `epsilon` to provide h between -0 and +0 for SameValue algorithm
                might need a bit handling for -0 and +0 in core of evomaster
             */
            if (equlity === EqualityAlgorithm.SameValue && !Object.is(a, b))
                return Number.EPSILON;
        }
        return this.getDistanceToEqualityNumber(a, b)
    }



    /**
     * Return a h=[0,1] heuristics from a scaled distance, taking into account a starting base
     * @param base
     * @param distance
     * @return
     */
    public static heuristicFromScaledDistanceWithBase(base: number, distance: number): number{

        if (base < 0 || base >=1)
            throw Error("Invalid base: " + base);
        if (distance < 0)
            throw Error("Negative distance: " + distance)
        if (!isFinite(distance) || distance == Number.MAX_VALUE)
            return base;

        return base + ((1-base)/(distance + 1));
    }
}