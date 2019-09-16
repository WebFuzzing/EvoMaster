package org.evomaster.core.search.impact.value.date

import org.evomaster.core.search.gene.DateGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.impact.GeneImpact
import org.evomaster.core.search.impact.ImpactUtils
import org.evomaster.core.search.impact.value.numeric.IntegerGeneImpact

/**
 * created by manzh on 2019-09-09
 */
class DateGeneImpact (
        id : String,
        degree: Double = 0.0,
        timesToManipulate : Int = 0,
        timesOfImpact : Int = 0,
        timesOfNoImpacts : Int = 0,
        counter : Int = 0,
        val yearGeneImpact: IntegerGeneImpact,
        val monthGeneImpact: IntegerGeneImpact,
        val dayGeneImpact : IntegerGeneImpact
) : GeneImpact(id, degree, timesToManipulate, timesOfImpact, timesOfNoImpacts, counter) {

    constructor(id: String, gene : DateGene)
            : this(id,
            yearGeneImpact = ImpactUtils.createGeneImpact(gene.year, gene.year.name) as? IntegerGeneImpact?:throw IllegalStateException("IntegerGeneImpact should be created"),
            monthGeneImpact = ImpactUtils.createGeneImpact(gene.month, gene.month.name)as? IntegerGeneImpact?:throw IllegalStateException("IntegerGeneImpact should be created"),
            dayGeneImpact = ImpactUtils.createGeneImpact(gene.day, gene.day.name) as? IntegerGeneImpact?:throw IllegalStateException("IntegerGeneImpact should be created")
    )

    override fun copy(): DateGeneImpact {
        return DateGeneImpact(id, degree, timesToManipulate, timesOfImpact, timesOfNoImpacts, counter, yearGeneImpact, monthGeneImpact, dayGeneImpact)
    }

    override fun validate(gene: Gene): Boolean = gene is DateGene

    fun countDateImpact(previous: DateGene, current : DateGene, hasImpact: Boolean){
        if (!current.year.containsSameValueAs(previous.year))
            yearGeneImpact.countImpact(hasImpact)
        if (!current.month.containsSameValueAs(previous.month))
            monthGeneImpact.countImpact(hasImpact)
        if (!current.day.containsSameValueAs(previous.day))
            dayGeneImpact.countImpact(hasImpact)
    }
}