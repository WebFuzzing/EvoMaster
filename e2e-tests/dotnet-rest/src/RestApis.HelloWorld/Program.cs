using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using Microsoft.AspNetCore;
using Microsoft.AspNetCore.Hosting;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.Logging;

namespace RestApis.HelloWorld {
    public class Program {
        private static CancellationTokenSource cancelTokenSource = new System.Threading.CancellationTokenSource ();

        public static void Main (string[] args) {
            // var host = CreateWebHostBuilder (args).Build ();
            // host.RunAsync (cancelTokenSource.Token).GetAwaiter ().GetResult ();

            CreateHostBuilderOld(args).Build().Run();
        }

        public static IWebHostBuilder CreateWebHostBuilder (string[] args) =>
            WebHost.CreateDefaultBuilder (args)
            .UseStartup<Startup> ();

        public static IHostBuilder CreateHostBuilderOld (string[] args) =>
            Host.CreateDefaultBuilder (args)
            .ConfigureWebHostDefaults (webBuilder => {
                webBuilder.UseStartup<Startup> ();
            });

        public static void Shutdown () {
            cancelTokenSource.Cancel ();
        }
    }
}