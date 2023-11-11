package org.evomaster.client.java.sql.distance.advanced.query_distance;

import java.util.Objects;

import static java.lang.String.format;
import static org.evomaster.client.java.sql.distance.advanced.helpers.distance.BranchDistanceHelper.normalize;

/**
 * Abstraction over distance. It is defined in terms of the distance components. For now, only
 * the branch distance component is present, but it will also contain the approach level when
 * subqueries are supported.
 */
public class Distance implements Comparable<Distance> {

    public static final Distance INF_DISTANCE = new Distance(Double.MAX_VALUE);
    public static final Distance ZERO_DISTANCE = new Distance(0D);

    private Double branchDistance;

    private Distance(Double branchDistance) {
        this.branchDistance = branchDistance;
    }

    public static Distance createDistance(Double branchDistance) {
        return new Distance(branchDistance);
    }

    public Double getBranchDistance() {
        return branchDistance;
    }

    public Double getValue() {
        return normalize(branchDistance);
    }

    @Override
    public String toString() {
        if(equals(INF_DISTANCE)) {
            return "inf";
        } else if(equals(ZERO_DISTANCE)) {
            return "0";
        } else {
            return format("%f (Unnormalized branch distance: %f)",
                getValue(), branchDistance);
        }
    }

    @Override
    public int compareTo(Distance distance) {
        return getValue().compareTo(distance.getValue());
    }

    @Override
    public boolean equals(Object object) {
        if(this == object) return true;
        if(object == null || getClass() != object.getClass()) return false;
        Distance distance = (Distance) object;
        return Objects.equals(branchDistance, distance.branchDistance);
    }

    @Override
    public int hashCode() {
        return Objects.hash(branchDistance);
    }
}
