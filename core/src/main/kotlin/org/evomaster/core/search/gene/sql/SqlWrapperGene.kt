package org.evomaster.core.search.gene.sql

import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.wrapper.WrapperGene

/**
 * A gene that either might contain other SQL special genes inside,
 * or being one of those genes
 */
interface SqlWrapperGene {

    fun getForeignKey() : SqlForeignKeyGene?
}