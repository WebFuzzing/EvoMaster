package org.evomaster.core.search.impact.value.date

import org.evomaster.core.search.gene.DateGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.TimeGene
import org.evomaster.core.search.impact.GeneImpact
import org.evomaster.core.search.impact.ImpactUtils
import org.evomaster.core.search.impact.MutatedGeneWithContext
import org.evomaster.core.search.impact.value.numeric.IntegerGeneImpact

/**
 * created by manzh on 2019-09-16
 */
class TimeGeneImpact(
        id : String,
        degree: Double = 0.0,
        timesToManipulate : Int = 0,
        timesOfImpact : Int = 0,
        timesOfNoImpacts : Int = 0,
        counter : Int = 0,
        val hourGeneImpact: IntegerGeneImpact,
        val minuteGeneImpact: IntegerGeneImpact,
        val secondGeneImpact : IntegerGeneImpact
) : GeneImpact(id, degree, timesToManipulate, timesOfImpact, timesOfNoImpacts, counter) {

    constructor(id: String, gene : TimeGene)
            : this(id,
            hourGeneImpact = ImpactUtils.createGeneImpact(gene.hour, gene.hour.name) as? IntegerGeneImpact ?:throw IllegalStateException("IntegerGeneImpact should be created"),
            minuteGeneImpact = ImpactUtils.createGeneImpact(gene.minute, gene.minute.name)as? IntegerGeneImpact ?:throw IllegalStateException("IntegerGeneImpact should be created"),
            secondGeneImpact = ImpactUtils.createGeneImpact(gene.second, gene.second.name) as? IntegerGeneImpact ?:throw IllegalStateException("IntegerGeneImpact should be created")
    )

    override fun copy(): TimeGeneImpact {
        return TimeGeneImpact(id, degree, timesToManipulate, timesOfImpact, timesOfNoImpacts, counter, hourGeneImpact, minuteGeneImpact, secondGeneImpact)
    }

    override fun validate(gene: Gene): Boolean = gene is TimeGene

    fun countTimeImpact(previous: TimeGene, current : TimeGene, hasImpact: Boolean){
        if (!current.hour.containsSameValueAs(previous.hour))
            hourGeneImpact.countImpact(hasImpact)
        if (!current.minute.containsSameValueAs(previous.minute))
            minuteGeneImpact.countImpact(hasImpact)
        if (!current.second.containsSameValueAs(previous.second))
            secondGeneImpact.countImpact(hasImpact)
    }
}