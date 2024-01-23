package org.evomaster.e2etests.spring.examples.filecreationissue;

import com.foo.rest.examples.spring.endpointfilter.EndpointFilterController;
import com.foo.rest.examples.spring.endpointfocusandprefix.EndpointFocusAndPrefixController;
import org.evomaster.core.problem.rest.HttpVerb;
import org.evomaster.core.problem.rest.RestIndividual;
import org.evomaster.core.search.Solution;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FileCreationIssueEMTest extends SpringTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        SpringTestBase.initClass(new EndpointFocusAndPrefixController());
    }

    /**
     * If executive summary is requested, the name of the file should be
     * EvoMaster_fault_representatives_Test.java, there should not be a file
     * with name EvoMaster_Test.java
     * @throws Throwable
     */
    @Test
    public void testFileCreationIssueWithSummary() throws Throwable {

        File outFolder = null;

        try {

            String outFolderName = "fileCreationIssueTest";

            List args = new ArrayList<String>();

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
            args.add("30s");

            // run evomaster
            initAndRun(args);

            // check that the folder org/foo/FileCreationIssue contains the file: EvoMaster_fault_representatives_Test
            // and does not contain the file EvoMaster_Test.java
            String currentDir = System.getProperty("user.dir");

            String filePath = currentDir + "/" + outFolderName;

            outFolder = new File(filePath);

            Assert.assertTrue(outFolder.exists() && outFolder.isDirectory());

            String files[] = outFolder.list();

            boolean summaryFound = false;
            boolean emptyFound = false;

            for (String s : files) {
                if (s.equals("EvoMaster_fault_representatives_Test.java")) {
                    summaryFound = true;
                } else if (s.equals("EvoMaster_Test.java")) {
                    emptyFound = true;
                }
            }

            // summary should be found, empty should not be found
            Assert.assertTrue(summaryFound);
            Assert.assertFalse(emptyFound);
        } catch(Exception e) {
            e.printStackTrace();
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
     * @throws Throwable
     */
    @Test
    public void testFileCreationIssueWithoutSummary() throws Throwable {

        File outFolder = null;

        try {

            String outFolderName = "fileCreationIssueTest";

            List args = new ArrayList<String>();

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
            args.add("30s");

            // run evomaster
            initAndRun(args);

            // check that the folder org/foo/FileCreationIssue contains the file: EvoMaster_fault_representatives_Test
            // and does not contain the file EvoMaster_Test.java
            String currentDir = System.getProperty("user.dir");

            String filePath = currentDir + "/" + outFolderName;

            outFolder = new File(filePath);

            Assert.assertTrue(outFolder.exists() && outFolder.isDirectory());

            String files[] = outFolder.list();

            boolean summaryFound = false;
            boolean emptyFound = false;

            for (String s : files) {
                if (s.equals("EvoMaster_fault_representatives_Test.java")) {
                    summaryFound = true;
                } else if (s.equals("EvoMaster_Test.java")) {
                    emptyFound = true;
                }
            }

            // summary should be found, empty should not be found
            Assert.assertFalse(summaryFound);
            Assert.assertFalse(emptyFound);
        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            if (outFolder != null) {
                if(outFolder.exists()) {
                    FileUtils.deleteDirectory(outFolder);
                }
            }
        }
    }
}