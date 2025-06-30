package org.evomaster.core.search.action

import org.evomaster.core.search.Individual
import org.evomaster.core.search.StructuralElement

abstract class MainAction(
    /**
     * Specify that this action is a cleanup one, although it has same type as a main action.
     * Cleanup actions are usually executed at the end of a test case in black-box mode, where we
     * do not have direct access to the databases
     */
    var isCleanUp : Boolean,
    children: List<StructuralElement>
) : Action(children) {

    final override fun shouldCountForFitnessEvaluations(): Boolean {
        return true
    }

    fun getPreviousMainActions() : List<MainAction>{
        return otherMainActions(true)
    }

    fun getFollowingMainActions() : List<MainAction>{
        return otherMainActions(false)
    }



    fun positionAmongMainActions(): Int{
        if(isCleanUp){
            return -1
        }

        if(!hasLocalId()){
            throw IllegalStateException("No local id defined for this main action")
        }


        if(!isMounted()){
            throw IllegalStateException("Action is not mounted inside an individual")
        }

        val ind = this.getRoot() as Individual
        val all = ind.seeMainExecutableActions()
        val index = all.indexOfFirst { it.getLocalId() == this.getLocalId()}
        if(index < 0){
            throw IllegalStateException("Current action with local id ${getLocalId()} not found among main actions")
        }
        return index
    }

    private fun otherMainActions(before: Boolean) : List<MainAction> {
        val all = (getRoot() as Individual).seeMainExecutableActions()
        if(isCleanUp){
            return if(before) {
                all
            } else {
                listOf()
            }
        }

        val index = positionAmongMainActions()

        if(before){
            if(index == 0){
                return listOf()
            }
            return all.subList(0,index)
        } else {
            if(index == all.lastIndex){
                return listOf()
            }
            return all.subList(index+1,all.size)
        }
    }
}