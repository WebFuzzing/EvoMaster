package org.evomaster.core.search.gene.collection

import org.evomaster.core.Lazy
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.gene.optional.FlexibleGene
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * This represents a MapGene whose key and value have fixed type, ie, not [FlexibleGene]
 * all elements must follow its [template],
 * for instance, if the value template is [ObjectGene], all added elements must be [ObjectGene]
 * and have same fields as the value template
 */
class FixedMapGene<K, V>(
    name: String,
    template: PairGene<K, V>,
    maxSize: Int? = null,
    minSize: Int? = null,
    elements: MutableList<PairGene<K, V>> = mutableListOf()
) : MapGene<K, V>(name, template, maxSize, minSize, elements)
        where K : Gene, V: Gene {

    constructor(name : String, key: K, value: V, maxSize: Int? = null, minSize: Int? = null): this(name,
        PairGene("template", key, value), maxSize, minSize)


    companion object{
        private val log: Logger = LoggerFactory.getLogger(FixedMapGene::class.java)
    }

    override fun copyContent(): Gene {
        return FixedMapGene(
            name,
            template.copy() as PairGene<K, V>,
            maxSize,
            minSize,
            elements.map { e -> e.copy() as PairGene<K, V> }.toMutableList()
        )
    }

    override fun copyValueFrom(other: Gene): Boolean {
        if (other !is FixedMapGene<*, *>) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }

        return updateValueOnlyIfValid({
            killAllChildren()
            // maxSize
            val copy = (if (maxSize!=null && other.elements.size > maxSize!!)
                other.elements.subList(0, maxSize!!)
            else other.elements)
                .map { e -> e.copy() as PairGene<K, V> }
                .toMutableList()
            addChildren(copy)
            true
        },false)
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is FixedMapGene<*, *>) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.elements.size == other.elements.size
                && this.elements.zip(other.elements) { thisElem, otherElem ->
            thisElem.containsSameValueAs(otherElem)
        }.all { it }
    }

    override fun isPrintable(): Boolean {
        return isPrintable(template) && getViewOfChildren().all { it.isPrintable() }
    }


    /*
        Note that value binding cannot be performed on the [elements]
     */
    override fun setValueBasedOn(gene: Gene): Boolean {
        if(gene is FixedMapGene<*, *> && gene.template::class.java.simpleName == template::class.java.simpleName){
            killAllChildren()
            val elements = gene.elements.mapNotNull { it.copy() as? PairGene<K, V> }.toMutableList()
            elements.forEach { it.resetLocalIdRecursively() }
            addChildren(elements)
            return true
        }
        LoggingUtil.uniqueWarn(
            log,
            "cannot bind the MapGene with the template (${template::class.java.simpleName}) with the gene ${gene::class.java.simpleName}"
        )
        return false
    }
}