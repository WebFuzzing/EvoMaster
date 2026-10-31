package org.evomaster.e2etests.spring.openapi.v3.arazzo

import com.foo.rest.examples.spring.openapi.v3.arazzo.ArazzoPetCouponsController
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.core.problem.rest.data.RestIndividual
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Solution
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class ArazzoEMTest : SpringTestBase() {

    companion object {
        private const val ARAZZO_LOCATION = "src/main/resources/static/pet-coupons-arazzo.yaml"

        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(ArazzoPetCouponsController())
        }
    }

    @Test
    fun testRunEMWithArazzoWorkflowSampling() {
        runTestHandlingFlakyAndCompilation(
            "ArazzoEM",
            "org.foo.ArazzoEM",
            150,
        ) { args ->
            configureArazzoArgs(args)

            val solution = initAndRun(args)

            assertTrue(solution.individuals.isNotEmpty())

            // workflowId: apply-coupon (findPetsByTags -> getPetCoupons -> placeOrder)
            val applyCouponSequence = listOf("findPetsByTags", "getPetCoupons", "placeOrder")
            val applyCouponIndividuals = solution.individuals.filter {
                operationIdSequence(it) == applyCouponSequence
            }
            assertTrue(
                applyCouponIndividuals.isNotEmpty(),
                "Missing operationId sequence $applyCouponSequence. Found sequences: ${collectedSequences(solution)}",
            )
            applyCouponIndividuals.forEach { ind ->
                assertTrue(hasAtLeastOne(ind, HttpVerb.GET, 200, "/pet/findByTags", null))
                assertTrue(hasAtLeastOne(ind, HttpVerb.GET, 200, "/pet/{petId}/coupons", null))
                assertTrue(hasAtLeastOne(ind, HttpVerb.POST, 200, "/store/order", null))
            }
        }
    }

    @Test
    fun testRunEMWithArazzoDeterministicBranchWorkflows() {
        runTestHandlingFlakyAndCompilation(
            "ArazzoEMDeterministicBranches",
            "org.foo.ArazzoEMDeterministicBranches",
            null,
            400,
            true,
            false,
            { args ->
            configureArazzoArgs(args)

            val solution = initAndRun(args)

            assertTrue(solution.individuals.isNotEmpty())

            // workflowId: early-exit-tags — onFailure -> end (single step)
            val earlyExitTagsSequence = listOf("findPetsByTags")
            val earlyExitTagsIndividuals = solution.individuals.filter {
                operationIdSequence(it) == earlyExitTagsSequence
            }
            assertTrue(
                earlyExitTagsIndividuals.isNotEmpty(),
                "Missing operationId sequence $earlyExitTagsSequence for early-exit-tags. " +
                    "Found sequences: ${collectedSequences(solution)}",
            )
            earlyExitTagsIndividuals.forEach { ind ->
                assertTrue(hasAtLeastOne(ind, HttpVerb.GET, 200, "/pet/findByTags", null))
                assertFalse(
                    ind.individual.seeMainExecutableActions().any { it.verb == HttpVerb.POST },
                    "early-exit-tags onFailure branch should not include placeOrder",
                )
            }

            // workflowId: skip-coupons-via-goto — onSuccess goto place-order
            val skipCouponsSequence = listOf("findPetsByTags", "placeOrder")
            val skipCouponsIndividuals = solution.individuals.filter {
                operationIdSequence(it) == skipCouponsSequence
            }
            assertTrue(
                skipCouponsIndividuals.isNotEmpty(),
                "Missing operationId sequence $skipCouponsSequence for skip-coupons-via-goto. " +
                    "Found sequences: ${collectedSequences(solution)}",
            )
            skipCouponsIndividuals.forEach { ind ->
                assertTrue(hasAtLeastOne(ind, HttpVerb.GET, 200, "/pet/findByTags", null))
                assertTrue(hasAtLeastOne(ind, HttpVerb.POST, 200, "/store/order", null))
                assertFalse(
                    hasAtLeastOne(ind, HttpVerb.GET, 200, "/pet/{petId}/coupons", null),
                    "skip-coupons-via-goto should not call getPetCoupons",
                )
            }

            // workflowId: jump-to-other-workflow — goto buy-available-pet
            val jumpToBuyAvailableSequence = listOf("findPetsByTags", "findPetsByStatus", "placeOrder")
            val jumpToBuyAvailableIndividuals = solution.individuals.filter {
                operationIdSequence(it) == jumpToBuyAvailableSequence
            }
            assertTrue(
                jumpToBuyAvailableIndividuals.isNotEmpty(),
                "Missing operationId sequence $jumpToBuyAvailableSequence for jump-to-other-workflow. " +
                    "Found sequences: ${collectedSequences(solution)}",
            )
            jumpToBuyAvailableIndividuals.forEach { ind ->
                assertTrue(hasAtLeastOne(ind, HttpVerb.GET, 200, "/pet/findByTags", null))
                assertTrue(hasAtLeastOne(ind, HttpVerb.GET, 200, "/pet/findByStatus", null))
                assertTrue(hasAtLeastOne(ind, HttpVerb.POST, 200, "/store/order", null))
            }

            // workflowId: branch-success-and-failure — onFailure end + onSuccess goto place-order
            val branchFailureSequence = listOf("findPetsByTags")
            val branchFailureIndividuals = solution.individuals.filter {
                operationIdSequence(it) == branchFailureSequence
            }
            assertTrue(
                branchFailureIndividuals.isNotEmpty(),
                "Missing operationId sequence $branchFailureSequence for branch-success-and-failure (onFailure). " +
                    "Found sequences: ${collectedSequences(solution)}",
            )
            branchFailureIndividuals.forEach { ind ->
                assertTrue(hasAtLeastOne(ind, HttpVerb.GET, 200, "/pet/findByTags", null))
                assertFalse(
                    ind.individual.seeMainExecutableActions().any { it.verb == HttpVerb.POST },
                    "branch-success-and-failure onFailure branch should not include placeOrder",
                )
            }

            val branchSuccessSequence = listOf("findPetsByTags", "placeOrder")
            val branchSuccessIndividuals = solution.individuals.filter {
                operationIdSequence(it) == branchSuccessSequence
            }
            assertTrue(
                branchSuccessIndividuals.isNotEmpty(),
                "Missing operationId sequence $branchSuccessSequence for branch-success-and-failure (onSuccess). " +
                    "Found sequences: ${collectedSequences(solution)}",
            )
            branchSuccessIndividuals.forEach { ind ->
                assertTrue(hasAtLeastOne(ind, HttpVerb.GET, 200, "/pet/findByTags", null))
                assertTrue(hasAtLeastOne(ind, HttpVerb.POST, 200, "/store/order", null))
            }

            // workflowId: workflow-success-inherit-and-override
            val inheritOverrideLongSequence = listOf("findPetsByTags", "findPetsByStatus", "placeOrder")
            val inheritOverrideLongIndividuals = solution.individuals.filter {
                operationIdSequence(it) == inheritOverrideLongSequence
            }
            assertTrue(
                inheritOverrideLongIndividuals.isNotEmpty(),
                "Missing operationId sequence $inheritOverrideLongSequence for workflow-success-inherit-and-override (alt-checkout). " +
                    "Found sequences: ${collectedSequences(solution)}",
            )
            inheritOverrideLongIndividuals.forEach { ind ->
                assertTrue(hasAtLeastOne(ind, HttpVerb.GET, 200, "/pet/findByTags", null))
                assertTrue(hasAtLeastOne(ind, HttpVerb.GET, 200, "/pet/findByStatus", null))
                assertTrue(hasAtLeastOne(ind, HttpVerb.POST, 200, "/store/order", null))
            }

            val inheritOverrideStopSequence = listOf("findPetsByTags")
            val inheritOverrideStopIndividuals = solution.individuals.filter {
                operationIdSequence(it) == inheritOverrideStopSequence
            }
            assertTrue(
                inheritOverrideStopIndividuals.isNotEmpty(),
                "Missing operationId sequence $inheritOverrideStopSequence for workflow-success-inherit-and-override (stop). " +
                    "Found sequences: ${collectedSequences(solution)}",
            )
            inheritOverrideStopIndividuals.forEach { ind ->
                assertTrue(hasAtLeastOne(ind, HttpVerb.GET, 200, "/pet/findByTags", null))
                assertFalse(
                    ind.individual.seeMainExecutableActions().any { it.verb == HttpVerb.POST },
                    "workflow-success-inherit-and-override stop branch should not include placeOrder",
                )
            }
            },
            8,
        )
    }
    
    @Test
    fun testRunEMWithArazzoStochasticBranchWorkflows() {
        runTestHandlingFlakyAndCompilation(
            "ArazzoEMStochasticBranches",
            "org.foo.ArazzoEMStochasticBranches",
            null,
            400,
            true,
            false,
            { args ->
            configureArazzoArgs(args)

            val solution = initAndRun(args)

            assertTrue(solution.individuals.isNotEmpty())

            // workflowId: branch-success-and-failure
            val branchFailureSequence = listOf("findPetsByTags")
            val branchFailureIndividuals = solution.individuals.filter {
                operationIdSequence(it) == branchFailureSequence
            }
            assertTrue(
                branchFailureIndividuals.isNotEmpty(),
                "Missing operationId sequence $branchFailureSequence for branch-success-and-failure (onFailure). " +
                    "Found sequences: ${collectedSequences(solution)}",
            )
            branchFailureIndividuals.forEach { ind ->
                assertTrue(hasAtLeastOne(ind, HttpVerb.GET, 200, "/pet/findByTags", null))
                assertFalse(
                    ind.individual.seeMainExecutableActions().any { it.verb == HttpVerb.POST },
                    "branch-success-and-failure onFailure branch should not include placeOrder",
                )
            }

            val branchSuccessSequence = listOf("findPetsByTags", "placeOrder")
            val branchSuccessIndividuals = solution.individuals.filter {
                operationIdSequence(it) == branchSuccessSequence
            }
            assertTrue(
                branchSuccessIndividuals.isNotEmpty(),
                "Missing operationId sequence $branchSuccessSequence for branch-success-and-failure (onSuccess). " +
                    "Found sequences: ${collectedSequences(solution)}",
            )
            branchSuccessIndividuals.forEach { ind ->
                assertTrue(hasAtLeastOne(ind, HttpVerb.GET, 200, "/pet/findByTags", null))
                assertTrue(hasAtLeastOne(ind, HttpVerb.POST, 200, "/store/order", null))
            }

            // workflowId: branch-multiple-on-success
            val multipleOnSuccessShortSequence = listOf("findPetsByTags", "placeOrder")
            val multipleOnSuccessShortIndividuals = solution.individuals.filter {
                operationIdSequence(it) == multipleOnSuccessShortSequence
            }
            assertTrue(
                multipleOnSuccessShortIndividuals.isNotEmpty(),
                "Missing operationId sequence $multipleOnSuccessShortSequence for branch-multiple-on-success (goto place-order). " +
                    "Found sequences: ${collectedSequences(solution)}",
            )
            multipleOnSuccessShortIndividuals.forEach { ind ->
                assertTrue(hasAtLeastOne(ind, HttpVerb.GET, 200, "/pet/findByTags", null))
                assertTrue(hasAtLeastOne(ind, HttpVerb.POST, 200, "/store/order", null))
            }

            val multipleOnSuccessLongSequence = listOf("findPetsByTags", "findPetsByStatus", "placeOrder")
            val multipleOnSuccessLongIndividuals = solution.individuals.filter {
                operationIdSequence(it) == multipleOnSuccessLongSequence
            }
            assertTrue(
                multipleOnSuccessLongIndividuals.isNotEmpty(),
                "Missing operationId sequence $multipleOnSuccessLongSequence for branch-multiple-on-success (goto buy-available-pet). " +
                    "Found sequences: ${collectedSequences(solution)}",
            )
            multipleOnSuccessLongIndividuals.forEach { ind ->
                assertTrue(hasAtLeastOne(ind, HttpVerb.GET, 200, "/pet/findByTags", null))
                assertTrue(hasAtLeastOne(ind, HttpVerb.GET, 200, "/pet/findByStatus", null))
                assertTrue(hasAtLeastOne(ind, HttpVerb.POST, 200, "/store/order", null))
            }

            // workflowId: workflow-success-inherit-and-override
            val inheritOverrideLongSequence = listOf("findPetsByTags", "findPetsByStatus", "placeOrder")
            val inheritOverrideLongIndividuals = solution.individuals.filter {
                operationIdSequence(it) == inheritOverrideLongSequence
            }
            assertTrue(
                inheritOverrideLongIndividuals.isNotEmpty(),
                "Missing operationId sequence $inheritOverrideLongSequence for workflow-success-inherit-and-override (step override -> buy-available-pet). " +
                    "Found sequences: ${collectedSequences(solution)}",
            )
            inheritOverrideLongIndividuals.forEach { ind ->
                assertTrue(hasAtLeastOne(ind, HttpVerb.GET, 200, "/pet/findByTags", null))
                assertTrue(hasAtLeastOne(ind, HttpVerb.GET, 200, "/pet/findByStatus", null))
                assertTrue(hasAtLeastOne(ind, HttpVerb.POST, 200, "/store/order", null))
            }

            val inheritOverrideStopSequence = listOf("findPetsByTags")
            val inheritOverrideStopIndividuals = solution.individuals.filter {
                operationIdSequence(it) == inheritOverrideStopSequence
            }
            assertTrue(
                inheritOverrideStopIndividuals.isNotEmpty(),
                "Missing operationId sequence $inheritOverrideStopSequence for workflow-success-inherit-and-override (workflow stop). " +
                    "Found sequences: ${collectedSequences(solution)}",
            )
            inheritOverrideStopIndividuals.forEach { ind ->
                assertTrue(hasAtLeastOne(ind, HttpVerb.GET, 200, "/pet/findByTags", null))
                assertFalse(
                    ind.individual.seeMainExecutableActions().any { it.verb == HttpVerb.POST },
                    "workflow-success-inherit-and-override stop branch should not include placeOrder",
                )
            }
            },
            5,
        )
    }

    private fun configureArazzoArgs(args: MutableList<String>) {
        setOption(args, "enableArazzoWorkflowSampling", "true")
        setOption(args, "arazzoLocation", ARAZZO_LOCATION)
        setOption(args, "probOfArazzoSampling", "1.0")
        setOption(args, "probOfSmartSampling", "0.0")
    }

    private fun operationIdSequence(ind: EvaluatedIndividual<RestIndividual>): List<String> {
        return ind.individual.seeMainExecutableActions()
            .mapNotNull { it.operationId }
    }

    private fun collectedSequences(solution: Solution<RestIndividual>): Set<List<String>> {
        return solution.individuals.map { operationIdSequence(it) }.toSet()
    }
}
