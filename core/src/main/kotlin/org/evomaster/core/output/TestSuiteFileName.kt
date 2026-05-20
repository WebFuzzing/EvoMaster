package org.evomaster.core.output


class TestSuiteFileName(
        rawName: String) {

    /**
     * Normalised name in the form `foo.bar.X` where each segment is a
     * valid Java/Kotlin identifier.  Hyphens and other non-identifier
     * characters in the user-supplied `--testSuiteFileName` flag are
     * coerced to underscores so the emitted public class compiles —
     * AsyncAPI black-box validation surfaced this because SUT directory
     * names like `aklivity-zilla` and `bitget-exchange` flow through as
     * `--testSuiteFileName "Aklivity-zilla_Test"` and the writer was
     * happily emitting `public class Aklivity-zilla_Test { ... }`.
     */
    val name: String = sanitise(rawName)


    fun getPackage() : String{
        if(! name.contains('.')){
            return ""
        }

        return name.substring(0, name.lastIndexOf('.'))
    }

    fun hasPackage() = getPackage().isNotBlank()


    fun getClassName(): String{
        if(! hasPackage()){
            return name
        }

        return name.substring(name.lastIndexOf('.') + 1, name.length)
    }


    fun getAsPath(format: OutputFormat) : String{

        //TODO what about C#? is it a behavior we want there as well
        val base = if(format.isJavaOrKotlin()) name.replace('.', '/') else name

        return base + when{
            format.isJava() -> ".java"
            format.isKotlin() -> ".kt"
            format.isJavaScript() -> ".js"
            format.isCsharp() -> ".cs"
            format.isPython() -> ".py"
            else -> throw IllegalStateException("Unsupported format $format")
        }
    }

    companion object {
        /**
         * Coerce each dot-separated segment of [raw] into a valid Java/Kotlin
         * identifier: leading digit → underscore-prefixed, every char that is
         * not a letter, digit, `_`, or `$` → `_`.  Empty segments stay empty
         * so a trailing dot would surface as an obvious error downstream
         * rather than being silently swallowed.
         */
        private fun sanitise(raw: String): String =
            raw.split('.').joinToString(".") { segment ->
                if (segment.isEmpty()) segment
                else buildString(segment.length + 1) {
                    val first = segment[0]
                    if (Character.isJavaIdentifierStart(first)) append(first) else append('_')
                    for (i in 1 until segment.length) {
                        val c = segment[i]
                        append(if (Character.isJavaIdentifierPart(c)) c else '_')
                    }
                }
            }
    }
}
