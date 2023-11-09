package org.evomaster.client.java.instrumentation.coverage.noninteger;

import org.evomaster.client.java.distance.heuristics.DistanceHelper;
import org.evomaster.client.java.distance.heuristics.TruthnessUtils;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

/**
 * Method replacement for LCMP, DCMPL, DCMPG, FCMPL, FCMPG instructions.
 * Those are used for when long, double and float numbers are compared.
 * See:
 * https://docs.oracle.com/javase/specs/jvms/se7/html/jvms-6.html
 *
 * Created by arcuri82 on 28-Feb-20.
 */
public class NonIntegerComparisons {

    /*
        WARN: all the names of the public methods here are used in the
        bytecode instrumentation. If change any name, should also change
        the instrumentator
     */

    private static final double REACHED = 0.2d;

    private static double heuristic(double distance){

        //value in [0,1], proportional to distance
        double normalized = TruthnessUtils.normalizeValue(distance);

        //the higher the distance, the worse the score
        double score = 1d - normalized;

        double scaled = REACHED + ((1d-REACHED) * score);

        return scaled;
    }

    public static int replaceLCMP(long a, long b, String id){

        double distance = DistanceHelper.getDistanceToEquality(a, b);

        double less = 0;
        double eq = 0;
        double greater = 0;

        int res;

        if(a == b){
            less = REACHED;
            eq = 1d;
            greater = REACHED;
            res = 0;
        } else if(a < b){
            less = 1d;
            eq = heuristic(distance);
            greater = heuristic(DistanceHelper.increasedDistance(distance, 1));
            res = -1;
        } else {
            assert  a > b;
            less = heuristic(DistanceHelper.increasedDistance(distance, 1));
            eq = heuristic(distance);
            greater = 1d;
            res = +1;
        }

        ExecutionTracer.executedNumericComparison(id, less, eq, greater);

        return res;
    }

    public static int replaceDCMPG(double a, double b, String id) {
        return replaceDCMP(a, b, id, 1);
    }

    public static int replaceDCMPL(double a, double b, String id) {
        return replaceDCMP(a, b, id, -1);
    }

    public static int replaceFCMPG(float a, float b, String id) {
        return replaceDCMP(a, b, id, 1);
    }

    public static int replaceFCMPL(float a, float b, String id) {
        return replaceDCMP(a, b, id, -1);
    }

    private static int replaceDCMP(double a, double b, String id, int resWhenNotFinite){


        double less = 0;
        double eq = 0;
        double greater = 0;

        int res;

        if (!Double.isFinite(a) || !Double.isFinite(b)) {
            less = REACHED;
            eq = REACHED;
            greater = REACHED;
            res = resWhenNotFinite;
        } else {

            double distance = DistanceHelper.getDistanceToEquality(a, b);

            if (a == b) {
                less = REACHED;
                eq = 1d;
                greater = REACHED;
                res = 0;
            } else if (a < b) {
                less = 1d;
                eq = heuristic(distance);
                greater = heuristic(DistanceHelper.increasedDistance(distance, 1));
                res = -1;
            } else {
                assert a > b;
                less = heuristic(DistanceHelper.increasedDistance(distance, 1));
                eq = heuristic(distance);
                greater = 1d;
                res = +1;
            }
        }

        ExecutionTracer.executedNumericComparison(id, less, eq, greater);

        return res;
    }
}
