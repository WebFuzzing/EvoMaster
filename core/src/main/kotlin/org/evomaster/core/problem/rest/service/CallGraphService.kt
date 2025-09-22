package org.evomaster.core.problem.rest.service

import com.google.inject.Inject
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.service.sampler.AbstractRestSampler

class CallGraphService {

    @Inject
    private lateinit var sampler: AbstractRestSampler

    /**
     * Among all the endpoints declared in the schema, find if there is any DELETE
     * operation for the resources created by the input action.
     * Note: the matching is based "best-practices" in REST API design.
     */
    fun findDeleteFor(action: RestCallAction) : RestCallAction?{
        if(action.verb != HttpVerb.PUT && action.verb != HttpVerb.POST){
            return null
        }

        /*
            Determining which DELETE would apply to the action is based on URI path.
            This is done heuristically, cannot be 100% sure.
            Algorithm is based on these examples seen in practice:

            – POST /data
              PUT   /data/{id}
              DELETE /data/{id}

            – POST /data/{id}
              PUT   /data/{id}
              DELETE /data/{id}

            – POST  /data/{id}/sub
              PUT    /data/{id}/sub/{x}
              DELETE    /data/{id}/sub/{x}

            – PUT /data/{x}/{y}
            – DELETE /data/{x}/{y}

            – POST /data/{x}
            – DELETE /data/{x}/foo

            – PUT /foo
            – DELETE /foo (based on auth)

            – POST /foo
            – PUT    /foo
            – DELETE /foo
            – DELETE /foo/{id}

            – POST /data/{id}/foo
            – DELETE /data/{id}/foo
            – POST /data
            – DELETE /data/{id}

            – POST /data/{x}/{y}
            – PUT   /data/{x}/{y}/{id}
            – DELETE /data/{x}/{y}/{id}

            – POST /data
            – PUT   /data
            – DELETE /data/{id0}/{id1}/{x}

            – PUT /data
            – DELETE /data/delete/{id}

         */

        val deletes = sampler.seeAvailableActions()
            .filterIsInstance<RestCallAction>()
            .filter { it.verb == HttpVerb.DELETE }

        val samePath = deletes.find { it.path == action.path }

        val directDescendant = deletes.find { it.path.isDirectChildOf(action.path)}

        if(samePath != null){

            if(action.verb == HttpVerb.PUT){
                return samePath
            }
            assert(action.verb == HttpVerb.POST)

            /*
                If we deal with a POST on a collection, and there is a parametric DELETE child,
                we go for that instead of DELETE on collection
             */
            if(directDescendant != null && directDescendant.path.isLastElementAParameter()
                && !action.path.isLastElementAParameter()){
                return directDescendant
            }
            return samePath
        }

        if(directDescendant != null){
            return directDescendant
        }

        /*
            If nothing worked, check if we are in one of these weird cases:
            – POST /data
            – PUT   /data
            – DELETE /data/{id0}/{id1}/{x}

            – PUT /data
            – DELETE /data/delete/{id}
         */
        return deletes.find { action.path.isSameOrAncestorOf(it.path) }
    }
}