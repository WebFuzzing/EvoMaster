package org.evomaster.core.search.impact.value

import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.impact.GeneImpact
import org.evomaster.core.search.impact.ImpactUtils

/**
 * created by manzh on 2019-09-09
 */
class ObjectGeneImpact (
        id : String,
        degree: Double = 0.0,
        timesToManipulate: Int = 0,
        timesOfImpact: Int = 0,
        timesOfNoImpacts: Int = 0,
        counter: Int = 0,
        positionSensitive: Boolean = false,
        val fields : MutableMap<String, GeneImpact> = mutableMapOf()
) : GeneImpact(id, degree, timesToManipulate, timesOfImpact, timesOfNoImpacts, counter, positionSensitive){


    constructor(id: String, objectGene: ObjectGene) : this (id, fields = objectGene.fields.map { Pair(it.name, ImpactUtils.createGeneImpact(it, it.name)) }.toMap().toMutableMap())

    override fun copy(): ObjectGeneImpact {
        return ObjectGeneImpact(id, degree, timesToManipulate, timesOfImpact, timesOfNoImpacts, counter, positionSensitive, fields)
    }

    fun countFieldImpact(mutatedFieldName : String, hasImpact : Boolean){
        fields.getValue(mutatedFieldName).countImpact(hasImpact)
    }

}