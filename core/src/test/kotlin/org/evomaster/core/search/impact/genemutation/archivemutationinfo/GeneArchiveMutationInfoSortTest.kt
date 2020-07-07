package org.evomaster.core.search.impact.genemutation.archivemutationinfo

import org.evomaster.core.search.service.mutator.geneMutation.IntMutationUpdate
import org.evomaster.core.search.service.mutator.geneMutation.archive.GeneArchieMutationInfo
import org.evomaster.core.search.service.mutator.geneMutation.archive.IntegerGeneArchiveMutationInfo
import org.evomaster.core.search.service.mutator.geneMutation.archive.StringGeneArchiveMutationInfo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals

/**
 * created by manzh on 2020-06-16
 */
class GeneArchiveMutationInfoSortTest {

    @Test
    fun testStringGeneArchiveMutationComparable(){

        val minLength = 0
        val maxLength = 16
        val value = "foo"

        val g1 = GeneArchieMutationInfo()

        val m0 = StringGeneArchiveMutationInfo(minLength = minLength, maxLength = maxLength)
        g1.map[0] = m0

        val m_len1 = StringGeneArchiveMutationInfo(minLength = minLength, maxLength = maxLength)
        m_len1.plusMutatedTimes()
        m_len1.charsMutation.clear()
        m_len1.charsMutation.addAll((0 until value.length).map { IntMutationUpdate(0, 3) })
        m_len1.charMutationInitialized()
        assertEquals(-1, m0.compareTo(m_len1))
        g1.map[1] = m_len1

        val m_len1_cha1 = m_len1.copy()
        m_len1_cha1.mutatedIndex = 0
        g1.map[2] = m_len1_cha1

        val m_len2_cha1 = m_len1_cha1.copy()
        m_len2_cha1.lengthMutation.preferMax = 4
        g1.map[3] = m_len2_cha1

        val m_len1_cha2 = m_len1_cha1.copy()
        m_len1_cha1.charsMutation[0].preferMin = 0
        m_len1_cha1.charsMutation[0].preferMax = 1
        g1.map[4] = m_len1_cha2

        val sorted = g1.sort(setOf(0,1,2,3,4))
        assertEquals(0, sorted.indexOf(m0))
        assertEquals(1, sorted.indexOf(m_len1))
        assertEquals(2, sorted.indexOf(m_len1_cha1))
        assertEquals(3, sorted.indexOf(m_len1_cha2))
        assertEquals(4, sorted.indexOf(m_len2_cha1))

    }

    @Test
    fun testIntegerGeneArchiveMutationComparable(){
        val g1 = GeneArchieMutationInfo()

        val m0 = IntegerGeneArchiveMutationInfo(0, 16)
        g1.map[0] = m0

        val m1 = IntegerGeneArchiveMutationInfo(0, 7)
        g1.map[1] = m1

        val m2 = IntegerGeneArchiveMutationInfo(-1, 8)
        g1.map[2] = m2

        val sorted = g1.sort(setOf(0,1,2))
        assertEquals(0, sorted.indexOf(m0))
        assertEquals(1, sorted.indexOf(m2))
        assertEquals(2, sorted.indexOf(m1))
    }
}