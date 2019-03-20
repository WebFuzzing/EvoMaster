package org.evomaster.core.search.service.impact

import org.evomaster.core.problem.rest.serviceII.ParamHandler
import org.evomaster.core.search.Action
import org.evomaster.core.search.Individual
import org.evomaster.core.search.gene.DisruptiveGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.IntegerGene
import org.evomaster.core.search.gene.OptionalGene


open class Impact (
        val id : String,
        var degree: Double,
        var timesToManipulate : Int = 0,
        var timesOfImpact : Int = 0,
        var timesOfNoImpacts : Int = 0,
        var counter : Int = 0
){
    open fun copy() : Impact{
        return Impact(id, degree, timesToManipulate, timesOfImpact, timesOfNoImpacts, counter)
    }

    fun countImpact(hasImpact:Boolean){
        timesToManipulate++
        if (hasImpact) {
            timesOfImpact++
            resetCounter()
        } else {
            counter++
            timesOfNoImpacts
        }
    }
    fun increaseDegree(delta : Double){
        degree += delta
    }

    private fun resetCounter(){
        counter = 0
    }


}

class ImpactOfGene:Impact{
    companion object {
        const val SEPARATOR_ACTION_TO_GENE = "::"

        fun generateId(action: Action, gene : Gene) : String = "${action.getName()}$SEPARATOR_ACTION_TO_GENE${gene.name}"

        /*
            LinearIndividual
         */
        fun generateId(gene: Gene) : String = geneId(gene)

        fun extractGeneById(actions: List<Action>, id: String) : MutableList<Gene> {
            val names = id.split(SEPARATOR_ACTION_TO_GENE)
            assert(names.size == 2)
            return actions.filter { it.getName() == names[0] }.flatMap { it.seeGenes() }.filter { it.name == names[1] }.toMutableList()
        }

        fun isAnyChange(geneA : Gene, geneB : Gene) : Boolean{
            assert(geneA::class.java.simpleName == geneB::class.java.simpleName)
            return geneA.getValueAsRawString() == geneB.getValueAsRawString()
        }

        fun geneId(gene: Gene):String{
            return when(gene){
                is DisruptiveGene<*> -> gene.name + SEPARATOR_ACTION_TO_GENE + geneId(gene.gene)
                is OptionalGene -> gene.name + SEPARATOR_ACTION_TO_GENE + geneId(gene.gene)
                else -> gene.name
            }
        }


    }

    constructor(id: String, degree: Double, timesToManipulate: Int,timesOfImpact: Int,timesOfNoImpacts: Int, counter: Int) : super(id, degree, timesToManipulate, timesOfImpact, timesOfNoImpacts, counter)

    constructor(id: String, degree: Double) : this(id, degree, 0, 0, 0, 0)

    constructor(action: Action, gene : Gene, degree: Double): this("${action.getName()}$SEPARATOR_ACTION_TO_GENE${gene.name}", degree)


    override fun copy():ImpactOfGene{
        return ImpactOfGene(id, degree, timesToManipulate, timesOfImpact, timesOfNoImpacts, counter)
    }



}

//class ImpactOfAction(action: Action, degree: Double)
//    : Impact(action.getName(), degree){
//
//    companion object {
//        const val SEPARATOR_ACTION_TO_GENE = ":"
//    }
//
//    val impactOfGenes : MutableMap<String, ImpactOfGene> = mutableMapOf()
//
//    init {
//        action.seeGenes().forEach {
//            val key = "${id}$SEPARATOR_ACTION_TO_GENE${it.name}"
//            impactOfGenes.putIfAbsent(key, ImpactOfGene(key, 0.0))
//        }
//    }
//
//}


class ImpactOfStructure :Impact{

    companion object {
        const val SEPARATOR_ACTION = ";"
        fun generateId(individual: Individual) : String = individual.seeActions().map { it.getName() }.joinToString(SEPARATOR_ACTION)

    }

    constructor(id: String, degree: Double, timesToManipulate: Int,timesOfImpact: Int,timesOfNoImpacts: Int, counter: Int) : super(id, degree, timesToManipulate, timesOfImpact, timesOfNoImpacts, counter)
    constructor(id: String, degree: Double) : this(id, degree, 0, 0, 0, 0)
    constructor(individual: Individual, degree: Double)  : this(individual.seeActions().map { it.getName() }.joinToString(SEPARATOR_ACTION), degree)

    override fun copy():ImpactOfStructure{
        return ImpactOfStructure(id, degree, timesToManipulate, timesOfImpact, timesOfNoImpacts,counter)
    }
}