package org.evomaster.e2etests.spring.openapi.v3.expectations

import com.foo.rest.examples.spring.openapi.v3.expectations.ExpectationBasicTestController
import org.evomaster.client.java.instrumentation.shared.ClassName
import org.evomaster.core.EMConfig
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertTimeoutPreemptively
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Duration

// expectations are removed
@Disabled
class ExpectationBasicEMTest : SpringTestBase() {
    companion object{
        @BeforeAll
        @JvmStatic
        fun init(){
            initClass(ExpectationBasicTestController())
        }
    }

    @Test
    fun testRunEMExpectationsOff() {
        val outputFolderName = "ExpectationsBasicEM"
        val className = ClassName("org.foo.ExpectationBasicEMOff")
        val splitType = EMConfig.TestSuiteSplitType.NONE
        testRunEMGeneric(false, splitType, className)

        val assertion = generatedCodeAssertion(outputFolderName, className.bytecodeName, OutputFormat.KOTLIN_JUNIT_5, false)
        Assertions.assertTrue(assertion)
        compileRunAndVerifyTests(outputFolderName, className)
    }

    @Test
    fun testRunEMExpectationsOn() {
        val outputFolderName = "ExpectationsBasicEM"
        val className = ClassName("org.foo.ExpectationBasicEMOn")
        val splitType = EMConfig.TestSuiteSplitType.NONE
        testRunEMGeneric(true, splitType, className)

        val assertion = generatedCodeAssertion(outputFolderName, className.bytecodeName, OutputFormat.KOTLIN_JUNIT_5, true)
        Assertions.assertTrue(assertion)
        compileRunAndVerifyTests(outputFolderName, className)
    }

    @Test
    fun testRunEMJavaExpectationsOff() {
        val outputFolderName = "ExpectationsBasicEM"
        val className = ClassName("org.foo.ExpectationBasicEMJavaOff")
        val splitType = EMConfig.TestSuiteSplitType.NONE

        testRunEMGeneric(false, splitType, className, OutputFormat.JAVA_JUNIT_5)

        val assertion = generatedCodeAssertion(outputFolderName, className.bytecodeName, OutputFormat.JAVA_JUNIT_5, false)
        Assertions.assertTrue(assertion)
    }

    @Test
    fun testRunEMJavaExpectationsOn() {
        val outputFolderName = "ExpectationsBasicEM"
        val className = ClassName("org.foo.ExpectationBasicEMJavaOn")
        val splitType = EMConfig.TestSuiteSplitType.NONE

        testRunEMGeneric(true, splitType, className, OutputFormat.JAVA_JUNIT_5)

        val assertion = generatedCodeAssertion(outputFolderName, className.bytecodeName, OutputFormat.JAVA_JUNIT_5, true)
        Assertions.assertTrue(assertion)
    }

    // Tests to check the split and default values
    @Test
    fun testRunEM_Split_ExpectationsOff() {
        val outputFolderName = "ExpectationsBasicEM"
        val className = ClassName("org.foo.ExpectationBasicEMOff")
        val splitType = EMConfig.TestSuiteSplitType.FAULTS
        testRunEMGeneric(false, splitType, className)

        val assertion = generatedCodeAssertion(outputFolderName, "${className.bytecodeName}_faults", OutputFormat.KOTLIN_JUNIT_5, false)
        Assertions.assertTrue(assertion)
        //compileRunAndVerifyTests(outputFolderName, ClassName("${className.bytecodeName}_faults"))
    }

    fun generatedCodeAssertion(outputFolderName: String, className: String, outputFormat: OutputFormat, expectationsActive: Boolean): Boolean {
        val extension = when {
            outputFormat.isJava() -> ".java"
            outputFormat.isKotlin() -> ".kt"
            else -> return false
        }
        val assertion = Files.lines(Paths.get(outputFolderPath(outputFolderName)
                + "/"
                + className
                + extension))

        return if(expectationsActive) assertion.anyMatch { l: String -> l.contains("expectationHandler.expect(ems)") }
        else assertion.noneMatch { l: String -> l.contains("expectationHandler.expect(ems)") }
    }

    fun setExpectations(args: MutableList<String>, expectationsActive: Boolean){
        val ind = args.indexOf("--expectationsActive") + 1
        if (ind == -1) {
            args.add("--expectationsActive")
            args.add(expectationsActive.toString())
        }
        else args.set(ind, expectationsActive.toString())
    }

    fun setSplitType(args: MutableList<String>, splitType: EMConfig.TestSuiteSplitType){
        val ind = args.indexOf("--testSuiteSplitType") + 1
        if (ind == -1){
            args.add("--testSuiteSplitType")
            args.add(splitType.name)
        }
        else args.set(ind, splitType.name)
    }


    fun testRunEMGeneric(expectationActive: Boolean = true,
                         testSuiteSplitType: EMConfig.TestSuiteSplitType = EMConfig.TestSuiteSplitType.NONE,
                         className: ClassName,
                         outputFormat: OutputFormat? = OutputFormat.KOTLIN_JUNIT_5){
        val outputFolderName = "ExpectationsBasicEM"
        val iterations = 1000

        val lambda = {args: MutableList<String> ->
            setOutputFormat(args, outputFormat)

            setExpectations(args, expectationActive)
            setSplitType(args, testSuiteSplitType)
            /*if(!expectationActive){
                val ind = args.indexOf("--expectationsActive") + 1
                args.set(ind, "false")
            }*/

            val solution = initAndRun(args)
            Assertions.assertTrue(solution.individuals.size >= 1)

            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/basic/exp/okString/{s}", "Success!")

            assertHasAtLeastOne(solution, HttpVerb.GET, 500, "/api/basic/exp/badString/{s}", null)

            assertHasAtLeastOne(solution, HttpVerb.GET, 500, "/api/basic/exp/failString/{s}", null)

        }

        assertTimeoutPreemptively(Duration.ofMinutes(3)) {
            clearGeneratedFiles(outputFolderName, className)
            handleFlaky {
                val args : MutableList<String> = getArgsWithCompilation(iterations,
                        outputFolderName,
                        className,
                        true)
                setOutputFormat(args, outputFormat)

                lambda.invoke(args)
            }
        }

    }
}