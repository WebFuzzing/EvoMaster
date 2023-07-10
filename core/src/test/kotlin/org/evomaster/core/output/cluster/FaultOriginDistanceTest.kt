package org.evomaster.core.output.cluster

import org.evomaster.core.output.clustering.metrics.FaultOriginDistance
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FaultOriginDistanceTest {

    @Test
    fun initialTest(){
        val p0 = "com/foo/rest/examples/spring/openapi/v3/cluster/ClusterApplication_35_timeout"

        val d = FaultOriginDistance.distance(p0, p0)

        assertTrue(d == 0.0)
    }

    @Test
    fun initialDifferentPaths(){
        val p0 = "com/foo/rest/examples/spring/openapi/v3/cluster/ClusterApplication_35_timeout"
        val p1 = "com/foo/rest/examples/spring/openapi/v3/cluster/ClusterApplication_28_get"

        val d = FaultOriginDistance.distance(p0, p1)

        assertTrue(d != 0.0)
    }
}