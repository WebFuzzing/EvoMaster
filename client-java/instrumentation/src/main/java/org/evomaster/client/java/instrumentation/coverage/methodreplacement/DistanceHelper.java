package org.evomaster.client.java.instrumentation.coverage.methodreplacement;

public class DistanceHelper {

    public static final double H_REACHED_BUT_NULL = 0.05d;

    public static final double H_NOT_NULL = 0.1d;

    //2^16=65536, max distance for a char
    public static final int MAX_CHAR_DISTANCE = 65_536;


    public static int distanceToDigit(char c) {
        return distanceToRange(c, '0', '9');
    }

    public static int distanceToRange(char c, char minInclusive, char maxInclusive) {

        if (minInclusive >= maxInclusive) {
            throw new IllegalArgumentException("Invalid char range '" + minInclusive + "'-'" + maxInclusive + "'");
        }

        int diffAfter = minInclusive - c;
        int diffBefore = c - maxInclusive;

        int dist = Math.max(diffAfter, 0) + Math.max(diffBefore, 0);

        return dist;
    }

    public static int distanceToChar(char c, char target) {
        return Math.abs(c - target);
    }


    public static long getLeftAlignmentDistance(String a, String b) {

        long diff = Math.abs(a.length() - b.length());
        long dist = diff * MAX_CHAR_DISTANCE;

        for (int i = 0; i < Math.min(a.length(), b.length()); i++) {
            dist += Math.abs(a.charAt(i) - b.charAt(i));
        }

        assert dist >= 0;
        return dist;
    }

}
