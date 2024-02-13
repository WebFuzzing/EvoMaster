package org.evomaster.core.problem.gui

import org.evomaster.core.problem.enterprise.EnterpriseChildTypeVerifier
import org.evomaster.core.problem.enterprise.EnterpriseIndividual
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.search.action.ActionComponent
import org.evomaster.core.search.GroupsOfChildren
import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.tracer.TrackOperator

/**
 * Test for a Graphical User Interface (GUI).
 * Those could be a Web Frontend in a Browser, or a Mobile app.
 * Usually, a test would be a series of interactions from the user with the GUI, likely clicking buttons.
 *
 * Each action is strongly dependent on all previous actions, as those define the current "view" of the GUI.
 * For example, a specific button could be clicked only if we are in the right page, accessed from following
 * a link in a previous action
 */
abstract class GuiIndividual (
    sampleType: SampleType,

    /**
     * a tracked operator to manipulate the individual (nullable)
     */
    trackOperator: TrackOperator? = null,
    /**
     * an index of individual indicating when the individual is initialized during the search
     * negative number means that such info is not collected
     */
    index : Int = -1,
    /**
     * a list of children of the individual
     */
    children: MutableList<out ActionComponent>,
    childTypeVerifier: EnterpriseChildTypeVerifier,
    groups : GroupsOfChildren<StructuralElement> = getEnterpriseTopGroups(children, children.size, 0, 0 ,0)
): EnterpriseIndividual(sampleType, trackOperator, index, children, childTypeVerifier, groups)
