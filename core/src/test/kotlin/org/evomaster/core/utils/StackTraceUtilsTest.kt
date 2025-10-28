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
            """.trimIndent(),

            // === Additional JVM Variants ===

            // Java with suppressed and multiple causes
            """
            java.util.concurrent.ExecutionException: Task execution failed
                at java.base/java.util.concurrent.FutureTask.report(FutureTask.java:122)
                at java.base/java.util.concurrent.FutureTask.get(FutureTask.java:191)
                at com.example.TaskExecutor.run(TaskExecutor.java:45)
            Caused by: java.lang.IllegalArgumentException: Invalid parameter
                at com.example.Validator.validate(Validator.java:78)
                at com.example.Task.execute(Task.java:34)
                ... 3 more
                Suppressed: java.io.IOException: Failed to cleanup
                    at com.example.ResourceManager.cleanup(ResourceManager.java:123)
                    at com.example.Task.finally$0(Task.java:89)
                Suppressed: java.sql.SQLException: Failed to close connection
                    at com.example.DatabaseConnection.close(DatabaseConnection.java:234)
            """.trimIndent(),

            // Kotlin with line numbers and Native Method
            """
            kotlin.UninitializedPropertyAccessException: lateinit property has not been initialized
                at com.example.UserService.getUser(UserService.kt:56)
                at com.example.UserController.handleRequest(UserController.kt:89)
                at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
                at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
            """.trimIndent(),

            // Android exception with resource
            """
            android.content.res.Resources$1NotFoundException: Resource ID #0x7f030001
                at android.content.res.ResourcesImpl.getValue(ResourcesImpl.java:220)
                at android.content.res.Resources.loadXmlResourceParser(Resources.java:2106)
                at com.example.MyActivity.onCreate(MyActivity.java:67)
                at android.app.Activity.performCreate(Activity.java:7224)
                at android.app.Instrumentation.callActivityOnCreate(Instrumentation.java:1271)
            """.trimIndent(),

            // === Additional Python Variants ===

            // Python with nested imports
            """
            Traceback (most recent call last):
              File "/usr/local/lib/python3.9/site-packages/requests/adapters.py", line 439, in send
                resp = conn.urlopen(
              File "/usr/local/lib/python3.9/site-packages/urllib3/connectionpool.py", line 696, in urlopen
                httplib_response = self._make_request(
              File "/usr/local/lib/python3.9/site-packages/urllib3/connectionpool.py", line 394, in _make_request
                conn.request(method, url, **httplib_request_kw)
            urllib3.exceptions.ProtocolError: ('Connection aborted.', ConnectionResetError(104, 'Connection reset by peer'))
            """.trimIndent(),

            // Python asyncio exception
            """
            Traceback (most recent call last):
              File "/app/main.py", line 23, in <module>
                asyncio.run(main())
              File "/usr/lib/python3.9/asyncio/runners.py", line 44, in run
                return loop.run_until_complete(main)
              File "/app/handlers.py", line 67, in handle_request
                await process_data(data)
            asyncio.TimeoutError
            """.trimIndent(),

            // === Additional C# Variants ===

            // C# with multiple inner exceptions
            """
            System.AggregateException: One or more errors occurred. (Database timeout) (Network failure) (Validation error)
             ---> System.TimeoutException: Database timeout
               at DatabaseService.ExecuteQuery() in C:\src\DatabaseService.cs:line 145
               at UserRepository.GetUsers() in C:\src\UserRepository.cs:line 67
               ---> System.Data.SqlClient.SqlException: Timeout expired
                  at System.Data.SqlClient.SqlConnection.OnError(SqlException exception)
               --- End of inner exception stack trace ---
             ---> System.Net.Http.HttpRequestException: Network failure
               at HttpClientService.SendRequest() in C:\src\HttpClientService.cs:line 234
            """.trimIndent(),

            // C# TaskCanceledException
            """
            System.Threading.Tasks.TaskCanceledException: A task was canceled.
               at System.Runtime.CompilerServices.TaskAwaiter.ThrowForNonSuccess(Task task)
               at System.Runtime.CompilerServices.TaskAwaiter.HandleNonSuccessAndDebuggerNotification(Task task)
               at MyApp.Services.DataService.<FetchDataAsync>d__12.MoveNext() in C:\Projects\MyApp\Services\DataService.cs:line 178
            --- End of stack trace from previous location ---
               at MyApp.Controllers.ApiController.<GetData>d__5.MoveNext() in C:\Projects\MyApp\Controllers\ApiController.cs:line 89
            """.trimIndent(),

            // === Additional JavaScript/Node.js Variants ===

            // Node.js Promise rejection
            """
            UnhandledPromiseRejectionWarning: Error: Failed to connect
                at Database.connect (/app/lib/database.js:234:15)
                at processTicksAndRejections (internal/process/task_queues:93:5)
                at async Server.initialize (/app/server.js:45:5)
                at async bootstrap (/app/index.js:12:3)
            UnhandledPromiseRejectionWarning: Unhandled promise rejection
            """.trimIndent(),

            // TypeScript with source maps
            """
            TypeError: Cannot read property 'name' of null
                at UserService.getUsername (/app/dist/services/UserService.js:45:23)
                at UserController.handleRequest (/app/dist/controllers/UserController.js:89:41)
                at Layer.handle [as handle_request] (/app/node_modules/express/lib/router/layer.js:95:5)
                at next (/app/node_modules/express/lib/router/route.js:137:13)
            """.trimIndent(),

            // === Additional Go Variants ===

            // Go panic with defer
            """
            panic: runtime error: slice bounds out of range [5:3]

            goroutine 1 [running]:
            main.processSlice(0xc00010a000, 0x5, 0x3)
                /home/user/project/processor.go:123 +0x234
            main.handleData(...)
                /home/user/project/handler.go:45
            main.main()
                /home/user/project/main.go:23 +0x156
            """.trimIndent(),

            // Go with HTTP handler
            """
            panic: interface conversion: interface {} is nil, not string

            goroutine 45 [running]:
            main.(*Handler).ServeHTTP(0xc0001a2000, 0x1234560, 0xc0001a4000, 0xc0001a6000)
                /go/src/myapp/handlers/api.go:234 +0x567
            net/http.(*ServeMux).ServeHTTP(0xc000178000, 0x1234560, 0xc0001a4000, 0xc0001a6000)
                /usr/local/go/src/net/http/server.go:2436 +0x1a5
            net/http.serverHandler.ServeHTTP(0xc00017a0e0, 0x1234560, 0xc0001a4000, 0xc0001a6000)
                /usr/local/go/src/net/http/server.go:2831 +0xa4
            """.trimIndent(),

            // === Additional PHP Variants ===

            // PHP with Composer/vendor paths
            """
            Fatal error: Uncaught TypeError: Argument 2 passed to App\Services\UserService::updateUser() must be of type array, null given
            Stack trace:
            #0 /var/www/vendor/laravel/framework/src/Illuminate/Routing/Controller.php(54): App\Services\UserService->updateUser()
            #1 /var/www/vendor/laravel/framework/src/Illuminate/Routing/ControllerDispatcher.php(45): Illuminate\Routing\Controller->callAction()
            #2 /var/www/app/Http/Controllers/UserController.php(123): App\Http\Controllers\UserController->update()
            #3 {main}
              thrown in /var/www/app/Services/UserService.php on line 89
            """.trimIndent(),

            // PHP warning followed by fatal error
            """
            Warning: Division by zero in /var/www/app/Calculator.php on line 45

            Fatal error: Uncaught DivisionByZeroError: Division by zero in /var/www/app/Calculator.php:45
            Stack trace:
            #0 /var/www/app/MathService.php(67): Calculator->divide(10, 0)
            #1 /var/www/public/index.php(23): MathService->calculate()
            #2 {main}
              thrown in /var/www/app/Calculator.php on line 45
            """.trimIndent(),

            // === Additional Ruby Variants ===

            // Ruby with Rails and ActiveRecord
            """
            ActiveRecord::RecordNotFound (Couldn't find User with 'id'=999):
              app/controllers/users_controller.rb:23:in `show'
              actionpack (6.1.4) lib/action_controller/metal/basic_implicit_render.rb:6:in `send_action'
              actionpack (6.1.4) lib/action_controller/metal/implicit_render.rb:12:in `default_render'
              actionpack (6.1.4) lib/abstract_controller/base.rb:228:in `process_action'
            """.trimIndent(),

            // Ruby with block syntax
            """
            NoMethodError (undefined method `fetch' for nil:NilClass):
              app/services/data_fetcher.rb:45:in `block in fetch_all'
              app/services/data_fetcher.rb:43:in `each'
              app/services/data_fetcher.rb:43:in `fetch_all'
              app/workers/sync_worker.rb:23:in `perform'
            """.trimIndent(),

            // === Additional Rust Variants ===

            // Rust with custom panic message
            """
            thread 'tokio-runtime-worker' panicked at 'called `Option::unwrap()` on a `None` value', src/handlers/api.rs:234:45
            stack backtrace:
               0: rust_begin_unwind
                         at /rustc/9bc8c42bb2f19e745a63f3445f1ac248fb015e53/library/std/src/panicking.rs:584:5
               1: core::panicking::panic_fmt
                         at /rustc/9bc8c42bb2f19e745a63f3445f1ac248fb015e53/library/core/src/panicking.rs:142:14
               2: core::panicking::panic
                         at /rustc/9bc8c42bb2f19e745a63f3445f1ac248fb015e53/library/core/src/panicking.rs:48:5
               3: myapp::handlers::api::process_request
                         at ./src/handlers/api.rs:234:45
               4: tokio::runtime::task::core::CoreStage<T>::poll
                         at /home/user/.cargo/registry/src/tokio-1.21.0/src/runtime/task/core.rs:184:17
            """.trimIndent(),

            // === Additional Swift Variants ===

            // Swift with optional unwrapping
            """
            Fatal error: Unexpectedly found nil while implicitly unwrapping an Optional value
            2023-01-15 14:30:45.123456+0000 MyApp[12345:67890] Fatal error: Unexpectedly found nil while implicitly unwrapping an Optional value
            Current stack trace:
            0    libswiftCore.so                    0x00007f9a1c3f4567 _swift_runtime_on_report
            1    MyApp                              0x0000000100004a89 specialized UserViewModel.loadUser() + 123 (UserViewModel.swift:156)
            2    MyApp                              0x0000000100004567 UserViewModel.viewDidLoad() + 45 (UserViewModel.swift:89)
            3    MyApp                              0x0000000100003f12 @objc UserViewModel.viewDidLoad() + 28
            """.trimIndent(),

            // === Mixed/Edge Cases ===

            // Multiline exception message
            """
            java.lang.IllegalStateException: Expected BEGIN_OBJECT but was STRING at line 1 column 1 path $
                at com.google.gson.stream.JsonReader.beginObject(JsonReader.java:385)
                at com.google.gson.internal.bind.ReflectiveTypeAdapterFactory$1Adapter.read(ReflectiveTypeAdapterFactory.java:215)
                at com.google.gson.Gson.fromJson(Gson.java:927)
                at com.example.JsonParser.parse(JsonParser.java:45)
            """.trimIndent(),

            // C# with generic type parameters
            """
            System.InvalidOperationException: Sequence contains no elements
               at System.Linq.Enumerable.First[TSource](IEnumerable`1 source)
               at MyApp.Services.DataService`1.<GetFirstItem>b__12_0() in C:\src\DataService.cs:line 234
               at System.Lazy`1.CreateValue()
            """.trimIndent(),

            // === JSON API Responses with Stack Traces (Critical for RESTful API testing) ===

            // Node.js/Express error response with stack trace
            """
            {
              "error": true,
              "message": "Internal server error",
              "stack": "Error: Database connection failed\n    at Database.connect (/app/db.js:45:11)\n    at Server.start (/app/server.js:89:5)\n    at main (/app/index.js:12:3)"
            }
            """.trimIndent(),

            // Java Spring Boot error response
            """
            {
              "timestamp": "2024-01-15T14:30:00.000+00:00",
              "status": 500,
              "error": "Internal Server Error",
              "message": "Failed to process request",
              "trace": "java.lang.NullPointerException: Cannot invoke method on null object\n\tat com.example.UserService.getUser(UserService.java:45)\n\tat com.example.UserController.handleRequest(UserController.java:89)\n\tat java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)",
              "path": "/api/users/123"
            }
            """.trimIndent(),

            // Python Flask/Django error response
            """
            {
              "error": {
                "type": "ValueError",
                "message": "Invalid user ID",
                "traceback": "Traceback (most recent call last):\n  File \"/app/views.py\", line 45, in get_user\n    user = User.objects.get(id=user_id)\n  File \"/usr/lib/python3.9/site-packages/django/db/models/manager.py\", line 85, in get\n    return self.get_queryset().get(*args, **kwargs)\nValueError: Invalid user ID"
              }
            }
            """.trimIndent(),

            // ASP.NET Core error response
            """
            {
              "type": "https://tools.ietf.org/html/rfc7231#section-6.6.1",
              "title": "An error occurred while processing your request.",
              "status": 500,
              "traceId": "00-abc123-def456-01",
              "exception": "System.NullReferenceException: Object reference not set to an instance of an object.\n   at MyApp.Services.UserService.GetUser(Int32 id) in C:\\src\\UserService.cs:line 67\n   at MyApp.Controllers.UsersController.Get(Int32 id) in C:\\src\\UsersController.cs:line 34"
            }
            """.trimIndent(),

            // Ruby on Rails error response
            """
            {
              "status": "error",
              "message": "Record not found",
              "backtrace": [
                "/app/controllers/users_controller.rb:23:in `show'",
                "/usr/local/bundle/gems/actionpack-6.1.4/lib/action_controller/metal/basic_implicit_render.rb:6:in `send_action'",
                "/usr/local/bundle/gems/actionpack-6.1.4/lib/abstract_controller/base.rb:228:in `process_action'"
              ],
              "exception": "ActiveRecord::RecordNotFound"
            }
            """.trimIndent(),

            // Go/Gin error response with panic
            """
            {
              "error": "Internal Server Error",
              "message": "runtime error: invalid memory address or nil pointer dereference",
              "stack": "panic: runtime error: invalid memory address or nil pointer dereference\n\ngoroutine 45 [running]:\nmain.(*Handler).GetUser(0xc0001a2000)\n\t/app/handlers/user.go:89 +0x234\nmain.main()\n\t/app/main.go:45 +0x156"
            }
            """.trimIndent(),

            // GraphQL error with stack trace
            """
            {
              "errors": [
                {
                  "message": "Cannot return null for non-nullable field User.email",
                  "locations": [{"line": 3, "column": 5}],
                  "path": ["user", "email"],
                  "extensions": {
                    "code": "INTERNAL_SERVER_ERROR",
                    "stacktrace": [
                      "Error: Cannot return null for non-nullable field User.email",
                      "    at resolveField (/app/resolvers/user.js:45:11)",
                      "    at executeField (/app/node_modules/graphql/execution/execute.js:467:18)"
                    ]
                  }
                }
              ]
            }
            """.trimIndent(),

            // Nested JSON with multiple stack traces
            """
            {
              "success": false,
              "errors": [
                {
                  "service": "user-service",
                  "error": "NullPointerException",
                  "details": "java.lang.NullPointerException\n\tat com.example.UserService.process(UserService.java:123)\n\tat com.example.Controller.handle(Controller.java:45)"
                },
                {
                  "service": "auth-service",
                  "error": "TokenExpiredException",
                  "details": "TokenExpiredException: JWT token has expired\n\tat com.auth.TokenValidator.validate(TokenValidator.java:89)\n\tat com.auth.AuthFilter.doFilter(AuthFilter.java:34)"
                }
              ]
            }
            """.trimIndent(),

            // PHP Laravel error response
            """
            {
              "message": "Server Error",
              "exception": "Symfony\\\\Component\\\\HttpKernel\\\\Exception\\\\HttpException",
              "file": "/var/www/app/Http/Controllers/UserController.php",
              "line": 45,
              "trace": [
                {
                  "file": "/var/www/app/Http/Controllers/UserController.php",
                  "line": 45,
                  "function": "handleRequest",
                  "class": "App\\\\Http\\\\Controllers\\\\UserController",
                  "type": "->"
                },
                {
                  "file": "/var/www/vendor/laravel/framework/src/Illuminate/Routing/Controller.php",
                  "line": 54,
                  "function": "callAction",
                  "class": "Illuminate\\\\Routing\\\\Controller",
                  "type": "->"
                }
              ]
            }
            """.trimIndent(),

            // Microservices aggregated error with stack traces
            """
            {
              "request_id": "req-123-abc",
              "timestamp": "2024-01-15T14:30:00Z",
              "errors": {
                "database_error": {
                  "message": "Connection timeout",
                  "stack": "SQLException: Connection timeout\\n\\tat com.zaxxer.hikari.pool.HikariPool.getConnection(HikariPool.java:197)\\n\\tat org.example.DatabaseService.query(DatabaseService.java:67)"
                },
                "cache_error": {
                  "message": "Redis connection failed",
                  "stack": "redis.exceptions.ConnectionError: Error connecting to Redis\\n  File \\"/app/cache.py\\", line 23, in get\\n    return self.client.get(key)"
                }
              }
            }
            """.trimIndent(),

            // WebSocket error message with stack trace
            """
            {
              "type": "error",
              "event": "message_processing_failed",
              "error": {
                "name": "TypeError",
                "message": "Cannot read property 'id' of undefined",
                "stack": "TypeError: Cannot read property 'id' of undefined\\n    at MessageHandler.process (/app/handlers/message.js:78:23)\\n    at WebSocket.handleMessage (/app/websocket.js:45:15)\\n    at WebSocket.emit (events.js:315:20)"
              }
            }
            """.trimIndent(),

            // Rust Actix-web error response
            """
            {
              "error": "Internal Server Error",
              "message": "thread 'actix-rt:worker' panicked at 'called `Option::unwrap()` on a `None` value'",
              "backtrace": "thread 'actix-rt:worker' panicked at 'called `Option::unwrap()` on a `None` value', src/handlers/user.rs:45:37\\nstack backtrace:\\n   0: rust_begin_unwind\\n   1: core::panicking::panic_fmt\\n   2: myapp::handlers::user::get_user\\n         at ./src/handlers/user.rs:45"
            }
            """.trimIndent(),

            // Spring WebFlux reactive error response
            """
            {
              "timestamp": "2024-01-15T14:30:00.123Z",
              "path": "/api/users",
              "status": 500,
              "error": "Internal Server Error",
              "message": "Error processing request",
              "requestId": "abc-123",
              "exception": "reactor.core.Exceptions$1ReactiveException",
              "stackTrace": "reactor.core.Exceptions$1ReactiveException: java.lang.IllegalStateException: Invalid state\\n\\tat reactor.core.Exceptions.propagate(Exceptions.java:392)\\n\\tat com.example.UserService.lambda$1getUser$0(UserService.java:56)\\n\\tat reactor.core.publisher.MonoFlatMap$1FlatMapMain.onNext(MonoFlatMap.java:125)"
            }
            """.trimIndent(),

            // JSON array of error objects with stack traces
            """
            [
              {
                "error": "NullPointerException",
                "trace": "java.lang.NullPointerException\\n\\tat com.example.Service.method1(Service.java:10)"
              },
              {
                "error": "IOException",
                "trace": "java.io.IOException: File not found\\n\\tat com.example.FileHandler.read(FileHandler.java:45)"
              }
            ]
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
            "at java.lang.String.valueOf(String.java:123)",

            // === Additional false positives to avoid ===

            // Code review comments
            """
            Review feedback:
            - Fix the logic at UserService.java:45
            - Update documentation at README.md:123
            - Refactor method at Controller.kt:89
            """.trimIndent(),

            // Build/Compile success messages
            """
            javac compiled successfully
            Generated 15 class files in target/classes
            Build completed at 2024-01-15 14:30:00
            """.trimIndent(),

            """
            tsc --noEmit
            Found 0 errors. Watching for file changes.
            """.trimIndent(),

            // Linter output (no errors)
            """
            eslint src/
            ✓ 45 files checked
            0 errors, 0 warnings
            """.trimIndent(),

            """
            pylint myapp/
            Your code has been rated at 10.00/10
            """.trimIndent(),

            // Coverage reports
            """
            Test Coverage Report:
            File                    Statements   Missing   Coverage
            ----------------------------------------------------------
            src/main.py                    100         0      100%
            src/utils.py                    50         5       90%
            ----------------------------------------------------------
            TOTAL                          150         5       97%
            """.trimIndent(),

            // Profiler output
            """
            Profiling Results:
            Function                Time (ms)    Calls
            main.py:process_data         123     1000
            utils.py:format_data          45      500
            helpers.py:validate           12      250
            """.trimIndent(),

            // Docker/Container logs (successful)
            """
            [2024-01-15 14:30:00] Container started successfully
            [2024-01-15 14:30:01] Listening on port:8080
            [2024-01-15 14:30:02] Health check passed
            [2024-01-15 14:30:03] Ready to accept connections
            """.trimIndent(),

            // Kubernetes events
            """
            NAMESPACE   NAME                    READY   STATUS    RESTARTS   AGE
            default     myapp-7d9f8b5c4-abc12   1/1     Running   0          5m
            default     myapp-7d9f8b5c4-def34   1/1     Running   0          5m
            """.trimIndent(),

            // SQL query results (multiple rows)
            """
            postgres=# SELECT id, name, created_at FROM users LIMIT 3;
             id |   name   |       created_at
            ----+----------+------------------------
              1 | Alice    | 2024-01-15 10:00:00
              2 | Bob      | 2024-01-15 11:00:00
              3 | Charlie  | 2024-01-15 12:00:00
            (3 rows)
            """.trimIndent(),

            // HTTP access logs
            """
            127.0.0.1 - - [15/Jan/2024:14:30:00 +0000] "GET /api/users HTTP/1.1" 200 1234
            127.0.0.1 - - [15/Jan/2024:14:30:01 +0000] "POST /api/data HTTP/1.1" 201 567
            127.0.0.1 - - [15/Jan/2024:14:30:02 +0000] "GET /health HTTP/1.1" 200 89
            """.trimIndent(),

            // Application info logs
            """
            2024-01-15 14:30:00 INFO  Starting application v1.2.3
            2024-01-15 14:30:01 INFO  Loading configuration from config.yml
            2024-01-15 14:30:02 INFO  Database connection established
            2024-01-15 14:30:03 INFO  Server listening on port 8080
            """.trimIndent(),

            // Metrics/Monitoring output
            """
            Metrics Summary (last 5 minutes):
            - Request rate: 1234 req/min
            - Error rate: 0.01%
            - P95 latency: 123ms
            - P99 latency: 234ms
            """.trimIndent(),

            // Makefile output
            """
            make build
            gcc -o myapp main.c utils.c
            Compilation successful
            Binary created: ./myapp
            """.trimIndent(),

            // CMake output
            """
            -- The C compiler identification is GNU 9.4.0
            -- Detecting C compiler ABI info
            -- Detecting C compiler ABI info - done
            -- Configuring done
            -- Generating done
            -- Build files written to: /app/build
            """.trimIndent(),

            // Migration output (successful)
            """
            Running migrations...
            ✓ 001_create_users_table.sql
            ✓ 002_add_email_column.sql
            ✓ 003_create_indexes.sql
            All migrations completed successfully
            """.trimIndent(),

            // Backup logs
            """
            [2024-01-15 14:30:00] Starting database backup
            [2024-01-15 14:30:15] Backup completed: backup_20240115.sql
            [2024-01-15 14:30:16] Size: 1.2 GB
            [2024-01-15 14:30:16] Checksum: abc123def456
            """.trimIndent(),

            // Certificate information
            """
            Certificate:
                Subject: CN=example.com
                Issuer: CN=Let's Encrypt Authority X3
                Valid from: 2024-01-01 00:00:00
                Valid to: 2024-04-01 00:00:00
                Serial: 1234567890abcdef
            """.trimIndent(),

            // Environment variables
            """
            NODE_ENV=production
            PORT=8080
            DATABASE_URL=postgresql://localhost:5432/myapp
            LOG_LEVEL=info
            """.trimIndent(),

            // Dependency tree
            """
            myapp@1.0.0
            ├── express@4.18.2
            ├── body-parser@1.20.1
            ├── cors@2.8.5
            └── dotenv@16.0.3
            """.trimIndent(),

            // File checksums
            """
            MD5 checksums:
            abc123def456  myapp-v1.0.0.jar
            789ghi012jkl  config.properties
            345mno678pqr  application.yml
            """.trimIndent(),

            // Resource usage
            """
            PID   USER     %CPU  %MEM    VSZ   RSS  COMMAND
            1234  appuser   2.5  10.3  1234M  512M  java
            5678  appuser   0.8   3.2   456M  128M  node
            """.trimIndent(),

            // Cron job logs
            """
            [2024-01-15 14:00:00] Starting scheduled task: cleanup
            [2024-01-15 14:00:15] Removed 123 old files
            [2024-01-15 14:00:16] Task completed successfully
            [2024-01-15 14:00:16] Next run: 2024-01-15 15:00:00
            """.trimIndent(),

            // Swagger/OpenAPI spec snippet
            """
            openapi: 3.0.0
            info:
              title: My API
              version: 1.0.0
            paths:
              /users:
                get:
                  summary: Get all users
                  responses:
                    200:
                      description: Success
            """.trimIndent(),

            // Regular expression test output
            """
            Pattern: ^\w+@\w+\.\w+$
            Test cases:
            ✓ user@example.com
            ✓ test@domain.org
            ✗ invalid.email
            ✗ @missing.com
            """.trimIndent(),

            // File watcher output
            """
            Watching for file changes in src/
            [14:30:00] File changed: src/main.js
            [14:30:01] Recompiling...
            [14:30:02] Compilation successful
            [14:30:02] Server restarted
            """.trimIndent(),

            // GraphQL query
            """
            query GetUser {
              user(id: 123) {
                name
                email
                posts {
                  title
                }
              }
            }
            """.trimIndent(),

            // Changelog/Release notes
            """
            # Changelog

            ## [1.2.0] - 2024-01-15

            ### Added
            - New user authentication feature (src/auth.js:45)
            - Support for multiple languages

            ### Fixed
            - Bug in data processing (src/processor.py:123)
            """.trimIndent(),

            // Memory dump header (not actual stack trace)
            """
            Heap Dump Summary:
            Total memory: 2048 MB
            Used memory: 1234 MB
            Free memory: 814 MB
            Objects created: 567890
            GC collections: 123
            """.trimIndent(),

            // JMX/MBean output
            """
            MBean: java.lang:type=Memory
            HeapMemoryUsage:
              committed: 2147483648
              init: 268435456
              max: 4294967296
              used: 1234567890
            """.trimIndent(),

            // Feature flag configuration
            """
            Feature Flags:
            - enable_new_ui: true (since v1.2.0)
            - enable_beta_api: false
            - enable_analytics: true (rollout: 50%)
            """.trimIndent(),

            // Rate limit information
            """
            Rate Limit Status:
            Limit: 1000 requests per hour
            Remaining: 856
            Reset at: 2024-01-15 15:00:00
            """.trimIndent(),

            // Simple error message without stack trace
            "Error: Connection timeout",
            "Failed to load resource",
            "Warning: Deprecated API usage",

            // File paths in documentation
            """
            To configure the application:
            1. Edit config/settings.json:15
            2. Update src/constants.js:42
            3. Modify database/schema.sql:234
            """.trimIndent(),

            // Comparison output
            """
            Comparing files:
            File A: /path/to/file1.txt (12345 bytes)
            File B: /path/to/file2.txt (12340 bytes)
            Difference: 5 bytes
            Files are not identical
            """.trimIndent(),

            // === RESTful API / Web Content (Critical for API testing) ===

            // Successful JSON API response
            """
            {
              "status": "success",
              "code": 200,
              "data": {
                "users": [
                  {"id": 1, "name": "Alice"},
                  {"id": 2, "name": "Bob"}
                ]
              },
              "metadata": {
                "page": 1,
                "total": 2
              }
            }
            """.trimIndent(),

            // API error response (without stack trace)
            """
            {
              "error": {
                "code": "VALIDATION_ERROR",
                "message": "Invalid input parameters",
                "details": {
                  "field": "email",
                  "reason": "Invalid email format"
                }
              }
            }
            """.trimIndent(),

            // OpenAPI/Swagger response
            """
            {
              "swagger": "2.0",
              "info": {
                "title": "User API",
                "version": "1.0.0"
              },
              "paths": {
                "/users": {
                  "get": {
                    "summary": "Get users",
                    "responses": {
                      "200": {
                        "description": "Success"
                      }
                    }
                  }
                }
              }
            }
            """.trimIndent(),

            // GraphQL response (successful)
            """
            {
              "data": {
                "user": {
                  "id": "123",
                  "name": "John Doe",
                  "posts": [
                    {"title": "Hello World", "views": 100}
                  ]
                }
              }
            }
            """.trimIndent(),

            // GraphQL error (without stack trace)
            """
            {
              "errors": [
                {
                  "message": "User not found",
                  "locations": [{"line": 2, "column": 3}],
                  "path": ["user"]
                }
              ]
            }
            """.trimIndent(),

            // HTML page content
            """
            <!DOCTYPE html>
            <html>
            <head>
                <title>User Dashboard</title>
            </head>
            <body>
                <h1>Welcome</h1>
                <div class="content">
                    <p>View your profile at /users/profile.html:123</p>
                </div>
            </body>
            </html>
            """.trimIndent(),

            // XML API response
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <response>
              <status>success</status>
              <data>
                <users>
                  <user id="1">Alice</user>
                  <user id="2">Bob</user>
                </users>
              </data>
            </response>
            """.trimIndent(),

            // SOAP response
            """
            <?xml version="1.0"?>
            <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
              <soap:Body>
                <GetUserResponse>
                  <User>
                    <Id>123</Id>
                    <Name>John Doe</Name>
                  </User>
                </GetUserResponse>
              </soap:Body>
            </soap:Envelope>
            """.trimIndent(),

            // HTTP headers
            """
            HTTP/1.1 200 OK
            Content-Type: application/json
            Content-Length: 1234
            X-Request-Id: abc-123-def
            Date: Mon, 15 Jan 2024 14:30:00 GMT
            """.trimIndent(),

            // API rate limit response
            """
            {
              "error": "Rate limit exceeded",
              "limit": 1000,
              "remaining": 0,
              "reset": 1705329600,
              "retry_after": 3600
            }
            """.trimIndent(),

            // Validation error response
            """
            {
              "status": 400,
              "error": "Bad Request",
              "message": "Validation failed",
              "errors": [
                {
                  "field": "username",
                  "message": "Username is required",
                  "code": "required"
                },
                {
                  "field": "email",
                  "message": "Invalid email format",
                  "code": "format"
                }
              ]
            }
            """.trimIndent(),

            // JWT token payload
            """
            {
              "sub": "1234567890",
              "name": "John Doe",
              "iat": 1516239022,
              "exp": 1516242622,
              "roles": ["user", "admin"]
            }
            """.trimIndent(),

            // WebSocket message
            """
            {
              "type": "message",
              "channel": "notifications",
              "data": {
                "id": 123,
                "text": "New message received"
              },
              "timestamp": 1705329600000
            }
            """.trimIndent(),

            // Pagination metadata
            """
            {
              "data": [],
              "pagination": {
                "page": 1,
                "per_page": 20,
                "total": 100,
                "total_pages": 5,
                "links": {
                  "first": "/api/users?page=1",
                  "last": "/api/users?page=5",
                  "next": "/api/users?page=2"
                }
              }
            }
            """.trimIndent(),

            // API health check response
            """
            {
              "status": "healthy",
              "version": "1.2.3",
              "uptime": 345600,
              "services": {
                "database": "ok",
                "cache": "ok",
                "queue": "ok"
              }
            }
            """.trimIndent(),

            // Search results
            """
            {
              "query": "javascript",
              "results": [
                {
                  "title": "JavaScript Tutorial",
                  "url": "https://example.com/tutorial.js:100",
                  "score": 0.95
                }
              ],
              "total": 1234
            }
            """.trimIndent(),

            // CSV API response
            """
            id,name,email,created_at
            1,Alice,alice@example.com,2024-01-15T10:00:00Z
            2,Bob,bob@example.com,2024-01-15T11:00:00Z
            3,Charlie,charlie@example.com,2024-01-15T12:00:00Z
            """.trimIndent(),

            // Metrics/Prometheus format
            """
            # HELP http_requests_total Total HTTP requests
            # TYPE http_requests_total counter
            http_requests_total{method="GET",status="200"} 1234
            http_requests_total{method="POST",status="201"} 567
            """.trimIndent(),

            // API documentation snippet
            """
            GET /api/users/{id}

            Description: Retrieve a user by ID

            Parameters:
              - id (path, required): User ID

            Responses:
              200: Success
              404: User not found
              500: Internal server error
            """.trimIndent(),

            // OAuth response
            """
            {
              "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
              "token_type": "Bearer",
              "expires_in": 3600,
              "refresh_token": "def50200a1b2c3d4...",
              "scope": "read write"
            }
            """.trimIndent(),

            // File upload response
            """
            {
              "success": true,
              "file": {
                "id": "file_123",
                "name": "document.pdf",
                "size": 1048576,
                "url": "https://cdn.example.com/files/document.pdf",
                "mime_type": "application/pdf"
              }
            }
            """.trimIndent(),

            // Webhook payload
            """
            {
              "event": "user.created",
              "timestamp": "2024-01-15T14:30:00Z",
              "data": {
                "user_id": 123,
                "username": "newuser",
                "email": "newuser@example.com"
              },
              "signature": "sha256=abc123..."
            }
            """.trimIndent()
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

    @Test
    fun `JSON with escaped newlines should be detected`() {
        // This is how it comes from a real API - escaped newlines in the JSON string value
        val json = """{"error": "NullPointerException", "stack": "java.lang.NullPointerException\n\tat com.example.Service.method(Service.java:10)\n\tat com.example.Main.main(Main.java:5)"}"""
        assertTrue(StackTraceUtils.looksLikeStackTrace(json), "Should detect stack trace with escaped newlines")
    }

    @Test
    fun `JSON with double-escaped newlines should be detected`() {
        // Some frameworks double-escape: \\n in the raw JSON becomes \n after one parse
        val json = """{"stack": "Error: test\\n\\tat Line1.js:10\\n\\tat Line2.js:20"}"""
        assertTrue(StackTraceUtils.looksLikeStackTrace(json), "Should detect double-escaped stack trace")
    }

    @Test
    fun `direct stack trace without JSON should work`() {
        // Control test - direct stack trace should always work
        val stackTrace = """
            java.lang.NullPointerException: test
                at com.example.Service.method(Service.java:10)
                at com.example.Main.main(Main.java:5)
        """.trimIndent()
        assertTrue(StackTraceUtils.looksLikeStackTrace(stackTrace), "Should detect direct stack trace")
    }
}
