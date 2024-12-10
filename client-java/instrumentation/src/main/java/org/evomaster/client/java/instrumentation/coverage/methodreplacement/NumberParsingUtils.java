package org.evomaster.client.java.instrumentation.coverage.methodreplacement;

import org.evomaster.client.java.distance.heuristics.DistanceHelper;

import static org.evomaster.client.java.distance.heuristics.DistanceHelper.*;

public class NumberParsingUtils {
    /**
     * Optimizes for Java regex pattern "['-']?[0-9]*['.']?[0-9]*"
     * @param input
     * @return
     */
    public static double getParsingHeuristicValueForFloat(String input) {

        if (input == null) {
            return H_REACHED_BUT_NULL;
        }


        if (input.length() == 0) {
            return H_NOT_NULL;
        }

        long distance = 0;

        if (input.length() == 1) {
            //cannot be '-'
            distance += distanceToDigit(input.charAt(0));
        } else {

            for (int i = 0; i < input.length(); i++) {

                int digitsDist = distanceToDigit(input.charAt(i));
                int minusDist = distanceToChar(input.charAt(i), '-');
                int dotDist = distanceToChar(input.charAt(i), '.');

                if (i == 0) {
                    /*
                        first symbol could be a '-'.
                        note that '.' could be in any position, including first and last, with the only
                        exception of 2nd when first is '-'
                     */
                    distance += Math.min(Math.min(digitsDist, minusDist), dotDist);

                } else {
                    int firstIndexOfDot = input.indexOf('.');
                    if (firstIndexOfDot < 0) {
                        // no dots, so can optimize for a '.'
                        distance += Math.min(digitsDist, dotDist);
                    } else if (i == firstIndexOfDot && (firstIndexOfDot != 1 || input.charAt(0)!='-' || input.length() > 2)) {
                        distance += 0;
                    } else {
                        distance += digitsDist;
                    }
                }
            }

        }

        if(distance < 0){
            distance = Long.MAX_VALUE; // overflow
        }

        //recall h in [0,1] where the highest the distance the closer to 0
        double base = H_NOT_NULL;
        double h = DistanceHelper.heuristicFromScaledDistanceWithBase(base, distance);
        return h;
    }

    private static double parseIntHeuristic(String input, int maxNumberOfDigits) {

        if (maxNumberOfDigits < 0) {
            throw new IllegalArgumentException("Number of digits cannot be negative");
        }

        if (input == null) {
            return H_REACHED_BUT_NULL;
        }

        double base = H_NOT_NULL;

        if (input.length() == 0) {
            return base;
        }

        long distance = 0;

        if (input.length() == 1) {
            //cannot be '-'
            distance += distanceToDigit(input.charAt(0));
        } else {
            for (int i = 0; i < input.length(); i++) {

                int digitsDist = distanceToDigit(input.charAt(i));

                if (i == 0) {
                    //first symbol could be a '-'
                    distance += Math.min(digitsDist, distanceToChar(input.charAt(i), '-'));
                } else if (i >= maxNumberOfDigits) {

                    //too long string would not be a valid 32bit/64bit integer representation
                    distance += MAX_CHAR_DISTANCE;
                } else {
                    distance += digitsDist;
                }

            }
        }

        if(distance < 0){
            distance = Long.MAX_VALUE; // overflow
        }


        //recall h in [0,1] where the highest the distance the closer to 0
        double h = DistanceHelper.heuristicFromScaledDistanceWithBase(base, distance);
        return h;
    }

    /*
        The -2 in the computations below is bit tricky...
        Given a maximum length N, not all digit strings of length N would be valid, eg
        3 billion is not a valid signed int.
        And we also need to consider the "-" for negative values.
     */

    public static double parseByteHeuristic(String input) {
        int maxNumberOfDigits = Byte.valueOf(Byte.MIN_VALUE).toString().length() - 2;
        return parseIntHeuristic(input, maxNumberOfDigits);
    }

    public static double parseShortHeuristic(String input) {
        int maxNumberOfDigits = Short.valueOf(Short.MIN_VALUE).toString().length() - 2;
        return parseIntHeuristic(input, maxNumberOfDigits);
    }

    public static double parseIntHeuristic(String input) {
        int maxNumberOfDigits = Integer.valueOf(Integer.MIN_VALUE).toString().length() - 2;
        return parseIntHeuristic(input, maxNumberOfDigits);
    }

    public static double parseLongHeuristic(String input) {
        int maxNumberOfDigits = Long.valueOf(Long.MIN_VALUE).toString().length() - 2;
        return parseIntHeuristic(input, maxNumberOfDigits);
    }
}
