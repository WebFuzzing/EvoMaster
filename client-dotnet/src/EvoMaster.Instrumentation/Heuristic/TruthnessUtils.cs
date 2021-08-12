using System;
using EvoMaster.Instrumentation.Heuristic;

public class TruthnessUtils {

    /**
     * scales to a positive double value to the [0,1] range
     *
     * @param v a non-negative double value
     * @return
     */
    public static double NormalizeValue(double v) {
        if (v < 0) {
            throw new ArgumentException("Negative value: " + v);
        }

        if(!Double.IsFinite(v) || v == Double.MaxValue){
            return 1d;
        }

        //normalization function from old ICST/STVR paper
        double normalized = v / (v + 1d);

        //assert normalized >= 0 && normalized <= 1; TODO

        return normalized;
    }



    public static Truthness GetEqualityTruthness(int a, int b) {
        double distance = DistanceHelper.GetDistanceToEquality(a, b);
        double normalizedDistance = NormalizeValue(distance);
        return new Truthness(
                1d - normalizedDistance,
                a != b ? 1d : 0d
        );
    }

    public static Truthness GetEqualityTruthness(long a, long b) {
        double distance = DistanceHelper.GetDistanceToEquality(a, b);
        double normalizedDistance = NormalizeValue(distance);
        return new Truthness(
                1d - normalizedDistance,
                a != b ? 1d : 0d
        );
    }

    public static Truthness GetLessThanTruthness(long a, long b) {
        double distance = DistanceHelper.GetDistanceToEquality(a, b);
        return new Truthness(
                a < b ? 1d : 1d / (1.1d + distance),
                a >= b ? 1d : 1d / (1.1d + distance)
        );
    }

    public static Truthness GetEqualityTruthness(double a, double b) {
        double distance = DistanceHelper.GetDistanceToEquality(a, b);
        double normalizedDistance = NormalizeValue(distance);
        return new Truthness(
                1d - normalizedDistance,
                a != b ? 1d : 0d
        );
    }


    /**
     * @param len a positive value for a length
     * @return
     */
    public static Truthness GetTruthnessToEmpty(int len) {
        Truthness t;
        if (len < 0) {
            throw new ArgumentException("lengths should always be non-negative. Invalid length " + len);
        }
        if (len == 0) {
            t = new Truthness(1, 0);
        } else {
            t = new Truthness(1d / (1d + len), 1);
        }
        return t;
    }
}
