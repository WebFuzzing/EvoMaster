package org.evomaster.core.search.gene.regex

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.root.CompositeGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class QuantifierRxGene(
        name: String,
        val template: Gene,
        val min: Int = 1,
        val max: Int = 1
) : RxTerm, CompositeGene(name, mutableListOf()) {

    init{
        if(template !is RxAtom){
            throw IllegalArgumentException("Template must be a RxAtom")
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(QuantifierRxGene::class.java)
        private const val MODIFY_LENGTH = 0.1
    }


    val atoms : List<Gene>
        get() {return getViewOfChildren() }

    /**
     *  A * quantifier could lead to billions of atom elements.
     *  Here, to avoid an unnecessary huge search space, we put
     *  a limit on the number of variable elements.
     *  But still constrained by min and max
     */
    private val LIMIT = 2

    private val limitedMax: Int

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
                val a = template.copy() as Gene
                a.resetLocalIdRecursively()
                addChild(a)
            }
        }
    }

    override fun checkForLocallyValidIgnoringChildren() : Boolean{
        val n = getViewOfChildren().size
        return n in min..limitedMax
    }

    override fun copyContent(): Gene {

        val copy = QuantifierRxGene(
                name,
                template.copy(),
                min,
                max
        )
        copy.killAllChildren()

        this.atoms.forEach {
            val a = it.copy()
            copy.addChild(a)
        }

        return copy
    }



    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {

        val length = randomness.nextInt(min, limitedMax)

        killAllChildren()

        if (length == 0) {
            //nothing to do
            return
        }

        for (i in 0 until length) {
           addNewAtom(randomness, tryToForceNewValue)
        }
    }

    override fun isMutable(): Boolean {
        return min != limitedMax || template.isMutable()
    }

    override fun customShouldApplyShallowMutation(randomness: Randomness,
                                                  selectionStrategy: SubsetGeneMutationSelectionStrategy,
                                                  enableAdaptiveGeneMutation: Boolean,
                                                  additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ) : Boolean {
        val length = atoms.size

        if(length > min  && randomness.nextBoolean(MODIFY_LENGTH)){
            return true
        }
        if(length < limitedMax && randomness.nextBoolean(MODIFY_LENGTH)){
            return true
        }
        return false
    }

    override fun adaptiveSelectSubsetToMutate(randomness: Randomness, internalGenes: List<Gene>, mwc: MutationWeightControl, additionalGeneMutationInfo: AdditionalGeneMutationInfo): List<Pair<Gene, AdditionalGeneMutationInfo?>> {
        /*
            atoms is dynamically modified, then we do not collect impacts for it now.
            thus for the internal genes, adaptive gene selection for mutation is not applicable
        */
        val s = randomness.choose(internalGenes)
        return listOf(s to additionalGeneMutationInfo.copyFoInnerGene(null, s))
    }

    override fun shallowMutate(randomness: Randomness, apc: AdaptiveParameterControl, mwc: MutationWeightControl, selectionStrategy: SubsetGeneMutationSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneMutationInfo?): Boolean {
        val length = atoms.size

        if (length < min || length > limitedMax){
            val regexGene = getFirstParent { p -> p is RegexGene } as? RegexGene
            throw IllegalArgumentException("invalid length when mutating Gene ($name, based on regular expression[${regexGene?.name ?: "none of its parent is RegexGene"}, ${regexGene?.sourceRegex?:"expression is null"}]). Its length should range from $min to $limitedMax, but it is $length")
        }


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
            killChildByIndex(randomness.nextInt(length))
        }
        if(add){
            addNewAtom(randomness, false)
        }

        return true
    }

    fun addNewAtom(randomness: Randomness, forceNewValue: Boolean){
        val base = template.copy()
        base.resetLocalIdRecursively()
        base.doInitialize(randomness)
        addChild(base)
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?, extraCheck: Boolean): String {

        return atoms.map { it.getValueAsPrintableString(previousGenes, mode, targetFormat) }
                .joinToString("")
    }

    override fun copyValueFrom(other: Gene): Boolean {
        if (other !is QuantifierRxGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }

        return updateValueOnlyIfValid(
            {
                if (this.atoms.size == other.atoms.size) {
                    //same size, so just copy over the values
                    var ok = true
                    for (i in 0 until other.atoms.size) {
                        ok = ok && this.atoms[i].copyValueFrom(other.atoms[i])
                    }
                    ok
                } else {
                    //different size, so clear and create new copies
                    this.killAllChildren()
                    other.atoms.forEach{
                        val a = it.copy()
                        a.resetLocalIdRecursively()
                        this.addChild(a)
                    }
                    true
                }
            }, true
        )
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



    override fun mutationWeight(): Double {
        return atoms.filter { isMutable() }.map { it.mutationWeight() }.sum() + 1.0
    }


    /*
        Note that value binding cannot be performed on the [atoms]
     */
    override fun setValueBasedOn(gene: Gene): Boolean {
        if (gene is QuantifierRxGene){
            var result = true
            if(atoms.size == gene.atoms.size){
                atoms.indices.forEach {
                    val r = atoms[it].setValueBasedOn(gene.atoms[it])
                    if (!r)
                        LoggingUtil.uniqueWarn(log, "value binding for QuantifierRxGene does not perform successfully at index $it")
                    result =  r && result
                }
            }else{
                this.killAllChildren()
                gene.atoms.forEach{
                    val a = it.copy()
                    a.resetLocalIdRecursively()
                    this.addChild(a)
                }
            }
            return result
        }
        LoggingUtil.uniqueWarn(log, "cannot bind the QuantifierRxGene with ${gene::class.java.simpleName}")
        return false
    }
}