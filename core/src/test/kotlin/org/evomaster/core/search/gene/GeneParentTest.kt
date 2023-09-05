package org.evomaster.core.search.gene


import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory


class GeneParentTest  : AbstractGeneTest() {



    @TestFactory
    fun testParent(): Collection<DynamicTest> {
        return (0..1000L).map {
            DynamicTest.dynamicTest("Seed: $it") { checkParent(it)}
        }.toList()
    }

    private fun checkParent(seed: Long) {
        val sample = getSample(seed)

        sample.forEach { root ->
            val wholeTree = root.flatView().filter { it != root }

            wholeTree.forEach { n ->
                var p = n
                while (p.parent != null) {
                    p = p.parent as Gene
                }
                assertEquals(root, p)
            }
        }
    }
}
