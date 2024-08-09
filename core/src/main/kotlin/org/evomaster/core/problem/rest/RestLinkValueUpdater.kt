package org.evomaster.core.problem.rest

object RestLinkValueUpdater {

    //TODO
    fun update(target: RestCallAction, link: RestLink, source: RestCallAction, sourceResults: RestCallResult){

        //TODO validation
        for (p in link.parameters) {
            when {
                p.isConstant() -> {
                    //TODO
                }
                p.isBodyField() ->{
                    //TODO
                }
            }
        }
    }

}