package org.evomaster.core.search.gene

import org.evomaster.core.search.service.Randomness


class ArrayGene<T>(
        name: String,
        val template: T,
        val maxSize: Int = 5,
        var elements: MutableList<T> = mutableListOf()
) : Gene(name)
where T : Gene {

    init {
        if (elements.size > maxSize) {
            throw IllegalArgumentException(
                    "More elements (${elements.size}) than allowed ($maxSize)")
        }
    }

    override fun copy(): Gene {
        return ArrayGene<T>(name,
                template.copy() as T,
                maxSize,
                elements.map { e -> e.copy() as T }.toMutableList()
        )
    }

    override fun copyValueFrom(other: Gene) {
        if (other !is ArrayGene<*>) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        this.elements = other.elements.map { e -> e.copy() as T }.toMutableList()
    }

    override fun randomize(randomness: Randomness, forceNewValue: Boolean) {

        //maybe not so important here to complicate code to enable forceNewValue

        elements.clear()
        val n = randomness.nextInt(maxSize)
        (0..n - 1).forEach {
            val gene = template.copy() as T
            gene.randomize(randomness, false)
            elements.add(gene)
        }
    }

    override fun getValueAsPrintableString(): String {
        return "[" +
                elements.map { g -> g.getValueAsPrintableString() }.joinToString(", ") +
                "]";
    }

    override fun flatView(): List<Gene>{
        return listOf(this).plus(elements.flatMap { g -> g.flatView() })
    }
}