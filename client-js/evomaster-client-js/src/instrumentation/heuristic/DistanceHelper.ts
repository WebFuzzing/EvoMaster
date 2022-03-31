import assert from "assert";

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

    public static getDistance(left: any, right: any) : number{
        // TODO check null?

        const ltype = typeof left;
        const rtype = typeof right;

        let d : number;

        if (ltype == "number" && rtype == "number")
            d = this.getDistanceToEqualityNumber(left, right);
        else if (ltype == "string" && rtype == "string")
            d = this.getLeftAlignmentDistance(left, right);
        else if ((ltype == "string" && rtype == "number") || ltype == "string" && rtype == "number"){
            d = this.getLeftAlignmentDistance(left, right);
        } else
            d = Number.MAX_VALUE;
        return d;
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