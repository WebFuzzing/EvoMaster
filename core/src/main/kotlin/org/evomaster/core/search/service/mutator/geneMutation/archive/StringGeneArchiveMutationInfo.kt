package org.evomaster.core.search.service.mutator.geneMutation.archive

import org.evomaster.core.search.gene.GeneIndependenceInfo
import org.evomaster.core.search.gene.StringGene
import org.evomaster.core.search.service.mutator.geneMutation.ArchiveMutator
import org.evomaster.core.search.service.mutator.geneMutation.IntMutationUpdate

/**
 * created by manzh on 2020-06-12
 */
class StringGeneArchiveMutationInfo(
        /**
         * collect info of mutation on its chars of [value]
         */
        val charsMutation: MutableList<IntMutationUpdate> = mutableListOf(),
        /**
         * collect info of mutation on its length of [value]
         */
        val lengthMutation: IntMutationUpdate,


        dependencyInfo: GeneIndependenceInfo = GeneIndependenceInfo(degreeOfIndependence = ArchiveMutator.WITHIN_NORMAL)) : ArchiveMutationInfo(dependencyInfo){

    /**
     * when [mutatedIndex] = -2, it means that chars of [this] have not be mutated yet
     * when [mutatedIndex] = -1, it means that charsMutation of [this] is initialized
     */
    var mutatedIndex: Int = NEVER_ARCHIVE_MUTATION

    constructor(minLength: Int, maxLength: Int) : this(lengthMutation = IntMutationUpdate(minLength, maxLength))
    constructor(stringGene: StringGene) : this(lengthMutation = IntMutationUpdate(stringGene.minLength, stringGene.maxLength))

    companion object{
        private const val NEVER_ARCHIVE_MUTATION = -2
        private const val CHAR_MUTATION_INITIALIZED = -1

    }

    override fun reachOptimal(): Boolean {
        return lengthMutation.reached && (charsMutation.all { it.reached } || charsMutation.isEmpty())
    }

    override fun copy(): StringGeneArchiveMutationInfo {
        return StringGeneArchiveMutationInfo(charsMutation.map { it.copy() }.toMutableList(), lengthMutation.copy(), dependencyInfo.copy()).also {
            it.mutatedIndex = this.mutatedIndex
        }
    }

    override fun compareTo(other: ArchiveMutationInfo): Int {
        if (other !is StringGeneArchiveMutationInfo)
            throw IllegalArgumentException("should compare with same type")
        // mutation index
        if (mutatedIndex != other.mutatedIndex)
            return mutatedIndex - other.mutatedIndex
        // length range
        if (lengthMutation != other.lengthMutation)
            return lengthMutation.compareTo(other.lengthMutation)
        if (mutatedIndex < 0)
            return 0
        // char range
        return charsMutation[mutatedIndex].compareTo(other.charsMutation[mutatedIndex])
    }

    fun charUpdate(previous: String, current: String, thisValue: String, diffIndex : List<Int>, invalidChars: List<Char>,isMutated : Boolean, mutatedArchiveMutationInfo : StringGeneArchiveMutationInfo, doesCurrentBetter: Boolean, archiveMutator: ArchiveMutator) {

        diffIndex.forEach {
            val charUpdate = charsMutation[it]
            if (!isMutated && mutatedArchiveMutationInfo.charsMutation.getOrNull(it)?.reached == true) {
                charUpdate.reached =
                        mutatedArchiveMutationInfo.charsMutation[it].reached
            }
            if (!doInitMutationIndex() && it == 0){
                mutatedIndex = it
            }

            val pchar = previous[it].toInt()
            val cchar = current[it].toInt()

            /*
                1) current char is not in min..max, but current is better -> reset
                2) cmutation is optimal, but current is better -> reset
             */
            val reset = doesCurrentBetter && (
                    cchar !in charUpdate.preferMin..charUpdate.preferMax ||
                            charUpdate.reached
                    )

            if (reset) {
                charUpdate.reset(archiveMutator.getDefaultCharMin(), archiveMutator.getDefaultCharMax())
                plusDependencyInfo()
                return
            }
            charUpdate.updateBoundary(pchar, cchar, doesCurrentBetter)

            //val exclude = thisValue[it].toInt()
            val excludes = invalidChars.map { it.toInt() }.plus(cchar).plus(pchar).toSet()

            if (0 == archiveMutator.validateCandidates(charUpdate.preferMin, charUpdate.preferMax, exclude = excludes.toList())) {
                charUpdate.reached = true
            }
        }
    }

    fun lengthUpdate(previous: String, current: String, thisGene: StringGene, doesCurrentBetter: Boolean, archiveMutator: ArchiveMutator) {
        //update charsMutation regarding value
        val added = thisGene.value.length - charsMutation.size
        if (added != 0) {
            if (added > 0) {
                (0 until added).forEach { _ ->
                    charsMutation.add(archiveMutator.createCharMutationUpdate())
                }
            } else {
                (0 until -added).forEach { _ ->
                    charsMutation.removeAt(thisGene.value.length)
                }
            }
        }

        if (thisGene.value.length != charsMutation.size) {
            throw IllegalArgumentException("invalid!")
        }
        /*
            1) current.length is not in min..max, but current is better -> reset
            2) lengthMutation is optimal, but current is better -> reset
         */
        val reset = doesCurrentBetter && (
                current.length !in lengthMutation.preferMin..lengthMutation.preferMax ||
                        lengthMutation.reached
                )

        if (reset) {
            lengthMutation.reset(thisGene.minLength, thisGene.maxLength)
            plusResetTimes()
            if (doesDependOnOthers()) setDefaultLikelyDependent()
            return
        }
        lengthMutation.updateBoundary(previous.length, current.length, doesCurrentBetter)

        if (0 == archiveMutator.validateCandidates(lengthMutation.preferMin, lengthMutation.preferMax, exclude = setOf(previous.length, current.length, thisGene.value.length).toList())) {
            lengthMutation.reached = true
        }
    }

    /************ mutatedIndex handling **************/
    fun charMutationInitialized() {
        mutatedIndex = CHAR_MUTATION_INITIALIZED
    }

    fun doInitMutationIndex() : Boolean{
        return mutatedIndex >= 0
    }

    fun doAnyMutation() : Boolean = mutatedIndex >= CHAR_MUTATION_INITIALIZED

    fun neverArchiveMutate() : Boolean = mutatedIndex == NEVER_ARCHIVE_MUTATION

}