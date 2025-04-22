package org.evomaster.core.search.gene.collection

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.optional.FlexibleGene
import org.evomaster.core.search.gene.optional.FlexibleGene.Companion.wrapWithFlexibleGene

/**
 * This represents a MapGene whose values do not follow a specific gene type, ie [FlexibleGene],
 * however, its key has the fixed type
 *
 * when adding elements into the map, we do not restrict values of the element, it could have
 * a different type with [template].
 * For instance, if the value template is FlexibleGene attached with [StringGene], a FlexibleGene
 * attached with [ObjectGene] is accepted to be added into this map. but for [FixedMapGene], this
 * case is not acceptable
 */
class FlexibleMapGene<T>(
    name: String,
    template: PairGene<T, FlexibleGene>,
    maxSize: Int? = null,
    minSize: Int? = null,
    elements: MutableList<PairGene<T, FlexibleGene>> = mutableListOf()
) : MapGene<T, FlexibleGene>(name, template, maxSize, minSize, elements)
where T : Gene {

    constructor(name : String, key: T, value: Gene, valueClasses : List<Class<*>>?, maxSize: Int? = null, minSize: Int? = null): this(name,
        PairGene("template", key, wrapWithFlexibleGene(value, valueClasses)), maxSize, minSize)

    override fun copyContent(): Gene {
        return FlexibleMapGene(
            name,
            template.copy() as PairGene<T, FlexibleGene>,
            maxSize,
            minSize,
            elements.map { e -> e.copy() as PairGene<T, FlexibleGene> }.toMutableList()
        )
    }

    override fun copyValueFrom(other: Gene): Boolean {
        //TODO

        return false
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is FlexibleMapGene<*>) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.elements.size == other.elements.size
                && this.elements.zip(other.elements) { thisElem, otherElem ->
            thisElem.containsSameValueAs(otherElem)
        }.all { it }
    }

    override fun setValueBasedOn(gene: Gene): Boolean {
        //TODO
        return false
    }

    override fun isPrintable(): Boolean {
        return elements.all { it.isPrintable() }
    }

}