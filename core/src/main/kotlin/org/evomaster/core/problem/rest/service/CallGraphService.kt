package org.evomaster.core.problem.rest.service

import com.google.inject.Inject
import org.evomaster.core.problem.rest.data.Endpoint
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestPath
import org.evomaster.core.problem.rest.service.sampler.AbstractRestSampler
import javax.annotation.PostConstruct
import kotlin.math.abs

/**
 * Utility service used to detect and analyse dependencies between REST operations
 * defined in the OpenAPI schema of the API
 */
class CallGraphService {

    @Inject
    private lateinit var sampler: AbstractRestSampler

    /**
     * Map of create POST/PUT endpoints and possible DELETE operations for them.
     * This linking is created dynamically based on heuristics.
     */
    private val deleteDependencies = mutableMapOf<Endpoint, RestCallAction>()


    @PostConstruct
    private fun init() {

        val calls = sampler.seeAvailableActions()
            .filterIsInstance<RestCallAction>()

        val deletes = calls.filter { it.verb == HttpVerb.DELETE }
        val creates = calls.filter { it.verb == HttpVerb.POST || it.verb == HttpVerb.PUT }

        val noExactMatch = mutableListOf<RestCallAction>()
        creates.forEach {
            val del = computeDeleteExactMatch(it, deletes)
            if(del != null) {
                deleteDependencies[it.endpoint] = del
            } else {
                noExactMatch.add(it)
            }
        }

        val noPartialMatch = mutableListOf<RestCallAction>()
        noExactMatch.forEach {
            val del = computeDeletePartialMatch(it, deletes)
            if(del != null) {
                deleteDependencies[it.endpoint] = del
            } else {
                noPartialMatch.add(it)
            }
        }

        noPartialMatch.forEach {
            val del = computeDeleteLastResort(it, deletes)
            if(del != null) {
                deleteDependencies[it.endpoint] = del
            }
        }
    }

    /**
     * Check in the schema if there is any action which is a direct child of [a] and last path element is a parameter
     */
    fun hasParameterChild(a: RestCallAction): Boolean {
        return sampler.seeAvailableActions()
            .filterIsInstance<RestCallAction>()
            .map { it.path }
            .any { it.isDirectChildOf(a.path) && it.isLastElementAParameter() }
    }

    fun getSimilarSiblingWithParameterChild(a: RestCallAction): RestCallAction? {

        return sampler.seeAvailableActions()
            .filterIsInstance<RestCallAction>()
            .find{ isSimilarDescendant(it.path, a.path) && it.path.isLastElementAParameter()}
    }

    fun resolveLocationForParentOfChildOperationUsingCreatedResource(create: RestCallAction): String? {

        if(hasParameterChild(create)) {
            //simple case
            return create.resolvedPath()
        }

        /*
            TODO algorithm and data structures would need to improve.
            eg, although it is a horrible design, we could have cases like:

            POST   /users
            GET    /users/get/{id}
            DELETE /users/delete/{id}

            this would imply that a create action might end up be used on different paths.
            this is currently not handle in EM, and would require quite a bit of refactoring.
            TODO would need to check how serious the issue is in practice
         */

        val sibling = getSimilarSiblingWithParameterChild(create)
        if(sibling != null) {
            val path = sibling.path.parentPath()
            return path.resolveOnlyPath(create.parameters)
        }

        return null

    }


    fun resolveLocationForChildOperationUsingCreatedResource(create: RestCallAction, id: String): String? {

        val parentLocation = resolveLocationForParentOfChildOperationUsingCreatedResource(create)

        if(parentLocation != null) {
            //simple case
            return "$parentLocation/$id"
        }

        return null
    }

    /**
     * Among all the endpoints declared in the schema, find if there is any DELETE
     * operation for the resources created by the input action.
     * Note: the matching is based "best-practices" in REST API design.
     */
    fun findDeleteFor(action: RestCallAction) : RestCallAction?{
        return deleteDependencies[action.endpoint]?.copy() as RestCallAction?
    }

    private fun isSimilarPath(a: RestPath, b: RestPath) : Boolean {

        if(a.levels() != b.levels()) {
            return false
        }

        if(a.isRoot() || b.isRoot()) {
            return false
        }

        if(a.parentPath() != b.parentPath()) {
            return false
        }

        if(a.isLastElementAParameter() && b.isLastElementAParameter()) {
            //same ancestor path, and just different name for the param?
            //then it is ok
            return true
        }

        val distance = org.apache.commons.text.similarity.LevenshteinDistance(2)

        if(!a.isLastElementAParameter() && !b.isLastElementAParameter()) {
            val x = a.lastElement()
            val y = b.lastElement()
            val d = distance.apply(x,y)
            if(d >= 0){
                // a negative value means it was greater than the threshold
                return true
            }
        }

        return false
    }

    private fun isSimilarDescendant(child: RestPath, parent: RestPath) : Boolean {

        if(child.levels() != parent.levels() + 1) {
            return false
        }
        if(parent.isRoot()){
            return true
        }

        return isSimilarPath(child.parentPath(), parent)
    }

    private fun computeDeleteLastResort(action: RestCallAction, deletes: List<RestCallAction>) : RestCallAction?{

        val usedDeletes = deleteDependencies.values

        val unusedDeletes = deletes.filter{ d ->  usedDeletes.none { d.endpoint == it.endpoint } }

        if(unusedDeletes.isEmpty()){
            return null
        }

        /*
            sort the option.
            the one with the longest shared ancestor paths come first (note the - for descending order).
            then, in case of ties, look at closest element level values
         */
        return unusedDeletes.sortedWith(
            compareBy<RestCallAction>{  -it.path.lengthSharedAncestors(action.path) }
                .thenBy { abs(it.path.levels() - action.path.levels()) }
        ).first()
    }

    private fun computeDeletePartialMatch(action: RestCallAction, deletes: List<RestCallAction>) : RestCallAction?{

        if(action.verb != HttpVerb.PUT && action.verb != HttpVerb.POST){
            throw IllegalArgumentException("Only PUT or POST are supported")
        }

        /*
            follow same algorithm as exact match, but allow some name mismatches
         */

        val samePath = deletes.find { isSimilarPath(it.path,action.path) }

        val directDescendant = deletes.find { isSimilarDescendant(it.path,action.path)}

        if(samePath != null){

            if(action.verb == HttpVerb.PUT){
                return samePath
            }
            assert(action.verb == HttpVerb.POST)

            if(directDescendant != null && directDescendant.path.isLastElementAParameter()
                && !action.path.isLastElementAParameter()){
                return directDescendant
            }
            return samePath
        }

        if(directDescendant != null){
            return directDescendant
        }

        return null
    }


    private fun computeDeleteExactMatch(action: RestCallAction, deletes: List<RestCallAction>) : RestCallAction?{
        if(action.verb != HttpVerb.PUT && action.verb != HttpVerb.POST){
            throw IllegalArgumentException("Only PUT or POST are supported")
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