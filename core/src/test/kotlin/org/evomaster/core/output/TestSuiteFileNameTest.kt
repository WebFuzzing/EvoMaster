package org.evomaster.core.output

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals

/**
 * Regression for the AsyncAPI validation finding: SUT directory names like
 * `aklivity-zilla` and `bitget-exchange` flowed through `--testSuiteFileName`
 * verbatim and produced `public class Aklivity-zilla_Test_successes` — which
 * never compiles because hyphens are not legal Java identifier characters.
 */
class TestSuiteFileNameTest {

    @Test
    fun `hyphens in class name are replaced with underscores`() {
        val name = TestSuiteFileName("Aklivity-zilla_Test")
        assertEquals("Aklivity_zilla_Test", name.name)
        assertEquals("Aklivity_zilla_Test", name.getClassName())
    }

    @Test
    fun `hyphens in each package segment are replaced`() {
        val name = TestSuiteFileName("com.foo-bar.Baz-Qux_Test")
        assertEquals("com.foo_bar.Baz_Qux_Test", name.name)
        assertEquals("com.foo_bar", name.getPackage())
        assertEquals("Baz_Qux_Test", name.getClassName())
    }

    @Test
    fun `leading digit becomes underscore`() {
        val name = TestSuiteFileName("9lives_Test")
        assertEquals("_lives_Test", name.name)
    }

    @Test
    fun `mixed special characters become underscores`() {
        val name = TestSuiteFileName("openagents-cache@v1.Foo")
        // Segments split on '.', each gets sanitised independently.
        assertEquals("openagents_cache_v1.Foo", name.name)
    }

    @Test
    fun `valid identifier passes through unchanged`() {
        val name = TestSuiteFileName("com.example.MyTest")
        assertEquals("com.example.MyTest", name.name)
        assertEquals("com.example", name.getPackage())
        assertEquals("MyTest", name.getClassName())
    }

    @Test
    fun `dollar sign in identifier is preserved`() {
        val name = TestSuiteFileName("My\$InnerClass_Test")
        assertEquals("My\$InnerClass_Test", name.name)
    }

    @Test
    fun `file path uses sanitised name`() {
        val name = TestSuiteFileName("aklivity-zilla.Foo_Test")
        assertEquals("aklivity_zilla/Foo_Test.java", name.getAsPath(OutputFormat.JAVA_JUNIT_5))
    }
}
