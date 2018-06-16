package org.evomaster.core.search.gene

import org.evomaster.core.search.service.Randomness


class SqlAutoIncrementGene(name: String) : Gene(name){

    override fun copy(): Gene {
        return SqlAutoIncrementGene(name)
    }

    override fun randomize(randomness: Randomness, forceNewValue: Boolean) {
        throw IllegalStateException("AutoIncrement fields are not part of the search")
    }

    override fun getValueAsPrintableString(): String {
        throw IllegalStateException("AutoIncrement fields should never be printed")
    }

    override fun copyValueFrom(other: Gene) {
        //do nothing
    }

    override fun isMutable() = false

    override fun isPrintable() = false
}