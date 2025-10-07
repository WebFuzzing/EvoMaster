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
            // === JVM (Kotlin/Java) ===

            // Standard Java exception with cause
            """
            java.lang.NullPointerException: boom
                at com.example.App.run(App.kt:42)
                at com.example.App.main(App.kt:10)
            Caused by: java.lang.IllegalStateException: oops
                at com.example.Service.doIt(Service.java:123)
            """.trimIndent(),

            // Java with nested causes and suppressed exceptions
            """
            java.lang.RuntimeException: Failed to process request
                at org.example.Controller.handle(Controller.java:89)
                at org.example.Dispatcher.dispatch(Dispatcher.java:45)
                at org.example.Main.main(Main.java:12)
            Caused by: java.sql.SQLException: Connection timeout
                at org.example.DatabaseManager.connect(DatabaseManager.java:234)
                at org.example.Repository.query(Repository.java:67)
                ... 3 more
            Caused by: java.net.SocketTimeoutException: Read timed out
                at java.base/java.net.SocketInputStream.socketRead0(Native Method)
                at java.base/java.net.SocketInputStream.socketRead(SocketInputStream.java:115)
                ... 5 more
                Suppressed: java.lang.IllegalStateException: Rollback failed
                    at org.example.TransactionManager.rollback(TransactionManager.java:156)
            """.trimIndent(),

            // Kotlin coroutine stack trace
            """
            kotlinx.coroutines.JobCancellationException: Job was cancelled
                at kotlinx.coroutines.JobSupport.cancel(JobSupport.kt:1234)
                at com.example.Worker$1doWork$1.invokeSuspend(Worker.kt:56)
                at kotlin.coroutines.jvm.internal.BaseContinuationImpl.resumeWith(ContinuationImpl.kt:33)
            Caused by: java.io.IOException: Network error
                at com.example.NetworkClient.fetch(NetworkClient.kt:78)
            """.trimIndent(),

            // Android/Java exception with "More" indicator
            """
            android.view.InflateException: Binary XML file line #12: Error inflating class
                at android.view.LayoutInflater.inflate(LayoutInflater.java:539)
                at android.view.LayoutInflater.inflate(LayoutInflater.java:423)
                at com.example.app.MainActivity.onCreate(MainActivity.java:45)
                ... 15 more
            """.trimIndent(),

            // === Python ===

            // Standard Python traceback
            """
            Traceback (most recent call last):
              File "script.py", line 12, in <module>
                main()
              File "script.py", line 8, in main
                fn()
            ValueError: bad value
            """.trimIndent(),

            // Python with chained exceptions
            """
            Traceback (most recent call last):
              File "/app/worker.py", line 45, in process_data
                result = self.parse(data)
              File "/app/parser.py", line 78, in parse
                return json.loads(data)
              File "/usr/lib/python3.9/json/__init__.py", line 346, in loads
                return _default_decoder.decode(s)
            json.decoder.JSONDecodeError: Expecting value: line 1 column 1 (char 0)

            During handling of the above exception, another exception occurred:

            Traceback (most recent call last):
              File "/app/main.py", line 23, in <module>
                worker.run()
              File "/app/worker.py", line 50, in process_data
                raise ProcessingError("Failed to process") from e
            app.errors.ProcessingError: Failed to process
            """.trimIndent(),

            // Python with multiple levels
            """
            Traceback (most recent call last):
              File "/home/user/app/main.py", line 156, in <module>
                app.start()
              File "/home/user/app/core.py", line 89, in start
                self.initialize()
              File "/home/user/app/core.py", line 134, in initialize
                self.db.connect()
            AttributeError: 'NoneType' object has no attribute 'connect'
            """.trimIndent(),

            // === C# / .NET ===

            // Standard C# exception
            """
            System.InvalidOperationException: bad op
               at MyApp.Service.Run() in C:\src\Service.cs:line 45
               at MyApp.Program.Main() in C:\src\Program.cs:line 10
            """.trimIndent(),

            // C# with inner exception
            """
            System.ApplicationException: Application failed to start
               at MyApp.Startup.Initialize() in D:\Projects\MyApp\Startup.cs:line 67
               at MyApp.Program.Main(String[] args) in D:\Projects\MyApp\Program.cs:line 23
            ---> System.IO.FileNotFoundException: Could not find config file
               at System.IO.FileStream.ValidateFileHandle(SafeFileHandle fileHandle)
               at System.IO.FileStream..ctor(String path, FileMode mode)
               at MyApp.ConfigLoader.Load() in D:\Projects\MyApp\ConfigLoader.cs:line 34
               --- End of inner exception stack trace ---
            """.trimIndent(),

            // C# async/await stack trace
            """
            System.NullReferenceException: Object reference not set to an instance of an object
               at MyApp.Controllers.UserController.<GetUserAsync>d__5.MoveNext() in C:\src\Controllers\UserController.cs:line 89
            --- End of stack trace from previous location ---
               at System.Runtime.ExceptionServices.ExceptionDispatchInfo.Throw()
               at System.Runtime.CompilerServices.TaskAwaiter.HandleNonSuccessAndDebuggerNotification(Task task)
               at Microsoft.AspNetCore.Mvc.Infrastructure.ActionMethodExecutor.AwaitableResultExecutor.Execute()
            """.trimIndent(),

            // C# with aggregate exception
            """
            System.AggregateException: One or more errors occurred. (Task failed) (Connection timeout)
               at System.Threading.Tasks.Task.ThrowIfExceptional(Boolean includeTaskCanceledExceptions)
               at System.Threading.Tasks.Task.Wait(Int32 millisecondsTimeout)
               at MyApp.Worker.ProcessAll() in C:\src\Worker.cs:line 123
            ---> (Inner Exception #0) System.InvalidOperationException: Task failed
               at MyApp.TaskRunner.Run() in C:\src\TaskRunner.cs:line 45
            ---> (Inner Exception #1) System.TimeoutException: Connection timeout
               at MyApp.Database.Connect() in C:\src\Database.cs:line 78
            """.trimIndent(),

            // === JavaScript / Node.js ===

            // Standard Node.js error
            """
            TypeError: cannot read property 'x' of undefined
                at Object.<anonymous> (/usr/app/index.js:12:5)
                at Module._compile (internal/modules/cjs/loader.js:778:30)
            """.trimIndent(),

            // Node.js with async stack trace
            """
            Error: Database connection failed
                at Database.connect (/app/db/connection.js:45:11)
                at processTicksAndRejections (node:internal/process/task_queues:95:5)
                at async Server.start (/app/server.js:89:5)
                at async main (/app/index.js:23:3)
            """.trimIndent(),

            // Browser JavaScript error
            """
            Uncaught TypeError: Cannot read properties of undefined (reading 'value')
                at HTMLButtonElement.handleClick (https://example.com/app.js:234:17)
                at HTMLButtonElement.dispatch (https://example.com/lib/jquery.min.js:3:8459)
                at HTMLButtonElement.y.handle (https://example.com/lib/jquery.min.js:3:6174)
            """.trimIndent(),

            // Node.js with multiple frames
            """
            ReferenceError: foo is not defined
                at Object.exports.run (/home/node/app/worker.js:156:23)
                at /home/node/app/main.js:89:15
                at Layer.handle [as handle_request] (/home/node/node_modules/express/lib/router/layer.js:95:5)
                at next (/home/node/node_modules/express/lib/router/route.js:137:13)
                at Route.dispatch (/home/node/node_modules/express/lib/router/route.js:112:3)
            """.trimIndent(),

            // === Ruby ===

            // Standard Ruby backtrace
            """
            /app/lib/worker.rb:21:in `perform'
            /app/lib/runner.rb:10:in `run'
            RuntimeError: kaboom
            """.trimIndent(),

            // Ruby with nested exceptions
            """
            /usr/local/bundle/gems/activerecord-6.1.4/lib/active_record/connection_adapters/abstract_adapter.rb:332:in `rescue in verify!'
            /usr/local/bundle/gems/activerecord-6.1.4/lib/active_record/connection_adapters/abstract_adapter.rb:329:in `verify!'
            /app/models/user.rb:45:in `find_by_email'
            /app/controllers/users_controller.rb:23:in `show'
            ActiveRecord::StatementInvalid: PG::ConnectionBad: connection is closed
            """.trimIndent(),

            // Ruby Rails full stack
            """
            ActionController::RoutingError (No route matches [GET] "/api/users/999"):
              app/controllers/application_controller.rb:78:in `handle_error'
              app/middleware/error_handler.rb:34:in `call'
              /usr/local/bundle/gems/rack-2.2.3/lib/rack/handler/webrick.rb:95:in `service'
            """.trimIndent(),

            // === Go ===

            // Go panic with goroutine
            """
            panic: oops
            main.go:15
            /usr/local/go/src/runtime/proc.go:203
            /usr/local/go/src/runtime/asm_amd64.s:1373
            """.trimIndent(),

            // Go with goroutine information
            """
            panic: runtime error: invalid memory address or nil pointer dereference
            [signal SIGSEGV: segmentation violation code=0x1 addr=0x0 pc=0x4a5f23]

            goroutine 1 [running]:
            main.processRequest(0xc00012a000, 0x15)
                /app/handlers/request.go:89 +0x123
            main.main()
                /app/main.go:45 +0x89
            """.trimIndent(),

            // Go with multiple goroutines
            """
            panic: send on closed channel

            goroutine 23 [running]:
            main.worker(0xc0001a4000, 0xc0001a6000)
                /home/user/app/worker.go:67 +0x234
            created by main.startWorkers
                /home/user/app/main.go:123 +0x89

            goroutine 1 [semacquire]:
            sync.runtime_Semacquire(0xc00001e0a0)
                /usr/local/go/src/runtime/sema.go:56 +0x45
            sync.(*WaitGroup).Wait(0xc00001e098)
                /usr/local/go/src/sync/waitgroup.go:130 +0x65
            main.main()
                /home/user/app/main.go:134 +0x145
            """.trimIndent(),

            // === PHP ===

            // Standard PHP stack trace
            """
            #0 /var/www/html/index.php(12): App->run()
            #1 /var/www/html/index.php(20): main()
            Fatal error: Uncaught Exception: boom in /var/www/html/index.php:12
            """.trimIndent(),

            // PHP with detailed trace
            """
            Fatal error: Uncaught TypeError: Argument 1 passed to User::setEmail() must be of type string, null given
            Stack trace:
            #0 /var/www/app/Controllers/UserController.php(45): User->setEmail(NULL)
            #1 /var/www/app/Router.php(89): UserController->update()
            #2 /var/www/public/index.php(23): Router->dispatch()
            #3 {main}
              thrown in /var/www/app/Models/User.php on line 67
            """.trimIndent(),

            // PHP exception with previous
            """
            PHP Fatal error:  Uncaught PDOException: SQLSTATE[HY000] [2002] Connection refused in /app/Database.php:34
            Stack trace:
            #0 /app/Database.php(34): PDO->__construct('mysql:host=loca...')
            #1 /app/Repository.php(23): Database->connect()
            #2 /app/UserService.php(56): Repository->query('SELECT * FROM u...')
            #3 /app/index.php(12): UserService->findAll()
            #4 {main}

            Next RuntimeException: Database connection failed in /app/Database.php:38
            Stack trace:
            #0 /app/Repository.php(23): Database->connect()
            #1 /app/UserService.php(56): Repository->query('SELECT * FROM u...')
            #2 /app/index.php(12): UserService->findAll()
            #3 {main}
            """.trimIndent(),

            // === Rust ===

            // Rust panic
            """
            thread 'main' panicked at 'index out of bounds: the len is 3 but the index is 5', src/main.rs:12:5
            stack backtrace:
               0: rust_begin_unwind
                         at /rustc/a55dd71d5fb0ec5a6a3a9e8c27b2127ba491ce52/library/std/src/panicking.rs:584:5
               1: core::panicking::panic_fmt
                         at /rustc/a55dd71d5fb0ec5a6a3a9e8c27b2127ba491ce52/library/core/src/panicking.rs:142:14
               2: myapp::process_data
                         at ./src/main.rs:12:5
               3: myapp::main
                         at ./src/main.rs:6:5
            """.trimIndent(),

            // Rust with Result unwrap
            """
            thread 'main' panicked at 'called `Result::unwrap()` on an `Err` value: Os { code: 2, kind: NotFound, message: "No such file or directory" }', src/config.rs:45:57
            stack backtrace:
               0: std::panicking::begin_panic
               1: core::result::unwrap_failed
                         at /rustc/library/core/src/result.rs:1617:5
               2: myapp::config::load_config
                         at ./src/config.rs:45
               3: myapp::main
                         at ./src/main.rs:8
            """.trimIndent(),

            // === Swift ===

            // Swift fatal error
            """
            Fatal error: Unexpectedly found nil while unwrapping an Optional value
            Current stack trace:
            0    libswiftCore.so                    0x00007f8a9c3f1234 swift_runtime_on_report
            1    MyApp                              0x0000000100003a45 specialized ViewController.loadData() + 67 (ViewController.swift:89)
            2    MyApp                              0x0000000100003456 @objc ViewController.viewDidLoad() + 34 (ViewController.swift:0)
            3    UIKitCore                          0x00007f8a9d123456 -[UIViewController _sendViewDidLoadWithAppearanceProxyObjectTaggingEnabled]
            """.trimIndent(),

            // === Dart / Flutter ===

            // Dart exception
            """
            Exception: Failed to load data
            #0      DataService.fetchData (package:myapp/services/data_service.dart:45:7)
            #1      HomeScreen.loadData (package:myapp/screens/home_screen.dart:89:23)
            #2      HomeScreen.initState (package:myapp/screens/home_screen.dart:34:5)
            #3      StatefulElement._firstBuild (package:flutter/src/widgets/framework.dart:4234:58)
            """.trimIndent(),

            // === Elixir ===

            // Elixir error
            """
            ** (RuntimeError) something went wrong
                (my_app 0.1.0) lib/my_app/worker.ex:45: MyApp.Worker.process/1
                (my_app 0.1.0) lib/my_app/server.ex:23: MyApp.Server.handle_request/2
                (stdlib 3.17) gen_server.erl:680: :gen_server.try_handle_call/4
            """.trimIndent(),

            // === Scala ===

            // Scala exception
            """
            scala.MatchError: 42 (of class java.lang.Integer)
              at com.example.Service.process(Service.scala:67)
              at com.example.Controller.handle(Controller.scala:34)
              at com.example.Main$.main(Main.scala:12)
              at com.example.Main.main(Main.scala)
            """.trimIndent()
        )

        @JvmStatic
        fun falseCases(): Stream<String> = Stream.of(
            // === Empty and simple text ===
            "",
            "Everything is fine. No errors detected.",
            "The application is running smoothly without any issues.",

            // === Time and location references that might look like stack traces ===
            "at noon we met at the station:12 — this is a time, not a stack trace.",
            "We arrived at the office at 9:30 AM.",
            "The meeting is scheduled at building:14 floor:3.",

            // === Single file:line pattern but no stack context ===
            "Check docs/app.kt:42 for details.",
            "See main.py:100 for more information.",
            "Refer to README.md:25 for installation instructions.",
            "The implementation is in service.go:88.",

            // === URLs that contain colons and numbers ===
            "Visit https://example.com:8080/api/users to see the documentation.",
            "The endpoint http://localhost:3000/health is responding.",
            "Download from ftp://files.example.com:21/archive.zip",

            // === Log messages that might have file references ===
            "INFO: Starting server on port 8080",
            "DEBUG: Loading configuration from config.yml",
            "Server started successfully at port:8080",
            "Processing file data.json with 1000 records",

            // === Code snippets without stack trace context ===
            """
            function calculateTotal(items) {
                return items.reduce((sum, item) => sum + item.price, 0);
            }
            """.trimIndent(),

            """
            def process_data(data):
                result = []
                for item in data:
                    result.append(item * 2)
                return result
            """.trimIndent(),

            // === Diff output (not a stack trace) ===
            """
            diff --git a/main.py b/main.py
            index 1234567..abcdefg 100644
            --- a/main.py
            +++ b/main.py
            @@ -10,7 +10,7 @@ def main():
            -    print("Hello")
            +    print("Hello, World!")
            """.trimIndent(),

            // === Git log output ===
            """
            commit a1b2c3d4e5f6g7h8i9j0
            Author: John Doe <john@example.com>
            Date:   Mon Jan 15 14:30:00 2024 +0000

                Update user service
            """.trimIndent(),

            // === Compiler output without errors ===
            """
            Compiling myapp v0.1.0 (/app/myapp)
                Finished dev [unoptimized + debuginfo] target(s) in 2.34s
                Running `target/debug/myapp`
            """.trimIndent(),

            """
            BUILD SUCCESSFUL in 3s
            5 actionable tasks: 5 executed
            """.trimIndent(),

            // === Test output without failures ===
            """
            Running tests...
            ✓ test_addition (0.001s)
            ✓ test_subtraction (0.001s)
            ✓ test_multiplication (0.002s)

            3 tests passed
            """.trimIndent(),

            // === Package manager output ===
            """
            npm install express
            + express@4.18.2
            added 57 packages in 3s
            """.trimIndent(),

            """
            pip install requests
            Successfully installed requests-2.28.1
            """.trimIndent(),

            // === JSON/XML data ===
            """
            {
                "name": "John Doe",
                "age": 30,
                "file": "user.json:15",
                "location": "at:home"
            }
            """.trimIndent(),

            """
            <error>
                <message>Configuration error</message>
                <location>config.xml:42</location>
            </error>
            """.trimIndent(),

            // === Markdown/Documentation ===
            """
            ## Installation

            1. Clone the repository
            2. Run `npm install`
            3. Start the server with `npm start`

            See docs/setup.md:25 for more details.
            """.trimIndent(),

            // === Database query output ===
            """
            SELECT * FROM users WHERE id = 123;

            | id  | name       | email              |
            |-----|------------|--------------------|
            | 123 | John Doe   | john@example.com   |

            1 row selected.
            """.trimIndent(),

            // === Shell script content ===
            """
            #!/bin/bash
            echo "Starting deployment..."
            cd /app
            npm run build
            echo "Deployment complete!"
            """.trimIndent(),

            // === Single-line file paths (not enough context) ===
            "app.js:42",
            "main.py:100",
            "service.go:88",
            "/usr/local/app/index.js:15",
            "C:\\Projects\\MyApp\\Service.cs:line 45",

            // === Numeric lists that might look suspicious ===
            """
            Shopping list:
            1. Buy milk
            2. Buy eggs at store:123
            3. Pick up package at building:45
            """.trimIndent(),

            // === Calendar/Schedule entries ===
            """
            Schedule for Monday:
            9:00 - Team meeting at room:401
            10:30 - Code review at desk:42
            12:00 - Lunch
            """.trimIndent(),

            // === Performance metrics ===
            """
            Performance Report:
            Response time: 123ms
            Throughput: 1000 req/s
            Memory usage: 256MB at peak:450
            """.trimIndent(),

            // === Configuration files ===
            """
            server:
              host: localhost
              port: 8080
              timeout: 30
            database:
              host: db.example.com
              port: 5432
            """.trimIndent(),

            // === Network/System info ===
            """
            Network Interfaces:
            eth0: 192.168.1.100:8080
            lo: 127.0.0.1:3000
            wlan0: 10.0.0.50:9000
            """.trimIndent(),

            // === CSV/Tabular data ===
            """
            id,name,file,line
            1,User Service,service.py,100
            2,API Handler,handler.js,200
            3,Database Manager,db.go,150
            """.trimIndent(),

            // === Comments from code ===
            """
            // TODO: Fix the bug in main.js:42
            // FIXME: Memory leak in worker.py:156
            // NOTE: See documentation at docs/api.md:78
            """.trimIndent(),

            // === Email/Message content ===
            """
            From: developer@example.com
            To: team@example.com
            Subject: Bug in production

            Hi team,

            There's an issue in the user service.
            Please check the implementation at service.js:89

            Thanks
            """.trimIndent(),

            // === API response (not an error) ===
            """
            HTTP/1.1 200 OK
            Content-Type: application/json

            {
                "status": "success",
                "data": {
                    "message": "Operation completed"
                }
            }
            """.trimIndent(),

            // === Directory listing ===
            """
            total 48
            drwxr-xr-x  5 user user 4096 Jan 15 10:30 .
            drwxr-xr-x 12 user user 4096 Jan 14 09:15 ..
            -rw-r--r--  1 user user 1234 Jan 15 10:25 main.py
            -rw-r--r--  1 user user 5678 Jan 15 10:28 service.py
            """.trimIndent(),

            // === Version control status ===
            """
            On branch main
            Your branch is up to date with 'origin/main'.

            nothing to commit, working tree clean
            """.trimIndent(),

            // === Mixed content that might be confusing ===
            """
            The error occurred at line:42 in the file.
            This is a description, not a stack trace.
            """.trimIndent(),

            // === Single stack trace line without context ===
            "at java.lang.String.valueOf(String.java:123)"
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
