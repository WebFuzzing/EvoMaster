package org.evomaster.e2etests.spring.examples.filecreationissue;

import org.evomaster.core.output.Termination;

import java.io.File;

/**
 * This is a simple class for avoiding code duplication in the test cases of file creation issue.
 */
public class FileCreationIssueTestHelper {

    /**
     * This method checks if the executive summary file and the empty
     * @param directory - directory containing test files.
     * @param filenamePrefix - Prefix of the filename
     * @param filenameSuffix - Suffix of the filename
     * @param summaryFileExpected
     * @param emptyFileExpected
     * @return
     */
    public static boolean checkExistenceOfRelevantFilesInDirectory(File directory,
                                                                   String filenamePrefix,
                                                                   String filenameSuffix,
                                                                   boolean summaryFileExpected,
                                                                   boolean emptyFileExpected) {


        // if the directory does not exist or if it is not a directory
        if (!directory.exists() || !directory.isDirectory()) {
            throw new RuntimeException("Directory " + directory + " does not exist or is not a directory");
        }
        else {
            // files in the current directory
            File[] files = directory.listFiles();

            // summary and empty found
            boolean summaryFound = false;
            boolean emptyFound = false;

            // filename
            String fileName = "";

            for (File f : files) {

                fileName = f.getName();

                if (fileName.contains(filenamePrefix + "_" + filenameSuffix)) {
                    emptyFound = true;
                }
                else if (fileName.contains(filenamePrefix + Termination.SUMMARY.getSuffix() + "_" + filenameSuffix)) {
                    summaryFound = true;
                }

            }
            // check with expected values.
            if (emptyFound == emptyFileExpected && summaryFound == summaryFileExpected) {
                return true;
            }
            else {
                return false;
            }

        }



    }

}
