package org.evomaster.core.search.impact.value.date

import org.evomaster.core.search.gene.DateTimeGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.impact.GeneImpact
import org.evomaster.core.search.impact.ImpactUtils

/**
 * created by manzh on 2019-09-16
 */

class DateTimeGeneImpact (
        id : String,
        degree: Double = 0.0,
        timesToManipulate : Int = 0,
        timesOfImpact : Int = 0,
        timesOfNoImpacts : Int = 0,
        counter : Int = 0,
        val dateGeneImpact: DateGeneImpact,
        val timeGeneImpact: TimeGeneImpact
) : GeneImpact(id, degree, timesToManipulate, timesOfImpact, timesOfNoImpacts, counter) {

    constructor(id: String, gene : DateTimeGene)
            : this(id,
            dateGeneImpact = ImpactUtils.createGeneImpact(gene.date, gene.date.name) as? DateGeneImpact ?:throw IllegalStateException("IntegerGeneImpact should be created"),
            timeGeneImpact = ImpactUtils.createGeneImpact(gene.time, gene.time.name)as? TimeGeneImpact ?:throw IllegalStateException("IntegerGeneImpact should be created")
    )

    override fun copy(): DateTimeGeneImpact {
        return DateTimeGeneImpact(id, degree, timesToManipulate, timesOfImpact, timesOfNoImpacts, counter,dateGeneImpact, timeGeneImpact)
    }

    override fun validate(gene: Gene): Boolean = gene is DateTimeGene

    fun countDateTimeImpact(previous: DateTimeGene, current : DateTimeGene, hasImpact: Boolean){
        if (!current.date.containsSameValueAs(previous.date)){
            dateGeneImpact.countImpact(hasImpact)
            dateGeneImpact.countDateImpact(previous = previous.date, current = current.date, hasImpact = hasImpact)
        }
        if (!current.time.containsSameValueAs(previous.time)){
            timeGeneImpact.countImpact(hasImpact)
            timeGeneImpact.countTimeImpact(previous = previous.time, current = current.time, hasImpact = hasImpact)
        }
    }
}