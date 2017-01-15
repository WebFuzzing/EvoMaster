package org.evomaster.core.output


class TestCaseWriter {

    companion object {
        fun convertToCompilableTestCode(format: OutputFormat, test: TestCase)
                : List<String> {

            val lines: MutableList<String> = mutableListOf()

            if (format.isJUnit()) {
                lines.add("@Test")
            }

            when {
                format.isJava() -> lines.add("public void ${test.name}() throws Exception {")
                format.isKotlin() -> lines.add("fun ${test.name}()  {")
            }

            val indent = "    ";
            lines.add(indent + "//TODO")
            //TODO

            lines.add("}")

            return lines
        }
    }
}