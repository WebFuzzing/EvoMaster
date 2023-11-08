package org.evomaster.client.java.sql.distance.advanced.helpers.distance;

public class BooleanDistanceHelper {

    public static Double calculateDistanceForEquals(Boolean left, Boolean right) {
        return left == right ? 0D : 1D;
    }

    public static Double calculateDistanceForNotEquals(Boolean left, Boolean right) {
        return left != right ? 0D : 1D;
    }
}