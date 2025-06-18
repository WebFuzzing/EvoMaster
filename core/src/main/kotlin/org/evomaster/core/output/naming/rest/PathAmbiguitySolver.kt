package org.evomaster.core.output.naming.rest

import org.evomaster.core.output.naming.AmbiguitySolver
import org.evomaster.core.output.naming.rest.RestNamingUtils.getPath
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestPath
import org.evomaster.core.search.action.Action

class PathAmbiguitySolver : AmbiguitySolver {

    /*
     * If the last element of a path is a parameter then we must go up a level
     *
     * Example: /products/{productName}/configurations/{configurationName}/features/{featureName}
     * must now include the name qualifier for configurations
     */
    override fun apply(action: Action, remainingNameChars: Int): List<String> {
        val restAction = action as RestCallAction
        val lastPath = restAction.path
        val lastPathQualifier = getPath(lastPath.nameQualifier)

        var parentPath = lastPath.parentPath()
        if (lastPath.isLastElementAParameter()) {
            parentPath = parentPath.parentPath()
        }
        val candidateTokens = listOf(getParentPathQualifier(parentPath), lastPathQualifier)

        return if (canAddNameTokens(candidateTokens, remainingNameChars)) candidateTokens else listOf(lastPathQualifier)
    }

    /*
     * If the parent path name qualifier is not root, then we make sure we obtain the sanitized version of it.
     * Otherwise, we'll keep the original path returning the empty string.
     */
    private fun getParentPathQualifier(parentPath: RestPath): String {
        val parentPathQualifier = parentPath.nameQualifier
        return if (parentPathQualifier == "/") "" else getPath(parentPathQualifier)
    }

    private fun canAddNameTokens(targetString: List<String>, remainingNameChars: Int): Boolean {
        return (remainingNameChars - targetString.sumOf { it.length }) >= 0
    }

}
