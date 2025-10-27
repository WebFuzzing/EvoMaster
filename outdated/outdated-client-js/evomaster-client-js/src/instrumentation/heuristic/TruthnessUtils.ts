import Truthness from "./Truthness";
import DistanceHelper from "./DistanceHelper";
import HeuristicsForBooleans from "./HeuristicsForBooleans";

export default class TruthnessUtils {

    public static getEqualityTruthnessNumber(a: number, b: number): Truthness {
        const distance = DistanceHelper.getDistanceToEqualityNumber(a, b);
        const normalizedDistance = Truthness.normalizeValue(distance);

        return new Truthness(1 - normalizedDistance, a !== b ? 1 : HeuristicsForBooleans.FLAG_NO_EXCEPTION);
    }

    public static getEqualityTruthnessString(a: string, b: string): Truthness {
        const distance = DistanceHelper.getLeftAlignmentDistance(a, b);
        const normalizedDistance = Truthness.normalizeValue(distance);

        return new Truthness(1 - normalizedDistance, a !== b ? 1 : HeuristicsForBooleans.FLAG_NO_EXCEPTION);
    }

    public static getLessThanTruthnessNumber(a: number, b: number): Truthness {
        const distance = DistanceHelper.getDistanceToEqualityNumber(a, b);
        return new Truthness(a < b ?
            1 : 1 / (1.1 + distance),
            a >= b ? 1 : 1 / (1.1 + distance));
    }

    public static getLessThanTruthnessString(a: string, b: string): Truthness {

        let distance: number = DistanceHelper.MAX_CHAR_DISTANCE;

        for(let i=0; i < a.length && i < b.length; i++){
            const x = a.charCodeAt(i);
            const y = b.charCodeAt(i);

            /*
                What determines the order is the first char they have different,
                starting from left to right
             */
            if(x===y){
                continue;
            }

            distance = Math.abs(x - y);
            break;
        }

        return new Truthness(
            a < b ? 1 : 1 / (1.1 + distance),
            a >= b ? 1 : 1 / (1.1 + distance));
    }

}
