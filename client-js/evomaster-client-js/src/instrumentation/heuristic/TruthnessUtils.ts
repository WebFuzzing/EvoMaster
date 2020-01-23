import Truthness from "./Truthness";
import DistanceHelper from "./DistanceHelper";

export default class TruthnessUtils {

    public static getEqualityTruthness(a: number, b: number): Truthness {
        const distance = DistanceHelper.getDistanceToEqualityNumber(a, b);
        const normalizedDistance = Truthness.normalizeValue(distance);

        return new Truthness(1 - normalizedDistance, a != b ? 1 : 0);
    }


    public static getLessThanTruthness(a: number, b: number): Truthness {
        const distance = DistanceHelper.getDistanceToEqualityNumber(a, b);
        return new Truthness(a < b ?
            1 : 1 / (1.1 + distance),
            a >= b ? 1 : 1 / (1.1 + distance));
    }
}
