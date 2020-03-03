package org.evomaster.client.java.instrumentation.coverage.methodreplacement;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.chrono.ChronoLocalDateTime;
import java.util.Date;
import java.util.Objects;

public class DistanceHelper {

    public static final double H_REACHED_BUT_NULL = 0.05d;

    public static final double H_NOT_NULL = 0.1d;

    public static final double H_REACHED_BUT_EMPTY = H_REACHED_BUT_NULL;

    public static final double H_NOT_EMPTY = H_NOT_NULL;


    //2^16=65536, max distance for a char
    public static final int MAX_CHAR_DISTANCE = 65_536;


    public static double increasedDistance(double distance, double delta){
        if(Double.isInfinite(distance) || distance == Double.MAX_VALUE){
            return distance;
        }

        if(distance > (Double.MAX_VALUE - delta)){
            return Double.MAX_VALUE;
        }

        return distance + delta;
    }


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

    /**
     * Computes a distance to a==b. If a-b overflows,
     *
     * @param a
     * @param b
     * @return
     */
    public static double getDistanceToEquality(long a, long b) {
        // TODO: Some long values cannot be precisely represented as double values
        return getDistanceToEquality((double) a, (double) b);
    }


    public static double getDistanceToEquality(int a, int b) {
        return getDistanceToEquality((double) a, (double) b);
    }

    public static double getDistanceToEquality(char a, char b) {
        return getDistanceToEquality((double) a, (double) b);
    }

    public static double getDistanceToEquality(double a, double b) {
        if (!Double.isFinite(a) || !Double.isFinite(b)) {
            // one of the values is not finite
            return Double.MAX_VALUE;
        }

        final double distance;
        if (a < b) {
            distance = b - a;
        } else {
            distance = a - b;
        }
        if (distance < 0 || !Double.isFinite(distance)) {
            // overflow has occurred
            return Double.MAX_VALUE;
        } else {
            return distance;
        }
    }

    public static double getDistanceToEquality(Date a, Date b) {
        Objects.requireNonNull(a);
        Objects.requireNonNull(b);
        return DistanceHelper.getDistanceToEquality(a.getTime(), b.getTime());
    }

    public static double getDistanceToEquality(LocalDate a, LocalDate b) {
        Objects.requireNonNull(a);
        Objects.requireNonNull(b);
        return DistanceHelper.getDistanceToEquality(a.toEpochDay(), b.toEpochDay());
    }

    public static double getDistanceToEquality(ChronoLocalDateTime<?> a, ChronoLocalDateTime<?> b) {
        Objects.requireNonNull(a);
        Objects.requireNonNull(b);
        return DistanceHelper.getDistanceToEquality(getEpochMilli(a), getEpochMilli(b));
    }

    private static long getEpochMilli(ChronoLocalDateTime<?> chronoLocalDateTime) {
        return chronoLocalDateTime.atZone(ZoneOffset.UTC).toInstant().toEpochMilli();
    }

    public static double getDistanceToEquality(LocalTime a, LocalTime b) {
        return getDistanceToEquality(a.toSecondOfDay(), b.toSecondOfDay());
    }

    public static double getDistance(Object left, Object right) {
        Objects.requireNonNull(left);
        Objects.requireNonNull(right);

        final double distance;
        if (left instanceof String && right instanceof String) {
            // TODO Add string specialization info for left and right

            // String
            String a = (String) left;
            String b = right.toString();
            distance = (double) getLeftAlignmentDistance(a, b);

        } else if (left instanceof Byte && right instanceof Byte) {
            // Byte
            Byte a = (Byte) left;
            Byte b = (Byte) right;
            distance = DistanceHelper.getDistanceToEquality(a.longValue(), b.longValue());

        } else if (left instanceof Short && right instanceof Short) {
            // Short
            Short a = (Short) left;
            Short b = (Short) right;
            distance = DistanceHelper.getDistanceToEquality(a.longValue(), b.longValue());

        } else if (left instanceof Integer && right instanceof Integer) {
            // Integer
            int a = (Integer) left;
            int b = (Integer) right;
            distance = getDistanceToEquality(a, b);

        } else if (left instanceof Long && right instanceof Long) {
            // Long
            long a = (Long) left;
            long b = (Long) right;
            distance = getDistanceToEquality(a, b);

        } else if (left instanceof Float && right instanceof Float) {
            // Float
            float a = (Float) left;
            float b = (Float) right;
            distance = getDistanceToEquality(a, b);

        } else if (left instanceof Double && right instanceof Double) {
            // Double
            double a = (Double) left;
            double b = (Double) right;
            distance = getDistanceToEquality(a, b);

        } else if (left instanceof Character && right instanceof Character) {
            // Character
            Character a = (Character) left;
            Character b = (Character) right;
            distance = getDistanceToEquality(a, b);

        } else if (left instanceof Date && right instanceof Date) {
            // Date
            Date a = (Date) left;
            Date b = (Date) right;
            distance = getDistanceToEquality(a, b);

        } else if (left instanceof LocalDate && right instanceof LocalDate) {
            // LocalDate
            LocalDate a = (LocalDate) left;
            LocalDate b = (LocalDate) right;
            distance = getDistanceToEquality(a, b);

        } else if (left instanceof LocalTime && right instanceof LocalTime) {
            // LocalTime
            LocalTime a = (LocalTime) left;
            LocalTime b = (LocalTime) right;
            distance = getDistanceToEquality(a, b);

        } else if (left instanceof LocalDateTime && right instanceof LocalDateTime) {
            // LocalDateTime
            LocalDateTime a = (LocalDateTime) left;
            LocalDateTime b = (LocalDateTime) right;
            distance = getDistanceToEquality(a, b);

        } else {
            distance = Double.MAX_VALUE;
        }

        return distance;
    }
}
