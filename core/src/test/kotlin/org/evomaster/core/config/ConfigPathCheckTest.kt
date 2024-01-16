package org.evomaster.core.config

import org.evomaster.core.EMConfig

import org.jetbrains.kotlin.incremental.createDirectory

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

import java.io.File

import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.Paths


class ConfigPathCheckTest {

    // the path to write tests
    private val pathToWriteTests = "path1/path2/path3"
    private val rootPath = "path1"

    /**
     * The full path does not exist and the currently working directory is read-only so the necessary folders
     * cannot be created. In this case, an exception should be thrown before starting tests
     */
    @Test
    fun checkNonExistingDirectoryCannotBeCreated() {

        val args = arrayOf("--outputFolder", pathToWriteTests)

        val filePathRoot = File(rootPath)

        val absoluteFilePath = File(pathToWriteTests).absolutePath

        try {

            // if the file with that name already exists,
            if (filePathRoot.exists()) {
                throw FileAlreadyExistsException(
                    "File with name: ${filePathRoot.name} already exists " +
                            "in the current working directory"
                )
            }

            // create directory
            filePathRoot.createDirectory()

            // set the file to read only
            filePathRoot.setReadOnly()

            val exception = Assertions.assertThrows(
                IllegalArgumentException::class.java
            ) {
                EMConfig.validateOptions(args)
            }

            Assertions.assertEquals(exception.message, "Parameter 'outputFolder' refers to a file that does not exist, " +
                    "but the provided file path cannot be used to create a directory: $absoluteFilePath\n" +
                    "Please check file permissions of parent directories")
        }
        catch (e : Exception) {

            System.err.println("Exception occurred during the test case: checkNonExistingDirectoryCannotBeCreated")
            e.printStackTrace()
        }
        finally {
            // delete the folder created for test case.
            filePathRoot.deleteRecursively()
        }
    }

    /**
     * The directory to write tests exists, but it is read-only.
     */
    @Test
    fun checkExistingDirectoryReadOnly() {

        val args = arrayOf("--outputFolder", pathToWriteTests)

        val filePathRoot = File(rootPath)

        val file = File(pathToWriteTests)

        val absolutePath = file.absolutePath

        try {

            if (file.exists()) {
                throw FileAlreadyExistsException(
                    "File with name: path1 already exists in the current working directory"
                )
            }

            // create directory and set it read only
            file.createDirectory()
            file.setReadOnly()

            val exception = Assertions.assertThrows(
                IllegalArgumentException::class.java
            ) {
                EMConfig.validateOptions(args)
            }

            Assertions.assertEquals(
                exception.message, "Parameter 'outputFolder' refers to a folder that already " +
                        "exists, but that cannot be written to: $absolutePath"
            )
        } catch (e : Exception) {

            System.err.println("Exception occurred during the test case: checkExistingDirectoryReadOnly")
            e.printStackTrace()
        }
        finally {
            // delete the folder created for test case.
            filePathRoot.deleteRecursively()
        }
    }

    /**
     * The path to the directory to write tests is a file, not a folder.
     */
    @Test
    fun checkExistingFileNotDirectoryReadOnly() {
        val args = arrayOf("--outputFolder", pathToWriteTests)

        val filePathRoot = File("path1")
        val file = File(pathToWriteTests)
        val absolutePath = file.absolutePath

        try {

            if (file.exists()) {
                throw FileAlreadyExistsException(
                    "File with name: path1 already exists in the current working " +
                            "directory"
                )
            }

            // create directories for path2
            Files.createDirectories(Paths.get("path1/path2"))

            // create path3 as a new file
            file.createNewFile()

            val exception = Assertions.assertThrows(
                IllegalArgumentException::class.java
            ) {
                EMConfig.validateOptions(args)
            }

            Assertions.assertEquals(
                exception.message, "Parameter 'outputFolder' refers to a file that already" +
                        " exists, but that it is not a folder: $absolutePath"
            )
        }
        catch (e : Exception) {

            System.err.println("Exception occurred during the test case: checkExistingFileNotDirectoryReadOnly")
            e.printStackTrace()
        }
        finally {
            // delete path to write tests
            filePathRoot.deleteRecursively()
        }
    }

    @Test
    fun checkDirectoryWithInvalidPathGiven() {

        val args = arrayOf("--outputFolder", "\u0000")

        val exception = Assertions.assertThrows(
            IllegalArgumentException::class.java
        ) {
            EMConfig.validateOptions(args)
        }

        Assertions.assertEquals(exception.message, "Parameter 'outputFolder' is not a valid FS path: " +
                "Nul character not allowed: \u0000")
    }
}