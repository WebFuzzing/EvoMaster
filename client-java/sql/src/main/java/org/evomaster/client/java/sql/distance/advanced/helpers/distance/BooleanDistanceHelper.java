package org.evomaster.client.java.sql.distance.advanced.helpers.distance;

/**
 * Class used to calculate the branch distance for booleans. Supports equals and not equals operators.
 */
public class BooleanDistanceHelper {

    public static double calculateDistanceForEquals(boolean left, boolean right) {
        return left == right ? 0D : 1D;
    }

    public static double calculateDistanceForNotEquals(boolean left, boolean right) {
        return left != right ? 0D : 1D;
    }
}