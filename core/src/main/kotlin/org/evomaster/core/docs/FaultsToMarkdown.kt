package org.evomaster.core.docs

import com.webfuzzing.commons.faults.MarkdownFaults
import org.evomaster.core.problem.enterprise.EmployedOracles

/**
 * Class used to create markdown documentation based on the oracles defined in [EmployedOracles]
 */
object FaultsToMarkdown {

    @JvmStatic
    fun main(args: Array<String>) {
        saveToDocs()
    }


    fun saveToDocs() {
        val path = "docs/faults.md"
        val categories= EmployedOracles.usedCategories()

        MarkdownFaults.createMarkdown(path, categories)
    }
}