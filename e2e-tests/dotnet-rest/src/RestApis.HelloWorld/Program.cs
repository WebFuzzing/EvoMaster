using System;
using System.Collections.Concurrent;
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

        private static ConcurrentDictionary<int, CancellationTokenSource> tokens = new ConcurrentDictionary<int, CancellationTokenSource> ();

        public static void Main (string[] args) {

            int port = Convert.ToInt32(args[0]);

            tokens.TryAdd (port, new CancellationTokenSource ());

            var host = CreateWebHostBuilder (args).Build ();

            host.RunAsync (tokens[port].Token).GetAwaiter ().GetResult ();
        }

        public static IWebHostBuilder CreateWebHostBuilder (string[] args) =>
            WebHost.CreateDefaultBuilder (args)
            .UseStartup<Startup> ().UseUrls ($"http://*:{args[0]}");

        public static void Shutdown (int port) {

            tokens.Remove (port, out var tokenSource);

            if (tokenSource != null)
                tokenSource.Cancel ();
            else
                //TODO: throw exception
                System.Console.WriteLine ($"No cancellation token source found for port {port}");
        }

        public static void Shutdown () {

            foreach (var pair in tokens)
            {
                pair.Value.Cancel();
            }

            tokens.Clear();
        }
    }
}