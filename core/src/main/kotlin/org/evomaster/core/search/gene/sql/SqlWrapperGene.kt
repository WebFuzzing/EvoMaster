package org.evomaster.core.search.gene.sql

import org.evomaster.core.search.gene.Gene

/**
 * A gene that either might contain other SQL special genes inside,
 * or being one of those genes
 */
abstract class SqlWrapperGene(name: String) : Gene(name){


    abstract fun getForeignKey() : SqlForeignKeyGene?

}