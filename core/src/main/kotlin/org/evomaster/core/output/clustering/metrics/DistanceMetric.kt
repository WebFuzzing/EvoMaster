package org.evomaster.core.output.clustering.metrics

abstract class DistanceMetric<V>{
    public abstract fun calculateDistance(val1: V, val2: V): Double
}