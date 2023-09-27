package org.evomaster.core.search


import org.evomaster.client.java.controller.api.dto.BootTimeInfoDto
import org.evomaster.client.java.controller.api.dto.TargetInfoDto
import org.evomaster.client.java.instrumentation.shared.ObjectiveNaming
import org.evomaster.core.search.service.IdMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class FitnessValueTest {


    @Test
    fun testUnionWithBootTimeCoveredTargets(){

        val idMapper = IdMapper().apply {
            addMapping(0, "Line_at_com.foo.rest.examples.spring.postcollection.CreateDto_00007")
            addMapping(1, "Class_com.foo.rest.examples.spring.postcollection.CreateDto")
            addMapping(2, "Success_Call_at_com.foo.rest.examples.spring.postcollection.CreateDto_00007_0")
            addMapping(3, "Line_at_com.foo.rest.examples.spring.postcollection.CreateDto_00008")
            addMapping(4, "Line_at_com.foo.rest.examples.spring.postcollection.PostCollectionRest_00025")
            addMapping(5, "Branch_at_com.foo.rest.examples.spring.postcollection.PostCollectionRest_at_line_00025_position_0_falseBranch")
            addMapping(6, "Branch_at_com.foo.rest.examples.spring.postcollection.PostCollectionRest_at_line_00025_position_0_trueBranch")
            addMapping(7, "Line_at_com.foo.rest.examples.spring.postcollection.PostCollectionRest_00026")
            addMapping(8, "Success_Call_at_com.foo.rest.examples.spring.postcollection.PostCollectionRest_00026_0")
            addMapping(9, "Line_at_com.foo.rest.examples.spring.postcollection.PostCollectionRest_00029")
            addMapping(10, "Success_Call_at_com.foo.rest.examples.spring.postcollection.PostCollectionRest_00029_0")
            addMapping(11, "Success_Call_at_com.foo.rest.examples.spring.postcollection.PostCollectionRest_00029_1")
            addMapping(-2, "201:POST:/api/pc")
            addMapping(-3, "HTTP_SUCCESS:POST:/api/pc")
            addMapping(-4, "HTTP_FAULT:POST:/api/pc")
            addMapping(15, "Line_at_com.foo.rest.examples.spring.postcollection.PostCollectionRest_00039")
            addMapping(16, "Success_Call_at_com.foo.rest.examples.spring.postcollection.PostCollectionRest_00039_0")
            addMapping(17, "Branch_at_com.foo.rest.examples.spring.postcollection.PostCollectionRest_at_line_00039_position_0_falseBranch")
            addMapping(18, "Branch_at_com.foo.rest.examples.spring.postcollection.PostCollectionRest_at_line_00039_position_0_trueBranch")
            addMapping(19, "Line_at_com.foo.rest.examples.spring.postcollection.PostCollectionRest_00040")
            addMapping(20, "Success_Call_at_com.foo.rest.examples.spring.postcollection.PostCollectionRest_00040_0")
            addMapping(21, "Success_Call_at_com.foo.rest.examples.spring.postcollection.PostCollectionRest_00040_1")
            addMapping(-5, "400:GET:/api/pc")
            addMapping(-6, "HTTP_SUCCESS:GET:/api/pc")
            addMapping(-7, "HTTP_FAULT:GET:/api/pc")
            addMapping(25, "PotentialFault_PartialOracle_CodeOracle GET:/api/pc")
            addMapping(26, "Line_at_com.foo.rest.examples.spring.postcollection.PostCollectionRest_00043")
            addMapping(27, "Success_Call_at_com.foo.rest.examples.spring.postcollection.PostCollectionRest_00043_0")
            addMapping(28, "Line_at_com.foo.rest.examples.spring.postcollection.ValuesDto_00006")
            addMapping(29, "Class_com.foo.rest.examples.spring.postcollection.ValuesDto")
            addMapping(30, "Success_Call_at_com.foo.rest.examples.spring.postcollection.ValuesDto_00006_0")
            addMapping(31, "Line_at_com.foo.rest.examples.spring.postcollection.ValuesDto_00008")
            addMapping(32, "Success_Call_at_com.foo.rest.examples.spring.postcollection.ValuesDto_00008_0")
            addMapping(33, "Line_at_com.foo.rest.examples.spring.postcollection.PostCollectionRest_00044")
            addMapping(34, "Success_Call_at_com.foo.rest.examples.spring.postcollection.PostCollectionRest_00044_0")
            addMapping(35, "Line_at_com.foo.rest.examples.spring.postcollection.PostCollectionRest_00046")
            addMapping(36, "Success_Call_at_com.foo.rest.examples.spring.postcollection.PostCollectionRest_00046_0")
            addMapping(-8, "200:GET:/api/pc")
            addMapping(-9, "200:GET:/v2/api-docs")
            addMapping(-10, "HTTP_SUCCESS:GET:/v2/api-docs")
            addMapping(-11, "HTTP_FAULT:GET:/v2/api-docs")
            addMapping(-12, "PotentialFault_PartialOracle_CodeOracle GET:/v2/api-docs")
        }

        val fv = FitnessValue(1.0)
        fv.coverTarget(0) //line
        fv.coverTarget(1)
        fv.coverTarget(2)
        fv.coverTarget(3) //line
        fv.coverTarget(4) //line
        fv.coverTarget(-12)

        assertEquals(6, fv.coveredTargets())

        var linesInfo = fv.unionWithBootTimeCoveredTargets(ObjectiveNaming.LINE, idMapper, null)
        assertEquals(3, linesInfo.total)

        var bootTimeInfoDto = BootTimeInfoDto().apply {
            targets = listOf(
                    TargetInfoDto().apply{//new
                        id = 35
                        descriptiveId = "Line_at_com.foo.rest.examples.spring.postcollection.PostCollectionRest_00046"
                        value = 1.0
                        actionIndex = -1
                    },
                    TargetInfoDto().apply{//other
                        id = 36
                        descriptiveId = "Success_Call_at_com.foo.rest.examples.spring.postcollection.PostCollectionRest_00046_0"
                        value = 1.0
                        actionIndex = -1
                    }
            )
        }

        linesInfo = fv.unionWithBootTimeCoveredTargets(ObjectiveNaming.LINE, idMapper, bootTimeInfoDto)
        assertEquals(4, linesInfo.total)
        assertEquals(1, linesInfo.bootTime)
        assertEquals(3, linesInfo.searchTime)

        bootTimeInfoDto = BootTimeInfoDto().apply {
            targets = listOf(
                    TargetInfoDto().apply{//duplicate
                        id = 0
                        descriptiveId = "Line_at_com.foo.rest.examples.spring.postcollection.CreateDto_00007"
                        value = 1.0
                        actionIndex = -1
                    },
                    TargetInfoDto().apply{//new
                        id = 35
                        descriptiveId = "Line_at_com.foo.rest.examples.spring.postcollection.PostCollectionRest_00046"
                        value = 1.0
                        actionIndex = -1
                    },
                    TargetInfoDto().apply{//other
                        id = 36
                        descriptiveId = "Success_Call_at_com.foo.rest.examples.spring.postcollection.PostCollectionRest_00046_0"
                        value = 1.0
                        actionIndex = -1
                    }
            )
        }

        linesInfo = fv.unionWithBootTimeCoveredTargets(ObjectiveNaming.LINE, idMapper, bootTimeInfoDto)
        assertEquals(4, linesInfo.total)
        assertEquals(2, linesInfo.bootTime)
        assertEquals(3, linesInfo.searchTime)
    }

}