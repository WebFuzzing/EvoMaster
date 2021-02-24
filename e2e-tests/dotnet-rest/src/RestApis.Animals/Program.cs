using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using Microsoft.AspNetCore;
using Microsoft.AspNetCore.Hosting;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.Logging;

namespace RestApis.Animals
{
    public class Program
    {
        private static readonly ConcurrentDictionary<int, CancellationTokenSource> Tokens = new ConcurrentDictionary<int, CancellationTokenSource> ();
        public static void Main (string[] args) {

            if (args.Length > 0) {

                var port = Convert.ToInt32 (args[0]);
                Tokens.TryAdd (port, new CancellationTokenSource ());
                var host = CreateWebHostBuilder (args).Build ();
                host.RunAsync (Tokens[port].Token).GetAwaiter ().GetResult ();
            } else {
                var host = CreateWebHostBuilder (args).Build ();
                host.RunAsync ().GetAwaiter ().GetResult ();
            }
        }

        private static IWebHostBuilder CreateWebHostBuilder (string[] args)
        {
            var webHostBuilder = WebHost.CreateDefaultBuilder (args)
                .UseStartup<Startup> ();

            return args.Length switch
            {
                0 => webHostBuilder,
                1 => webHostBuilder.UseUrls($"http://*:{args[0]}"),
                _ => webHostBuilder.UseUrls($"http://*:{args[0]}").UseSetting("ConnectionString", args[1])
            };
        }

        public static void Shutdown () {

            foreach (var pair in Tokens) {
                pair.Value.Cancel ();
            }

            Tokens.Clear ();
        }
    }
}
