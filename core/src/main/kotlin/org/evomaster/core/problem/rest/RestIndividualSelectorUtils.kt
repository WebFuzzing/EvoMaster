package org.evomaster.core.problem.rest

import org.evomaster.core.search.EvaluatedIndividual

/**
 * Utility functions to select one or more REST Individual from a group, based on different criteria.
 * Or to select specific actions.
 */
object RestIndividualSelectorUtils {

    //FIXME this needs to be cleaned-up


    /**
     * Finds the first index of a main REST action with a given verb and path among actions in the individual.
     *
     * @return negative value if not found
     */
    fun getActionIndexFromIndividual(
        individual: RestIndividual,
        actionVerb: HttpVerb,
        path: RestPath
    ) : Int {
        return individual.seeMainExecutableActions().indexOfFirst  {
            it.verb == actionVerb && it.path.isEquivalent(path)
        }
    }

    fun findActionFromIndividual(
        individual: EvaluatedIndividual<RestIndividual>,
        actionVerb: HttpVerb,
        actionStatus: Int
    ): RestCallAction? {

        individual.evaluatedMainActions().forEach { currentAct ->

            val act = currentAct.action as RestCallAction
            val res = currentAct.result as RestCallResult

            if ( (res.getStatusCode() == actionStatus) && act.verb == actionVerb)  {
                return act
            }
        }

        return null
    }

    fun findIndividuals(
        individuals: List<EvaluatedIndividual<RestIndividual>>,
        verb: HttpVerb,
        statusGroup: String
    ):List<EvaluatedIndividual<RestIndividual>> {

        val individualsList = mutableListOf<EvaluatedIndividual<RestIndividual>>()

        individuals.forEach { ind ->
            val actions = ind.evaluatedMainActions()

            val successfulDeleteContained = false

            for (a in actions) {

                val act = a.action as RestCallAction
                val res = a.result as RestCallResult


                if ( (res.getStatusCode().toString().first() == statusGroup.first())
                    && act.verb == verb
                ) {
                    if (!individualsList.contains(ind)) {
                        individualsList.add(ind)
                    }
                }

            }
        }

        return individualsList
    }

    /*
        Find individuals containing a certain action and STATUS
    */
    fun getIndividualsWithActionAndStatus(individualsInSolution: List<EvaluatedIndividual<RestIndividual>>,
                                                  verb: HttpVerb, statusCode: Int)
            :List<EvaluatedIndividual<RestIndividual>> {

        val individualsList = mutableListOf<EvaluatedIndividual<RestIndividual>>()

        individualsInSolution.forEach { ind ->
            val actions = ind.evaluatedMainActions()

            val successfulDeleteContained = false

            for (a in actions) {

                val act = a.action as RestCallAction
                val res = a.result as RestCallResult


                if ( (res.getStatusCode() == statusCode) && act.verb == verb)  {

                    if (!individualsList.contains(ind)) {
                        individualsList.add(ind)
                    }
                }
            }
        }

        return individualsList
    }

    /*
  This method identifies a specific action in an individual. It is used to transform an individual containing
  one action to RestCallAction
   */
    fun findActionFromIndividuals(individualList: List<EvaluatedIndividual<RestIndividual>>,
                                          verb: HttpVerb, path: RestPath): RestCallAction? {

        // search for RESTCall action in an individual.
        var foundRestAction : RestCallAction? = null

        for (ind : EvaluatedIndividual<RestIndividual> in individualList) {

            for (act : RestCallAction in ind.individual.seeMainExecutableActions()) {

                if (act.verb == verb && act.path == path) {
                    foundRestAction = act
                }

            }

        }

        // the action that has been found
        return foundRestAction
    }

    fun getIndividualsWithActionAndStatus(
        individuals: List<EvaluatedIndividual<RestIndividual>>,
        verb: HttpVerb,
        path: RestPath,
        statusCode: Int
    ): List<EvaluatedIndividual<RestIndividual>> {

        return individuals.filter { ind ->
            ind.evaluatedMainActions().any{ea ->
                val a = ea.action as RestCallAction
                val r = ea.result as RestCallResult

                a.verb == verb && a.path.isEquivalent(path) && r.getStatusCode() == statusCode
            }
        }
    }

   fun getIndividualsWithActionAndStatusGroup(
        individuals: List<EvaluatedIndividual<RestIndividual>>,
        verb: HttpVerb,
        path: RestPath,
        statusGroup: String
    ): List<EvaluatedIndividual<RestIndividual>> {

        return individuals.filter { ind ->
            ind.evaluatedMainActions().any{ea ->
                val a = ea.action as RestCallAction
                val r = ea.result as RestCallResult

                a.verb == verb && a.path.isEquivalent(path) &&
                        r.getStatusCode().toString().first() == statusGroup.first()
            }
        }
    }

    fun getIndividualsWithAction(
        individuals: List<EvaluatedIndividual<RestIndividual>>,
        verb: HttpVerb,
        path: RestPath,
    ): List<EvaluatedIndividual<RestIndividual>> {

        return individuals.filter { ind ->
            ind.evaluatedMainActions().any{ea ->
                val a = ea.action as RestCallAction

                a.verb == verb && a.path.isEquivalent(path)
            }
        }
    }
}