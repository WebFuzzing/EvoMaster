package org.evomaster.core.output.clustering.metrics

abstract class DistanceMetric<V>{
    /**
     * The [recommendedEpsilon] is used to allow each implementation of [DistanceMetric]
     * to suggest individual epsilon values.
     */
    public abstract fun calculateDistance(first: V, second: V): Double
    public abstract fun getName(): String
    public abstract fun getRecommendedEpsilon(): Double
}
