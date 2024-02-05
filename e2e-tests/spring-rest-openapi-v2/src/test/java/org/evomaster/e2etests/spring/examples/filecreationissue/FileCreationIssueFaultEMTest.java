package org.evomaster.e2etests.spring.examples.filecreationissue;

import com.foo.rest.examples.spring.filecreationissuewithfault.FileCreationIssueWithFaultController;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileCreationIssueFaultEMTest extends SpringTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        SpringTestBase.initClass(new FileCreationIssueWithFaultController());
    }

    /**
     * If executive summary is requested, the name of the file should be
     * EvoMaster_fault_representatives_Test.java, there should not be a file
     * with name EvoMaster_Test.java
     * @throws Throwable - exception is thrown and test fails if the outputFolder is not a folder.
     */
    @Test
    public void testFileCreationIssueWithSummary() throws Throwable {

        File outFolder = null;

        try {

            String outFolderName = "fileCreationIssueTest";

            List<String> args = new ArrayList<>();

            args.add("--blackBox");
            args.add("true");
            args.add("--bbTargetUrl");
            args.add(baseUrlOfSut);
            args.add("--bbSwaggerUrl");
            args.add(baseUrlOfSut + "/v2/api-docs");
            args.add("--outputFormat");
            args.add("JAVA_JUNIT_4");
            args.add("--outputFolder");
            args.add(outFolderName);
            args.add("--executiveSummary");
            args.add("true");
            args.add("--maxTime");
            args.add("5s");

            // run evomaster
            Solution sol = initAndRun(args);

            // check that the folder org/foo/FileCreationIssue contains the file: EvoMaster_fault_representatives_Test
            // and does not contain the file EvoMaster_Test.java
            String currentDir = System.getProperty("user.dir");

            String filePath = currentDir + "/" + outFolderName;

            outFolder = new File(filePath);

            // if we are testing with faults, summary file is expected but the empty file is not
            // expected.
            Assert.assertTrue(
                    FileCreationIssueTestHelper.checkExistenceOfRelevantFilesInDirectory(
                            outFolder,
                            sol.getTestSuiteNamePrefix(),
                            sol.getTestSuiteNameSuffix(),
                            true,
                            false));
        } finally {
            if (outFolder != null) {
                if(outFolder.exists()) {
                    FileUtils.deleteDirectory(outFolder);
                }
            }
        }
    }

    /**
     * If executive summary is not requested, there should not be a file with name
     * EvoMaster_fault_representatives_Test.java, and there should not be a file
     * with name EvoMaster_Test.java
     * @throws Throwable - exception is thrown and test fails if the outputFolder is not a folder.
     */
    @Test
    public void testFileCreationIssueWithoutSummary() throws Throwable {

        File outFolder = null;

        try {

            String outFolderName = "fileCreationIssueTest";

            List<String> args = new ArrayList<>();

            args.add("--blackBox");
            args.add("true");
            args.add("--bbTargetUrl");
            args.add(baseUrlOfSut);
            args.add("--bbSwaggerUrl");
            args.add(baseUrlOfSut + "/v2/api-docs");
            args.add("--outputFormat");
            args.add("JAVA_JUNIT_4");
            args.add("--outputFolder");
            args.add(outFolderName);
            args.add("--executiveSummary");
            args.add("false");
            args.add("--maxTime");
            args.add("5s");

            // run evomaster
            Solution sol = initAndRun(args);

            // check that the folder org/foo/FileCreationIssue contains the file: EvoMaster_fault_representatives_Test
            // and does not contain the file EvoMaster_Test.java
            String currentDir = System.getProperty("user.dir");

            String filePath = currentDir + "/" + outFolderName;

            outFolder = new File(filePath);

            // if we are testing with faults, summary file is expected but the empty file is not
            // expected.
            Assert.assertTrue(
                    FileCreationIssueTestHelper.checkExistenceOfRelevantFilesInDirectory(
                            outFolder,
                            sol.getTestSuiteNamePrefix(),
                            sol.getTestSuiteNameSuffix(),
                            false,
                            false));


        } finally {
            if (outFolder != null) {
                if(outFolder.exists()) {
                    FileUtils.deleteDirectory(outFolder);
                }
            }
        }
    }
}