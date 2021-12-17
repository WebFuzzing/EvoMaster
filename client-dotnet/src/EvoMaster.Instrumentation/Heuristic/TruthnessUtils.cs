using System;
using System.Diagnostics;
using EvoMaster.Instrumentation.Heuristic;

public class TruthnessUtils {
    ///<summary>scales to a positive double value to the [0,1] range</summary>
    /// <param name="v">A non-negative double value</param>
    public static double NormalizeValue(double v) {
        if (v < 0) {
            throw new ArgumentException("Negative value: " + v);
        }

        if (!double.IsFinite(v) || v == double.MaxValue) {
            return 1d;
        }

        //normalization function from old ICST/STVR paper
        var normalized = v / (v + 1d);

        Trace.Assert(normalized >= 0 && normalized <= 1);

        return normalized;
    }


    public static Truthness GetEqualityTruthness(int a, int b) {
        var distance = DistanceHelper.GetDistanceToEquality(a, b);
        var normalizedDistance = NormalizeValue(distance);
        return new Truthness(
            1d - normalizedDistance,
            a != b ? 1d : 0d
        );
    }

    public static Truthness GetEqualityTruthness(long a, long b) {
        var distance = DistanceHelper.GetDistanceToEquality(a, b);
        var normalizedDistance = NormalizeValue(distance);
        return new Truthness(
            1d - normalizedDistance,
            a != b ? 1d : 0d
        );
    }

    public static Truthness GetLessThanTruthness(long a, long b) {
        var distance = DistanceHelper.GetDistanceToEquality(a, b);
        return new Truthness(
            a < b ? 1d : 1d / (1.1d + distance),
            a >= b ? 1d : 1d / (1.1d + distance)
        );
    }

    public static Truthness GetEqualityTruthness(double a, double b) {
        var distance = DistanceHelper.GetDistanceToEquality(a, b);
        var normalizedDistance = NormalizeValue(distance);
        return new Truthness(
            1d - normalizedDistance,
            a != b ? 1d : 0d
        );
    }

    ///<param name="len">A positive value for a length</param>
    public static Truthness GetTruthnessToEmpty(int len) {
        if (len < 0) {
            throw new ArgumentException("lengths should always be non-negative. Invalid length " + len);
        }

        var t = len == 0 ? new Truthness(1, 0) : new Truthness(1d / (1d + len), 1);

        return t;
    }
}