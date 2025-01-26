package org.evomaster.core.output.naming.rest

import org.evomaster.core.output.naming.AmbiguitySolver
import org.evomaster.core.output.naming.rest.RestUtils.getPath
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.RestPath
import org.evomaster.core.search.action.Action

class PathAmbiguitySolver : AmbiguitySolver {

    /*
     * If the last element of a path is a parameter then we must go up a level
     *
     * Example: /products/{productName}/configurations/{configurationName}/features/{featureName}
     * must now include the name qualifier for configurations
     */
    override fun apply(action: Action): List<String> {
        val restAction = action as RestCallAction
        val lastPath = restAction.path
        var parentPath = restAction.path.parentPath()
        if (lastPath.isLastElementAParameter()) {
            parentPath = parentPath.parentPath()
        }
        return listOf(getParentPathQualifier(parentPath), getPath(restAction.path.nameQualifier))
    }

    /*
     * If the parent path name qualifier is not root, then we make sure we obtain the sanitized version of it.
     * Otherwise, we'll keep the original path returning the empty string.
     */
    private fun getParentPathQualifier(parentPath: RestPath): String {
        val parentPathQualifier = parentPath.nameQualifier
        return if (parentPathQualifier == "/") "" else getPath(parentPathQualifier)
    }

}
