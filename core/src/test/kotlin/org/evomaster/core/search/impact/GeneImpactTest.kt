package org.evomaster.core.search.impact

import org.evomaster.core.search.gene.*
import org.evomaster.core.search.impact.value.ObjectGeneImpact
import org.evomaster.core.search.impact.value.StringGeneImpact
import org.evomaster.core.search.impact.value.collection.CollectionGeneImpact
import org.evomaster.core.search.impact.value.collection.EnumGeneImpact
import org.evomaster.core.search.impact.value.numeric.DoubleGeneImpact
import org.evomaster.core.search.impact.value.numeric.IntegerGeneImpact
import org.evomaster.core.search.impact.value.numeric.LongGeneImpact
import org.junit.jupiter.api.Test

/**
 * created by manzh on 2019-09-11
 */
class GeneImpactTest {

    @Test
    fun testObjectGeneImpact(){
        val f1 = StringGene("string_1")
        val f2 = IntegerGene("integer_2")
        val f3 = DoubleGene("double_3")
        val f4 = LongGene("long_4")
        val f5_1 = StringGene("object_5_string_1")
        val f5_2 = DoubleGene("object_5_double_2")
        val f5 = ObjectGene("object_5", listOf(f5_1, f5_2))
        val f6 = EnumGene("enum_6", listOf("L1", "L2", "L3"), 1)
        val f7_1 = StringGene("map_7_string_1")
        val f7_2 = StringGene("map_7_string_2")
        val f7 = MapGene("map_7", StringGene("map"), 5,  mutableListOf(f7_1, f7_2))

        val objGene = ObjectGene("object", listOf(f1, f2, f3, f4, f5, f6, f7))

        val id = ImpactUtils.generateGeneId(objGene)
        val impact = ImpactUtils.createGeneImpact(objGene, id)

        impact.apply {
            assert(this is ObjectGeneImpact)
            assert((this as ObjectGeneImpact).fields.size == 7)
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

            assert(fields.getValue(f6.name) is EnumGeneImpact)
            assert((fields.getValue(f6.name) as EnumGeneImpact).values.size == f6.values.size)

            assert(fields.getValue(f7.name) is CollectionGeneImpact)
        }

        val tracking0 = objGene.copy()
        f2.value = f2.value + 1

        val hasImpact = true

        val mutatedGeneWithContext = MutatedGeneWithContext(current = objGene, previous =  tracking0, action = "none", position = -1)

        ImpactUtils.processImpact(impact, mutatedGeneWithContext, hasImpact)

        assert(impact.timesOfImpact == 1)
        assert(impact.timesOfNoImpacts == 0)
        assert(impact.timesToManipulate == 1)

        (impact as ObjectGeneImpact).fields.forEach { (t, u) ->
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

        val tracking1 = objGene.copy()
        f5_1.value = if (f5_1.value.length == f5_1.maxLength) f5_1.value.substring(1) else "${f5_1.value}z"
        val mutatedGeneWithContext1 = MutatedGeneWithContext(current = objGene, previous =  tracking1, action = "none", position = -1)
        ImpactUtils.processImpact(impact, mutatedGeneWithContext1, hasImpact = false, countDeepObjectImpact = true)

        assert(impact.timesOfImpact == 1)
        assert(impact.timesOfNoImpacts == 1)
        assert(impact.timesToManipulate == 2)

        impact.fields.forEach { (t, u) ->
            if (t == f2.name){
                assert(u.timesOfImpact == 1)
                assert(u.timesOfNoImpacts == 0)
                assert(u.timesToManipulate == 1)
            }else if(t == f5.name){
                assert(u.timesOfImpact == 0)
                assert(u.timesOfNoImpacts == 1)
                assert(u.timesToManipulate == 1)

                (u as ObjectGeneImpact).fields.forEach { (n, g) ->
                    if (n == f5_1.name){
                        assert(g.timesOfImpact == 0)
                        assert(g.timesOfNoImpacts == 1)
                        assert(g.timesToManipulate == 1)
                    }else{
                        assert(g.timesOfImpact == 0)
                        assert(g.timesOfNoImpacts == 0)
                        assert(g.timesToManipulate == 0)
                    }
                }

            }else{
                assert(u.timesOfImpact == 0)
                assert(u.timesOfNoImpacts == 0)
                assert(u.timesToManipulate == 0)
            }
        }

        val tracking2 = objGene.copy()
        f6.index = 2
        val f7_3 = StringGene("map_7_string_3")
        f7.elements.add(f7_3)

        val mutatedGeneWithContext2 = MutatedGeneWithContext(current = objGene, previous =  tracking2, action = "none", position = -1)
        ImpactUtils.processImpact(impact, mutatedGeneWithContext2, hasImpact = false, countDeepObjectImpact = true)

        assert(impact.timesOfImpact == 1)
        assert(impact.timesOfNoImpacts == 2)
        assert(impact.timesToManipulate == 3)

        impact.fields.forEach { (t, u) ->
            if (t == f2.name){
                assert(u.timesOfImpact == 1)
                assert(u.timesOfNoImpacts == 0)
                assert(u.timesToManipulate == 1)
            }else if(t == f5.name){
                assert(u.timesOfImpact == 0)
                assert(u.timesOfNoImpacts == 1)
                assert(u.timesToManipulate == 1)

                (u as ObjectGeneImpact).fields.forEach { (n, g) ->
                    if (n == f5_1.name){
                        assert(g.timesOfImpact == 0)
                        assert(g.timesOfNoImpacts == 1)
                        assert(g.timesToManipulate == 1)
                    }else{
                        assert(g.timesOfImpact == 0)
                        assert(g.timesOfNoImpacts == 0)
                        assert(g.timesToManipulate == 0)
                    }
                }

            }else if(t == f6.name){
                assert(u.timesOfImpact == 0)
                assert(u.timesOfNoImpacts == 1)
                assert(u.timesToManipulate == 1)

                (u as EnumGeneImpact).values.forEachIndexed { index, g ->
                    if (index == 2){
                        assert(g.timesOfImpact == 0)
                        assert(g.timesOfNoImpacts == 1)
                        assert(g.timesToManipulate == 1)
                    }else{
                        assert(g.timesOfImpact == 0)
                        assert(g.timesOfNoImpacts == 0)
                        assert(g.timesToManipulate == 0)
                    }
                }

            }else if(t == f7.name){
                assert(u.timesOfImpact == 0)
                assert(u.timesOfNoImpacts == 1)
                assert(u.timesToManipulate == 1)

                (u as CollectionGeneImpact).sizeImpact.apply {
                    assert(timesOfImpact == 0)
                    assert(timesOfNoImpacts == 1)
                    assert(timesToManipulate == 1)
                }

            }else{
                assert(u.timesOfImpact == 0)
                assert(u.timesOfNoImpacts == 0)
                assert(u.timesToManipulate == 0)
            }
        }


    }

}