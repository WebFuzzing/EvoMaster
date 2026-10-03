package org.evomaster.client.java.controller.mongo.utils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;
import java.util.OptionalLong;

public abstract class MongoUtils {

    /**
     * Enumeration representing the geospatial model used for distance calculations.
     * PLANAR: Uses Euclidean distance for flat surfaces.
     * SPHERICAL: Uses Haversine distance for spherical surfaces (e.g., Earth)
     */
    public enum GeoSpatialModel {
        /**
         * PLANAR: Uses Euclidean distance for flat surfaces.
         */
        PLANAR,
        /**
         * SPHERICAL: Uses Haversine distance for spherical surfaces (e.g., Earth)
         */
        SPHERICAL
    }

    /**
     * Calculates the Euclidean distance between two points in a 2D Cartesian coordinate system.
     * The Euclidean distance is the straight-line distance between two points in Euclidean space.
     *
     * @param x1 the x-coordinate of the first point
     * @param y1 the y-coordinate of the first point
     * @param x2 the x-coordinate of the second point
     * @param y2 the y-coordinate of the second point
     * @return the Euclidean distance between the two points
     */
    private static double euclideanDistance(
            double x1,
            double y1,
            double x2,
            double y2) {
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }

    /**
     * Calculates the Haversine distance between two geographical points specified
     * in radians. The Haversine formula determines the great-circle distance between
     * two points on a sphere given their latitudes and longitudes.
     *
     * @param x1 the longitude of the first point in radians
     * @param y1 the latitude of the first point in radians
     * @param x2 the longitude of the second point in radians
     * @param y2 the latitude of the second point in radians
     * @return the Haversine distance between the two points in meters
     */
    private static double haversineDistance(
            double x1,
            double y1,
            double x2,
            double y2) {

        // Earth's radius in meters
        double radius = 6371000.0;

        double dLat = y2 - y1;
        double dLon = x2 - x1;

        double a = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(y1) * Math.cos(y2)
                * Math.pow(Math.sin(dLon / 2), 2);

        double c = 2 * Math.atan2(
                Math.sqrt(a),
                Math.sqrt(1 - a));

        return radius * c;
    }

    /**
     * Calculates the distance between two points based on the specified geospatial model.
     * This method supports both planar (Euclidean) and spherical (Haversine) distance calculations
     * depending on the provided {@code GeoSpatialModel}.
     *
     * @param x1 the x-coordinate (or longitude in radians for spherical model) of the first point
     * @param y1 the y-coordinate (or latitude in radians for spherical model) of the first point
     * @param x2 the x-coordinate (or longitude in radians for spherical model) of the second point
     * @param y2 the y-coordinate (or latitude in radians for spherical model) of the second point
     * @param geoSpatialModel the geospatial model to apply for distance calculation; must be either
     *                        {@code GeoSpatialModel.PLANAR} or {@code GeoSpatialModel.SPHERICAL}
     * @return the calculated distance between the two points. For the planar model, this value
     *         represents the Euclidean distance. For the spherical model, this is the Haversine
     *         distance in meters.
     * @throws IllegalArgumentException if an unsupported {@code GeoSpatialModel} is provided
     */
    public static double getDistanceBetweenPoints(double x1, double y1, double x2, double y2, GeoSpatialModel geoSpatialModel) {
        double distanceBetweenPoints;
        switch (geoSpatialModel) {
            case PLANAR:
                distanceBetweenPoints = euclideanDistance(x1, y1, x2, y2);
                break;
            case SPHERICAL:
                distanceBetweenPoints = haversineDistance(x1, y1, x2, y2);
                break;
            default:
                throw new IllegalArgumentException("Unsupported GeoSpatialModel: " + geoSpatialModel);
        }
        return distanceBetweenPoints;
    }


    /**
     * Converts the provided {@code Number} into a long value if it represents an integral number
     * within the range of a {@code long}. Supports various {@code Number} types including
     * {@code Byte}, {@code Short}, {@code Integer}, {@code Long}, {@code BigInteger}, and
     * {@code BigDecimal}.
     *
     * If the provided number is of type {@code BigInteger} or {@code BigDecimal} and falls outside
     * the range of {@code long}, or if it cannot be expressed as an exact integral value,
     * the method will return an empty {@code OptionalLong}.
     *
     * @param value the {@code Number} to convert. Must not be {@code null}.
     * @return an {@code OptionalLong} containing the long value if the input represents a valid integral
     *         value within the range of {@code long}, or an empty {@code OptionalLong} otherwise.
     * @throws NullPointerException if the provided {@code value} is {@code null}.
     */
    public static OptionalLong getIntegralLongValue(Number value) {
        Objects.requireNonNull(value);

        if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long) {
            return OptionalLong.of(value.longValue());
        }

        if (value instanceof BigInteger) {
            BigInteger bigInteger = (BigInteger) value;
            if (bigInteger.compareTo(BigInteger.valueOf(Long.MIN_VALUE)) < 0
                    || bigInteger.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0) {
                return OptionalLong.empty();
            }
            return OptionalLong.of(bigInteger.longValue());
        }

        if (value instanceof BigDecimal) {
            try {
                return OptionalLong.of(((BigDecimal) value).longValueExact());
            } catch (ArithmeticException e) {
                return OptionalLong.empty();
            }
        }

        double doubleValue = value.doubleValue();
        if (!Double.isFinite(doubleValue)
                || doubleValue < Long.MIN_VALUE
                || doubleValue > Long.MAX_VALUE
                || doubleValue != Math.rint(doubleValue)) {
            return OptionalLong.empty();
        }

        return OptionalLong.of((long) doubleValue);
    }
}
