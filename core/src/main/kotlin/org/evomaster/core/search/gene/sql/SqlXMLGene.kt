package org.evomaster.core.search.gene.sql

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.root.CompositeFixedGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.impact.impactinfocollection.sql.SqlXmlGeneImpact
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory

//TODO is this really necessary? or is it just a printing option for ObjetGene?

class SqlXMLGene(name: String,
                 val objectGene: ObjectGene = ObjectGene(name, fields = listOf())
) : CompositeFixedGene(name, mutableListOf(objectGene)) {

    companion object{
        private val log: Logger = LoggerFactory.getLogger(SqlXMLGene::class.java)
    }


    override fun isMutable(): Boolean {
        return objectGene.isMutable()
    }

    override fun checkForLocallyValidIgnoringChildren() : Boolean{
        return true
    }

    override fun copyContent(): Gene = SqlXMLGene(
            name,
            objectGene = this.objectGene.copy() as ObjectGene)


    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        objectGene.randomize(randomness, tryToForceNewValue)
    }


    override fun adaptiveSelectSubsetToMutate(randomness: Randomness, internalGenes: List<Gene>, mwc: MutationWeightControl, additionalGeneMutationInfo: AdditionalGeneMutationInfo): List<Pair<Gene, AdditionalGeneMutationInfo?>> {
        if (additionalGeneMutationInfo.impact != null && additionalGeneMutationInfo.impact is SqlXmlGeneImpact){
            if (internalGenes.size != 1 || !internalGenes.contains(objectGene))
                throw IllegalStateException("mismatched input: the internalGenes should only contain objectGene")
            return listOf(objectGene to additionalGeneMutationInfo.copyFoInnerGene(additionalGeneMutationInfo.impact.geneImpact, objectGene))
        }
        throw IllegalArgumentException("impact is null or not SqlXmlGeneImpact")
    }


    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?, extraCheck: Boolean): String {
        val rawValue = objectGene.getValueAsPrintableString(previousGenes, GeneUtils.EscapeMode.XML , targetFormat)
        when {
            // TODO: refactor with StringGene.getValueAsPrintableString(()
            (targetFormat == null) -> return "\"$rawValue\""
            targetFormat.isKotlin() -> return "\"$rawValue\""
                    .replace("\\", "\\\\")
                    .replace("$", "\\$")
            else -> return "\"$rawValue\""
                    .replace("\\", "\\\\")
        }

    }



    /**
     * Genes might contain a value that is also stored
     * in another gene of the same type.
     */
    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is SqlXMLGene) {
            return false
        }
        /*
         * Edge case: when displayed as XML, the "name" is part of the output.
         * so we should check it. but this is currently passed as parameter to
         * the display function.
         * This is done because, for same body payload, can have same ObjectGene
         * to represent different types (eg JSON and XML)
         * TODO: would need to find a more robust solution to handle this
         */
        if(this.name != other.name) {
            return false
        }
        return this.objectGene.containsSameValueAs(other.objectGene)
    }




    override fun mutationWeight(): Double {
        return  objectGene.mutationWeight()
    }


    override fun getPhenotype(): Gene {
        return objectGene
    }

    override fun unsafeCopyValueFrom(other: Gene): Boolean {

        val gene = other.getPhenotype()

        return when(gene){
            is ObjectGene -> objectGene.unsafeCopyValueFrom(gene)
            else->{
                LoggingUtil.uniqueWarn(log, "cannot bind SqlXMLGene with ${gene::class.java.simpleName}")
                false
            }
        }
    }

    override fun customShouldApplyShallowMutation(
        randomness: Randomness,
        selectionStrategy: SubsetGeneMutationSelectionStrategy,
        enableAdaptiveGeneMutation: Boolean,
        additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ): Boolean {
        return false
    }

}
