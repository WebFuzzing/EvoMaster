package org.evomaster.core.search.impact

import org.evomaster.core.search.gene.*
import org.evomaster.core.search.impact.value.ObjectGeneImpact
import org.evomaster.core.search.impact.value.OptionalGeneImpact
import org.evomaster.core.search.impact.value.StringGeneImpact
import org.evomaster.core.search.impact.value.numeric.DoubleGeneImpact
import org.evomaster.core.search.impact.value.numeric.IntegerGeneImpact
import org.evomaster.core.search.impact.value.numeric.LongGeneImpact
import org.junit.jupiter.api.Test

/**
 * created by manzh on 2019-09-11
 */
class OptionalGeneImpactTest {

    @Test
    fun testObjectGeneImpact(){
        val f1 = StringGene("string_1")
        val f2 = IntegerGene("integer_2")
        val f3 = DoubleGene("double_3")
        val f4 = LongGene("long_4")
        val f5_1 = StringGene("object_5_string_1")
        val f5_2 = DoubleGene("object_5_double_2")
        val f5 = ObjectGene("object_5", listOf(f5_1, f5_2))

        val objGene = ObjectGene("object", listOf(f1, f2, f3, f4, f5))

        val optionalGene = OptionalGene("optional", objGene, false)

        val id = ImpactUtils.generateGeneId(optionalGene)
        val impact = ImpactUtils.createGeneImpact(optionalGene, id)

        assert(impact is OptionalGeneImpact)

        (impact as OptionalGeneImpact).geneImpact.apply {
            assert(this is ObjectGeneImpact)
            assert((this as ObjectGeneImpact).fields.size == 5)
            assert(fields.keys.zip(objGene.fields.map { it.name }).all {
                it.first == it.second
            })
            assert(fields.getValue(f1.name) is StringGeneImpact)
            assert(fields.getValue(f2.name) is IntegerGeneImpact)
            assert(fields.getValue(f3.name) is DoubleGeneImpact)
            assert(fields.getValue(f4.name) is LongGeneImpact)
            assert(fields.getValue(f5.name) is ObjectGeneImpact)

            assert((fields.getValue(f5.name) as ObjectGeneImpact).fields.getValue(f5_1.name) is StringGeneImpact)
            assert((fields.getValue(f5.name) as ObjectGeneImpact).fields.getValue(f5_2.name) is DoubleGeneImpact)
        }

        val previous = optionalGene.copy()
        f2.value = f2.value + 1
        optionalGene.isActive = false

        val hasImpact = true

        val mutatedGeneWithContext = MutatedGeneWithContext(current = optionalGene, previous =  previous, action = "action", position = 0)

        ImpactUtils.processImpact(impact, mutatedGeneWithContext, hasImpact)

        assert(impact.timesOfImpact == 1)
        assert(impact.timesOfNoImpacts == 0)
        assert(impact.timesToManipulate == 1)

        impact.activeImpact._true.apply {
            assert(timesOfImpact == 0)
            assert(timesOfNoImpacts == 0)
            assert(timesToManipulate == 0)
        }

        impact.activeImpact._false.apply {
            assert(timesOfImpact == 1)
            assert(timesOfNoImpacts == 0)
            assert(timesToManipulate == 1)
        }

        (impact.geneImpact as ObjectGeneImpact).fields.forEach { (t, u) ->
            if (t == f2.name){
                assert(u.timesOfImpact == 1)
                assert(u.timesOfNoImpacts == 0)
                assert(u.timesToManipulate == 1)
            }else{
                assert(u.timesOfImpact == 0)
                assert(u.timesOfNoImpacts == 0)
                assert(u.timesToManipulate == 0)
            }
        }
    }
}