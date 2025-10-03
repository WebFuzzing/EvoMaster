import org.evomaster.core.utils.StackTraceUtils
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class StackTraceUtilsTest {

    companion object {
        @JvmStatic
        fun trueCases(): Stream<String> = Stream.of(
            // JVM (Kotlin/Java)
            """
            java.lang.NullPointerException: boom
                at com.example.App.run(App.kt:42)
                at com.example.App.main(App.kt:10)
            Caused by: java.lang.IllegalStateException: oops
                at com.example.Service.doIt(Service.java:123)
            """.trimIndent(),

            // Python
            """
            Traceback (most recent call last):
              File "script.py", line 12, in <module>
                main()
              File "script.py", line 8, in main
                fn()
            ValueError: bad value
            """.trimIndent(),

            // C#
            """
            System.InvalidOperationException: bad op
               at MyApp.Service.Run() in C:\src\Service.cs:line 45
               at MyApp.Program.Main() in C:\src\Program.cs:line 10
            """.trimIndent(),

            // Node.js
            """
            TypeError: cannot read property 'x' of undefined
                at Object.<anonymous> (/usr/app/index.js:12:5)
                at Module._compile (internal/modules/cjs/loader.js:778:30)
            """.trimIndent(),

            // Ruby
            """
            /app/lib/worker.rb:21:in `perform'
            /app/lib/runner.rb:10:in `run'
            RuntimeError: kaboom
            """.trimIndent(),

            // Go
            """
            panic: oops
            main.go:15
            /usr/local/go/src/runtime/proc.go:203
            /usr/local/go/src/runtime/asm_amd64.s:1373
            """.trimIndent(),

            // PHP
            """
            #0 /var/www/html/index.php(12): App->run()
            #1 /var/www/html/index.php(20): main()
            Fatal error: Uncaught Exception: boom in /var/www/html/index.php:12
            """.trimIndent()
        )

        @JvmStatic
        fun falseCases(): Stream<String> = Stream.of(
            "",
            "Everything is fine. No errors detected.",
            "at noon we met at the station:12 — this is a time, not a stack trace.",
            // Single file:line pattern but no stack context
            "Check docs/app.kt:42 for details."
        )
    }

    @Nested
    @DisplayName("Positive cases")
    inner class PositiveCases {
        @ParameterizedTest
        @MethodSource("StackTraceUtilsTest#trueCases")
        fun `should detect stack traces`(text: String) {
            assertTrue(StackTraceUtils.looksLikeStackTrace(text))
        }
    }

    @Nested
    @DisplayName("Negative cases")
    inner class NegativeCases {
        @ParameterizedTest
        @MethodSource("StackTraceUtilsTest#falseCases")
        fun `should not detect non stack traces`(text: String) {
            assertFalse(StackTraceUtils.looksLikeStackTrace(text))
        }
    }

    @Test
    fun `threshold tuning demo`() {
        val text = """
            service.go:10
            controller.go:55
            handler.go:78
        """.trimIndent()
        assertTrue(StackTraceUtils.looksLikeStackTrace(text))
    }
}
