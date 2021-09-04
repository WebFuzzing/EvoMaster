package org.evomaster.core.search.gene.regex

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.GeneUtils
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class QuantifierRxGene(
        name: String,
        val template: RxAtom,
        val min: Int = 1,
        val max: Int = 1
) : RxTerm(name, listOf(template)) {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(QuantifierRxGene::class.java)
        private const val MODIFY_LENGTH = 0.1
    }


    val atoms = mutableListOf<RxAtom>()

    /**
     *  A * quantifier could lead to billions of atom elements.
     *  Here, to avoid an unnecessary huge search space, we put
     *  a limit on the number of variable elements.
     *  But still constrained by min and max
     */
    private val LIMIT = 2

    val limitedMax: Int

    init {
        if (min < 0) {
            throw IllegalArgumentException("Invalid min value '$min': should be positive")
        }
        if (max < 1) {
            throw IllegalArgumentException("Invalid max value '$max': should be at least 1")
        }
        if (min > max) {
            throw IllegalArgumentException("Invalid min-max values '$min-$max': min is greater than max")
        }

        limitedMax = if ((max - min) > LIMIT) {
            min + LIMIT
        } else {
            max
        }

        if(min == limitedMax && !template.isMutable()){
            /*
                this means this whole gene is immutable. still need to initialize it
             */
            for(i in 0 until min){
                val a = template.copy() as RxAtom
//                a.parent = this
                atoms.add(a)
                addChild(a)
            }
        }
    }


    override fun getChildren(): List<RxAtom> = listOf(template).plus(atoms)

    override fun copyContent(): Gene {

        val copy = QuantifierRxGene(
                name,
                template.copyContent() as RxAtom,
                min,
                max
        )
        copy.atoms.clear()
        this.atoms.forEach {
            val a = it.copyContent() as RxAtom
//            a.parent = copy
            copy.atoms.add(a)
            copy.addChild(a)
        }

        return copy
    }



    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {

        val length = randomness.nextInt(min, limitedMax)

        atoms.clear()

        if (length == 0) {
            //nothing to do
            return
        }

        for (i in 0 until length) {
           addNewAtom(randomness, forceNewValue, allGenes)
        }
        addChildren(atoms)
    }

    override fun isMutable(): Boolean {
        return min != limitedMax || template.isMutable()
    }

    override fun candidatesInternalGenes(randomness: Randomness, apc: AdaptiveParameterControl, allGenes: List<Gene>, selectionStrategy: SubsetGeneSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneMutationInfo?): List<Gene> {
        val length = atoms.size

        return if( length > min  && randomness.nextBoolean(MODIFY_LENGTH)){
            log.trace("Removing atom")
            emptyList()
        } else if(length < limitedMax && randomness.nextBoolean(MODIFY_LENGTH)){
            emptyList()
        } else {
            atoms.filter { it.isMutable() }
        }
    }


    override fun adaptiveSelectSubset(randomness: Randomness, internalGenes: List<Gene>, mwc: MutationWeightControl, additionalGeneMutationInfo: AdditionalGeneMutationInfo): List<Pair<Gene, AdditionalGeneMutationInfo?>> {
        /*
            atoms is dynamically modified, then we do not collect impacts for it now.
            thus for the internal genes, adaptive gene selection for mutation is not applicable
        */
        val s = randomness.choose(internalGenes)
        return listOf(s to additionalGeneMutationInfo.copyFoInnerGene(null, s))
    }

    override fun mutate(randomness: Randomness, apc: AdaptiveParameterControl, mwc: MutationWeightControl, allGenes: List<Gene>, selectionStrategy: SubsetGeneSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneMutationInfo?): Boolean {
        val length = atoms.size

        if (length < min || length > limitedMax)
            throw IllegalArgumentException("invalid length")

        var remove = length == limitedMax
        var add = length == min

        if (remove == add){
            if (add)
                throw IllegalArgumentException("min == limitedMax")

            remove = randomness.nextBoolean()
            add = !remove
        }

        if(remove){
            log.trace("Removing atom")
            atoms.removeAt(randomness.nextInt(length))
        }
        if(add){
            addNewAtom(randomness, false, listOf())
        }

        return true
    }

    fun addNewAtom(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>){
        val base = template.copy() as RxAtom
//        base.parent = this
        if (base.isMutable()) {
            base.randomize(randomness, forceNewValue, allGenes)
        }
        atoms.add(base)
        addChild(base)
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?, extraCheck: Boolean): String {

        return atoms.map { it.getValueAsPrintableString(previousGenes, mode, targetFormat) }
                .joinToString("")
    }

    override fun copyValueFrom(other: Gene) {
        if (other !is QuantifierRxGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }

        if (this.atoms.size == other.atoms.size) {
            //same size, so just copy over the values
            for (i in 0 until other.atoms.size) {
                this.atoms[i].copyValueFrom(other.atoms[i])
            }
        } else {
            //different size, so clear and create new copies
            this.atoms.clear()
            other.atoms.forEach{
                val a = it.copy() as RxAtom
//                a.parent = this
                this.atoms.add(a)
                this.addChild(a)
            }
        }
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is QuantifierRxGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }

        if (this.atoms.size != other.atoms.size) {
            return false
        }

        for (i in 0 until other.atoms.size) {
            if (!this.atoms[i].containsSameValueAs(other.atoms[i])) {
                return false
            }
        }

        return true
    }

    override fun flatView(excludePredicate: (Gene) -> Boolean): List<Gene> {
        return if (excludePredicate(this)) listOf(this)
        else listOf(this).plus(atoms.flatMap { it.flatView(excludePredicate) })
    }

    override fun mutationWeight(): Double {
        return atoms.filter { isMutable() }.map { it.mutationWeight() }.sum() + 1.0
    }

    override fun innerGene(): List<Gene> = atoms

    /*
        Note that value binding cannot be performed on the [atoms]
     */
    override fun bindValueBasedOn(gene: Gene): Boolean {
        if (gene is QuantifierRxGene){
            var result = true
            if(atoms.size == gene.atoms.size){
                atoms.indices.forEach {
                    val r = atoms[it].bindValueBasedOn(gene.atoms[it])
                    if (!r)
                        LoggingUtil.uniqueWarn(log, "value binding for QuantifierRxGene does not perform successfully at index $it")
                    result =  r && result
                }
            }else{
                this.atoms.clear()
                gene.atoms.forEach{
                    val a = it.copy() as RxAtom
//                    a.parent = this
                    this.atoms.add(a)
                }
            }
            return result
        }
        LoggingUtil.uniqueWarn(log, "cannot bind the QuantifierRxGene with ${gene::class.java.simpleName}")
        return false
    }
}