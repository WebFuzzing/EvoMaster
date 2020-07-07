package org.evomaster.core.search.service.mutator.genemutation.archive

import org.evomaster.core.Lazy
import org.evomaster.core.search.gene.GeneIndependenceInfo
import org.evomaster.core.search.gene.StringGene
import org.evomaster.core.search.service.mutator.genemutation.ArchiveGeneMutator
import org.evomaster.core.search.service.mutator.genemutation.IntMutationUpdate

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


        dependencyInfo: GeneIndependenceInfo = GeneIndependenceInfo()) : ArchiveMutationInfo(dependencyInfo){

    /**
     * when [mutatedIndex] = -2, it means that chars of [this] have not be mutated yet
     * when [mutatedIndex] = -1, it means that charsMutation of [this] is initialized
     */
    var mutatedIndex: Int = NEVER_ARCHIVE_MUTATION

    constructor(minLength: Int, maxLength: Int) : this(lengthMutation = IntMutationUpdate(minLength, maxLength))
    constructor(stringGene: StringGene, archiveMutator: ArchiveGeneMutator) : this(lengthMutation = IntMutationUpdate(stringGene.minLength, stringGene.maxLength), charsMutation = (0 until stringGene.value.length).map { archiveMutator.createCharMutationUpdate()}.toMutableList())

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

    fun charUpdate(previous: String, mutated: String, diffIndex: List<Int>, invalidChars: List<Char>, mutatedBetter: Boolean, archiveMutator: ArchiveGeneMutator) {

        diffIndex.forEach {
            val charUpdate = charsMutation[it]

            if (!doInitMutationIndex() && it == 0){
                mutatedIndex = it
            }

            val pchar = previous[it].toInt()
            val mchar = mutated[it].toInt()

            /*
                1) current char is not in min..max, but current is better -> reset
                2) cmutation is optimal, but current is better -> reset
             */
            val reset = mutatedBetter && (
                    mchar !in charUpdate.preferMin..charUpdate.preferMax ||
                            charUpdate.reached
                    )

            if (reset) {
                charUpdate.reset(archiveMutator.getDefaultCharMin(), archiveMutator.getDefaultCharMax())
                plusDependencyInfo()
                return
            }
            charUpdate.updateBoundary(pchar, mchar, mutatedBetter)

            //val exclude = thisValue[it].toInt()
            val excludes = invalidChars.map { it.toInt() }.plus(mchar).plus(pchar).toSet()

            if (0 == archiveMutator.validateCandidates(charUpdate.preferMin, charUpdate.preferMax, exclude = excludes.toList())) {
                charUpdate.reached = true
            }
        }
    }

    fun synCharMutation(value : String, doLengthMutation: Boolean, mutatedBetter: Boolean, archiveMutator: ArchiveGeneMutator){
        if (neverArchiveMutate())
            charMutationInitialized()

        val added = value.length -  charsMutation.size

        if (added > 0) {
            (0 until added).forEach { _ ->
                charsMutation.add(archiveMutator.createCharMutationUpdate())
            }
        } else if(mutatedBetter && doLengthMutation){ // only remove charMutation when mutated is better
            (0 until -added).forEach { _ ->
                charsMutation.removeAt(value.length)
            }
        }
    }

    fun lengthUpdate(previous: String, mutated: String, template: StringGene, mutatedBetter: Boolean, archiveMutator: ArchiveGeneMutator) {

        Lazy.assert {  mutated.length - previous.length != 0 }
        /*
            1) current.length is not in min..max, but current is better -> reset
            2) lengthMutation is optimal, but current is better -> reset
         */
        val reset = mutatedBetter && (
                mutated.length !in lengthMutation.preferMin..lengthMutation.preferMax ||
                        lengthMutation.reached
                )

        if (reset) {
            lengthMutation.reset(template.minLength, template.maxLength)
            plusResetTimes()
            if (doesDependOnOthers()) setDefaultLikelyDependent()
            return
        }
        lengthMutation.updateBoundary(previous.length, mutated.length, mutatedBetter)

        if (0 == archiveMutator.validateCandidates(lengthMutation.preferMin, lengthMutation.preferMax, exclude = setOf(previous.length, mutated.length).toList())) {
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