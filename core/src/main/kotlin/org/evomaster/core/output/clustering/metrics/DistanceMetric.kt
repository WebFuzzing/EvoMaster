package org.evomaster.core.output.clustering.metrics

abstract class DistanceMetric<V>{
    public abstract fun calculateDistance(first: V, second: V): Double
    public abstract fun getName(): String
}
