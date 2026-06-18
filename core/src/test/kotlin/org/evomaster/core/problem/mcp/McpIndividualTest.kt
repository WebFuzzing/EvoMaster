package org.evomaster.core.problem.mcp

import org.evomaster.core.problem.enterprise.EnterpriseActionGroup
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.search.GroupsOfChildren
import org.evomaster.core.search.action.ActionComponent
import org.evomaster.core.search.gene.ObjectGene
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class McpIndividualTest {

    private fun buildIndividual(vararg actions: McpAction): McpIndividual {
        val wrappedActions: MutableList<EnterpriseActionGroup<McpAction>> =
            actions.map { EnterpriseActionGroup(it) }.toMutableList()
        @Suppress("UNCHECKED_CAST")
        return McpIndividual(
            sampleType = SampleType.RANDOM,
            allActions = wrappedActions as MutableList<out ActionComponent>
        )
    }

    @Test
    fun `seeMainExecutableActions returns all actions`() {
        val toolAction = McpToolCallAction("myTool", ObjectGene("input", emptyList()))
        val resourceAction = McpResourceReadAction(uriTemplate = "file:///data", uriParams = emptyList())

        val individual = buildIndividual(toolAction, resourceAction)

        val mainActions = individual.seeMainExecutableActions()
        assertEquals(2, mainActions.size)
    }

    @Test
    fun `copyContent returns equal-sized independent copy`() {
        val toolAction = McpToolCallAction("myTool", ObjectGene("input", emptyList()))
        val resourceAction = McpResourceReadAction(uriTemplate = "file:///data", uriParams = emptyList())

        val individual = buildIndividual(toolAction, resourceAction)
        val copy = individual.copy() as McpIndividual

        assertEquals(
            individual.seeMainExecutableActions().size,
            copy.seeMainExecutableActions().size
        )
        // Must be independent: different action instances
        assertNotSame(
            individual.seeMainExecutableActions()[0],
            copy.seeMainExecutableActions()[0]
        )
    }

    @Test
    fun `addMcpAction increases action count`() {
        val toolAction = McpToolCallAction("myTool", ObjectGene("input", emptyList()))
        val individual = buildIndividual(toolAction)

        assertEquals(1, individual.seeMainExecutableActions().size)

        val newAction = McpToolCallAction("anotherTool", ObjectGene("input2", emptyList()))
        individual.addMcpAction(action = newAction)

        assertEquals(2, individual.seeMainExecutableActions().size)
    }

    @Test
    fun `group size is tracked correctly`() {
        val toolAction = McpToolCallAction("myTool", ObjectGene("input", emptyList()))
        val resourceAction = McpResourceReadAction(uriTemplate = "file:///data", uriParams = emptyList())

        val individual = buildIndividual(toolAction, resourceAction)

        assertEquals(2, individual.groupsView()!!.sizeOfGroup(GroupsOfChildren.MAIN))
        assertEquals(0, individual.groupsView()!!.sizeOfGroup(GroupsOfChildren.INITIALIZATION_SQL))
        assertEquals(0, individual.groupsView()!!.sizeOfGroup(GroupsOfChildren.INITIALIZATION_MONGO))
    }
}
