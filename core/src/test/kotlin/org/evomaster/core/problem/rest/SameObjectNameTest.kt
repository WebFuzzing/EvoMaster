package org.evomaster.core.problem.rest

import io.swagger.parser.OpenAPIParser
import org.evomaster.core.problem.rest.builder.RestActionBuilderV3
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.param.BodyParam
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.gene.ObjectGene
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SameObjectNameTest {

    @Test
    fun test(){

        val xyz1 = OpenAPIParser().readLocation("swagger/artificial/xyz1.json", null, null).openAPI
        val xyz1Cluster: MutableMap<String, Action> = mutableMapOf()
        RestActionBuilderV3.addActionsFromSwagger(xyz1, xyz1Cluster, enableConstraintHandling = false)

        assertEquals(1, xyz1Cluster.size)
        val xyz1post = xyz1Cluster["POST:/v1/xyz1"]
        assertTrue(xyz1post is RestCallAction)
        (xyz1post!! as RestCallAction).apply {
            assertEquals(1, parameters.size)
            assertTrue(parameters.first() is BodyParam)
            assertTrue((parameters.first().gene) is ObjectGene)
            (parameters.first().gene as ObjectGene).apply {
                assertEquals(4, fields.size)
                assertEquals("xyzf1", fields[0].name)
                assertEquals("xyzf2", fields[1].name)
                assertEquals("xyzf3", fields[2].name)
                assertEquals("xyzf4", fields[3].name)
            }
        }

        val xyz2 = OpenAPIParser().readLocation("swagger/artificial/xyz2.json", null, null).openAPI
        val xyz2Cluster: MutableMap<String, Action> = mutableMapOf()
        RestActionBuilderV3.addActionsFromSwagger(xyz2, xyz2Cluster, enableConstraintHandling = false)

        assertEquals(1, xyz2Cluster.size)
        val xyz2post = xyz2Cluster["POST:/v2/xyz2"]

        assertTrue(xyz2post is RestCallAction)
        (xyz2post!! as RestCallAction).apply {
            assertEquals(1, parameters.size)
            assertTrue(parameters.first() is BodyParam)
            assertTrue((parameters.first().gene) is ObjectGene)
            (parameters.first().gene as ObjectGene).apply {

//                assertEquals(4, fields.size)
//                assertEquals("xyzf1", fields[0].name)
//                assertEquals("xyzf2", fields[1].name)
//                assertEquals("xyzf3", fields[2].name)
//                assertEquals("xyzf4", fields[3].name)

                // but this should be 3
                assertEquals(3, fields.size)
                assertEquals("f1", fields[0].name)
                assertEquals("f2", fields[1].name)
                assertEquals("f3", fields[2].name)
            }
        }

    }

}