package org.evomaster.core.output.service

import org.evomaster.core.EMConfig
import org.evomaster.core.output.Lines
import java.nio.file.Files
import java.nio.file.Paths

class PythonUtilsFileWriter {

    private val utilsFilename = "em_test_utils.py"

    fun writePythonUtilsFile(config: EMConfig) {
        val lines = Lines(config.outputFormat)
        addImports(lines)
        writeResolveLocation(lines)
        writeIsValidUriOrEmpty(lines)
        writeExpectationHandler(lines)
        saveToDisk(lines.toString(), config)
    }

    private fun addImports(lines: Lines) {
        lines.add("from urllib.parse import urlparse")
        lines.add("import validators")
        lines.addEmpty(2)
    }

    private fun writeResolveLocation(lines: Lines) {
        lines.add("def resolve_location(location_header: str, expected_template: str) -> str:")
        lines.indent()
        lines.add("if not location_header:")
        lines.indented {
            lines.add("return expected_template")
        }
        lines.addEmpty()
        lines.add("location_uri = urlparse(location_header)")
        lines.add("location_path = location_uri.path")
        lines.add("location_tokens = location_path.split('/')")
        lines.addEmpty()
        lines.add("normalized_template = expected_template.replace('{', '').replace('}', '')")
        lines.add("template_uri = urlparse(normalized_template)")
        lines.add("template_path = template_uri.path")
        lines.add("template_tokens = template_path.split('/')")
        lines.addEmpty()
        lines.add("target_path = location_path")
        lines.add("if len(template_tokens) > len(location_tokens):")
        lines.indent()
        lines.add("for i in range(len(location_tokens), len(template_tokens)):")
        lines.indent()
        lines.add("target_path += '/' + template_tokens[i]")
        lines.deindent(2)
        lines.addEmpty()
        lines.add("target_uri = location_uri if location_uri.hostname else template_uri")
        lines.add("target_uri = target_uri._replace(path=target_path)")
        lines.add("return target_uri.geturl()")
        lines.deindent()
        lines.addEmpty(2)
    }

    private fun writeIsValidUriOrEmpty(lines: Lines) {
        lines.add("def is_valid_uri_or_empty(uri: str):")
        lines.indent()
        lines.add("if uri == \"\":")
        lines.indented {
            lines.add("return True")
        }
        lines.add("try:")
        lines.indented {
            lines.add("validators.url(uri, r_ve=True)")
        }
        lines.add("except Exception as e:")
        lines.indented {
            lines.add("return False")
        }
        lines.add("return True")
        lines.deindent()
        lines.addEmpty(2)
    }

    private fun writeExpectationHandler(lines: Lines) {
        lines.add("class ExpectationHandler:")
        lines.addEmpty()
        lines.indent()
        lines.add("def __init__(self):")
        lines.indented {
            lines.add("self.master_switch = False")
        }
        lines.addEmpty()
        lines.add("def expect(self, master_switch=False):")
        lines.indented {
            lines.add("self.master_switch = master_switch")
            lines.add("return self")
        }
        lines.addEmpty()
        lines.add("def that(self, active, condition):")
        lines.indent()
        lines.add("if (not active) or (not self.master_switch):")
        lines.indented {
            lines.add("return self")
        }
        lines.add("if not condition:")
        lines.indented {
            lines.add("raise AssertionError('Failed Expectation Exception')")
        }
        lines.add("return self")
        lines.deindent(2)
        lines.addEmpty(2)
    }

    private fun saveToDisk(
        testFileContent: String,
        config: EMConfig
    ) {

        val path = Paths.get(config.outputFolder, utilsFilename)

        Files.createDirectories(path.parent)
        Files.deleteIfExists(path)
        Files.createFile(path)

        path.toFile().appendText(testFileContent)
    }
}
