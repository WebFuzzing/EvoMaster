package org.evomaster.core.search.service

import com.webfuzzing.commons.faults.FaultCategory
import org.evomaster.client.java.instrumentation.shared.ObjectiveNaming
import java.util.concurrent.atomic.AtomicInteger


/**
 * To represent and identify a coverage target, we use numeric ids.
 * But those are not very "descriptive" of what the targets actually are.
 * So, we need a mapping to a String description (which itself would be
 * a unique id).
 * Note: we do not use these strings directly as it would be too inefficient.
 * Furthermore, as the ids are passed as query parameters in HTTP GET requests,
 * there are limits on length
 */
class IdMapper {

    companion object {

        private const val FAULT_OBJECTIVE_PREFIX = "PotentialFault"

        /**
         * local objective prefix might depend on problems, eg, HTTP_SUCCESS and HTTP_FAULT for REST
         * it can be identified with its numeric id, ie, less than 0
         * however we need this key to specify whether to consider such objectives in impact collections
         */
        const val LOCAL_OBJECTIVE_KEY = "Local"

        /**
         * all prefixes used for defining testing objectives
         */
        val ALL_ACCEPTED_OBJECTIVE_PREFIXES : List<String> = ObjectiveNaming.getAllObjectivePrefixes().plus(LOCAL_OBJECTIVE_KEY).plus(
            FAULT_OBJECTIVE_PREFIX)


        // an RPC call which is handled as defined
        private const val RPC_HANDLED = "RPC_HANDLED_"

        // an RPC call which achieves a successful business logic
        private const val RPC_HANDLED_SUCCESS = "RPC_HANDLED_SUCCESS_"

        private const val GQL_NO_ERRORS = "GQL_NO_ERRORS"


        fun isFault(descriptiveId: String) = descriptiveId.startsWith(FAULT_OBJECTIVE_PREFIX)

        fun isSpecifiedFault(descriptiveId: String, category: FaultCategory) =
            isFault(descriptiveId) && descriptiveId.contains(category.label)

        fun isAnySpecifiedFault(descriptiveId: String, categories: Collection<FaultCategory>) =
            isFault(descriptiveId) && categories.any { descriptiveId.contains(it.label) }


        /**
         * @return if [descriptiveId] represents an RPC call which is handled as defined
         */
        fun isRPCHandled(descriptiveId: String) = descriptiveId.startsWith(RPC_HANDLED)

        /**
         * @return if [descriptiveId] represents an RPC call which achieves a successful scenario
         */
        fun isRPCHandledAndSuccess(descriptiveId: String) = descriptiveId.startsWith(RPC_HANDLED_SUCCESS)

        fun isGQLNoErrors(descriptiveId: String) = descriptiveId.startsWith(GQL_NO_ERRORS)

        fun isLocal(id: Int): Boolean = id < 0
    }

    private val mapping: MutableMap<Int, String> = mutableMapOf()

    private val reverseMapping: MutableMap<String, Int> = mutableMapOf()

    /**
     * Counter used to create local id, based on the return values
     * of the interactions with the SUT.
     * The counter has to be negative to avoid collisions with
     * ids generated on the SUT (eg, via bytecode instrumentation
     * monitoring)
     */
    private val localCounter = AtomicInteger(-1)

    fun addMapping(id: Int, descriptiveId: String) {
        mapping[id] = descriptiveId
        reverseMapping[descriptiveId] = id
    }

    fun hasMappingFor(id: Int) = mapping[id] != null

    fun getDescriptiveId(id: Int) = mapping[id] ?: "undefined"

    fun handleLocalTarget(descriptiveId: String): Int {
        return reverseMapping.getOrPut(descriptiveId, {
            val k = localCounter.decrementAndGet()
            mapping[k] = descriptiveId
            k
        })
    }

    fun getFaultDescriptiveId(category: FaultCategory, postfix: String): String{
        return "$FAULT_OBJECTIVE_PREFIX ${category.label} $postfix"
    }

    fun getGQLNoErrors(method: String) = "$GQL_NO_ERRORS:$method"

    fun getHandledRPC(postfix: String): String {
        return RPC_HANDLED + postfix
    }

    fun getHandledRPCAndSuccess(postfix: String): String {
        return RPC_HANDLED_SUCCESS + postfix
    }

    fun isFault(id: Int) : Boolean = mapping[id]?.let{ isFault(it)} ?: false

    fun isSpecifiedFault(id: Int, category: FaultCategory) =
        isFault(id) && mapping[id]?.let { Companion.isSpecifiedFault(it,category)} ?: false

    fun isAnySpecifiedFault(id: Int, categories: Collection<FaultCategory>) =
        isFault(id) && mapping[id]?.let { Companion.isAnySpecifiedFault(it,categories)} ?: false

    fun isGQLNoErrors(id : Int) : Boolean = mapping[id]?.let { isGQLNoErrors(it) } == true


    /**
     * @return if [id] refers to an RPC call which is handled as defined
     */
    fun isRPCHandled(id: Int) = mapping[id]?.let { isRPCHandled(it) } == true

    /**
     * @return if [id] refers to an RPC call which achieves a successful scenario
     */
    fun isRPCHandledAndSuccess(id: Int) = mapping[id]?.let { isRPCHandledAndSuccess(it) } == true


}