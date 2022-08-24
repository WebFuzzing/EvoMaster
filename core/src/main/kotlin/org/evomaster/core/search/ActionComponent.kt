package org.evomaster.core.search


/**
 * Shared superclass to represent single [Action] and groups [ActionTree]
 */
abstract class ActionComponent(
        children: MutableList<out StructuralElement>,
        childTypeVerifier: (Class<*>) -> Boolean,
        groups : GroupsOfChildren<out StructuralElement>? = null,
        /**
         * a unique id is used to identify this action component in the context of an individual
         */
        private var localId : String,
) : StructuralElement(children, childTypeVerifier, groups as GroupsOfChildren<StructuralElement>?){


    companion object{
        /**
         * a constant string represents that an id of the action is not assigned
         */
        const val NONE_ACTION_COMPONENT_ID = "NONE_ACTION_COMPONENT_ID"
    }


    /**
     * set a local id of the action
     * note that the id can be assigned only if the current id is NONE_ACTION_ID
     */
    fun setLocalId(id: String) {
        if (this.localId == NONE_ACTION_COMPONENT_ID)
            this.localId = id
        else
            throw IllegalStateException("cannot re-assign the id of the action, the current id is ${this.localId}")
    }

    /**
     * return if the action has been assigned with a local id
     */
    fun hasLocalId() = localId != NONE_ACTION_COMPONENT_ID

    /**
     * reset local id of the action
     */
    fun resetLocalId() {
        localId = NONE_ACTION_COMPONENT_ID
    }

    fun getLocalId() = localId

    /**
     * Return a flattened reference to all actions in this component
     */
    abstract fun flatten() : List<Action>

    /**
     * @return a recursive list of all nested action component
     */
    abstract fun flatView() : List<ActionComponent>
}