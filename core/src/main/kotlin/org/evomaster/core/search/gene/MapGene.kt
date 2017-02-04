package org.evomaster.core.search.gene

import org.evomaster.core.search.service.Randomness


class MapGene<T>(
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
        return MapGene<T>(name,
                template.copy() as T,
                maxSize,
                elements.map { e -> e.copy() as T }.toMutableList()
        )
    }

    override fun randomize(randomness: Randomness, forceNewValue: Boolean) {

        //maybe not so important here to complicate code to enable forceNewValue

        elements.clear()
        val n = randomness.nextInt(maxSize)
        (0..n - 1).forEach {
            val gene = template.copy() as T
            gene.randomize(randomness, false)
            gene.name = "key_$it"
            elements.add(gene)
        }
    }

    override fun getValueAsString(): String {
        return "{" +
                elements.map { f ->
                    """
                    "${f.name}":${f.getValueAsString()}
                    """
                }.joinToString { "," } +
                "}";
    }
}