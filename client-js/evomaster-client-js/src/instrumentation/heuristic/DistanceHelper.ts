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

}