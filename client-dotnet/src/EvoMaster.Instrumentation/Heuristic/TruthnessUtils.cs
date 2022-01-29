using System;
using System.Diagnostics;
using EvoMaster.Instrumentation.Heuristic;

public class TruthnessUtils{

    //https://docs.microsoft.com/en-us/dotnet/csharp/language-reference/builtin-types/floating-point-numeric-types
    //15 digits
    private const double CLOSE_TO_ONE = 0.999999999999999;
    
    ///<summary>scales to a positive double value to the [0,1] range</summary>
    /// <param name="v">A non-negative double value</param>
    public static double NormalizeValue(double v){
        if (v < 0){
            throw new ArgumentException("Negative value: " + v);
        }
        
        if (!double.IsFinite(v) || v == double.MaxValue){
            return 1d;
        }

        //normalization function from old ICST/STVR paper
        var normalized = v / (v + 1d);

        Trace.Assert(normalized >= 0 && normalized <= 1);

        return normalized;
    }


    public static Truthness GetEqualityTruthness(int a, int b){
        var distance = DistanceHelper.GetDistanceToEquality(a, b);
        var normalizedDistance = NormalizeValue(distance);
        return new Truthness(
            1d - normalizedDistance,
            a != b ? 1d : 0d
        );
    }

    public static Truthness GetEqualityTruthness(long a, long b){
        var distance = DistanceHelper.GetDistanceToEquality(a, b);
        var normalizedDistance = NormalizeValue(distance);
        return new Truthness(
            1d - normalizedDistance,
            a != b ? 1d : 0d
        );
    }

    public static Truthness GetLessThanTruthness(long a, long b){
        var distance = DistanceHelper.GetDistanceToEquality(a, b);
        return new Truthness(
            a < b ? 1d : 1d / (1.1d + distance),
            a >= b ? 1d : 1d / (1.1d + distance)
        );
    }

    public static Truthness GetEqualityTruthness(double a, double b){
        var distance = DistanceHelper.GetDistanceToEquality(a, b);
        var normalizedDistance = NormalizeValue(distance);
        var valueOfTrue = 1d - normalizedDistance;
        
        // handle precision problem due to small value
        if (valueOfTrue == 1d && a!=b){
            valueOfTrue = CLOSE_TO_ONE;
        }
        return new Truthness(
            valueOfTrue,
            a != b ? 1d : 0d
        );
    }
    
    /**
     * calculate less than for double
     * the implementation is based on Long and js for less than
     * TODO need to check this with Andrea
     */
    public static Truthness GetLessThanTruthness(double a, double b) {
        double distance = DistanceHelper.GetDistanceToEquality(a, b);
        return new Truthness(
            a < b ? 1d : 1d / (1.1d + distance),
            a >= b ? 1d : 1d / (1.1d + distance)
        );
    }

    ///<param name="len">A positive value for a length</param>
    public static Truthness GetTruthnessToEmpty(int len){
        if (len < 0){
            throw new ArgumentException("lengths should always be non-negative. Invalid length " + len);
        }

        var t = len == 0 ? new Truthness(1, 0) : new Truthness(1d / (1d + len), 1);

        return t;
    }

    /**
     * with dotnet,
     * for cgt.un and clt.un, push 1 if any value is NaN. more info could be found as below two links
     * see https://docs.microsoft.com/en-us/dotnet/api/system.reflection.emit.opcodes.cgt_un?view=net-6.0
     * see https://docs.microsoft.com/en-us/dotnet/api/system.reflection.emit.opcodes.clt_un?view=net-6.0
     * 
     * for others, as checked, push 0 if any value is NaN
     *
     * need to check a bit with Andrea, if 0 is a proper distance for handling `ofTrue` value for NaN when 0 is pushed
     */
    public static Truthness GetTruthnessForNaN(bool isUnOpCode){
        return isUnOpCode ? new Truthness(1, 0) : new Truthness(0, 1);
    }
}